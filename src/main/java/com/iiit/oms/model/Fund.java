package com.iiit.oms.model;

import java.math.BigDecimal;

public class Fund {
    private String fundID;
    private String fundName;
    private String fundFamily;
    private BigDecimal NAV;

    public Fund() {
    }

    public Fund(String fundID, String fundName, String fundFamily, BigDecimal NAV) {
        this.fundID = fundID;
        this.fundName = fundName;
        this.fundFamily = fundFamily;
        this.NAV = NAV;
    }

    public String getFundID() {
        return fundID;
    }

    public void setFundID(String fundID) {
        this.fundID = fundID;
    }

    public String getFundName() {
        return fundName;
    }

    public void setFundName(String fundName) {
        this.fundName = fundName;
    }

    public String getFundFamily() {
        return fundFamily;
    }

    public void setFundFamily(String fundFamily) {
        this.fundFamily = fundFamily;
    }

    public BigDecimal getNAV() {
        return NAV;
    }

    public void setNAV(BigDecimal NAV) {
        this.NAV = NAV;
    }

    @Override
    public String toString() {
        return "Fund{" +
                "fundID='" + fundID + '\'' +
                ", fundName='" + fundName + '\'' +
                ", fundFamily='" + fundFamily + '\'' +
                ", NAV=" + NAV +
                '}';
    }
}
