package com.brainbucket.post;

import jakarta.validation.constraints.*;
import java.time.*;

public record ScheduleRequest(
    @NotNull LocalDate displayFrom,
    @NotNull LocalDate displayUntil,
    LocalDateTime eventAt,
    LocalDateTime deadlineAt,
    @NotBlank @Size(max = 2000) String reason,
    Long version) {}
