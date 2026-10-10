package com.brainbucket.post;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PublicPostDetailResponse(
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
    LocalDateTime publishedAt,
    boolean archived,
    String imageUrl,
    Long viewCount) {}
