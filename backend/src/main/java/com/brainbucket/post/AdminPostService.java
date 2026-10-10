package com.brainbucket.post;

import com.brainbucket.user.User;
import com.brainbucket.user.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminPostService {

  private final UserRepository userRepository;
  private final PostRepository postRepository;
  private final ApprovalLogRepository approvalLogRepository;
  private final PostFileRepository postFileRepository;
  private final PosterStorage posterStorage;

  public AdminPostService(
      UserRepository userRepository,
      PostRepository postRepository,
      ApprovalLogRepository approvalLogRepository,
      PostFileRepository postFileRepository,
      PosterStorage posterStorage) {
    this.userRepository = userRepository;
    this.postRepository = postRepository;
    this.approvalLogRepository = approvalLogRepository;
    this.postFileRepository = postFileRepository;
    this.posterStorage = posterStorage;
  }

  @Transactional
  public CreatePostResponse approve(String email, Long postId) {
    User reviewer = requireAdmin(email);
    Post post = requirePendingPost(postId);

    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    if (post.getDisplayUntil() == null || post.getDisplayUntil().isBefore(today)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "게시 종료일이 없거나 이미 지나 승인할 수 없습니다.");
    }

    PostFile poster =
        postFileRepository
            .findByPost_IdAndFileKind(postId, "POSTER")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "전단지 이미지가 없습니다."));

    posterStorage.loadStored(poster.getStorageKey());

    PostRules.window(post.getDisplayFrom(), post.getDisplayUntil());
    var evidence =
        postFileRepository
            .findByPost_IdAndFileKind(postId, "EVIDENCE")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "승인 증빙이 없습니다."));
    posterStorage.loadStored(evidence.getStorageKey());
    post.approve();

    approvalLogRepository.save(snapshotLog(post, reviewer, "APPROVED", null));

    postRepository.saveAndFlush(post);

    return response(post);
  }

  @Transactional
  public CreatePostResponse reject(String email, Long postId, String reason) {
    User reviewer = requireAdmin(email);

    if (reason == null || reason.isBlank() || reason.length() > 2000) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "반려 사유는 비어 있지 않아야 하며 2000자 이하여야 합니다.");
    }

    Post post = requirePendingPost(postId);

    post.reject();

    approvalLogRepository.save(snapshotLog(post, reviewer, "REJECTED", reason.strip()));

    postRepository.saveAndFlush(post);

    return response(post);
  }

  private User requireAdmin(String email) {
    User reviewer =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

    if (!"ADMIN".equals(reviewer.getRole())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    return reviewer;
  }

  private Post requirePendingPost(Long postId) {
    Post post =
        postRepository
            .findForReview(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (post.getStatus() != PostStatus.PENDING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "승인 대기 중인 게시물만 처리할 수 있습니다.");
    }

    return post;
  }

  private CreatePostResponse response(Post post) {
    return new CreatePostResponse(post.getId(), post.getTitle(), post.getStatus());
  }

  private ApprovalLog snapshotLog(Post post, User reviewer, String decision, String reason) {
    ApprovalLog log = new ApprovalLog(post, reviewer, decision, reason);
    log.capture(postFileRepository.findByPost_Id(post.getId()));
    return log;
  }
}
