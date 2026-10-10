package com.brainbucket.post;

import java.time.*;

public record PublicPostResponse(
    Long id,
    String title,
    String organizer,
    String category,
    LocalDate displayUntil,
    LocalDate displayFrom,
    long version,
    String imageUrl,
    String description,
    String location,
    String applicationUrl,
    LocalDateTime eventAt,
    LocalDateTime deadlineAt,
    LocalDateTime publishedAt,
    PostStatus status,
    Long viewCount) {
  public static PublicPostResponse from(Post p) {
    return new PublicPostResponse(
        p.getId(),
        p.getTitle(),
        p.getOrganizer(),
        p.getCategory(),
        p.getDisplayUntil(),
        p.getDisplayFrom(),
        p.getVersion(),
        "/api/posts/public/" + p.getId() + "/poster",
        p.getDescription(),
        p.getLocation(),
        p.getApplicationUrl(),
        p.getEventAt(),
        p.getDeadlineAt(),
        p.getPublishedAt(),
        p.getStatus(),
        p.isShowViewCount() ? p.getViewCount() : null);
  }
}
