package com.vapedealspot.auth;

import com.vapedealspot.user.model.User;
import com.vapedealspot.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;

/**
 * Unified session lifecycle management service.
 * Handles session fixation protection, session version validation,
 * and constant-time CSRF token generation and verification.
 */
@Service
public class AuthSessionService {

    private static final Logger log = LoggerFactory.getLogger(AuthSessionService.class);

    public static final String SESSION_USER_ID = "AUTH_USER_ID";
    public static final String SESSION_VERSION = "AUTH_SESSION_VERSION";
    public static final String SESSION_CSRF_TOKEN = "CSRF_TOKEN";

    public static final String REQ_ATTR_CURRENT_USER = "currentUser";
    public static final String REQ_ATTR_CURRENT_USER_DETAILS = "currentUserDetails";
    public static final String REQ_ATTR_CSRF = "_csrf";

    private final UserRepository userRepository;

    public AuthSessionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Establishes a fresh authenticated session following successful login.
     * Enforces session fixation protection by invalidating the prior session.
     */
    public void establishSession(HttpServletRequest request, User user) {
        if (request == null || user == null) {
            return;
        }

        // Invalidate old session to prevent session fixation attacks
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        // Create fresh session and assign authentication attributes
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute(SESSION_USER_ID, user.getId());
        newSession.setAttribute(SESSION_VERSION, user.getSessionVersion());

        // Generate fresh CSRF token for the new session
        String csrfToken = UUID.randomUUID().toString();
        newSession.setAttribute(SESSION_CSRF_TOKEN, csrfToken);

        // Cache in request context
        request.setAttribute(REQ_ATTR_CURRENT_USER, user);
        request.setAttribute(REQ_ATTR_CURRENT_USER_DETAILS, user.getUserDetails());
        request.setAttribute(REQ_ATTR_CSRF, new CsrfToken(csrfToken));

        log.debug("Established fresh session for user '{}' with session version {}",
                user.getUsername(), user.getSessionVersion());
    }

    /**
     * Retrieves and verifies the currently authenticated User for this request.
     * Validates active status and compares session version against the database.
     * Caches the resolved user on the request attribute to eliminate redundant queries.
     */
    public Optional<User> getAuthenticatedUser(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }

        // 1. Return cached user attribute if already resolved during this request
        Object cached = request.getAttribute(REQ_ATTR_CURRENT_USER);
        if (cached instanceof User cachedUser) {
            return Optional.of(cachedUser);
        }

        // 2. Inspect session
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        String userId = (String) session.getAttribute(SESSION_USER_ID);
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        // 3. Load user from database
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("Session user ID '{}' no longer exists in database; destroying session", userId);
            destroySession(session);
            return Optional.empty();
        }

        User user = userOpt.get();

        // 4. Verify user is ACTIVE
        if (!user.isActive()) {
            log.warn("Session user '{}' is no longer ACTIVE (status: '{}'); destroying session",
                    user.getUsername(), user.getStatus());
            destroySession(session);
            return Optional.empty();
        }

        // 5. Verify session_version matches (concurrent logout / password reset revocation)
        Integer sessionVersion = (Integer) session.getAttribute(SESSION_VERSION);
        if (sessionVersion == null || !sessionVersion.equals(user.getSessionVersion())) {
            log.warn("Session version mismatch for user '{}' (session: {}, db: {}); destroying session",
                    user.getUsername(), sessionVersion, user.getSessionVersion());
            destroySession(session);
            return Optional.empty();
        }

        // 6. Cache on request for downstream handlers and views
        request.setAttribute(REQ_ATTR_CURRENT_USER, user);
        request.setAttribute(REQ_ATTR_CURRENT_USER_DETAILS, user.getUserDetails());

        return Optional.of(user);
    }

    /**
     * Completely invalidates the session and clears authentication state.
     */
    public void invalidateSession(HttpServletRequest request) {
        if (request == null) {
            return;
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            destroySession(session);
        }
        request.removeAttribute(REQ_ATTR_CURRENT_USER);
        request.removeAttribute(REQ_ATTR_CURRENT_USER_DETAILS);
    }

    /**
     * Safely invalidates a session ignoring already invalidated states.
     */
    private void destroySession(HttpSession session) {
        try {
            session.invalidate();
        } catch (IllegalStateException ignored) {
            // Session was already invalidated
        }
    }

    /**
     * Retrieves or generates a session CSRF token.
     */
    public String getOrCreateCsrfToken(HttpSession session) {
        if (session == null) {
            return UUID.randomUUID().toString();
        }
        String token = (String) session.getAttribute(SESSION_CSRF_TOKEN);
        if (token == null || token.isBlank()) {
            token = UUID.randomUUID().toString();
            session.setAttribute(SESSION_CSRF_TOKEN, token);
        }
        return token;
    }

    /**
     * Constant-time comparison of submitted CSRF token against session token.
     */
    public boolean validateCsrfToken(HttpSession session, String submittedToken) {
        if (session == null || submittedToken == null || submittedToken.isBlank()) {
            return false;
        }
        String sessionToken = (String) session.getAttribute(SESSION_CSRF_TOKEN);
        if (sessionToken == null || sessionToken.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                submittedToken.getBytes(StandardCharsets.UTF_8),
                sessionToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}

