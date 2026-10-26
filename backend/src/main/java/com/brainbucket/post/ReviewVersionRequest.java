package com.brainbucket.post;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** The revision actually inspected by the reviewer, never the latest revision fetched at click time. */
public record ReviewVersionRequest(@NotNull @PositiveOrZero Long version) {}
