package com.brainbucket.post;

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
        PostStatus status,
        boolean showViewCount,
        long viewCount,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}