package com.brainbucket.post;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostFileRepository extends JpaRepository<PostFile, Long> {

  Optional<PostFile> findByPost_IdAndFileKind(Long postId, String fileKind);

  boolean existsByPost_IdAndFileKind(Long postId, String fileKind);

  java.util.List<PostFile> findByPost_Id(Long postId);
  java.util.List<PostFile> findByPost_IdIn(java.util.Collection<Long> ids);
}
