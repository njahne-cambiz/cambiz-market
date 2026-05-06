package com.cambiz.market.service;

import com.cambiz.market.dto.*;
import com.cambiz.market.model.*;
import com.cambiz.market.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {
    
    private final CartService cartService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    
    @Autowired
    private TransactionService transactionService;
    
    private final ConcurrentHashMap<Long, OrderResponse> orderStorage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> orderPaymentStatus = new ConcurrentHashMap<>();
    
    public OrderResponse checkout(Long buyerId, CheckoutRequest request) {
        log.info("Processing checkout for buyer: {}", buyerId);
        
        CartResponse cart = cartService.getCart(buyerId);
        
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }
        
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new RuntimeException("Buyer not found"));
        
        log.info("Buyer {} ({}) checking out", buyer.getId(), buyer.getEmail());
        
        Map<Long, List<CartItem>> itemsBySeller = cart.getItems().stream()
            .collect(Collectors.groupingBy(CartItem::getSellerId));
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<SellerOrderResponse> sellerOrderResponses = new ArrayList<>();
        
        for (Map.Entry<Long, List<CartItem>> entry : itemsBySeller.entrySet()) {
            Long sellerId = entry.getKey();
            List<CartItem> sellerItems = entry.getValue();
            
            User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
            
            BigDecimal sellerSubtotal = BigDecimal.ZERO;
            List<OrderItemResponse> itemResponses = new ArrayList<>();
            
            for (CartItem cartItem : sellerItems) {
                Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
                
                product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
                productRepository.save(product);
                
                BigDecimal itemSubtotal = BigDecimal.valueOf(cartItem.getSubtotal());
                sellerSubtotal = sellerSubtotal.add(itemSubtotal);
                
                itemResponses.add(OrderItemResponse.builder()
                    .productId(cartItem.getProductId())
                    .productName(cartItem.getProductName())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(BigDecimal.valueOf(cartItem.getPrice()))
                    .subtotal(itemSubtotal)
                    .isNegotiated(cartItem.getIsNegotiated())
                    .build());
            }
            
            BigDecimal commissionRate = new BigDecimal("0.05");
            BigDecimal commission = sellerSubtotal.multiply(commissionRate);
            BigDecimal sellerPayout = sellerSubtotal.subtract(commission);
            
            totalAmount = totalAmount.add(sellerSubtotal);
            
            String sellerName = "Seller";
            if (seller.getFirstName() != null && seller.getLastName() != null) {
                sellerName = seller.getFirstName() + " " + seller.getLastName();
            } else if (seller.getFirstName() != null) {
                sellerName = seller.getFirstName();
            }
            
            sellerOrderResponses.add(SellerOrderResponse.builder()
                .sellerOrderId(System.currentTimeMillis() + sellerId)
                .sellerId(sellerId)
                .sellerName(sellerName)
                .subtotal(sellerSubtotal)
                .commission(commission)
                .sellerPayout(sellerPayout)
                .status("PENDING")
                .items(itemResponses)
                .build());
        }
        
        cartService.clearCart(buyerId);
        
        Long orderId = System.currentTimeMillis();
        String orderNumber = "ORD-" + orderId;
        
        OrderResponse orderResponse = OrderResponse.builder()
            .orderId(orderId)
            .orderNumber(orderNumber)
            .totalAmount(totalAmount)
            .status("PENDING")
            .paymentMethod(request.getPaymentMethod())
            .sellerOrders(sellerOrderResponses)
            .createdAt(LocalDateTime.now())
            .build();
        
        orderStorage.put(orderId, orderResponse);
        orderPaymentStatus.put(orderId, "PENDING");
        
        // Record transactions for each seller order
        String buyerName = buyer.getFirstName() + " " + buyer.getLastName();
        for (SellerOrderResponse so : sellerOrderResponses) {
            String productNames = so.getItems().stream()
                .map(OrderItemResponse::getProductName)
                .collect(Collectors.joining(", "));
            transactionService.recordTransaction(
                orderId, orderNumber, buyerId, buyerName,
                so.getSellerId(), so.getSellerName(),
                productNames, so.getSubtotal().doubleValue(),
                so.getCommission().doubleValue(), so.getSellerPayout().doubleValue(),
                "SALE", request.getPaymentMethod()
            );
        }
        
        log.info("Order created: {} for amount: {} XAF", orderNumber, totalAmount);
        
        return orderResponse;
    }
    
    public List<OrderResponse> getBuyerOrders(Long buyerId) {
        log.debug("Fetching orders for buyer: {}", buyerId);
        return new ArrayList<>(orderStorage.values());
    }
    
    public OrderResponse getOrder(Long orderId, Long userId) {
        log.debug("User {} fetching order: {}", userId, orderId);
        OrderResponse order = orderStorage.get(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found with ID: " + orderId);
        }
        return order;
    }
    
    public void updateOrderPaymentStatus(Long orderId, String status) {
        log.info("Updating order {} payment status to: {}", orderId, status);
        OrderResponse order = orderStorage.get(orderId);
        if (order == null) {
            log.warn("Order not found: {}", orderId);
            return;
        }
        orderPaymentStatus.put(orderId, status);
        if ("PAID".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
            OrderResponse updatedOrder = OrderResponse.builder()
                .orderId(order.getOrderId()).orderNumber(order.getOrderNumber())
                .totalAmount(order.getTotalAmount()).status("CONFIRMED")
                .paymentMethod(order.getPaymentMethod()).sellerOrders(order.getSellerOrders())
                .createdAt(order.getCreatedAt()).build();
            orderStorage.put(orderId, updatedOrder);
            log.info("Order {} status updated to CONFIRMED", orderId);
        }
    }
    
    public String getOrderPaymentStatus(Long orderId) {
        return orderPaymentStatus.getOrDefault(orderId, "UNKNOWN");
    }
    
    public void updateOrderStatus(Long orderId, String newStatus) {
        log.info("Updating order {} status to: {}", orderId, newStatus);
        OrderResponse order = orderStorage.get(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found: " + orderId);
        }
        List<SellerOrderResponse> updatedSellerOrders = new ArrayList<>();
        if (order.getSellerOrders() != null) {
            for (SellerOrderResponse so : order.getSellerOrders()) {
                updatedSellerOrders.add(SellerOrderResponse.builder()
                    .sellerOrderId(so.getSellerOrderId()).orderId(so.getOrderId())
                    .sellerId(so.getSellerId()).sellerName(so.getSellerName())
                    .subtotal(so.getSubtotal()).commission(so.getCommission())
                    .sellerPayout(so.getSellerPayout()).status(newStatus)
                    .items(so.getItems()).build());
            }
        }
        OrderResponse updatedOrder = OrderResponse.builder()
            .orderId(order.getOrderId()).orderNumber(order.getOrderNumber())
            .totalAmount(order.getTotalAmount()).status(newStatus)
            .paymentMethod(order.getPaymentMethod()).sellerOrders(updatedSellerOrders)
            .createdAt(order.getCreatedAt()).build();
        orderStorage.put(orderId, updatedOrder);
        log.info("Order {} status updated to {}", orderId, newStatus);
    }
    
    public Long findMainOrderIdBySellerOrderId(Long sellerOrderId) {
        for (Map.Entry<Long, OrderResponse> entry : orderStorage.entrySet()) {
            OrderResponse order = entry.getValue();
            if (order.getSellerOrders() != null) {
                for (SellerOrderResponse so : order.getSellerOrders()) {
                    if (so.getSellerOrderId().equals(sellerOrderId)) return entry.getKey();
                }
            }
        }
        return sellerOrderId;
    }
    
    public List<OrderResponse> getAllOrders() {
        return new ArrayList<>(orderStorage.values());
    }
    
    public List<SellerOrderResponse> getSellerOrders(Long sellerId) {
        List<SellerOrderResponse> sellerOrders = new ArrayList<>();
        for (OrderResponse order : orderStorage.values()) {
            if (order.getSellerOrders() != null) {
                for (SellerOrderResponse sellerOrder : order.getSellerOrders()) {
                    if (sellerOrder.getSellerId().equals(sellerId)) sellerOrders.add(sellerOrder);
                }
            }
        }
        return sellerOrders;
    }
}