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

import java.util.Optional;

/**
 * Customer authentication controller for storefront customers.
 * Handles customer login, self-registration, and logout.
 */
@Controller
public class CustomerAuthController {

    private static final Logger log = LoggerFactory.getLogger(CustomerAuthController.class);

    private final AuthService authService;
    private final AuthSessionService authSessionService;

    public CustomerAuthController(AuthService authService, AuthSessionService authSessionService) {
        this.authService = authService;
        this.authSessionService = authSessionService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(name = "continue", required = false) String continueParam,
                            HttpServletRequest request,
                            Model model) {
        Optional<User> currentUser = authSessionService.getAuthenticatedUser(request);
        if (currentUser.isPresent()) {
            if (currentUser.get().isCustomer()) {
                return "redirect:/";
            } else if (currentUser.get().hasAdminPrivilege()) {
                return "redirect:/admin/dashboard";
            }
        }

        if (continueParam != null && !continueParam.isBlank()) {
            model.addAttribute("continueUrl", continueParam.trim());
        }
        model.addAttribute("pageTitle", "Customer Sign In | Vape Deal Spot");
        return "storefront/login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam("email") String email,
                              @RequestParam("password") String password,
                              @RequestParam(name = "continue", required = false) String continueParam,
                              HttpServletRequest request,
                              Model model) {
        Optional<User> userOpt = authService.authenticate(email, password);

        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Invalid email address or password. Please try again.");
            model.addAttribute("email", email != null ? email.trim() : "");
            if (continueParam != null && !continueParam.isBlank()) {
                model.addAttribute("continueUrl", continueParam.trim());
            }
            model.addAttribute("pageTitle", "Customer Sign In | Vape Deal Spot");
            return "storefront/login";
        }

        User user = userOpt.get();

        // Customer login allows CUSTOMER role only
        if (user.getRole() != UserRole.CUSTOMER) {
            log.warn("Non-customer '{}' ({}) attempted customer login", user.getUsername(), user.getRole());
            model.addAttribute("error", "Staff accounts must sign in via the Administration portal.");
            model.addAttribute("email", email != null ? email.trim() : "");
            model.addAttribute("pageTitle", "Customer Sign In | Vape Deal Spot");
            return "storefront/login";
        }

        // Establish fresh session
        authSessionService.establishSession(request, user);

        // Safe redirect
        if (isSafeCustomerRedirect(continueParam)) {
            return "redirect:" + continueParam.trim();
        }

        return "redirect:/";
    }

    @GetMapping("/register")
    public String registerPage(HttpServletRequest request, Model model) {
        Optional<User> currentUser = authSessionService.getAuthenticatedUser(request);
        if (currentUser.isPresent()) {
            return "redirect:/";
        }

        model.addAttribute("pageTitle", "Create an Account | Vape Deal Spot");
        return "storefront/register";
    }

    @PostMapping("/register")
    public String registerSubmit(@RequestParam("firstName") String firstName,
                                 @RequestParam("lastName") String lastName,
                                 @RequestParam("email") String email,
                                 @RequestParam("password") String password,
                                 @RequestParam(name = "confirmPassword", required = false) String confirmPassword,
                                 @RequestParam(name = "phone", required = false) String phone,
                                 HttpServletRequest request,
                                 Model model) {
        // Confirm password check
        if (confirmPassword != null && !confirmPassword.equals(password)) {
            model.addAttribute("error", "Passwords do not match. Please re-enter.");
            populateFormModel(model, firstName, lastName, email, phone);
            return "storefront/register";
        }

        if (password == null || password.length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters long.");
            populateFormModel(model, firstName, lastName, email, phone);
            return "storefront/register";
        }

        try {
            User customer = authService.registerCustomer(firstName, lastName, email, password, phone);
            // Automatically establish session for the newly registered customer
            authSessionService.establishSession(request, customer);
            return "redirect:/?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            populateFormModel(model, firstName, lastName, email, phone);
            return "storefront/register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        authSessionService.invalidateSession(request);
        return "redirect:/?logout=true";
    }

    private void populateFormModel(Model model, String firstName, String lastName, String email, String phone) {
        model.addAttribute("firstName", firstName != null ? firstName.trim() : "");
        model.addAttribute("lastName", lastName != null ? lastName.trim() : "");
        model.addAttribute("email", email != null ? email.trim() : "");
        model.addAttribute("phone", phone != null ? phone.trim() : "");
        model.addAttribute("pageTitle", "Create an Account | Vape Deal Spot");
    }

    /**
     * Validates that the customer redirect target is a safe relative public route.
     * Rejects absolute URLs, protocol-relative (//), path traversal, and /admin/** paths.
     */
    public static boolean isSafeCustomerRedirect(String target) {
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
        // Customers must NEVER be redirected into administration
        if (path.startsWith("/admin")) {
            return false;
        }
        if (path.equals("/login") || path.equals("/register") || path.equals("/logout")) {
            return false;
        }
        return true;
    }
}

