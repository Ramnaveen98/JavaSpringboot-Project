package com.autobridge_api.profile;

public class ProfileDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;

    public ProfileDto() {}
    public ProfileDto(Long id, String email, String firstName, String lastName, String phone) {
        this.id = id; this.email = email; this.firstName = firstName; this.lastName = lastName; this.phone = phone;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }

    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setPhone(String phone) { this.phone = phone; }
}
