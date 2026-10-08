package com.brainbucket.post;

import java.time.LocalDate;

public record PublicPostResponse(
        Long id,
        String title,
        String organizer,
        String category,
        LocalDate displayUntil,
        String imageUrl
) {
}