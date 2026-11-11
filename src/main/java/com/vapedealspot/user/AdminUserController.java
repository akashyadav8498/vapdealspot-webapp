package com.vapedealspot.user;

import com.vapedealspot.auth.AuthSessionService;
import com.vapedealspot.user.model.User;
import com.vapedealspot.user.model.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private final UserManagementService userManagementService;
    private final AuthSessionService authSessionService;

    public AdminUserController(UserManagementService userManagementService,
                               AuthSessionService authSessionService) {
        this.userManagementService = userManagementService;
        this.authSessionService = authSessionService;
    }

    @GetMapping
    public String listUsers(@RequestParam(value = "search", required = false) String search,
                            @RequestParam(value = "role", required = false) String role,
                            @RequestParam(value = "status", required = false) String status,
                            HttpServletRequest request,
                            Model model) {
        User currentUser = resolveAuthenticatedUser(request);
        if (currentUser == null) {
            return "redirect:/admin/login";
        }

        List<User> users = userManagementService.listUsers(search, role, status, currentUser);

        // Determine roles available for assignment/filtering based on operator role
        List<UserRole> availableRoles = currentUser.isSuperAdmin()
                ? List.of(UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.CUSTOMER)
                : List.of(UserRole.CUSTOMER);

        model.addAttribute("users", users);
        model.addAttribute("availableRoles", availableRoles);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("selectedRole", role != null ? role : "");
        model.addAttribute("selectedStatus", status != null ? status : "");
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeNav", "users");
        model.addAttribute("adminSectionTitle", "Users Management");
        model.addAttribute("pageTitle", "Users Management | Vape Deal Spot Admin");

        return "admin/users";
    }

    @PostMapping
    public String createUser(@ModelAttribute CreateUserRequest formRequest,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        User currentUser = resolveAuthenticatedUser(request);
        if (currentUser == null) {
            return "redirect:/admin/login";
        }

        try {
            User created = userManagementService.createUser(formRequest, currentUser);
            redirectAttributes.addFlashAttribute("successMessage",
                    "User '" + created.getUsername() + "' created successfully with role " + created.getRole() + ".");
        } catch (Exception e) {
            log.error("Failed to create user: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/update")
    public String updateUser(@PathVariable("id") String id,
                             @ModelAttribute UpdateUserRequest formRequest,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        User currentUser = resolveAuthenticatedUser(request);
        if (currentUser == null) {
            return "redirect:/admin/login";
        }

        try {
            User updated = userManagementService.updateUser(id, formRequest, currentUser);
            redirectAttributes.addFlashAttribute("successMessage",
                    "User '" + updated.getUsername() + "' updated successfully.");
        } catch (Exception e) {
            log.error("Failed to update user '{}': {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") String id,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        User currentUser = resolveAuthenticatedUser(request);
        if (currentUser == null) {
            return "redirect:/admin/login";
        }

        try {
            User updated = userManagementService.toggleUserStatus(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage",
                    "User '" + updated.getUsername() + "' status changed to '" + updated.getStatus() + "'.");
        } catch (Exception e) {
            log.error("Failed to toggle status for user '{}': {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable("id") String id,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        User currentUser = resolveAuthenticatedUser(request);
        if (currentUser == null) {
            return "redirect:/admin/login";
        }

        try {
            userManagementService.deleteUser(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "User account deactivated successfully.");
        } catch (Exception e) {
            log.error("Failed to deactivate user '{}': {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable("id") String id,
                                @RequestParam("password") String password,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        User currentUser = resolveAuthenticatedUser(request);
        if (currentUser == null) {
            return "redirect:/admin/login";
        }

        try {
            userManagementService.resetPassword(id, password, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Password reset successfully. Active sessions invalidated.");
        } catch (Exception e) {
            log.error("Failed to reset password for user '{}': {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    private User resolveAuthenticatedUser(HttpServletRequest request) {
        Object cached = request.getAttribute(AuthSessionService.REQ_ATTR_CURRENT_USER);
        if (cached instanceof User user) {
            return user;
        }
        Optional<User> userOpt = authSessionService.getAuthenticatedUser(request);
        return userOpt.orElse(null);
    }
}
