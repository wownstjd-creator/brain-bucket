package com.brainbucket.post;

import com.brainbucket.user.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ManagementService {
  private final PostRepository posts;
  private final UserRepository users;
  private final PostFileRepository files;
  private final ApprovalLogRepository logs;

  public ManagementService(
      PostRepository p, UserRepository u, PostFileRepository f, ApprovalLogRepository l) {
    posts = p;
    users = u;
    files = f;
    logs = l;
  }

  private User admin(String email) {
    var u =
        users
            .findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    if (!"ADMIN".equals(u.getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return u;
  }

  private Post locked(Long id) {
    return posts
        .findForReview(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private String reason(String r) {
    if (r == null || r.isBlank() || r.length() > 2000)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사유는 1~2000자여야 합니다.");
    return r.strip();
  }

  private ApprovalLog record(Post p, User u, String decision, String r) {
    var l = new ApprovalLog(p, u, decision, reason(r));
    l.capture(files.findByPost_Id(p.getId()));
    return l;
  }

  public CreatePostResponse visibility(String email, Long id, boolean hide, ChangeRequest r) {
    var u = admin(email);
    var p = locked(id);
    PostRules.version(p, r.version());
    if (p.getStatus() != (hide ? PostStatus.APPROVED : PostStatus.HIDDEN))
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    var l = record(p, u, hide ? "HIDDEN" : "RESTORED", r.reason());
    if (hide) p.hide();
    else p.restore();
    logs.save(l);
    posts.flush();
    return new CreatePostResponse(p.getId(), p.getTitle(), p.getStatus());
  }

  public CreatePostResponse schedule(String email, Long id, ScheduleRequest r) {
    var u = admin(email);
    var p = locked(id);
    PostRules.version(p, r.version());
    PostRules.window(r.displayFrom(), r.displayUntil());
    if (p.getStatus() == PostStatus.DRAFT) throw new ResponseStatusException(HttpStatus.CONFLICT);
    var l = record(p, u, "SCHEDULE_CHANGED", r.reason());
    p.changeSchedule(r);
    l.appendScheduleAfter(p);
    logs.save(l);
    posts.flush();
    return new CreatePostResponse(p.getId(), p.getTitle(), p.getStatus());
  }

  @Transactional(readOnly = true)
  public List<ManagedPost> list(String email, String status, int page, int size) {
    admin(email);
    if (page < 0
        || size < 1
        || size > 50
        || !Set.of("ALL", "PENDING", "APPROVED", "HIDDEN", "REJECTED").contains(status))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    return posts
        .findAll(
            (r, q, b) ->
                b.and(
                    b.isNull(r.get("deletedAt")),
                    status.equals("ALL")
                        ? b.notEqual(r.get("status"), PostStatus.DRAFT)
                        : b.equal(r.get("status"), PostStatus.valueOf(status))),
            PageRequest.of(page, size, Sort.by("id").descending()))
        .map(p -> new ManagedPost(p.getId(), p.getTitle(), p.getStatus(), p.getVersion()))
        .getContent();
  }

  public record ManagedPost(Long id, String title, PostStatus status, long version) {}

  public record ChangeRequest(String reason, Long version) {}
}
