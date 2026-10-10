package com.brainbucket.post;

import com.brainbucket.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "approval_logs")
public class ApprovalLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reviewer_id", nullable = false)
  private User reviewer;

  @Column(nullable = false, length = 20)
  private String decision;

  @Column(columnDefinition = "TEXT")
  private String reason;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(columnDefinition = "TEXT")
  private String snapshot;

  @Column(name = "poster_key", length = 512)
  private String posterKey;

  @Column(name = "evidence_key", length = 512)
  private String evidenceKey;

  @Column(name = "original_key", length = 512)
  private String originalKey;

  private String posterType;
  private String evidenceType;
  private String originalType;

  protected ApprovalLog() {}

  public ApprovalLog(Post post, User reviewer, String decision, String reason) {
    this.post = post;
    this.reviewer = reviewer;
    this.decision = decision;
    this.reason = reason;
    this.snapshot = post.snapshot();
  }

  @PrePersist
  private void beforeInsert() {
    this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
  }

  public Long getId() {
    return id;
  }

  public Post getPost() {
    return post;
  }

  public User getReviewer() {
    return reviewer;
  }

  public String getDecision() {
    return decision;
  }

  public String getReason() {
    return reason;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void capture(java.util.List<PostFile> files) {
    for (PostFile f : files)
      switch (f.getFileKind()) {
        case "POSTER" -> {
          posterKey = f.getStorageKey();
          posterType = f.getContentType();
        }
        case "EVIDENCE" -> {
          evidenceKey = f.getStorageKey();
          evidenceType = f.getContentType();
        }
        case "ORIGINAL" -> {
          originalKey = f.getStorageKey();
          originalType = f.getContentType();
        }
      }
  }

  public void appendScheduleAfter(Post p) {
    snapshot += "\n\n[변경 후]\n" + p.snapshot();
  }

  public String getSnapshot() {
    return snapshot;
  }

  public String key(String kind) {
    return switch (kind) {
      case "POSTER" -> posterKey;
      case "EVIDENCE" -> evidenceKey;
      case "ORIGINAL" -> originalKey;
      default -> null;
    };
  }

  public String type(String kind) {
    return switch (kind) {
      case "POSTER" -> posterType;
      case "EVIDENCE" -> evidenceType;
      case "ORIGINAL" -> originalType;
      default -> null;
    };
  }
}
