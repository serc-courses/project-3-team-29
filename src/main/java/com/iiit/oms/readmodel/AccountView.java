package com.iiit.oms.readmodel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Flattened read-model representation of Account optimized for UI consumption.
 * Includes aggregated order metrics for the account.
 */
public class AccountView {
    private String accountId;
    private String accountName;
    private Long totalOrders;
    private Long confirmedOrders;
    private Long bookedOrders;
    private Long erroredOrders;
    private BigDecimal totalInvested;
    private BigDecimal totalQuantityHeld;
    private Map<String, BigDecimal> fundQuantities; // fundId -> quantity
    private LocalDateTime firstOrderAt;
    private LocalDateTime lastOrderAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public AccountView() {
    }

    public AccountView(String accountId, String accountName, Long totalOrders, Long confirmedOrders,
                       Long bookedOrders, Long erroredOrders, BigDecimal totalInvested,
                       BigDecimal totalQuantityHeld, Map<String, BigDecimal> fundQuantities,
                       LocalDateTime firstOrderAt, LocalDateTime lastOrderAt, LocalDateTime updatedAt) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.totalOrders = totalOrders;
        this.confirmedOrders = confirmedOrders;
        this.bookedOrders = bookedOrders;
        this.erroredOrders = erroredOrders;
        this.totalInvested = totalInvested;
        this.totalQuantityHeld = totalQuantityHeld;
        this.fundQuantities = fundQuantities;
        this.firstOrderAt = firstOrderAt;
        this.lastOrderAt = lastOrderAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }

    public Long getConfirmedOrders() { return confirmedOrders; }
    public void setConfirmedOrders(Long confirmedOrders) { this.confirmedOrders = confirmedOrders; }

    public Long getBookedOrders() { return bookedOrders; }
    public void setBookedOrders(Long bookedOrders) { this.bookedOrders = bookedOrders; }

    public Long getErroredOrders() { return erroredOrders; }
    public void setErroredOrders(Long erroredOrders) { this.erroredOrders = erroredOrders; }

    public BigDecimal getTotalInvested() { return totalInvested; }
    public void setTotalInvested(BigDecimal totalInvested) { this.totalInvested = totalInvested; }

    public BigDecimal getTotalQuantityHeld() { return totalQuantityHeld; }
    public void setTotalQuantityHeld(BigDecimal totalQuantityHeld) { this.totalQuantityHeld = totalQuantityHeld; }

    public Map<String, BigDecimal> getFundQuantities() { return fundQuantities; }
    public void setFundQuantities(Map<String, BigDecimal> fundQuantities) { this.fundQuantities = fundQuantities; }

    public LocalDateTime getFirstOrderAt() { return firstOrderAt; }
    public void setFirstOrderAt(LocalDateTime firstOrderAt) { this.firstOrderAt = firstOrderAt; }

    public LocalDateTime getLastOrderAt() { return lastOrderAt; }
    public void setLastOrderAt(LocalDateTime lastOrderAt) { this.lastOrderAt = lastOrderAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "AccountView{" +
                "accountId='" + accountId + '\'' +
                ", accountName='" + accountName + '\'' +
                ", totalOrders=" + totalOrders +
                ", confirmedOrders=" + confirmedOrders +
                ", bookedOrders=" + bookedOrders +
                ", totalInvested=" + totalInvested +
                ", totalQuantityHeld=" + totalQuantityHeld +
                '}';
    }
}
