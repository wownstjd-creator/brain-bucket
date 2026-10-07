package com.brainbucket.post;

import java.time.LocalDateTime;

public record MyPostResponse(
        Long id,
        String title,
        String organizer,
        String category,
        PostStatus status,
        LocalDateTime deadlineAt,
        LocalDateTime createdAt
) {
}