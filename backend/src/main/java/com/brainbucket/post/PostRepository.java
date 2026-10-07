package com.brainbucket.post;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByAuthor_IdAndDeletedAtIsNull(
            Long authorId,
            Pageable pageable
    );
    Optional<Post> findByIdAndAuthor_IdAndDeletedAtIsNull(
            Long id,
            Long authorId
    );
}