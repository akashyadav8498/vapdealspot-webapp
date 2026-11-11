package com.vapedealspot.auth;

import com.vapedealspot.user.model.User;
import com.vapedealspot.user.model.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * Single application-wide request interceptor.
 * Coordinates static resource bypass, CSRF token validation,
 * session authentication, and role-based route authorization.
 */
@Component
public class RequestInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestInterceptor.class);

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final AuthSessionService authSessionService;

    public RequestInterceptor(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod() != null ? request.getMethod().toUpperCase() : "GET";
        boolean isSafeMethod = SAFE_METHODS.contains(method);

        // 1. Static Resource Bypass
        if (isStaticResource(uri)) {
            return true;
        }

        // 2. Session & CSRF Token Management
        // For safe requests, ensure session and CSRF token exist so forms can render with _csrf
        if (isSafeMethod) {
            HttpSession session = request.getSession(true);
            String token = authSessionService.getOrCreateCsrfToken(session);
            request.setAttribute(AuthSessionService.REQ_ATTR_CSRF, new CsrfToken(token));
        }

        // 3. State-Changing Requests: CSRF Token Validation
        if (!isSafeMethod) {
            String submittedToken = request.getHeader(CsrfToken.DEFAULT_HEADER_NAME);
            if (submittedToken == null || submittedToken.isBlank()) {
                submittedToken = request.getParameter(CsrfToken.DEFAULT_PARAM_NAME);
            }

            HttpSession currentSession = request.getSession(false);
            if (!authSessionService.validateCsrfToken(currentSession, submittedToken)) {
                log.warn("CSRF validation failed for {} {} from IP: {}", method, uri, request.getRemoteAddr());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token");
                return false;
            }
        }

        // 4. Resolve Authenticated User (single DB query per request, cached in request attributes)
        Optional<User> currentUserOpt = authSessionService.getAuthenticatedUser(request);

        // 5. Route-Based Authorization
        if (uri.startsWith("/admin")) {
            return handleAdminAuthorization(request, response, uri, currentUserOpt);
        }

        // All non-admin public storefront endpoints pass through
        return true;
    }

    private boolean handleAdminAuthorization(HttpServletRequest request,
                                            HttpServletResponse response,
                                            String uri,
                                            Optional<User> userOpt) throws Exception {
        // /admin/login is the public administration login page
        if ("/admin/login".equals(uri)) {
            if (userOpt.isPresent() && userOpt.get().hasAdminPrivilege()) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                return false;
            }
            return true;
        }

        // All other /admin/** routes require authentication
        if (userOpt.isEmpty()) {
            String continueParam = "";
            if ("GET".equalsIgnoreCase(request.getMethod()) && !"/admin/logout".equals(uri)) {
                continueParam = "?continue=" + URLEncoder.encode(uri, StandardCharsets.UTF_8);
            }
            response.sendRedirect(request.getContextPath() + "/admin/login" + continueParam);
            return false;
        }

        User user = userOpt.get();

        // CUSTOMER accounts are strictly forbidden from the administration panel
        if (user.getRole() == UserRole.CUSTOMER) {
            log.warn("Access denied: Customer '{}' attempted to access administrative route '{}'",
                    user.getUsername(), uri);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden: Administrative access required");
            return false;
        }

        // ADMIN Role Restrictions
        if (user.getRole() == UserRole.ADMIN) {
            // ADMIN cannot publish or restore Page Builder revisions
            if ("/admin/page-builder/publish".equals(uri) || "/admin/page-builder/restore".equals(uri)) {
                log.warn("Access denied: Admin '{}' attempted Super Admin Page Builder operation on '{}'",
                        user.getUsername(), uri);
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Forbidden: Super Admin privileges required for this action");
                return false;
            }

            // ADMIN cannot access legacy roles management
            if (uri.startsWith("/admin/roles")) {
                log.warn("Access denied: Admin '{}' attempted to access obsolete roles route '{}'",
                        user.getUsername(), uri);
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Forbidden: Dynamic role management is disabled");
                return false;
            }
        }

        // SUPER_ADMIN has full authorized access to administrative routes
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           @Nullable ModelAndView modelAndView) {
        if (modelAndView != null) {
            // Expose CSRF token to views
            Object csrf = request.getAttribute(AuthSessionService.REQ_ATTR_CSRF);
            if (csrf != null && !modelAndView.getModel().containsKey(AuthSessionService.REQ_ATTR_CSRF)) {
                modelAndView.addObject(AuthSessionService.REQ_ATTR_CSRF, csrf);
            }

            // Expose authenticated user to views
            Object currentUser = request.getAttribute(AuthSessionService.REQ_ATTR_CURRENT_USER);
            if (currentUser != null && !modelAndView.getModel().containsKey(AuthSessionService.REQ_ATTR_CURRENT_USER)) {
                modelAndView.addObject(AuthSessionService.REQ_ATTR_CURRENT_USER, currentUser);
            }

            // Expose user details to views where available
            Object details = request.getAttribute(AuthSessionService.REQ_ATTR_CURRENT_USER_DETAILS);
            if (details != null && !modelAndView.getModel().containsKey(AuthSessionService.REQ_ATTR_CURRENT_USER_DETAILS)) {
                modelAndView.addObject(AuthSessionService.REQ_ATTR_CURRENT_USER_DETAILS, details);
            }
        }
    }

    private boolean isStaticResource(String uri) {
        if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/")
                || uri.startsWith("/demo-assets/") || "/favicon.ico".equals(uri) || "/error".equals(uri)) {
            return true;
        }
        String lower = uri.toLowerCase();
        return lower.endsWith(".css") || lower.endsWith(".js")
                || lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".svg") || lower.endsWith(".gif") || lower.endsWith(".ico")
                || lower.endsWith(".woff") || lower.endsWith(".woff2") || lower.endsWith(".ttf");
    }
}

