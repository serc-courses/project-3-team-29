package com.iiit.oms.readmodel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Aggregated read-model representation for dashboard/summary view.
 * Provides high-level metrics about orders and bulk orders.
 */
public class DashboardView {
    private Long totalOrders;
    private Long plannedOrders;
    private Long validatedOrders;
    private Long confirmedOrders;
    private Long bookedOrders;
    private Long erroredOrders;
    private BigDecimal totalAmount;
    private BigDecimal totalQuantity;
    private Long totalBulkOrders;
    private Long bulgedBulkOrders;
    private Long confirmedBulkOrders;
    private Long bookedBulkOrders;
    private Map<String, Long> ordersByAccount;
    private Map<String, Long> ordersByFund;
    private List<FundMetric> topFunds;
    private Long totalAccounts;
    private Long lastUpdatedTimestamp;

    // Default constructor
    public DashboardView() {
    }

    // Builder pattern for convenient construction
    public static class Builder {
        private Long totalOrders = 0L;
        private Long plannedOrders = 0L;
        private Long validatedOrders = 0L;
        private Long confirmedOrders = 0L;
        private Long bookedOrders = 0L;
        private Long erroredOrders = 0L;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private BigDecimal totalQuantity = BigDecimal.ZERO;
        private Long totalBulkOrders = 0L;
        private Long bulgedBulkOrders = 0L;
        private Long confirmedBulkOrders = 0L;
        private Long bookedBulkOrders = 0L;
        private Map<String, Long> ordersByAccount;
        private Map<String, Long> ordersByFund;
        private List<FundMetric> topFunds;
        private Long totalAccounts = 0L;
        private Long lastUpdatedTimestamp;

        public Builder totalOrders(Long val) { this.totalOrders = val; return this; }
        public Builder plannedOrders(Long val) { this.plannedOrders = val; return this; }
        public Builder validatedOrders(Long val) { this.validatedOrders = val; return this; }
        public Builder confirmedOrders(Long val) { this.confirmedOrders = val; return this; }
        public Builder bookedOrders(Long val) { this.bookedOrders = val; return this; }
        public Builder erroredOrders(Long val) { this.erroredOrders = val; return this; }
        public Builder totalAmount(BigDecimal val) { this.totalAmount = val; return this; }
        public Builder totalQuantity(BigDecimal val) { this.totalQuantity = val; return this; }
        public Builder totalBulkOrders(Long val) { this.totalBulkOrders = val; return this; }
        public Builder bulgedBulkOrders(Long val) { this.bulgedBulkOrders = val; return this; }
        public Builder confirmedBulkOrders(Long val) { this.confirmedBulkOrders = val; return this; }
        public Builder bookedBulkOrders(Long val) { this.bookedBulkOrders = val; return this; }
        public Builder ordersByAccount(Map<String, Long> val) { this.ordersByAccount = val; return this; }
        public Builder ordersByFund(Map<String, Long> val) { this.ordersByFund = val; return this; }
        public Builder topFunds(List<FundMetric> val) { this.topFunds = val; return this; }
        public Builder totalAccounts(Long val) { this.totalAccounts = val; return this; }
        public Builder lastUpdatedTimestamp(Long val) { this.lastUpdatedTimestamp = val; return this; }

        public DashboardView build() {
            DashboardView view = new DashboardView();
            view.totalOrders = this.totalOrders;
            view.plannedOrders = this.plannedOrders;
            view.validatedOrders = this.validatedOrders;
            view.confirmedOrders = this.confirmedOrders;
            view.bookedOrders = this.bookedOrders;
            view.erroredOrders = this.erroredOrders;
            view.totalAmount = this.totalAmount;
            view.totalQuantity = this.totalQuantity;
            view.totalBulkOrders = this.totalBulkOrders;
            view.bulgedBulkOrders = this.bulgedBulkOrders;
            view.confirmedBulkOrders = this.confirmedBulkOrders;
            view.bookedBulkOrders = this.bookedBulkOrders;
            view.ordersByAccount = this.ordersByAccount;
            view.ordersByFund = this.ordersByFund;
            view.topFunds = this.topFunds;
            view.totalAccounts = this.totalAccounts;
            view.lastUpdatedTimestamp = this.lastUpdatedTimestamp;
            return view;
        }
    }

    // Getters and setters
    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }

    public Long getPlannedOrders() { return plannedOrders; }
    public void setPlannedOrders(Long plannedOrders) { this.plannedOrders = plannedOrders; }

    public Long getValidatedOrders() { return validatedOrders; }
    public void setValidatedOrders(Long validatedOrders) { this.validatedOrders = validatedOrders; }

    public Long getConfirmedOrders() { return confirmedOrders; }
    public void setConfirmedOrders(Long confirmedOrders) { this.confirmedOrders = confirmedOrders; }

    public Long getBookedOrders() { return bookedOrders; }
    public void setBookedOrders(Long bookedOrders) { this.bookedOrders = bookedOrders; }

    public Long getErroredOrders() { return erroredOrders; }
    public void setErroredOrders(Long erroredOrders) { this.erroredOrders = erroredOrders; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal totalQuantity) { this.totalQuantity = totalQuantity; }

    public Long getTotalBulkOrders() { return totalBulkOrders; }
    public void setTotalBulkOrders(Long totalBulkOrders) { this.totalBulkOrders = totalBulkOrders; }

    public Long getBulgedBulkOrders() { return bulgedBulkOrders; }
    public void setBulgedBulkOrders(Long bulgedBulkOrders) { this.bulgedBulkOrders = bulgedBulkOrders; }

    public Long getConfirmedBulkOrders() { return confirmedBulkOrders; }
    public void setConfirmedBulkOrders(Long confirmedBulkOrders) { this.confirmedBulkOrders = confirmedBulkOrders; }

    public Long getBookedBulkOrders() { return bookedBulkOrders; }
    public void setBookedBulkOrders(Long bookedBulkOrders) { this.bookedBulkOrders = bookedBulkOrders; }

    public Map<String, Long> getOrdersByAccount() { return ordersByAccount; }
    public void setOrdersByAccount(Map<String, Long> ordersByAccount) { this.ordersByAccount = ordersByAccount; }

    public Map<String, Long> getOrdersByFund() { return ordersByFund; }
    public void setOrdersByFund(Map<String, Long> ordersByFund) { this.ordersByFund = ordersByFund; }

    public List<FundMetric> getTopFunds() { return topFunds; }
    public void setTopFunds(List<FundMetric> topFunds) { this.topFunds = topFunds; }

    public Long getTotalAccounts() { return totalAccounts; }
    public void setTotalAccounts(Long totalAccounts) { this.totalAccounts = totalAccounts; }

    public Long getLastUpdatedTimestamp() { return lastUpdatedTimestamp; }
    public void setLastUpdatedTimestamp(Long lastUpdatedTimestamp) { this.lastUpdatedTimestamp = lastUpdatedTimestamp; }

    @Override
    public String toString() {
        return "DashboardView{" +
                "totalOrders=" + totalOrders +
                ", confirmedOrders=" + confirmedOrders +
                ", bookedOrders=" + bookedOrders +
                ", erroredOrders=" + erroredOrders +
                ", totalAmount=" + totalAmount +
                ", totalBulkOrders=" + totalBulkOrders +
                ", confirmedBulkOrders=" + confirmedBulkOrders +
                ", bookedBulkOrders=" + bookedBulkOrders +
                '}';
    }

    /**
     * Nested class for fund-level metrics in top funds list
     */
    public static class FundMetric {
        private String fundId;
        private Long orderCount;
        private BigDecimal totalAmount;
        private BigDecimal totalQuantity;
        private BigDecimal nav;

        public FundMetric() {}

        public FundMetric(String fundId, Long orderCount, BigDecimal totalAmount, 
                         BigDecimal totalQuantity, BigDecimal nav) {
            this.fundId = fundId;
            this.orderCount = orderCount;
            this.totalAmount = totalAmount;
            this.totalQuantity = totalQuantity;
            this.nav = nav;
        }

        public String getFundId() { return fundId; }
        public void setFundId(String fundId) { this.fundId = fundId; }

        public Long getOrderCount() { return orderCount; }
        public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

        public BigDecimal getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(BigDecimal totalQuantity) { this.totalQuantity = totalQuantity; }

        public BigDecimal getNav() { return nav; }
        public void setNav(BigDecimal nav) { this.nav = nav; }

        @Override
        public String toString() {
            return "FundMetric{" +
                    "fundId='" + fundId + '\'' +
                    ", orderCount=" + orderCount +
                    ", totalAmount=" + totalAmount +
                    ", totalQuantity=" + totalQuantity +
                    '}';
        }
    }
}
