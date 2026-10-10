package com.brainbucket.post;

public record UploadPosterResponse(
    Long id,
    Long postId,
    String originalName,
    String contentType,
    long sizeBytes,
    String imageUrl) {}
