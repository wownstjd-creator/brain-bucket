package com.brainbucket.post;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostFileRepository extends JpaRepository<PostFile, Long> {

    Optional<PostFile> findByPost_IdAndFileKind(
            Long postId,
            String fileKind
    );

    boolean existsByPost_IdAndFileKind(
            Long postId,
            String fileKind
    );
}