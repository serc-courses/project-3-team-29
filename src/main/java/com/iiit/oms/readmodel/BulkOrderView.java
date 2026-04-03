package com.iiit.oms.readmodel;

import java.math.BigDecimal;
import java.util.List;

/**
 * Flattened read-model representation of BulkOrder optimized for UI consumption.
 * Aggregates bulk order data with matched orders.
 */
public class BulkOrderView {
    private String bulkOrderID;
    private String fundID;
    private String fundName;
    private String orderSide; // "BUY" or "SELL"
    private String bulkOrderStatus;
    private BigDecimal totalAmount;
    private BigDecimal totalQuantity;
    private BigDecimal nav;
    private List<String> matchedOrderIDs;
    private Integer matchedOrderCount;

    public BulkOrderView() {
    }

    public BulkOrderView(String bulkOrderID, String fundID, String fundName,
                         String orderSide, String bulkOrderStatus, BigDecimal totalAmount,
                         BigDecimal totalQuantity, BigDecimal nav, List<String> matchedOrderIDs,
                         Integer matchedOrderCount) {
        this.bulkOrderID = bulkOrderID;
        this.fundID = fundID;
        this.fundName = fundName;
        this.orderSide = orderSide;
        this.bulkOrderStatus = bulkOrderStatus;
        this.totalAmount = totalAmount;
        this.totalQuantity = totalQuantity;
        this.nav = nav;
        this.matchedOrderIDs = matchedOrderIDs;
        this.matchedOrderCount = matchedOrderCount;
    }

    // Getters and Setters
    public String getBulkOrderID() { return bulkOrderID; }
    public void setBulkOrderID(String bulkOrderID) { this.bulkOrderID = bulkOrderID; }

    public String getFundID() { return fundID; }
    public void setFundID(String fundID) { this.fundID = fundID; }

    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public String getOrderSide() { return orderSide; }
    public void setOrderSide(String orderSide) { this.orderSide = orderSide; }

    public String getBulkOrderStatus() { return bulkOrderStatus; }
    public void setBulkOrderStatus(String bulkOrderStatus) { this.bulkOrderStatus = bulkOrderStatus; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal totalQuantity) { this.totalQuantity = totalQuantity; }

    public BigDecimal getNAV() { return nav; }
    public void setNAV(BigDecimal nav) { this.nav = nav; }

    public List<String> getMatchedOrderIDs() { return matchedOrderIDs; }
    public void setMatchedOrderIDs(List<String> matchedOrderIDs) { this.matchedOrderIDs = matchedOrderIDs; }

    public Integer getMatchedOrderCount() { return matchedOrderCount; }
    public void setMatchedOrderCount(Integer matchedOrderCount) { this.matchedOrderCount = matchedOrderCount; }

    @Override
    public String toString() {
        return "BulkOrderView{" +
                "bulkOrderID='" + bulkOrderID + '\'' +
                ", fundID='" + fundID + '\'' +
                ", fundName='" + fundName + '\'' +
                ", orderSide='" + orderSide + '\'' +
                ", bulkOrderStatus='" + bulkOrderStatus + '\'' +
                ", totalAmount=" + totalAmount +
                ", totalQuantity=" + totalQuantity +
                ", nav=" + nav +
                ", matchedOrderCount=" + matchedOrderCount +
                '}';
    }
}
