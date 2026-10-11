package com.brainbucket.post;

import com.brainbucket.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PostLifecycleService {
  private final PostRepository posts;
  private final UserRepository users;
  private final PostFileRepository files;
  private final ApprovalLogRepository logs;

  public PostLifecycleService(PostRepository posts, UserRepository users,
      PostFileRepository files, ApprovalLogRepository logs) {
    this.posts=posts; this.users=users; this.files=files; this.logs=logs;
  }

  public void change(String email, Long id, Long version, boolean delete) {
    var owner=users.findByEmail(email)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    var post=posts.findForReview(id)
        .filter(p -> p.getAuthor().getId().equals(owner.getId()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    PostRules.version(post, version);
    if (!delete && post.getStatus()!=PostStatus.PENDING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "승인 대기 중인 게시물만 요청을 취소할 수 있습니다.");
    }
    var log=new ApprovalLog(post, owner, delete ? "DELETED" : "REQUEST_CANCELLED",
        delete ? "작성자가 게시물을 삭제했습니다." : "작성자가 승인 요청을 취소했습니다.");
    log.capture(files.findByPost_Id(id));
    logs.save(log);
    if (delete) post.deleteByAuthor(); else post.cancelApproval();
    posts.flush();
  }
}
