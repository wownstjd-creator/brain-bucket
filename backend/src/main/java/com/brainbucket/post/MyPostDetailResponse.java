package com.brainbucket.post;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MyPostDetailResponse(
    Long id,
    String title,
    String organizer,
    String category,
    String description,
    String location,
    String applicationUrl,
    LocalDateTime eventAt,
    LocalDateTime deadlineAt,
    LocalDate displayUntil,
    LocalDate displayFrom,
    long version,
    boolean hasEvidence,
    boolean hasOriginal,
    PostStatus status,
    boolean showViewCount,
    long viewCount,
    LocalDateTime publishedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public String imageUrl() {
    return "/api/posts/mine/" + id + "/poster";
  }
}
