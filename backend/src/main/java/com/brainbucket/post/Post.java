package com.brainbucket.post;

import com.brainbucket.user.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "posts")
public class Post {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false)
  private User author;

  @Column(length = 150)
  private String title;

  @Column(length = 100)
  private String organizer;

  @Column(length = 30)
  private String category;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(length = 150)
  private String location;

  @Column(name = "application_url", length = 2048)
  private String applicationUrl;

  @Column(name = "event_at")
  private LocalDateTime eventAt;

  @Column(name = "deadline_at")
  private LocalDateTime deadlineAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PostStatus status;

  @Column(name = "show_view_count", nullable = false)
  private boolean showViewCount;

  @Column(name = "view_count", nullable = false)
  private long viewCount;

  @Column(name = "published_at")
  private LocalDateTime publishedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "display_until")
  private LocalDate displayUntil;

  @Column(name = "display_from")
  private LocalDate displayFrom;

  @Version private long version;

  protected Post() {}

  public Post(
      User author,
      String title,
      String organizer,
      String category,
      String description,
      String location,
      String applicationUrl,
      LocalDateTime eventAt,
      LocalDateTime deadlineAt,
      LocalDate displayUntil,
      boolean showViewCount) {
    this.author = author;
    this.title = title;
    this.organizer = organizer;
    this.category = category;
    this.description = description;
    this.location = location;
    this.applicationUrl = applicationUrl;
    this.eventAt = eventAt;
    this.deadlineAt = deadlineAt;
    this.displayUntil = displayUntil;
    this.showViewCount = showViewCount;
    this.status = PostStatus.DRAFT;
    this.viewCount = 0;
  }

  @PrePersist
  private void beforeInsert() {
    LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  private void beforeUpdate() {
    this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
  }

  public void updateDraft(CreatePostRequest request) {
    this.title = request.title();
    this.organizer = request.organizer();
    this.category = request.category();
    this.description = request.description();
    this.location = request.location();
    this.applicationUrl = request.applicationUrl();
    this.eventAt = request.eventAt();
    this.deadlineAt = request.deadlineAt();
    this.displayUntil = request.displayUntil();
    this.displayFrom = request.displayFrom();
    this.showViewCount = request.showViewCount() == null || request.showViewCount();
    this.status = PostStatus.DRAFT;
  }

  public Long getId() {
    return id;
  }

  public User getAuthor() {
    return author;
  }

  public String getTitle() {
    return title;
  }

  public String getOrganizer() {
    return organizer;
  }

  public String getCategory() {
    return category;
  }

  public String getDescription() {
    return description;
  }

  public String getLocation() {
    return location;
  }

  public String getApplicationUrl() {
    return applicationUrl;
  }

  public LocalDateTime getEventAt() {
    return eventAt;
  }

  public LocalDateTime getDeadlineAt() {
    return deadlineAt;
  }

  public PostStatus getStatus() {
    return status;
  }

  public boolean isShowViewCount() {
    return showViewCount;
  }

  public long getViewCount() {
    return viewCount;
  }

  public LocalDateTime getPublishedAt() {
    return publishedAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public LocalDate getDisplayUntil() {
    return displayUntil;
  }

  public void requestApproval() {
    this.status = PostStatus.PENDING;
  }

  public void approve() {
    this.status = PostStatus.APPROVED;
    this.publishedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
  }

  public void reject() {
    this.status = PostStatus.REJECTED;
  }

  public LocalDate getDisplayFrom() {
    return displayFrom;
  }

  public long getVersion() {
    return version;
  }

  public void setDisplayFrom(LocalDate value) {
    displayFrom = value;
  }

  public void hide() {
    status = PostStatus.HIDDEN;
  }

  public void restore() {
    status = PostStatus.APPROVED;
  }

  public void changeSchedule(ScheduleRequest r) {
    displayFrom = r.displayFrom();
    displayUntil = r.displayUntil();
    eventAt = r.eventAt();
    deadlineAt = r.deadlineAt();
  }

  public boolean isPublic(LocalDate today) {
    return status == PostStatus.APPROVED
        && deletedAt == null
        && displayUntil != null
        && (displayFrom == null || !displayFrom.isAfter(today));
  }

  public String snapshot() {
    return "제목: "
        + title
        + "\n주최: "
        + organizer
        + "\n분류: "
        + category
        + "\n설명: "
        + description
        + "\n장소: "
        + location
        + "\n신청: "
        + applicationUrl
        + "\n행사: "
        + eventAt
        + "\n마감: "
        + deadlineAt
        + "\n게시: "
        + displayFrom
        + " ~ "
        + displayUntil;
  }

  public void cancelApproval() { status = PostStatus.DRAFT; }

  public void deleteByAuthor() {
    deletedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
  }

  public void touchFiles() {
    updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
  }
}
