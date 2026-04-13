package com.cambiz.market.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private String shippingAddress;
    private String shippingCity;
    private String shippingPhone;
    private String paymentMethod;
    private String notes;
}