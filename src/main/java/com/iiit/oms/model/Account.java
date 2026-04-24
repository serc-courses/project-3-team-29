package com.iiit.oms.model;

public class Account {
    private String accountID;
    private String accountName;
    private String nationalIdentity;
    private java.math.BigDecimal cashBalance = new java.math.BigDecimal("1000000.00");

    public Account() {
    }

    public Account(String accountID, String accountName, String nationalIdentity) {
        this.accountID = accountID;
        this.accountName = accountName;
        this.nationalIdentity = nationalIdentity;
        this.cashBalance = new java.math.BigDecimal("1000000.00");
    }

    public Account(String accountID, String accountName, String nationalIdentity, java.math.BigDecimal cashBalance) {
        this.accountID = accountID;
        this.accountName = accountName;
        this.nationalIdentity = nationalIdentity;
        this.cashBalance = cashBalance != null ? cashBalance : new java.math.BigDecimal("1000000.00");
    }

    public String getAccountID() {
        return accountID;
    }

    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getNationalIdentity() {
        return nationalIdentity;
    }

    public void setNationalIdentity(String nationalIdentity) {
        this.nationalIdentity = nationalIdentity;
    }

    public java.math.BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(java.math.BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountID='" + accountID + '\'' +
                ", accountName='" + accountName + '\'' +
                ", nationalIdentity='" + nationalIdentity + '\'' +
                ", cashBalance=" + cashBalance +
                '}';
    }
}
