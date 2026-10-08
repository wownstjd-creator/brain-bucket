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

    protected ApprovalLog() {
    }

    public ApprovalLog(
            Post post,
            User reviewer,
            String decision,
            String reason
    ) {
        this.post = post;
        this.reviewer = reviewer;
        this.decision = decision;
        this.reason = reason;
    }

    @PrePersist
    private void beforeInsert() {
        this.createdAt = LocalDateTime.now(
                ZoneId.of("Asia/Seoul")
        );
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
}