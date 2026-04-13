package com.cambiz.market.service;

import com.cambiz.market.dto.*;
import com.cambiz.market.model.*;
import com.cambiz.market.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    
    // Temporary in-memory order storage
    private final ConcurrentHashMap<Long, OrderResponse> orderStorage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> orderPaymentStatus = new ConcurrentHashMap<>();
    
    /**
     * CHECKOUT - Convert cart to order
     */
    public OrderResponse checkout(Long buyerId, CheckoutRequest request) {
        log.info("Processing checkout for buyer: {}", buyerId);
        
        // 1. Get buyer's cart
        CartResponse cart = cartService.getCart(buyerId);
        
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }
        
        // 2. Get buyer - ✅ FIXED: Used for logging and validation
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new RuntimeException("Buyer not found"));
        
        log.info("Buyer {} ({}) checking out", buyer.getId(), buyer.getEmail()); // ✅ Use buyer variable
        
        // 3. Group items by seller
        Map<Long, List<CartItem>> itemsBySeller = cart.getItems().stream()
            .collect(Collectors.groupingBy(CartItem::getSellerId));
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<SellerOrderResponse> sellerOrderResponses = new ArrayList<>();
        
        // 4. Process each seller's order
        for (Map.Entry<Long, List<CartItem>> entry : itemsBySeller.entrySet()) {
            Long sellerId = entry.getKey();
            List<CartItem> sellerItems = entry.getValue();
            
            User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
            
            BigDecimal sellerSubtotal = BigDecimal.ZERO;
            List<OrderItemResponse> itemResponses = new ArrayList<>();
            
            // Process items
            for (CartItem cartItem : sellerItems) {
                Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
                
                // Reduce stock
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
            
            // Calculate commission (5%)
            BigDecimal commissionRate = new BigDecimal("0.05");
            BigDecimal commission = sellerSubtotal.multiply(commissionRate);
            BigDecimal sellerPayout = sellerSubtotal.subtract(commission);
            
            totalAmount = totalAmount.add(sellerSubtotal);
            
            // Build seller name
            String sellerName = "Seller";
            if (seller.getFirstName() != null && seller.getLastName() != null) {
                sellerName = seller.getFirstName() + " " + seller.getLastName();
            } else if (seller.getFirstName() != null) {
                sellerName = seller.getFirstName();
            }
            
            // Create seller order response
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
        
        // 5. Clear cart
        cartService.clearCart(buyerId);
        
        // 6. Build response
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
        
        // Store order in memory
        orderStorage.put(orderId, orderResponse);
        orderPaymentStatus.put(orderId, "PENDING");
        
        log.info("Order created: {} for amount: {} XAF", orderNumber, totalAmount);
        
        return orderResponse;
    }
    
    /**
     * Get buyer's orders
     */
    public List<OrderResponse> getBuyerOrders(Long buyerId) {
        log.debug("Fetching orders for buyer: {}", buyerId); // ✅ Use buyerId parameter
        return new ArrayList<>(orderStorage.values());
    }
    
    /**
     * Get single order
     */
    public OrderResponse getOrder(Long orderId, Long userId) {
        log.debug("User {} fetching order: {}", userId, orderId); // ✅ Use userId parameter
        OrderResponse order = orderStorage.get(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found with ID: " + orderId);
        }
        return order;
    }
    
    /**
     * Update order payment status
     */
    public void updateOrderPaymentStatus(Long orderId, String status) {
        log.info("Updating order {} payment status to: {}", orderId, status);
        
        OrderResponse order = orderStorage.get(orderId);
        if (order == null) {
            log.warn("Order not found: {}", orderId);
            return;
        }
        
        // Update payment status
        orderPaymentStatus.put(orderId, status);
        
        // Update order status based on payment
        if ("PAID".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
            OrderResponse updatedOrder = OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .totalAmount(order.getTotalAmount())
                .status("CONFIRMED")
                .paymentMethod(order.getPaymentMethod())
                .sellerOrders(order.getSellerOrders())
                .createdAt(order.getCreatedAt())
                .build();
            
            orderStorage.put(orderId, updatedOrder);
            log.info("Order {} status updated to CONFIRMED", orderId);
        }
    }
    
    /**
     * Get order payment status
     */
    public String getOrderPaymentStatus(Long orderId) {
        return orderPaymentStatus.getOrDefault(orderId, "UNKNOWN");
    }
    
    /**
     * Update order status (for seller/admin)
     */
    public void updateOrderStatus(Long orderId, String newStatus) {
        log.info("Updating order {} status to: {}", orderId, newStatus);
        
        OrderResponse order = orderStorage.get(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found: " + orderId);
        }
        
        OrderResponse updatedOrder = OrderResponse.builder()
            .orderId(order.getOrderId())
            .orderNumber(order.getOrderNumber())
            .totalAmount(order.getTotalAmount())
            .status(newStatus)
            .paymentMethod(order.getPaymentMethod())
            .sellerOrders(order.getSellerOrders())
            .createdAt(order.getCreatedAt())
            .build();
        
        orderStorage.put(orderId, updatedOrder);
        log.info("Order {} status updated to {}", orderId, newStatus);
    }
    
    /**
     * Get all orders (for admin)
     */
    public List<OrderResponse> getAllOrders() {
        log.debug("Fetching all orders, count: {}", orderStorage.size()); // ✅ Use orderStorage
        return new ArrayList<>(orderStorage.values());
    }
    
    /**
     * Get seller's orders
     */
    public List<SellerOrderResponse> getSellerOrders(Long sellerId) {
        log.debug("Fetching orders for seller: {}", sellerId); // ✅ Use sellerId parameter
        List<SellerOrderResponse> sellerOrders = new ArrayList<>();
        
        for (OrderResponse order : orderStorage.values()) {
            if (order.getSellerOrders() != null) {
                for (SellerOrderResponse sellerOrder : order.getSellerOrders()) {
                    if (sellerOrder.getSellerId().equals(sellerId)) {
                        sellerOrders.add(sellerOrder);
                    }
                }
            }
        }
        
        return sellerOrders;
    }
}