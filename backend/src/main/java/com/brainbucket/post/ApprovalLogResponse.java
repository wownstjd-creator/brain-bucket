package com.brainbucket.post;

import java.time.LocalDateTime;

public record ApprovalLogResponse(
        Long id,
        String decision,
        String reason,
        LocalDateTime createdAt
) {
}