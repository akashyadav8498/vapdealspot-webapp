package com.vapedealspot.user.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Unified user entity mapping the 'users' table.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "username", length = 100, nullable = false, unique = true)
    private String username;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 32, nullable = false)
    private UserRole role;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "session_version", nullable = false)
    private Integer sessionVersion = 1;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private UserDetails userDetails;

    public User() {
    }

    public User(String id, String username, String password, UserRole role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.status = "ACTIVE";
        this.sessionVersion = 1;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSessionVersion() {
        return sessionVersion;
    }

    public void setSessionVersion(Integer sessionVersion) {
        this.sessionVersion = sessionVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public UserDetails getUserDetails() {
        return userDetails;
    }

    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
        if (userDetails != null && userDetails.getUser() != this) {
            userDetails.setUser(this);
        }
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }

    public boolean isSuperAdmin() {
        return UserRole.SUPER_ADMIN == this.role;
    }

    public boolean isAdmin() {
        return UserRole.ADMIN == this.role;
    }

    public boolean isCustomer() {
        return UserRole.CUSTOMER == this.role;
    }

    public boolean hasAdminPrivilege() {
        return this.role == UserRole.SUPER_ADMIN || this.role == UserRole.ADMIN;
    }

    public String getEmail() {
        return userDetails != null ? userDetails.getEmail() : username;
    }

    public String getFirstName() {
        return userDetails != null ? userDetails.getFirstName() : "";
    }

    public String getLastName() {
        return userDetails != null ? userDetails.getLastName() : "";
    }

    public String getPhone() {
        return userDetails != null && userDetails.getPhone() != null ? userDetails.getPhone() : "";
    }

    public String getFullName() {
        if (userDetails != null) {
            String name = (userDetails.getFirstName() + " " + userDetails.getLastName()).trim();
            if (!name.isEmpty()) {
                return name;
            }
        }
        return username;
    }

    public java.util.Set<String> getRoleSlugs() {
        if (this.role == null) {
            return java.util.Collections.emptySet();
        }
        return java.util.Set.of(this.role.name().toLowerCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", role=" + role +
                ", status='" + status + '\'' +
                ", sessionVersion=" + sessionVersion +
                ", lastLoginAt=" + lastLoginAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

