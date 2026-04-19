package com.iiit.oms.model;

public class User {
    private String userID;
    private String username;
    private String password;
    private String role;          // "INVESTOR" or "ADVISOR"
    private String displayName;
    private String accountID;     // set for INVESTOR role
    private String advisorID;     // set for ADVISOR role

    public User() {}

    public User(String userID, String username, String password,
                String role, String displayName, String accountID, String advisorID) {
        this.userID = userID;
        this.username = username;
        this.password = password;
        this.role = role;
        this.displayName = displayName;
        this.accountID = accountID;
        this.advisorID = advisorID;
    }

    public String getUserID()      { return userID; }
    public void setUserID(String v){ this.userID = v; }
    public String getUsername()    { return username; }
    public void setUsername(String v){ this.username = v; }
    public String getPassword()    { return password; }
    public void setPassword(String v){ this.password = v; }
    public String getRole()        { return role; }
    public void setRole(String v)  { this.role = v; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String v){ this.displayName = v; }
    public String getAccountID()   { return accountID; }
    public void setAccountID(String v){ this.accountID = v; }
    public String getAdvisorID()   { return advisorID; }
    public void setAdvisorID(String v){ this.advisorID = v; }
}
