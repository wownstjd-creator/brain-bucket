package com.brainbucket.post;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublicPostService {

  private final PostRepository postRepository;
  private final PostFileRepository postFileRepository;
  private final PosterStorage posterStorage;

  public PublicPostService(
      PostRepository postRepository,
      PostFileRepository postFileRepository,
      PosterStorage posterStorage) {
    this.postRepository = postRepository;
    this.postFileRepository = postFileRepository;
    this.posterStorage = posterStorage;
  }

  @Transactional(readOnly = true)
  public PublicPostDetailResponse getDetail(Long postId) {
    Post post =
        postRepository
            .findByIdAndStatusAndDeletedAtIsNull(postId, PostStatus.APPROVED)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    if (!post.isPublic(today)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    boolean archived = post.getDisplayUntil() != null && post.getDisplayUntil().isBefore(today);

    return new PublicPostDetailResponse(
        post.getId(),
        post.getTitle(),
        post.getOrganizer(),
        post.getCategory(),
        post.getDescription(),
        post.getLocation(),
        post.getApplicationUrl(),
        post.getEventAt(),
        post.getDeadlineAt(),
        post.getDisplayUntil(),
        post.getDisplayFrom(),
        post.getVersion(),
        post.getPublishedAt(),
        archived,
        "/api/posts/public/" + post.getId() + "/poster",
        post.isShowViewCount() ? post.getViewCount() : null);
  }

  @Transactional(readOnly = true)
  public PublicPosterDownload getPoster(Long postId) {
    Post publicPost =
        postRepository
            .findByIdAndStatusAndDeletedAtIsNull(postId, PostStatus.APPROVED)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!publicPost.isPublic(LocalDate.now(ZoneId.of("Asia/Seoul"))))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    PostFile file =
        postFileRepository
            .findByPost_IdAndFileKind(postId, "POSTER")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    Resource resource = new FileSystemResource(posterStorage.loadStored(file.getStorageKey()));

    return new PublicPosterDownload(resource, file.getContentType());
  }

  public record PublicPosterDownload(Resource resource, String contentType) {}

  @Transactional(readOnly = true)
  public List<PublicPostResponse> getPosts(
      boolean archived, int page, int size, String search, String category, Integer year) {
    if (page < 0 || size < 1 || size > 50) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page는 0 이상, size는 1~50이어야 합니다.");
    }

    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    var pageable =
        PageRequest.of(page, size, Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id")));

    if (search != null && search.length() > 200)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    if (category != null
        && !java.util.Set.of("CLUB", "CONTEST", "EVENT", "NOTICE", "OTHER").contains(category))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    if (year != null && (year < 1900 || year > 9998))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    var posts =
        postRepository.findAll(
            (r, q, b) ->
                b.and(
                    b.equal(r.get("status"), PostStatus.APPROVED),
                    b.isNull(r.get("deletedAt")),
                    b.or(
                        b.isNull(r.get("displayFrom")),
                        b.lessThanOrEqualTo(r.get("displayFrom"), today)),
                    archived
                        ? b.lessThan(r.get("displayUntil"), today)
                        : b.greaterThanOrEqualTo(r.get("displayUntil"), today),
                    category == null ? b.conjunction() : b.equal(r.get("category"), category),
                    year == null
                        ? b.conjunction()
                        : b.and(
                            b.greaterThanOrEqualTo(r.get("displayUntil"), LocalDate.of(year, 1, 1)),
                            b.lessThan(r.get("displayUntil"), LocalDate.of(year + 1, 1, 1))),
                    search == null || search.isBlank()
                        ? b.conjunction()
                        : b.or(
                            b.like(
                                b.lower(r.get("title")),
                                "%"
                                    + escape(search.strip().toLowerCase(java.util.Locale.ROOT))
                                    + "%",
                                '!'),
                            b.like(
                                b.lower(r.get("organizer")),
                                "%"
                                    + escape(search.strip().toLowerCase(java.util.Locale.ROOT))
                                    + "%",
                                '!'),
                            b.like(
                                b.lower(r.get("description")),
                                "%"
                                    + escape(search.strip().toLowerCase(java.util.Locale.ROOT))
                                    + "%",
                                '!'))),
            pageable);

    return posts.getContent().stream().map(PublicPostResponse::from).toList();
  }

  @Transactional(readOnly=true)
  public List<Integer> archiveYears(){return postRepository.archiveYears(LocalDate.now(ZoneId.of("Asia/Seoul")));}

  private String escape(String s) {
    return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
  }
}
