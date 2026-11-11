package com.vapedealspot.storefront;

import com.vapedealspot.common.dto.SystemInfoRecord;
import com.vapedealspot.common.dto.VerificationItemRecord;
import com.vapedealspot.common.model.SystemStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@Profile("dev")
public class FoundationVerificationController {

    private final FoundationService foundationService;

    public FoundationVerificationController(FoundationService foundationService) {
        this.foundationService = foundationService;
    }

    @GetMapping("/foundation/verification")
    public String showVerificationPage(Model model) {
        SystemInfoRecord sysInfo = foundationService.getSystemInfo();
        SystemStatus status = foundationService.evaluateSystemStatus();
        List<VerificationItemRecord> checklist = foundationService.getChecklist();

        model.addAttribute("pageTitle", "Milestone 15 Foundation & Verification");
        model.addAttribute("systemInfo", sysInfo);
        model.addAttribute("systemStatus", status);
        model.addAttribute("checklist", checklist);

        return "foundation/verification";
    }
}

