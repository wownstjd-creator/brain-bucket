package com.brainbucket.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectPostRequest(
    @NotBlank(message = "반려 사유를 입력해주세요.") @Size(max = 2000, message = "반려 사유는 2000자 이하여야 합니다.")
        String reason,
    @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.PositiveOrZero Long version) {}
