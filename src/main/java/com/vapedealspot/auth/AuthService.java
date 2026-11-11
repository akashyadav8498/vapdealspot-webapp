package com.vapedealspot.auth;

import com.vapedealspot.user.model.User;
import com.vapedealspot.user.model.UserDetails;
import com.vapedealspot.user.model.UserRole;
import com.vapedealspot.user.repository.UserDetailsRepository;
import com.vapedealspot.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Unified application authentication service.
 * Handles database-backed user authentication and customer registration.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserDetailsRepository userDetailsRepository;
    private final PasswordService passwordService;

    public AuthService(UserRepository userRepository,
                       UserDetailsRepository userDetailsRepository,
                       PasswordService passwordService) {
        this.userRepository = userRepository;
        this.userDetailsRepository = userDetailsRepository;
        this.passwordService = passwordService;
    }

    /**
     * Authenticates a user against unified database records.
     * Supports BCrypt and PBKDF2 accounts through self-describing encoded passwords.
     *
     * @param username    email or login username
     * @param rawPassword plaintext password attempt
     * @return Optional containing the authenticated User, or empty if authentication fails
     */
    @Transactional
    public Optional<User> authenticate(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            return Optional.empty();
        }

        String normalizedUsername = username.trim().toLowerCase();
        if (normalizedUsername.isEmpty() || rawPassword.isEmpty()) {
            return Optional.empty();
        }

        Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(normalizedUsername);
        if (userOpt.isEmpty()) {
            log.warn("Authentication failed: user '{}' not found", normalizedUsername);
            return Optional.empty();
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            log.warn("Authentication failed: user '{}' is not ACTIVE (status: {})", normalizedUsername, user.getStatus());
            return Optional.empty();
        }

        boolean valid = passwordService.verifyPassword(rawPassword, user);
        if (!valid) {
            log.warn("Authentication failed: invalid credentials for user '{}'", normalizedUsername);
            return Optional.empty();
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User '{}' ({}) successfully authenticated", normalizedUsername, user.getRole());
        return Optional.of(user);
    }

    /**
     * Registers a new customer account.
     * Atomically creates and persists User (role: CUSTOMER, status: ACTIVE) and UserDetails.
     *
     * @param firstName   customer first name
     * @param lastName    customer last name
     * @param email       customer email address (used as username)
     * @param rawPassword plaintext password
     * @param phone       optional phone number
     * @return the persisted User with its associated UserDetails
     */
    @Transactional
    public User registerCustomer(String firstName, String lastName, String email, String rawPassword, String phone) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        String normalizedEmail = email.trim().toLowerCase();
        String normalizedFirstName = firstName.trim();
        String normalizedLastName = lastName.trim();
        String normalizedPhone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : null;

        if (userRepository.existsByUsernameIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }
        if (userDetailsRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        String encodedPassword = passwordService.encodePassword(rawPassword);
        String userId = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        User user = new User();
        user.setId(userId);
        user.setUsername(normalizedEmail);
        user.setPassword(encodedPassword);
        user.setRole(UserRole.CUSTOMER);
        user.setStatus("ACTIVE");
        user.setSessionVersion(1);

        UserDetails details = new UserDetails();
        details.setUserId(userId);
        details.setUser(user);
        details.setFirstName(normalizedFirstName);
        details.setLastName(normalizedLastName);
        details.setEmail(normalizedEmail);
        details.setPhone(normalizedPhone);

        user.setUserDetails(details);

        try {
            User saved = userRepository.save(user);
            log.info("Customer registered successfully with ID '{}' and email '{}'", userId, normalizedEmail);
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.warn("Database constraint collision during customer registration for email '{}': {}", normalizedEmail, e.getMessage());
            throw new IllegalArgumentException("An account with this email already exists", e);
        }
    }

    /**
     * Overload for customer registration without phone.
     */
    @Transactional
    public User registerCustomer(String firstName, String lastName, String email, String rawPassword) {
        return registerCustomer(firstName, lastName, email, rawPassword, null);
    }

    /**
     * Look up an active user by ID.
     */
    @Transactional(readOnly = true)
    public Optional<User> findActiveUserById(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findById(userId)
                .filter(User::isActive);
    }
}

