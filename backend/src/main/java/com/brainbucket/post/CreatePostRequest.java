package com.brainbucket.post;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreatePostRequest(
    @Size(max = 150) String title,
    @Size(max = 100) String organizer,
    @Size(max = 30) String category,
    @Size(max = 10000) String description,
    @Size(max = 150) String location,
    @Size(max = 2048) String applicationUrl,
    LocalDateTime eventAt,
    LocalDateTime deadlineAt,
    LocalDate displayUntil,
    LocalDate displayFrom,
    Long version,
    Boolean showViewCount) {}
