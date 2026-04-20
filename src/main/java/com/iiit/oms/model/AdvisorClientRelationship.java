package com.iiit.oms.model;

public class AdvisorClientRelationship {
    private String advisorID;
    private String accountID;
    private String relationshipStatus;

    public AdvisorClientRelationship() {}

    public AdvisorClientRelationship(String advisorID, String accountID, String relationshipStatus) {
        this.advisorID = advisorID;
        this.accountID = accountID;
        this.relationshipStatus = relationshipStatus;
    }

    public String getAdvisorID() { return advisorID; }
    public void setAdvisorID(String advisorID) { this.advisorID = advisorID; }
    public String getAccountID() { return accountID; }
    public void setAccountID(String accountID) { this.accountID = accountID; }
    public String getRelationshipStatus() { return relationshipStatus; }
    public void setRelationshipStatus(String relationshipStatus) { this.relationshipStatus = relationshipStatus; }
}
