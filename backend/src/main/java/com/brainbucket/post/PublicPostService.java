package com.brainbucket.post;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class PublicPostService {

    private final PostRepository postRepository;
    private final PostFileRepository postFileRepository;
    private final PosterStorage posterStorage;

    public PublicPostService(
            PostRepository postRepository,
            PostFileRepository postFileRepository,
            PosterStorage posterStorage
    ) {
        this.postRepository = postRepository;
        this.postFileRepository = postFileRepository;
        this.posterStorage = posterStorage;
    }

    @Transactional(readOnly = true)
    public PublicPostDetailResponse getDetail(Long postId) {
        Post post = postRepository
                .findByIdAndStatusAndDeletedAtIsNull(
                        postId,
                        PostStatus.APPROVED
                )
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND)
                );

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        boolean archived = post.getDisplayUntil() != null
                && post.getDisplayUntil().isBefore(today);

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
                post.getPublishedAt(),
                archived,
                "/api/posts/public/" + post.getId() + "/poster"
        );
    }
    @Transactional(readOnly = true)
    public PublicPosterDownload getPoster(Long postId) {
        postRepository
                .findByIdAndStatusAndDeletedAtIsNull(
                        postId,
                        PostStatus.APPROVED
                )
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND)
                );

        PostFile file = postFileRepository
                .findByPost_IdAndFileKind(postId, "POSTER")
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND)
                );

        Resource resource = new FileSystemResource(
                posterStorage.loadStored(file.getStorageKey())
        );

        return new PublicPosterDownload(
                resource,
                file.getContentType()
        );
    }

    public record PublicPosterDownload(
            Resource resource,
            String contentType
    ) {
    }
    @Transactional(readOnly = true)
    public List<PublicPostResponse> getPosts(
            boolean archived,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "page는 0 이상, size는 1~50이어야 합니다."
            );
        }

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("publishedAt"),
                        Sort.Order.desc("id")
                )
        );

        var posts = archived
                ? postRepository
                .findByStatusAndDeletedAtIsNullAndDisplayUntilLessThan(
                        PostStatus.APPROVED,
                        today,
                        pageable
                )
                : postRepository
                .findByStatusAndDeletedAtIsNullAndDisplayUntilGreaterThanEqual(
                        PostStatus.APPROVED,
                        today,
                        pageable
                );

        return posts.getContent()
                .stream()
                .map(post -> new PublicPostResponse(
                        post.getId(),
                        post.getTitle(),
                        post.getOrganizer(),
                        post.getCategory(),
                        post.getDisplayUntil(),
                        "/api/posts/public/" + post.getId() + "/poster"
                ))
                .toList();
    }
}