package com.vapedealspot.auth;

import com.vapedealspot.user.model.User;
import com.vapedealspot.user.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("pageBuilderSecurity")
public class PageBuilderSecurityService {

    private static final Logger log = LoggerFactory.getLogger(PageBuilderSecurityService.class);

    @Value("${pagebuilder.allowed-roles:#{null}}")
    private String allowedRolesEnv;

    public PageBuilderSecurityService() {}

    /**
     * Unified 3-role authorization check for Page Builder operations.
     * SUPER_ADMIN: Full access including Publish and Restore.
     * ADMIN: View, create and edit drafts, preview drafts; CANNOT publish or restore.
     * CUSTOMER: No Page Builder access.
     */
    public boolean canPerform(User user, PageBuilderAction action) {
        if (user == null || !user.isActive() || action == null) {
            return false;
        }

        // Restrictive allowlist from environment variable if defined
        if (allowedRolesEnv != null) {
            String trimmed = allowedRolesEnv.trim();
            if (trimmed.isEmpty()) {
                log.warn("PAGE_BUILDER_ALLOWED_ROLES is defined but empty; failing closed");
                return false;
            }
            Set<String> envAllowlist = new HashSet<>(Arrays.asList(trimmed.split("\\s*,\\s*")));
            boolean hasAllowedRole = user.getRoleSlugs().stream().anyMatch(envAllowlist::contains);
            if (!hasAllowedRole) {
                return false;
            }
        }

        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        if (user.getRole() == UserRole.ADMIN) {
            return action == PageBuilderAction.VIEW
                    || action == PageBuilderAction.EDIT
                    || action == PageBuilderAction.MANAGE_MEDIA;
        }
        return false;
    }
}
