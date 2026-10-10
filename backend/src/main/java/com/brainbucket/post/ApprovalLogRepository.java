package com.brainbucket.post;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalLogRepository extends JpaRepository<ApprovalLog, Long> {
  List<ApprovalLog> findByPost_IdOrderByCreatedAtDescIdDesc(Long postId);

  @org.springframework.data.jpa.repository.Query(
      "select count(a) from ApprovalLog a where a.posterKey=:key or a.evidenceKey=:key or"
          + " a.originalKey=:key")
  long countFileReferences(@org.springframework.data.repository.query.Param("key") String key);
}
