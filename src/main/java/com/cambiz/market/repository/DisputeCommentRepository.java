package com.cambiz.market.repository;

import com.cambiz.market.model.DisputeComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DisputeCommentRepository extends JpaRepository<DisputeComment, Long> {
    List<DisputeComment> findByDisputeIdOrderByCreatedAtAsc(Long disputeId);
}