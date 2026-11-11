package com.vapedealspot.user;

import com.vapedealspot.auth.PasswordService;
import com.vapedealspot.user.model.User;
import com.vapedealspot.user.model.UserDetails;
import com.vapedealspot.user.model.UserRole;
import com.vapedealspot.user.repository.UserDetailsRepository;
import com.vapedealspot.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Unified user management service operating exclusively on 'users' and 'user_details'.
 * Implements strict role-based access control, last active Super Admin protection,
 * and automatic session revocation upon sensitive account modifications.
 */
@Service
public class UserManagementService {

    private static final Logger log = LoggerFactory.getLogger(UserManagementService.class);

    private final UserRepository userRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final PasswordService passwordService;

    public UserManagementService(UserRepository userRepository,
                                 UserDetailsRepository userDetailsRepository,
                                 PasswordService passwordService) {
        this.userRepository = userRepository;
        this.userDetailsRepository = userDetailsRepository;
        this.passwordService = passwordService;
    }

    /**
     * Lists users filtered by operator permissions, search keyword, role, and status.
     * ADMIN operators can only see CUSTOMER accounts.
     * SUPER_ADMIN operators can view all accounts.
     */
    @Transactional(readOnly = true)
    public List<User> listUsers(String search, String roleFilter, String statusFilter, User actor) {
        validateOperator(actor);

        List<User> all = userRepository.findAllByOrderByCreatedAtDesc();

        return all.stream()
                .filter(u -> {
                    // ADMIN is strictly restricted to viewing CUSTOMER accounts only
                    if (actor.getRole() == UserRole.ADMIN) {
                        if (u.getRole() != UserRole.CUSTOMER) {
                            return false;
                        }
                    } else if (actor.getRole() == UserRole.SUPER_ADMIN) {
                        // SUPER_ADMIN can optionally filter by role
                        if (roleFilter != null && !roleFilter.isBlank()) {
                            UserRole parsedRole = UserRole.fromString(roleFilter);
                            if (parsedRole != null && u.getRole() != parsedRole) {
                                return false;
                            }
                        }
                    }

                    // Status filter
                    if (statusFilter != null && !statusFilter.isBlank()) {
                        if (!statusFilter.equalsIgnoreCase(u.getStatus())) {
                            return false;
                        }
                    }

                    // Search filter: checks full name, email, username, phone
                    if (search != null && !search.isBlank()) {
                        String s = search.trim().toLowerCase();
                        boolean matchName = u.getFullName().toLowerCase().contains(s);
                        boolean matchEmail = u.getEmail().toLowerCase().contains(s);
                        boolean matchUsername = u.getUsername().toLowerCase().contains(s);
                        boolean matchPhone = u.getPhone().toLowerCase().contains(s);
                        if (!matchName && !matchEmail && !matchUsername && !matchPhone) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Retrieves an individual user profile with permission enforcement.
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(String id, User actor) {
        validateOperator(actor);

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User target = userOpt.get();
        if (actor.getRole() == UserRole.ADMIN && target.getRole() != UserRole.CUSTOMER) {
            throw new SecurityException("Admins are not permitted to view staff accounts.");
        }

        return Optional.of(target);
    }

    /**
     * Creates a new user and associated user_details record atomically.
     * ADMIN operators can only create CUSTOMER accounts.
     * SUPER_ADMIN operators can create CUSTOMER, ADMIN, or SUPER_ADMIN accounts.
     */
    @Transactional
    public User createUser(CreateUserRequest request, User actor) {
        validateOperator(actor);
        validateUserDetails(request.getFirstName(), request.getLastName(), request.getEmail());

        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Initial password must be at least 8 characters.");
        }

        UserRole targetRole;
        if (actor.getRole() == UserRole.ADMIN) {
            // ADMIN cannot create ADMIN or SUPER_ADMIN accounts
            String requestedRole = request.getRole();
            if (requestedRole != null && !requestedRole.isBlank()) {
                UserRole parsed = UserRole.fromString(requestedRole);
                if (parsed != null && parsed != UserRole.CUSTOMER) {
                    throw new SecurityException("Admins cannot create privileged staff accounts.");
                }
            }
            targetRole = UserRole.CUSTOMER;
        } else {
            // SUPER_ADMIN can assign any permitted role
            UserRole parsed = UserRole.fromString(request.getRole());
            targetRole = parsed != null ? parsed : UserRole.CUSTOMER;
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByUsernameIgnoreCase(normalizedEmail)
                || userDetailsRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("An account with email '" + normalizedEmail + "' already exists.");
        }

        String userId = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String encodedPassword = passwordService.encodePassword(request.getPassword());
        String status = "inactive".equalsIgnoreCase(request.getStatus()) ? "INACTIVE" : "ACTIVE";

        User user = new User(userId, normalizedEmail, encodedPassword, targetRole);
        user.setStatus(status);
        user.setSessionVersion(1);

        UserDetails details = new UserDetails(user, request.getFirstName().trim(), request.getLastName().trim(), normalizedEmail);
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            details.setPhone(request.getPhone().trim());
        }

        user.setUserDetails(details);

        User saved = userRepository.save(user);
        log.info("Created user account '{}' ({}, role: {}) by operator '{}'",
                saved.getUsername(), saved.getId(), saved.getRole(), actor.getUsername());
        return saved;
    }

    /**
     * Updates an existing user profile with privilege protection and last active Super Admin defense.
     */
    @Transactional
    public User updateUser(String id, UpdateUserRequest request, User actor) {
        validateOperator(actor);
        validateUserDetails(request.getFirstName(), request.getLastName(), request.getEmail());

        User target = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        // Authorization checks
        if (actor.getRole() == UserRole.ADMIN) {
            if (target.getRole() != UserRole.CUSTOMER) {
                throw new SecurityException("Admins cannot modify staff accounts.");
            }
            if (target.getId().equals(actor.getId())) {
                throw new SecurityException("Admins cannot modify their own account through user management.");
            }
            String reqRole = request.getRole();
            if (reqRole != null && !reqRole.isBlank()) {
                UserRole parsed = UserRole.fromString(reqRole);
                if (parsed != null && parsed != UserRole.CUSTOMER) {
                    throw new SecurityException("Admins cannot promote accounts to staff roles.");
                }
            }
        }

        // Email uniqueness verification
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (!target.getUsername().equalsIgnoreCase(normalizedEmail)) {
            if (userRepository.existsByUsernameIgnoreCaseAndIdNot(normalizedEmail, id)
                    || userDetailsRepository.existsByEmailIgnoreCaseAndUserIdNot(normalizedEmail, id)) {
                throw new IllegalArgumentException("Email '" + normalizedEmail + "' is already in use by another account.");
            }
            target.setUsername(normalizedEmail);
            if (target.getUserDetails() != null) {
                target.getUserDetails().setEmail(normalizedEmail);
            }
        }

        // Update basic details
        if (target.getUserDetails() != null) {
            target.getUserDetails().setFirstName(request.getFirstName().trim());
            target.getUserDetails().setLastName(request.getLastName().trim());
            if (request.getPhone() != null) {
                target.getUserDetails().setPhone(request.getPhone().trim().isEmpty() ? null : request.getPhone().trim());
            }
        }

        // Role update logic
        if (actor.getRole() == UserRole.SUPER_ADMIN) {
            UserRole newRole = UserRole.fromString(request.getRole());
            if (newRole != null && newRole != target.getRole()) {
                // Last active Super Admin demotion protection
                if (target.isSuperAdmin() && target.isActive()) {
                    long activeSuperAdmins = userRepository.countByRoleAndStatus(UserRole.SUPER_ADMIN, "ACTIVE");
                    if (activeSuperAdmins <= 1) {
                        throw new IllegalStateException("Cannot change role: this is the last active Super Admin.");
                    }
                }
                target.setRole(newRole);
                target.setSessionVersion(target.getSessionVersion() + 1);
                log.info("Role changed to {} for user '{}' by operator '{}'",
                        newRole, target.getUsername(), actor.getUsername());
            }
        }

        // Status update logic
        String newStatus = "inactive".equalsIgnoreCase(request.getStatus()) ? "INACTIVE" : "ACTIVE";
        if (!target.getStatus().equalsIgnoreCase(newStatus)) {
            // Last active Super Admin deactivation protection
            if (target.isSuperAdmin() && target.isActive() && "INACTIVE".equals(newStatus)) {
                long activeSuperAdmins = userRepository.countByRoleAndStatus(UserRole.SUPER_ADMIN, "ACTIVE");
                if (activeSuperAdmins <= 1) {
                    throw new IllegalStateException("Cannot deactivate the last active Super Admin.");
                }
            }
            target.setStatus(newStatus);
            if ("INACTIVE".equals(newStatus)) {
                target.setSessionVersion(target.getSessionVersion() + 1);
            }
            log.info("Status changed to {} for user '{}' by operator '{}'",
                    newStatus, target.getUsername(), actor.getUsername());
        }

        // Optional password reset
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getPassword().length() < 8) {
                throw new IllegalArgumentException("New password must be at least 8 characters.");
            }
            target.setPassword(passwordService.encodePassword(request.getPassword()));
            target.setSessionVersion(target.getSessionVersion() + 1);
            log.info("Password reset for user '{}' by operator '{}'", target.getUsername(), actor.getUsername());
        }

        User saved = userRepository.save(target);
        log.info("Updated user profile '{}' ({}) by operator '{}'", saved.getUsername(), saved.getId(), actor.getUsername());
        return saved;
    }

    /**
     * Toggles active / inactive status of an account.
     */
    @Transactional
    public User toggleUserStatus(String id, User actor) {
        validateOperator(actor);

        User target = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        if (actor.getRole() == UserRole.ADMIN && target.getRole() != UserRole.CUSTOMER) {
            throw new SecurityException("Admins cannot modify staff accounts.");
        }

        if (target.isActive()) {
            // Deactivating
            if (target.isSuperAdmin()) {
                long activeSuperAdmins = userRepository.countByRoleAndStatus(UserRole.SUPER_ADMIN, "ACTIVE");
                if (activeSuperAdmins <= 1) {
                    throw new IllegalStateException("Cannot deactivate the last active Super Admin.");
                }
            }
            target.setStatus("INACTIVE");
            target.setSessionVersion(target.getSessionVersion() + 1);
            log.info("Deactivated user '{}' by operator '{}'", target.getUsername(), actor.getUsername());
        } else {
            // Reactivating
            target.setStatus("ACTIVE");
            log.info("Reactivated user '{}' by operator '{}'", target.getUsername(), actor.getUsername());
        }

        return userRepository.save(target);
    }

    /**
     * Soft-deactivates a user account while protecting the last active Super Admin.
     */
    @Transactional
    public void deleteUser(String id, User actor) {
        validateOperator(actor);

        User target = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        if (target.getId().equals(actor.getId())) {
            throw new IllegalStateException("You cannot delete or deactivate your own active session account.");
        }

        if (actor.getRole() == UserRole.ADMIN && target.getRole() != UserRole.CUSTOMER) {
            throw new SecurityException("Admins cannot modify staff accounts.");
        }

        if (target.isSuperAdmin() && target.isActive()) {
            long activeSuperAdmins = userRepository.countByRoleAndStatus(UserRole.SUPER_ADMIN, "ACTIVE");
            if (activeSuperAdmins <= 1) {
                throw new IllegalStateException("Cannot delete or deactivate the last active Super Admin.");
            }
        }

        // Soft deactivation preserves audit trail and database integrity
        target.setStatus("INACTIVE");
        target.setSessionVersion(target.getSessionVersion() + 1);
        userRepository.save(target);
        log.info("Soft-deactivated user '{}' ({}) by operator '{}'", target.getUsername(), target.getId(), actor.getUsername());
    }

    /**
     * Resets a user's password and invalidates active sessions.
     */
    @Transactional
    public void resetPassword(String id, String newPassword, User actor) {
        validateOperator(actor);

        User target = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        if (actor.getRole() == UserRole.ADMIN && target.getRole() != UserRole.CUSTOMER) {
            throw new SecurityException("Admins cannot modify staff accounts.");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }

        target.setPassword(passwordService.encodePassword(newPassword));
        target.setSessionVersion(target.getSessionVersion() + 1);
        userRepository.save(target);
        log.info("Password successfully reset for user '{}' by operator '{}'", target.getUsername(), actor.getUsername());
    }

    private void validateOperator(User actor) {
        if (actor == null || !actor.hasAdminPrivilege()) {
            throw new SecurityException("Forbidden: Administrative privilege required.");
        }
    }

    private void validateUserDetails(String firstName, String lastName, String email) {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required.");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required.");
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
    }
}

