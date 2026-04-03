package com.iiit.oms.readmodel;

import com.iiit.oms.model.OrderSide;
import com.iiit.oms.model.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Flattened read-model representation of Order optimized for UI consumption.
 * Denormalizes order and bulk order data into a single view document.
 */
public class OrderView {
    private String orderId;
    private String accountId;
    private String fundId;
    private OrderSide side;
    private BigDecimal amount;
    private BigDecimal quantity;
    private BigDecimal nav;
    private OrderStatus status;
    private String bulkOrderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor for deserialization
    public OrderView() {
    }

    public OrderView(String orderId, String accountId, String fundId, OrderSide side, 
                     BigDecimal amount, BigDecimal quantity, BigDecimal nav,
                     OrderStatus status, String bulkOrderId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.fundId = fundId;
        this.side = side;
        this.amount = amount;
        this.quantity = quantity;
        this.nav = nav;
        this.status = status;
        this.bulkOrderId = bulkOrderId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getFundId() {
        return fundId;
    }

    public void setFundId(String fundId) {
        this.fundId = fundId;
    }

    public OrderSide getSide() {
        return side;
    }

    public void setSide(OrderSide side) {
        this.side = side;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getNav() {
        return nav;
    }

    public void setNav(BigDecimal nav) {
        this.nav = nav;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getBulkOrderId() {
        return bulkOrderId;
    }

    public void setBulkOrderId(String bulkOrderId) {
        this.bulkOrderId = bulkOrderId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "OrderView{" +
                "orderId='" + orderId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", fundId='" + fundId + '\'' +
                ", side=" + side +
                ", amount=" + amount +
                ", quantity=" + quantity +
                ", nav=" + nav +
                ", status=" + status +
                ", bulkOrderId='" + bulkOrderId + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
