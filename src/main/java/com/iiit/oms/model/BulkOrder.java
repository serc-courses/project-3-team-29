package com.iiit.oms.model;

import java.math.BigDecimal;

public class BulkOrder {
    private String orderID;
    private String productID;
    private OrderSide orderSide;
    private BulkOrderStatus bulkOrderStatus = BulkOrderStatus.BULKED;
    private BigDecimal quantity;
    private BigDecimal amount;
    private String accountID;
    private String transferAgent;
    private String transmissionRef;
    private String contractRef;
    private BigDecimal bulkNav;

    public BulkOrder() {
    }

    public BulkOrder(String orderID, String productID, OrderSide orderSide, BigDecimal quantity, BigDecimal amount, String accountID) {
        this(orderID, productID, orderSide, BulkOrderStatus.BULKED, quantity, amount, accountID);
    }

    public BulkOrder(String orderID, String productID, OrderSide orderSide, BulkOrderStatus bulkOrderStatus,
                     BigDecimal quantity, BigDecimal amount, String accountID) {
        this.orderID = orderID;
        this.productID = productID;
        this.orderSide = orderSide;
        this.bulkOrderStatus = bulkOrderStatus;
        this.quantity = quantity;
        this.amount = amount;
        this.accountID = accountID;
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

    public OrderSide getOrderSide() {
        return orderSide;
    }

    public void setOrderSide(OrderSide orderSide) {
        this.orderSide = orderSide;
    }

    public BulkOrderStatus getBulkOrderStatus() {
        return bulkOrderStatus;
    }

    public void setBulkOrderStatus(BulkOrderStatus bulkOrderStatus) {
        this.bulkOrderStatus = bulkOrderStatus;
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

    public String getTransferAgent() { return transferAgent; }
    public void setTransferAgent(String transferAgent) { this.transferAgent = transferAgent; }

    public String getTransmissionRef() { return transmissionRef; }
    public void setTransmissionRef(String transmissionRef) { this.transmissionRef = transmissionRef; }

    public String getContractRef() { return contractRef; }
    public void setContractRef(String contractRef) { this.contractRef = contractRef; }

    public BigDecimal getBulkNav() { return bulkNav; }
    public void setBulkNav(BigDecimal bulkNav) { this.bulkNav = bulkNav; }
}
