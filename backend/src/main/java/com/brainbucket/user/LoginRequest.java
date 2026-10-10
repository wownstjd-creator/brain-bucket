package com.brainbucket.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "이메일을 입력해주세요.") @Email(message = "이메일 형식이 올바르지 않습니다.") @Size(max = 255)
        String email,
    @NotBlank(message = "비밀번호를 입력해주세요.") @Size(max = 72) String password) {}
