package com.brainbucket.user;

public record LoginResponse(
        Long id,
        String email,
        String displayName,
        String role
) {
}