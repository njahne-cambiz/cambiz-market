package com.cambiz.market.repository;

import com.cambiz.market.model.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    List<Dispute> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    List<Dispute> findByStatusOrderByCreatedAtDesc(Dispute.DisputeStatus status);
    long countByStatus(Dispute.DisputeStatus status);
    List<Dispute> findByOrderByCreatedAtDesc();
}