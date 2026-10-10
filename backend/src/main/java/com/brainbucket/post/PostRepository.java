package com.brainbucket.post;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository
    extends JpaRepository<Post, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Post> {

  Page<Post> findByAuthor_IdAndDeletedAtIsNull(Long authorId, Pageable pageable);

  Optional<Post> findByIdAndAuthor_IdAndDeletedAtIsNull(Long id, Long authorId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select p from Post p
      where p.id = :postId
        and p.deletedAt is null
      """)
  Optional<Post> findForReview(@Param("postId") Long postId);

  Optional<Post> findByIdAndStatusAndDeletedAtIsNull(Long id, PostStatus status);

  Page<Post> findByStatusAndDeletedAtIsNullAndDisplayUntilGreaterThanEqual(
      PostStatus status, LocalDate today, Pageable pageable);

  Page<Post> findByStatusAndDeletedAtIsNullAndDisplayUntilLessThan(
      PostStatus status, LocalDate today, Pageable pageable);

  Page<Post> findByStatusAndDeletedAtIsNull(PostStatus status, Pageable pageable);

  Optional<Post> findByIdAndDeletedAtIsNull(Long id);
}
