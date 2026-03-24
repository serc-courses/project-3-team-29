package com.iiit.oms.model;

public class Account {
    private String accountID;
    private String accountName;
    private String nationalIdentity;

    public Account() {
    }

    public Account(String accountID, String accountName, String nationalIdentity) {
        this.accountID = accountID;
        this.accountName = accountName;
        this.nationalIdentity = nationalIdentity;
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

    @Override
    public String toString() {
        return "Account{" +
                "accountID='" + accountID + '\'' +
                ", accountName='" + accountName + '\'' +
                ", nationalIdentity='" + nationalIdentity + '\'' +
                '}';
    }
}
