package com.autobridge_api.profile;

public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String phone;

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }

    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setPhone(String phone) { this.phone = phone; }
}
