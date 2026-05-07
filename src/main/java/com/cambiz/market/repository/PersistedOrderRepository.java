package com.cambiz.market.repository;

import com.cambiz.market.model.PersistedOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersistedOrderRepository extends JpaRepository<PersistedOrder, Long> {
    Optional<PersistedOrder> findByOrderId(Long orderId);
    List<PersistedOrder> findAllByOrderByCreatedAtDesc();
}