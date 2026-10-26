package com.brainbucket.post;

import com.brainbucket.user.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostApprovalService {

  private static final Set<String> CATEGORIES =
      Set.of("CLUB", "CONTEST", "EVENT", "NOTICE", "OTHER");

  private final UserRepository userRepository;
  private final PostRepository postRepository;
  private final PostFileRepository postFileRepository;
  private final PosterStorage posterStorage;
  private final ApprovalLogRepository approvalLogRepository;

  public PostApprovalService(
      UserRepository userRepository,
      PostRepository postRepository,
      PostFileRepository postFileRepository,
      PosterStorage posterStorage,
      ApprovalLogRepository approvalLogRepository) {
    this.userRepository = userRepository;
    this.postRepository = postRepository;
    this.postFileRepository = postFileRepository;
    this.posterStorage = posterStorage;
    this.approvalLogRepository = approvalLogRepository;
  }

  @Transactional
  public CreatePostResponse requestApproval(String email, Long postId) {
    return requestApproval(email, postId, null);
  }

  @Transactional
  public CreatePostResponse requestApproval(String email, Long postId, Long version) {
    var author =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

    Post post =
        postRepository
            .findForReview(postId)
            .filter(value -> value.getAuthor().getId().equals(author.getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    PostRules.version(post, version);

    if (post.getStatus() != PostStatus.DRAFT && post.getStatus() != PostStatus.REJECTED) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "작성 중이거나 반려된 게시물만 승인 요청할 수 있습니다.");
    }

    if (post.getTitle() == null || post.getTitle().isBlank()) {
      throw badRequest("제목을 입력해주세요.");
    }

    if (post.getOrganizer() == null || post.getOrganizer().isBlank()) {
      throw badRequest("주최자를 입력해주세요.");
    }

    if (post.getCategory() == null || !CATEGORIES.contains(post.getCategory())) {
      throw badRequest("분류는 CLUB, CONTEST, EVENT, NOTICE, OTHER 중 하나여야 합니다.");
    }

    if (post.getDisplayUntil() == null) {
      throw badRequest("게시 종료일을 입력해주세요.");
    }

    PostRules.window(post.getDisplayFrom(), post.getDisplayUntil());
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    if (post.getDisplayUntil().isBefore(today)) {
      throw badRequest("게시 종료일은 오늘 이후 날짜여야 합니다.");
    }

    PostFile poster =
        postFileRepository
            .findByPost_IdAndFileKind(postId, "POSTER")
            .orElseThrow(() -> badRequest("전단지 이미지를 등록해주세요."));

    // DB 기록뿐 아니라 실제 저장 파일도 있는지 확인
    posterStorage.loadStored(poster.getStorageKey());

    PostFile evidence =
        postFileRepository
            .findByPost_IdAndFileKind(postId, "EVIDENCE")
            .orElseThrow(() -> badRequest("승인 증빙 이미지를 등록해주세요."));
    posterStorage.loadStored(evidence.getStorageKey());
    post.requestApproval();
    postRepository.saveAndFlush(post);

    return new CreatePostResponse(post.getId(), post.getTitle(), post.getStatus());
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  @Transactional(readOnly = true)
  public List<ApprovalLogResponse> getMyApprovalLogs(String email, Long postId) {
    var author =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

    postRepository
        .findByIdAndAuthor_IdAndDeletedAtIsNull(postId, author.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    return approvalLogRepository.findByPost_IdOrderByCreatedAtDescIdDesc(postId).stream()
        .map(
            log ->
                new ApprovalLogResponse(
                    log.getId(), log.getDecision(), log.getReason(), log.getCreatedAt()))
        .toList();
  }
}
