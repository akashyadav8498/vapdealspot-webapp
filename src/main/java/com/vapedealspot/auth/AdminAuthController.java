package com.vapedealspot.auth;

import com.vapedealspot.user.model.User;
import com.vapedealspot.user.model.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Unified administrative authentication controller.
 * Handles login, logout, and safe redirect navigation for ADMIN and SUPER_ADMIN roles.
 */
@Controller
public class AdminAuthController {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthController.class);

    private final AuthService authService;
    private final AuthSessionService authSessionService;

    public AdminAuthController(AuthService authService, AuthSessionService authSessionService) {
        this.authService = authService;
        this.authSessionService = authSessionService;
    }

    @GetMapping("/admin/login")
    public String loginPage(@RequestParam(name = "continue", required = false) String continueParam,
                            HttpServletRequest request,
                            Model model) {
        Optional<User> currentUserOpt = authSessionService.getAuthenticatedUser(request);
        if (currentUserOpt.isPresent() && currentUserOpt.get().hasAdminPrivilege()) {
            return "redirect:/admin/dashboard";
        }
        if (continueParam != null && !continueParam.isBlank()) {
            model.addAttribute("continueUrl", continueParam.trim());
        }
        return "admin/login";
    }

    @PostMapping("/admin/login")
    public String loginSubmit(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(name = "continue", required = false) String continueParam,
            HttpServletRequest request
    ) {
        Optional<User> userOpt = authService.authenticate(email, password);

        // Security contract: failure does not reveal whether username, password, or status failed
        if (userOpt.isEmpty()) {
            String redirectUrl = "/admin/login?error=true";
            if (continueParam != null && !continueParam.isBlank()) {
                redirectUrl += "&continue=" + URLEncoder.encode(continueParam, StandardCharsets.UTF_8);
            }
            return "redirect:" + redirectUrl;
        }

        User user = userOpt.get();

        // CUSTOMER accounts are strictly prohibited from entering administration
        if (user.getRole() == UserRole.CUSTOMER) {
            log.warn("Customer '{}' attempted administration panel login; rejected", user.getUsername());
            return "redirect:/admin/login?error=true";
        }

        // Establish fresh session with fixation protection & new CSRF token
        authSessionService.establishSession(request, user);

        // Safe Open-Redirect Validation
        if (isSafeAdminRedirect(continueParam, user.getRole())) {
            return "redirect:" + continueParam.trim();
        }

        return "redirect:/admin/dashboard";
    }

    @PostMapping("/admin/logout")
    public String logout(HttpServletRequest request) {
        authSessionService.invalidateSession(request);
        return "redirect:/admin/login?logout=true";
    }

    /**
     * Validates that a redirect destination is a safe relative admin path.
     * Rejects absolute URLs, protocol-relative (//), path traversal, and role privilege violations.
     */
    public static boolean isSafeAdminRedirect(String target, UserRole role) {
        if (target == null || target.isBlank()) {
            return false;
        }
        String trimmed = target.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//") || trimmed.startsWith("/\\")) {
            return false;
        }
        int queryIndex = trimmed.indexOf('?');
        String path = queryIndex >= 0 ? trimmed.substring(0, queryIndex) : trimmed;
        if (path.contains(":") || path.contains("\\")) {
            return false;
        }
        if (!path.startsWith("/admin")) {
            return false;
        }
        if (path.equals("/admin/login") || path.equals("/admin/logout")) {
            return false;
        }
        // ADMIN role cannot be redirected to SUPER_ADMIN-exclusive operations
        if (role == UserRole.ADMIN) {
            if (path.startsWith("/admin/roles")
                    || path.equals("/admin/page-builder/publish")
                    || path.equals("/admin/page-builder/restore")) {
                return false;
            }
        }
        return true;
    }
}
