package com.vapedealspot.cms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vapedealspot.cms.dto.*;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class CmsDocumentValidator {

    public static final int CURRENT_CMS_SCHEMA_VERSION = 1;
    public static final int MAX_DOCUMENT_JSON_BYTES = 256 * 1024; // 256 KB

    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile("<script\\b[^<]*(?:(?!</script>)<[^<]*)*</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern SECTION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private static final Set<String> ALLOWED_SECTION_TYPES = Set.of(
            "hero", "categories", "top_savings", "new_arrivals", "coupons", "editorial", "recent_reads"
    );

    private final ObjectMapper objectMapper;

    public CmsDocumentValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public List<CmsValidationIssue> validate(PageDocumentDto doc) {
        List<CmsValidationIssue> issues = new ArrayList<>();
        if (doc == null) {
            issues.add(new CmsValidationIssue("document", "Document payload cannot be null"));
            return issues;
        }

        // Byte size limit
        try {
            String json = objectMapper.writeValueAsString(doc);
            int byteLength = json.getBytes(StandardCharsets.UTF_8).length;
            if (byteLength > MAX_DOCUMENT_JSON_BYTES) {
                issues.add(new CmsValidationIssue("document",
                        "Document size (" + (byteLength / 1024) + " KB) exceeds maximum allowable limit of 256 KB"));
            }
        } catch (Exception e) {
            issues.add(new CmsValidationIssue("document", "Failed to serialize document to JSON: " + e.getMessage()));
        }

        // Basic document metadata
        validateSafeString(doc.getId(), 64, true, "id", issues);
        validateSafeString(doc.getTitle(), 100, true, "title", issues);

        if (doc.getSlug() == null || !doc.getSlug().startsWith("/")) {
            issues.add(new CmsValidationIssue("slug", "Slug must start with /"));
        } else if (doc.getSlug().length() > 255) {
            issues.add(new CmsValidationIssue("slug", "Slug cannot exceed 255 characters"));
        }

        if (doc.getSchemaVersion() == null || doc.getSchemaVersion() != CURRENT_CMS_SCHEMA_VERSION) {
            issues.add(new CmsValidationIssue("schemaVersion",
                    "Unsupported schema version: " + doc.getSchemaVersion() + ". Expected " + CURRENT_CMS_SCHEMA_VERSION));
        }

        if (doc.getVersion() == null || doc.getVersion() < 1) {
            issues.add(new CmsValidationIssue("version", "Document version must be a positive integer"));
        }

        // SEO Validation
        if (doc.getSeo() == null) {
            issues.add(new CmsValidationIssue("seo", "SEO metadata is required"));
        } else {
            validateSafeString(doc.getSeo().getMetaTitle(), 70, true, "seo.metaTitle", issues);
            validateSafeString(doc.getSeo().getMetaDescription(), 160, true, "seo.metaDescription", issues);
            validateSafeUrl(doc.getSeo().getCanonicalUrl(), 1024, true, "seo.canonicalUrl", issues);
        }

        // Sections Validation
        List<PageSectionDto> sections = doc.getSections();
        if (sections == null) {
            issues.add(new CmsValidationIssue("sections", "Sections list cannot be null"));
            return issues;
        }

        Set<String> seenIds = new HashSet<>();
        for (int i = 0; i < sections.size(); i++) {
            PageSectionDto section = sections.get(i);
            String prefix = "sections[" + i + "]";

            if (section.getId() == null || !SECTION_ID_PATTERN.matcher(section.getId().trim()).matches() || section.getId().trim().length() > 64) {
                issues.add(new CmsValidationIssue(prefix + ".id",
                        "Section ID must be 1-64 alphanumeric characters, underscores, or hyphens"));
            } else if (!seenIds.add(section.getId().trim())) {
                issues.add(new CmsValidationIssue(prefix + ".id",
                        "Duplicate section ID: " + section.getId().trim() + ". All section IDs must be unique."));
            }

            if (section.getType() == null || !ALLOWED_SECTION_TYPES.contains(section.getType())) {
                issues.add(new CmsValidationIssue(prefix + ".type",
                        "Unsupported section type: " + section.getType() + ". Allowed types: " + ALLOWED_SECTION_TYPES));
            } else {
                validateSectionProps(section.getType(), section.getProps(), prefix + ".props", issues);
            }
        }

        return issues;
    }

    @SuppressWarnings("unchecked")
    private void validateSectionProps(String type, Map<String, Object> props, String prefix, List<CmsValidationIssue> issues) {
        if (props == null) {
            issues.add(new CmsValidationIssue(prefix, "Section props cannot be null"));
            return;
        }

        switch (type) {
            case "hero" -> {
                validateSafeString(getString(props, "pillIcon"), 10, true, prefix + ".pillIcon", issues);
                validateSafeString(getString(props, "pillText"), 100, true, prefix + ".pillText", issues);
                validateSafeString(getString(props, "titlePrimary"), 150, true, prefix + ".titlePrimary", issues);
                validateSafeString(getString(props, "titleHighlight"), 150, true, prefix + ".titleHighlight", issues);
                validateSafeString(getString(props, "ledeText"), 500, true, prefix + ".ledeText", issues);
                Object statsObj = props.get("stats");
                if (statsObj instanceof List<?> statsList) {
                    if (statsList.size() > 6) {
                        issues.add(new CmsValidationIssue(prefix + ".stats", "Maximum 6 statistics allowed"));
                    }
                    for (int s = 0; s < statsList.size(); s++) {
                        Object itemObj = statsList.get(s);
                        if (itemObj instanceof Map<?, ?> item) {
                            validateSafeString(getStringFromMap(item, "icon"), 10, true, prefix + ".stats[" + s + "].icon", issues);
                            validateSafeString(getStringFromMap(item, "text"), 100, true, prefix + ".stats[" + s + "].text", issues);
                        }
                    }
                }
            }
            case "categories" -> {
                validateSafeString(getString(props, "eyebrow"), 60, true, prefix + ".eyebrow", issues);
                validateSafeString(getString(props, "title"), 120, true, prefix + ".title", issues);
                Object catsObj = props.get("categories");
                if (catsObj instanceof List<?> catList) {
                    if (catList.isEmpty()) {
                        issues.add(new CmsValidationIssue(prefix + ".categories", "At least 1 category required"));
                    } else if (catList.size() > 20) {
                        issues.add(new CmsValidationIssue(prefix + ".categories", "Maximum 20 categories allowed"));
                    }
                    for (int c = 0; c < catList.size(); c++) {
                        Object itemObj = catList.get(c);
                        if (itemObj instanceof Map<?, ?> item) {
                            validateSafeString(getStringFromMap(item, "id"), 64, true, prefix + ".categories[" + c + "].id", issues);
                            validateSafeString(getStringFromMap(item, "label"), 100, true, prefix + ".categories[" + c + "].label", issues);
                            validateSafeString(getStringFromMap(item, "icon"), 10, true, prefix + ".categories[" + c + "].icon", issues);
                            validateSafeUrl(getStringFromMap(item, "path"), 1024, false, prefix + ".categories[" + c + "].path", issues);
                        }
                    }
                } else {
                    issues.add(new CmsValidationIssue(prefix + ".categories", "Categories array is required"));
                }
            }
            case "top_savings", "new_arrivals" -> {
                validateSafeString(getString(props, "title"), 120, true, prefix + ".title", issues);
                validateSafeString(getString(props, "eyebrow"), 60, true, prefix + ".eyebrow", issues);
                Object pidsObj = props.get("productIds");
                if (pidsObj instanceof List<?> pidsList) {
                    if (pidsList.isEmpty()) {
                        issues.add(new CmsValidationIssue(prefix + ".productIds", "At least 1 product ID required"));
                    } else if (pidsList.size() > 16) {
                        issues.add(new CmsValidationIssue(prefix + ".productIds", "Maximum 16 products allowed"));
                    }
                } else {
                    issues.add(new CmsValidationIssue(prefix + ".productIds", "Product IDs array is required"));
                }
            }
            case "coupons" -> {
                validateSafeString(getString(props, "eyebrowIcon"), 10, true, prefix + ".eyebrowIcon", issues);
                validateSafeString(getString(props, "eyebrowText"), 60, true, prefix + ".eyebrowText", issues);
                validateSafeString(getString(props, "title"), 120, true, prefix + ".title", issues);
                Object cpObj = props.get("coupons");
                if (cpObj instanceof List<?> cpList) {
                    if (cpList.isEmpty()) {
                        issues.add(new CmsValidationIssue(prefix + ".coupons", "At least 1 coupon required"));
                    } else if (cpList.size() > 12) {
                        issues.add(new CmsValidationIssue(prefix + ".coupons", "Maximum 12 coupons allowed"));
                    }
                    for (int cp = 0; cp < cpList.size(); cp++) {
                        Object itemObj = cpList.get(cp);
                        if (itemObj instanceof Map<?, ?> item) {
                            validateSafeString(getStringFromMap(item, "id"), 64, true, prefix + ".coupons[" + cp + "].id", issues);
                            validateSafeString(getStringFromMap(item, "store"), 100, true, prefix + ".coupons[" + cp + "].store", issues);
                            validateSafeString(getStringFromMap(item, "offer"), 150, true, prefix + ".coupons[" + cp + "].offer", issues);
                            validateSafeString(getStringFromMap(item, "code"), 50, true, prefix + ".coupons[" + cp + "].code", issues);
                            validateSafeUrl(getStringFromMap(item, "affiliateUrl"), 1024, true, prefix + ".coupons[" + cp + "].affiliateUrl", issues);
                        }
                    }
                } else {
                    issues.add(new CmsValidationIssue(prefix + ".coupons", "Coupons array is required"));
                }
            }
            case "editorial" -> {
                Object promosObj = props.get("promos");
                if (promosObj instanceof List<?> promoList) {
                    if (promoList.isEmpty()) {
                        issues.add(new CmsValidationIssue(prefix + ".promos", "At least 1 promo required"));
                    } else if (promoList.size() > 8) {
                        issues.add(new CmsValidationIssue(prefix + ".promos", "Maximum 8 promos allowed"));
                    }
                    for (int pr = 0; pr < promoList.size(); pr++) {
                        Object itemObj = promoList.get(pr);
                        if (itemObj instanceof Map<?, ?> item) {
                            validateSafeString(getStringFromMap(item, "id"), 64, true, prefix + ".promos[" + pr + "].id", issues);
                            validateSafeString(getStringFromMap(item, "title"), 150, true, prefix + ".promos[" + pr + "].title", issues);
                            validateSafeUrl(getStringFromMap(item, "image"), 1024, false, prefix + ".promos[" + pr + "].image", issues);
                            validateSafeUrl(getStringFromMap(item, "path"), 1024, false, prefix + ".promos[" + pr + "].path", issues);
                            validateSafeString(getStringFromMap(item, "ctaText"), 50, false, prefix + ".promos[" + pr + "].ctaText", issues);
                        }
                    }
                } else {
                    issues.add(new CmsValidationIssue(prefix + ".promos", "Editorial promos array is required"));
                }
            }
            case "recent_reads" -> {
                validateSafeString(getString(props, "eyebrowIcon"), 10, true, prefix + ".eyebrowIcon", issues);
                validateSafeString(getString(props, "eyebrowText"), 60, true, prefix + ".eyebrowText", issues);
                validateSafeString(getString(props, "title"), 120, true, prefix + ".title", issues);
                Object readsObj = props.get("reads");
                if (readsObj instanceof List<?> readList) {
                    if (readList.isEmpty()) {
                        issues.add(new CmsValidationIssue(prefix + ".reads", "At least 1 read item required"));
                    } else if (readList.size() > 10) {
                        issues.add(new CmsValidationIssue(prefix + ".reads", "Maximum 10 reads allowed"));
                    }
                    for (int r = 0; r < readList.size(); r++) {
                        Object itemObj = readList.get(r);
                        if (itemObj instanceof Map<?, ?> item) {
                            validateSafeString(getStringFromMap(item, "id"), 64, true, prefix + ".reads[" + r + "].id", issues);
                            validateSafeString(getStringFromMap(item, "title"), 200, true, prefix + ".reads[" + r + "].title", issues);
                            validateSafeString(getStringFromMap(item, "excerpt"), 500, true, prefix + ".reads[" + r + "].excerpt", issues);
                            validateSafeString(getStringFromMap(item, "date"), 50, true, prefix + ".reads[" + r + "].date", issues);
                            validateSafeUrl(getStringFromMap(item, "path"), 1024, true, prefix + ".reads[" + r + "].path", issues);
                        }
                    }
                } else {
                    issues.add(new CmsValidationIssue(prefix + ".reads", "Reads array is required"));
                }
            }
        }
    }

    private void validateSafeString(String val, int maxLen, boolean required, String path, List<CmsValidationIssue> issues) {
        if (val == null || val.trim().isEmpty()) {
            if (required) {
                issues.add(new CmsValidationIssue(path, "Field cannot be empty"));
            }
            return;
        }
        String trimmed = val.trim();
        if (trimmed.length() > maxLen) {
            issues.add(new CmsValidationIssue(path, "Field cannot exceed " + maxLen + " characters"));
        }
        if (SCRIPT_TAG_PATTERN.matcher(trimmed).find()) {
            issues.add(new CmsValidationIssue(path, "HTML script tags are strictly prohibited"));
        }
        if (HTML_TAG_PATTERN.matcher(trimmed).find()) {
            issues.add(new CmsValidationIssue(path, "Raw HTML tags are not permitted in plain text fields"));
        }
    }

    private void validateSafeUrl(String val, int maxLen, boolean optional, String path, List<CmsValidationIssue> issues) {
        if (val == null || val.trim().isEmpty()) {
            if (!optional) {
                issues.add(new CmsValidationIssue(path, "URL cannot be empty"));
            }
            return;
        }
        String trimmed = val.trim();
        if (trimmed.length() > maxLen) {
            issues.add(new CmsValidationIssue(path, "URL cannot exceed " + maxLen + " characters"));
        }
        if (trimmed.toLowerCase().startsWith("javascript:")) {
            issues.add(new CmsValidationIssue(path, "javascript: URLs are strictly prohibited"));
        }
        if (!trimmed.startsWith("/") && !trimmed.startsWith("https://")) {
            issues.add(new CmsValidationIssue(path, "URLs must be root-relative (e.g. /category/slug) or secure https://"));
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    private String getStringFromMap(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : null;
    }
}
