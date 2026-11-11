package com.vapedealspot.cms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vapedealspot.auth.PageBuilderAction;
import com.vapedealspot.auth.PageBuilderSecurityService;
import com.vapedealspot.cms.dto.*;
import com.vapedealspot.cms.exception.*;
import com.vapedealspot.user.model.User;
import com.vapedealspot.user.model.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/admin/page-builder")
public class PageBuilderController {

    private static final Logger log = LoggerFactory.getLogger(PageBuilderController.class);

    private final PageBuilderService pageBuilderService;
    private final PageBuilderSecurityService pageBuilderSecurityService;
    private final CmsProductResolver cmsProductResolver;
    private final ObjectMapper objectMapper;

    public PageBuilderController(PageBuilderService pageBuilderService,
                                 PageBuilderSecurityService pageBuilderSecurityService,
                                 CmsProductResolver cmsProductResolver,
                                 ObjectMapper objectMapper) {
        this.pageBuilderService = pageBuilderService;
        this.pageBuilderSecurityService = pageBuilderSecurityService;
        this.cmsProductResolver = cmsProductResolver;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Main server-rendered Page Builder editor UI.
     */
    @GetMapping
    public String showEditor(HttpServletRequest request, Model model) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser != null && !pageBuilderSecurityService.canPerform(currentUser, PageBuilderAction.VIEW)) {
            throw new CmsForbiddenException("Insufficient permissions: View permission required for Page Builder.");
        }

        PageDocumentDto draft = pageBuilderService.getDraftHomepage();

        boolean canEdit = pageBuilderSecurityService.canPerform(currentUser, PageBuilderAction.EDIT);
        boolean canPublish = pageBuilderSecurityService.canPerform(currentUser, PageBuilderAction.PUBLISH);

        String userRole = "viewer";
        if (currentUser != null) {
            if (currentUser.getRole() == UserRole.SUPER_ADMIN) {
                userRole = "super_admin";
            } else if (currentUser.getRole() == UserRole.ADMIN) {
                userRole = "admin";
            }
        }

        String documentJson = "{}";
        try {
            documentJson = objectMapper.writeValueAsString(draft);
        } catch (Exception e) {
            log.error("Failed to serialize draft to JSON: {}", e.getMessage());
        }

        model.addAttribute("activeNav", "page-builder");
        model.addAttribute("pageTitle", "Page Builder | Vape Deal Spot");
        model.addAttribute("document", draft);
        model.addAttribute("documentJson", documentJson);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("canPublish", canPublish);
        model.addAttribute("userRole", userRole);

        return "admin/page-builder";
    }

    /**
     * Protected preview route for the saved draft homepage.
     */
    @GetMapping("/preview")
    public String showPreview(HttpServletRequest request, Model model) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser != null && !pageBuilderSecurityService.canPerform(currentUser, PageBuilderAction.VIEW)) {
            throw new CmsForbiddenException("Insufficient permissions: View permission required to preview draft.");
        }

        PageDocumentDto draft = pageBuilderService.getDraftHomepage();

        // Sort active sections
        List<PageSectionDto> activeSections = draft.getSections().stream()
                .filter(PageSectionDto::isEnabled)
                .sorted(Comparator.comparingInt(PageSectionDto::getOrder))
                .toList();

        // Resolve product IDs for product grids
        List<ProductDisplayDto> topSavingsProducts = Collections.emptyList();
        List<ProductDisplayDto> newArrivalProducts = Collections.emptyList();

        for (PageSectionDto section : activeSections) {
            if ("top_savings".equals(section.getType()) && section.getProps() != null) {
                Object pids = section.getProps().get("productIds");
                if (pids instanceof List<?> pidList) {
                    List<String> ids = pidList.stream().map(String::valueOf).toList();
                    topSavingsProducts = cmsProductResolver.resolve(ids);
                }
            } else if ("new_arrivals".equals(section.getType()) && section.getProps() != null) {
                Object pids = section.getProps().get("productIds");
                if (pids instanceof List<?> pidList) {
                    List<String> ids = pidList.stream().map(String::valueOf).toList();
                    newArrivalProducts = cmsProductResolver.resolve(ids);
                }
            }
        }

        model.addAttribute("pageTitle", draft.getSeo() != null && draft.getSeo().getMetaTitle() != null
                ? draft.getSeo().getMetaTitle()
                : "Vape Deal Spot | Best Vape Deals & Coupons");
        model.addAttribute("metaDescription", draft.getSeo() != null && draft.getSeo().getMetaDescription() != null
                ? draft.getSeo().getMetaDescription()
                : "Compare vape prices across 25+ vape retailers.");
        model.addAttribute("document", draft);
        model.addAttribute("draftVersion", draft.getVersion());
        model.addAttribute("sections", activeSections);
        model.addAttribute("topSavingsProducts", topSavingsProducts);
        model.addAttribute("newArrivalProducts", newArrivalProducts);

        return "admin/page-builder-preview";
    }

    /**
     * Save Draft endpoint.
     */
    @PostMapping("/draft")
    @ResponseBody
    public ResponseEntity<?> saveDraft(@RequestBody SaveDraftRequest saveRequest, HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");
        try {
            if (saveRequest == null || saveRequest.getDocument() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "error", "Missing draft document payload"));
            }

            Map<String, Object> result = pageBuilderService.saveDraftHomepage(
                    saveRequest.getDocument(),
                    saveRequest.getExpectedVersion(),
                    currentUser
            );
            return ResponseEntity.ok(result);
        } catch (CmsValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage(),
                            "issues", e.getIssues()
                    ));
        } catch (CmsConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (CmsForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error saving draft: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Internal server error while saving draft"));
        }
    }

    /**
     * Publish Homepage endpoint.
     */
    @PostMapping("/publish")
    @ResponseBody
    public ResponseEntity<?> publishHomepage(@RequestBody(required = false) PublishRequest publishRequest,
                                             HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");
        Integer expectedVersion = publishRequest != null ? publishRequest.getExpectedVersion() : null;

        try {
            Map<String, Object> result = pageBuilderService.publishHomepage(expectedVersion, currentUser);
            return ResponseEntity.ok(result);
        } catch (CmsValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage(),
                            "issues", e.getIssues()
                    ));
        } catch (CmsConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (CmsForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error publishing homepage: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Internal server error while publishing homepage"));
        }
    }

    /**
     * Revision History endpoint.
     */
    @GetMapping("/revisions")
    @ResponseBody
    public ResponseEntity<?> getRevisions(@RequestParam(defaultValue = "20") int limit,
                                          @RequestParam(defaultValue = "0") int offset,
                                          HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser != null && !pageBuilderSecurityService.canPerform(currentUser, PageBuilderAction.VIEW)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "error", "Insufficient permissions"));
        }

        Map<String, Object> revisions = pageBuilderService.listHomepageRevisions(limit, offset);
        return ResponseEntity.ok(revisions);
    }

    /**
     * Restore Historical Revision to Draft endpoint.
     */
    @PostMapping("/restore")
    @ResponseBody
    public ResponseEntity<?> restoreRevision(@RequestBody RestoreRequest restoreRequest, HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");
        if (restoreRequest == null || restoreRequest.getRevisionId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", "Missing revisionId in restore request"));
        }

        try {
            Map<String, Object> result = pageBuilderService.restoreHomepageRevisionToDraft(
                    restoreRequest.getRevisionId(),
                    restoreRequest.getExpectedDraftVersion(),
                    currentUser
            );
            return ResponseEntity.ok(result);
        } catch (CmsNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (CmsConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (CmsForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (CmsValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", e.getMessage(), "issues", e.getIssues()));
        } catch (Exception e) {
            log.error("Error restoring revision: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Internal server error while restoring revision"));
        }
    }
}
