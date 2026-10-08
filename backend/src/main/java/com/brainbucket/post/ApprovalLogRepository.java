package com.brainbucket.post;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalLogRepository
        extends JpaRepository<ApprovalLog, Long> {
    List<ApprovalLog> findByPost_IdOrderByCreatedAtDescIdDesc(Long postId);
}