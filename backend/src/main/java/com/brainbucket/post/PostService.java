package com.brainbucket.post;

import com.brainbucket.user.User;
import com.brainbucket.user.UserRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostService {

  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final PostFileRepository postFileRepository;

  public PostService(
      PostRepository postRepository,
      UserRepository userRepository,
      PostFileRepository postFileRepository) {
    this.postRepository = postRepository;
    this.userRepository = userRepository;
    this.postFileRepository = postFileRepository;
  }

  @Transactional
  public CreatePostResponse createDraft(String email, CreatePostRequest request) {
    User author =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

    PostRules.draft(request);
    boolean showViewCount = request.showViewCount() == null || request.showViewCount();

    Post post =
        new Post(
            author,
            request.title(),
            request.organizer(),
            request.category(),
            request.description(),
            request.location(),
            request.applicationUrl(),
            request.eventAt(),
            request.deadlineAt(),
            request.displayUntil(),
            showViewCount);

    post.setDisplayFrom(request.displayFrom());
    Post savedPost = postRepository.save(post);

    return new CreatePostResponse(savedPost.getId(), savedPost.getTitle(), savedPost.getStatus());
  }

  @Transactional(readOnly = true)
  public List<MyPostResponse> getMyPosts(String email, int page, int size) {
    if (page < 0 || size < 1 || size > 50) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page는 0 이상, size는 1~50이어야 합니다.");
    }

    User author =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

    var pageable =
        PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

    return postRepository
        .findByAuthor_IdAndDeletedAtIsNull(author.getId(), pageable)
        .map(
            post ->
                new MyPostResponse(
                    post.getId(),
                    post.getTitle(),
                    post.getOrganizer(),
                    post.getCategory(),
                    post.getStatus(),
                    post.getDeadlineAt(),
                    post.getCreatedAt()))
        .getContent();
  }

  @Transactional(readOnly=true)
  public List<MyPostDetailResponse> getMyCards(String email,int page,int size){
    if(page<0||size<1||size>50)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    var user=userRepository.findByEmail(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    return PostCards.from(postRepository.findByAuthor_IdAndDeletedAtIsNull(user.getId(),
      PageRequest.of(page,size,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")))).getContent(),postFileRepository);
  }

  @Transactional(readOnly = true)
  public MyPostDetailResponse getMyPost(String email, Long postId) {

    User author =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

    Post post =
        postRepository
            .findByIdAndAuthor_IdAndDeletedAtIsNull(postId, author.getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시물을 찾을 수 없습니다."));

    return new MyPostDetailResponse(
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
        postFileRepository.existsByPost_IdAndFileKind(post.getId(), "EVIDENCE"),
        postFileRepository.existsByPost_IdAndFileKind(post.getId(), "ORIGINAL"),
        post.getStatus(),
        post.isShowViewCount(),
        post.getViewCount(),
        post.getPublishedAt(),
        post.getCreatedAt(),
        post.getUpdatedAt());
  }

  @Transactional
  public CreatePostResponse updateMyDraft(String email, Long postId, CreatePostRequest request) {
    User author =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

    Post post =
        postRepository
            .findForReview(postId)
            .filter(p -> p.getAuthor().getId().equals(author.getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    PostRules.draft(request);
    PostRules.version(post, request.version());
    if (post.getStatus() != PostStatus.DRAFT
        && post.getStatus() != PostStatus.REJECTED
        && post.getStatus() != PostStatus.APPROVED) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "현재 상태에서는 임시 게시물을 수정할 수 없습니다.");
    }

    post.updateDraft(request);
    postRepository.saveAndFlush(post);

    return new CreatePostResponse(post.getId(), post.getTitle(), post.getStatus());
  }
}
