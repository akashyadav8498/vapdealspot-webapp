package com.vapedealspot.user;

public class UpdateUserRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String password;
    private String role;
    private String roleId;
    private String status;

    public UpdateUserRequest() {}

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        if (role != null && !role.isBlank()) {
            return role;
        }
        return roleId;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRoleId() {
        if (roleId != null && !roleId.isBlank()) {
            return roleId;
        }
        return role;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
