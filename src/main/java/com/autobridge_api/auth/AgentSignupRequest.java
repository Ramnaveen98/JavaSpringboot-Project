package com.autobridge_api.auth;

public class AgentSignupRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;

    public String getFirstName() { return firstName; }
    public String getLastName()  { return lastName; }
    public String getEmail()     { return email; }
    public String getPassword()  { return password; }
    public String getPhone()     { return phone; }

    public void setFirstName(String v) { this.firstName = v; }
    public void setLastName(String v)  { this.lastName = v; }
    public void setEmail(String v)     { this.email = v; }
    public void setPassword(String v)  { this.password = v; }
    public void setPhone(String v)     { this.phone = v; }
}
