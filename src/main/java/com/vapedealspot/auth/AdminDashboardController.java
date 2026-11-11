package com.vapedealspot.auth;

import com.vapedealspot.user.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(HttpServletRequest request, Model model) {
        User currentUser = (User) request.getAttribute("currentUser");
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("adminSectionTitle", "System Overview");
        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("currentUser", currentUser);
        return "admin/dashboard";
    }
}

