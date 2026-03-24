package com.iiit.oms.model;

import java.math.BigDecimal;

public class Order {
    private String orderID;
    private String productID;
    private BigDecimal quantity;
    private BigDecimal amount;
    private String accountID;
    private OrderSide orderSide = OrderSide.BUY;
    private OrderStatus orderStatus = OrderStatus.PLANNED;
    private boolean isProcessed;
    private String errorDescription;

    public Order() {
    }

    public Order(String orderID, String productID, BigDecimal quantity, BigDecimal amount, String Account) {
        this.orderID = orderID;
        this.productID = productID;
        this.quantity = quantity;
        this.amount = amount;
        this.accountID = Account;
        this.orderSide = OrderSide.BUY;
        this.orderStatus = OrderStatus.PLANNED;
        this.isProcessed = false;
    }

    public Order(String orderID, String productID, BigDecimal quantity, BigDecimal amount, String accountID, OrderSide orderSide) {
        this.orderID = orderID;
        this.productID = productID;
        this.quantity = quantity;
        this.amount = amount;
        this.accountID = accountID;
        this.orderSide = orderSide;
        this.orderStatus = OrderStatus.PLANNED;
        this.isProcessed = false;
    }

    public Order(String orderID, String productID, BigDecimal quantity, BigDecimal amount, String accountID, OrderStatus orderStatus) {
        this.orderID = orderID;
        this.productID = productID;
        this.quantity = quantity;
        this.amount = amount;
        this.accountID = accountID;
        this.orderSide = OrderSide.BUY;
        this.orderStatus = orderStatus;
        this.isProcessed = false;
    }

    public Order(String orderID, String productID, BigDecimal quantity, BigDecimal amount, String accountID, OrderSide orderSide, OrderStatus orderStatus) {
        this.orderID = orderID;
        this.productID = productID;
        this.quantity = quantity;
        this.amount = amount;
        this.accountID = accountID;
        this.orderSide = orderSide;
        this.orderStatus = orderStatus;
        this.isProcessed = false;
    }

    public Order(String orderID, String productID, BigDecimal quantity, BigDecimal amount, String accountID, OrderStatus orderStatus, boolean isProcessed) {
        this.orderID = orderID;
        this.productID = productID;
        this.quantity = quantity;
        this.amount = amount;
        this.accountID = accountID;
        this.orderSide = OrderSide.BUY;
        this.orderStatus = orderStatus;
        this.isProcessed = isProcessed;
    }

    public Order(String orderID, String productID, BigDecimal quantity, BigDecimal amount, String accountID, OrderSide orderSide, OrderStatus orderStatus, boolean isProcessed) {
        this.orderID = orderID;
        this.productID = productID;
        this.quantity = quantity;
        this.amount = amount;
        this.accountID = accountID;
        this.orderSide = orderSide;
        this.orderStatus = orderStatus;
        this.isProcessed = isProcessed;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public String getProductID() {
        return productID;
    }

    public void setProductID(String productID) {
        this.productID = productID;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getAccountID() {
        return accountID;
    }

    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }

    public OrderSide getOrderSide() {
        return orderSide;
    }

    public void setOrderSide(OrderSide orderSide) {
        this.orderSide = orderSide;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public boolean isProcessed() {
        return isProcessed;
    }

    public void setProcessed(boolean processed) {
        isProcessed = processed;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderID='" + orderID + '\'' +
                ", productID='" + productID + '\'' +
                ", quantity=" + quantity +
                ", amount=" + amount +
                ", accountID='" + accountID + '\'' +
                ", orderSide=" + orderSide +
                ", orderStatus=" + orderStatus +
                ", isProcessed=" + isProcessed +
                ", errorDescription='" + errorDescription + '\'' +
                '}';
    }
}
