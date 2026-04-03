package com.iiit.oms.readmodel;

import com.iiit.oms.model.BulkOrderStatus;
import com.iiit.oms.model.OrderSide;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Flattened read-model representation of BulkOrder optimized for UI consumption.
 * Aggregates bulk order data with matched individual orders.
 */
public class BulkOrderView {
    private String bulkOrderId;
    private String fundId;
    private OrderSide side;
    private BulkOrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal totalQuantity;
    private BigDecimal nav;
    private List<String> matchedOrderIds;
    private Integer matchedOrderCount;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime bookedAt;
    private LocalDateTime contractedAt;

    // Default constructor for deserialization
    public BulkOrderView() {
    }

    public BulkOrderView(String bulkOrderId, String fundId, OrderSide side,
                         BulkOrderStatus status, BigDecimal totalAmount, BigDecimal totalQuantity,
                         BigDecimal nav, List<String> matchedOrderIds, Integer matchedOrderCount,
                         LocalDateTime createdAt, LocalDateTime confirmedAt, LocalDateTime bookedAt,
                         LocalDateTime contractedAt) {
        this.bulkOrderId = bulkOrderId;
        this.fundId = fundId;
        this.side = side;
        this.status = status;
        this.totalAmount = totalAmount;
        this.totalQuantity = totalQuantity;
        this.nav = nav;
        this.matchedOrderIds = matchedOrderIds;
        this.matchedOrderCount = matchedOrderCount;
        this.createdAt = createdAt;
        this.confirmedAt = confirmedAt;
        this.bookedAt = bookedAt;
        this.contractedAt = contractedAt;
    }

    // Getters and setters
    public String getBulkOrderId() {
        return bulkOrderId;
    }

    public void setBulkOrderId(String bulkOrderId) {
        this.bulkOrderId = bulkOrderId;
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

    public BulkOrderStatus getStatus() {
        return status;
    }

    public void setStatus(BulkOrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getNav() {
        return nav;
    }

    public void setNav(BigDecimal nav) {
        this.nav = nav;
    }

    public List<String> getMatchedOrderIds() {
        return matchedOrderIds;
    }

    public void setMatchedOrderIds(List<String> matchedOrderIds) {
        this.matchedOrderIds = matchedOrderIds;
    }

    public Integer getMatchedOrderCount() {
        return matchedOrderCount;
    }

    public void setMatchedOrderCount(Integer matchedOrderCount) {
        this.matchedOrderCount = matchedOrderCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public void setBookedAt(LocalDateTime bookedAt) {
        this.bookedAt = bookedAt;
    }

    public LocalDateTime getContractedAt() {
        return contractedAt;
    }

    public void setContractedAt(LocalDateTime contractedAt) {
        this.contractedAt = contractedAt;
    }

    @Override
    public String toString() {
        return "BulkOrderView{" +
                "bulkOrderId='" + bulkOrderId + '\'' +
                ", fundId='" + fundId + '\'' +
                ", side=" + side +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                ", totalQuantity=" + totalQuantity +
                ", nav=" + nav +
                ", matchedOrderCount=" + matchedOrderCount +
                ", createdAt=" + createdAt +
                ", confirmedAt=" + confirmedAt +
                ", bookedAt=" + bookedAt +
                ", contractedAt=" + contractedAt +
                '}';
    }
}
