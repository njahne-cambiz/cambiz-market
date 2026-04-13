package com.cambiz.market.dto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class SellerOrderResponse {
    private Long sellerOrderId;
    private Long sellerId;
    private String sellerName;
    private BigDecimal subtotal;
    private BigDecimal commission;
    private BigDecimal sellerPayout;
    private String status;
    private List<OrderItemResponse> items;
}