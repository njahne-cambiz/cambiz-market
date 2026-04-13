package com.cambiz.market.repository;

import com.cambiz.market.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByTransactionId(String transactionId);
    
    Optional<Payment> findByOrderId(Long orderId);
    
    List<Payment> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    
    List<Payment> findByStatus(Payment.PaymentStatus status);
    
    List<Payment> findByStatusAndPaymentMethod(Payment.PaymentStatus status, Payment.PaymentMethod method);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal getTotalSuccessfulPayments();
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paymentMethod = :method AND p.status = 'SUCCESS'")
    BigDecimal getTotalByPaymentMethod(@Param("method") Payment.PaymentMethod method);
    
    boolean existsByOrderIdAndStatus(Long orderId, Payment.PaymentStatus status);
}