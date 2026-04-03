package com.iiit.oms.readmodel;

import java.math.BigDecimal;

/**
 * Flattened read-model representation of Order optimized for UI consumption.
 * Denormalizes order and fund data into a single view document.
 */
public class OrderView {
    private String orderID;
    private String accountID;
    private String fundID;
    private String fundName;
    private String orderSide; // "BUY" or "SELL"
    private BigDecimal amount;
    private BigDecimal quantity;
    private BigDecimal nav;
    private String orderStatus;
    private String bulkOrderID;

    public OrderView() {
    }

    public OrderView(String orderID, String accountID, String fundID, String fundName,
                     String orderSide, BigDecimal amount, BigDecimal quantity, BigDecimal nav,
                     String orderStatus, String bulkOrderID) {
        this.orderID = orderID;
        this.accountID = accountID;
        this.fundID = fundID;
        this.fundName = fundName;
        this.orderSide = orderSide;
        this.amount = amount;
        this.quantity = quantity;
        this.nav = nav;
        this.orderStatus = orderStatus;
        this.bulkOrderID = bulkOrderID;
    }

    // Getters and Setters
    public String getOrderID() { return orderID; }
    public void setOrderID(String orderID) { this.orderID = orderID; }

    public String getAccountID() { return accountID; }
    public void setAccountID(String accountID) { this.accountID = accountID; }

    public String getFundID() { return fundID; }
    public void setFundID(String fundID) { this.fundID = fundID; }

    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public String getOrderSide() { return orderSide; }
    public void setOrderSide(String orderSide) { this.orderSide = orderSide; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getNAV() { return nav; }
    public void setNAV(BigDecimal nav) { this.nav = nav; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getBulkOrderID() { return bulkOrderID; }
    public void setBulkOrderID(String bulkOrderID) { this.bulkOrderID = bulkOrderID; }

    @Override
    public String toString() {
        return "OrderView{" +
                "orderID='" + orderID + '\'' +
                ", accountID='" + accountID + '\'' +
                ", fundID='" + fundID + '\'' +
                ", fundName='" + fundName + '\'' +
                ", orderSide='" + orderSide + '\'' +
                ", amount=" + amount +
                ", quantity=" + quantity +
                ", nav=" + nav +
                ", orderStatus='" + orderStatus + '\'' +
                ", bulkOrderID='" + bulkOrderID + '\'' +
                '}';
    }
}
