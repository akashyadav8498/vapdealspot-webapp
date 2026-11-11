package com.vapedealspot.cms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vapedealspot.auth.PageBuilderAction;
import com.vapedealspot.auth.PageBuilderSecurityService;
import com.vapedealspot.cms.dto.*;
import com.vapedealspot.cms.exception.*;
import com.vapedealspot.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PageBuilderService {

    private static final Logger log = LoggerFactory.getLogger(PageBuilderService.class);
    private static final String HOMEPAGE_PAGE_ID = "homepage";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private final CmsPageRepository cmsPageRepository;
    private final CmsPageDocumentRepository cmsPageDocumentRepository;
    private final CmsPageRevisionRepository cmsPageRevisionRepository;
    private final CmsDocumentValidator cmsDocumentValidator;
    private final PageBuilderSecurityService pageBuilderSecurityService;
    private final ObjectMapper objectMapper;

    public PageBuilderService(CmsPageRepository cmsPageRepository,
                              CmsPageDocumentRepository cmsPageDocumentRepository,
                              CmsPageRevisionRepository cmsPageRevisionRepository,
                              CmsDocumentValidator cmsDocumentValidator,
                              PageBuilderSecurityService pageBuilderSecurityService,
                              ObjectMapper objectMapper) {
        this.cmsPageRepository = cmsPageRepository;
        this.cmsPageDocumentRepository = cmsPageDocumentRepository;
        this.cmsPageRevisionRepository = cmsPageRevisionRepository;
        this.cmsDocumentValidator = cmsDocumentValidator;
        this.pageBuilderSecurityService = pageBuilderSecurityService;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Loads the current draft document for the canonical homepage.
     */
    @Transactional(readOnly = true)
    public PageDocumentDto getDraftHomepage() {
        Optional<CmsPageDocument> draftOpt = cmsPageDocumentRepository.findByPageIdAndStatus(HOMEPAGE_PAGE_ID, "draft");
        if (draftOpt.isPresent()) {
            CmsPageDocument draftDoc = draftOpt.get();
            PageDocumentDto doc = deserializeDocument(draftDoc.getContentJson());
            doc.setVersion(draftDoc.getVersion());
            return doc;
        }

        // Fallback to published if draft does not yet exist
        Optional<CmsPageDocument> pubOpt = cmsPageDocumentRepository.findByPageIdAndStatus(HOMEPAGE_PAGE_ID, "published");
        if (pubOpt.isPresent()) {
            CmsPageDocument pubDoc = pubOpt.get();
            PageDocumentDto doc = deserializeDocument(pubDoc.getContentJson());
            doc.setVersion(pubDoc.getVersion());
            return doc;
        }

        throw new CmsNotFoundException("No homepage document found in CMS tables.");
    }

    /**
     * Loads the current published document for the canonical homepage.
     */
    @Transactional(readOnly = true)
    public Optional<PageDocumentDto> getPublishedHomepage() {
        return cmsPageDocumentRepository.findByPageIdAndStatus(HOMEPAGE_PAGE_ID, "published")
                .map(doc -> {
                    PageDocumentDto dto = deserializeDocument(doc.getContentJson());
                    dto.setVersion(doc.getVersion());
                    return dto;
                });
    }

    @Transactional
    public Map<String, Object> saveDraftHomepage(PageDocumentDto candidateDoc, Integer expectedVersion, User actor) {
        if (actor != null && !pageBuilderSecurityService.canPerform(actor, PageBuilderAction.EDIT)) {
            throw new CmsForbiddenException("Insufficient permissions: Edit permission required to save drafts.");
        }

        String nowIso = LocalDateTime.now(ZoneOffset.UTC).format(ISO_FORMATTER);

        candidateDoc.setId(HOMEPAGE_PAGE_ID);
        candidateDoc.setSlug("/");
        candidateDoc.setSchemaVersion(CmsDocumentValidator.CURRENT_CMS_SCHEMA_VERSION);
        candidateDoc.setUpdatedAt(nowIso);
        candidateDoc.setPublishedAt(null);

        // Server-side validation
        List<CmsValidationIssue> issues = cmsDocumentValidator.validate(candidateDoc);
        if (!issues.isEmpty()) {
            throw new CmsValidationException("Cannot save invalid draft document", issues);
        }

        // Lock existing draft row
        Optional<CmsPageDocument> draftOpt = cmsPageDocumentRepository.findByPageIdAndStatusForUpdate(HOMEPAGE_PAGE_ID, "draft");
        int nextVersion = 1;

        CmsPageDocument draftEntity;
        if (draftOpt.isPresent()) {
            draftEntity = draftOpt.get();
            int currentVersion = draftEntity.getVersion() != null ? draftEntity.getVersion() : 1;
            if (expectedVersion != null && currentVersion != expectedVersion) {
                throw new CmsConflictException("Draft version conflict: current draft is at version "
                        + currentVersion + ", but expected version was " + expectedVersion + ". Please reload.");
            }
            nextVersion = currentVersion + 1;
        } else {
            // Check published row if draft doesn't exist yet
            Optional<CmsPageDocument> pubOpt = cmsPageDocumentRepository.findByPageIdAndStatus(HOMEPAGE_PAGE_ID, "published");
            if (pubOpt.isPresent()) {
                nextVersion = (pubOpt.get().getVersion() != null ? pubOpt.get().getVersion() : 1) + 1;
            }
            draftEntity = new CmsPageDocument(HOMEPAGE_PAGE_ID, "draft", "{}", 1, nextVersion);
        }

        candidateDoc.setVersion(nextVersion);
        String jsonString = serializeDocument(candidateDoc);

        draftEntity.setContentJson(jsonString);
        draftEntity.setVersion(nextVersion);
        draftEntity.setSchemaVersion(CmsDocumentValidator.CURRENT_CMS_SCHEMA_VERSION);
        draftEntity.setUpdatedAt(LocalDateTime.now());
        draftEntity.setPublishedAt(null);

        cmsPageDocumentRepository.save(draftEntity);

        log.info("Saved homepage draft version v{} by user {}", nextVersion, actor != null ? actor.getEmail() : "system");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("version", nextVersion);
        response.put("updatedAt", nowIso);
        response.put("document", candidateDoc);
        return response;
    }

    @Transactional
    public Map<String, Object> publishHomepage(Integer expectedVersion, User actor) {
        if (actor == null || !pageBuilderSecurityService.canPerform(actor, PageBuilderAction.PUBLISH)) {
            throw new CmsForbiddenException("Insufficient permissions: Publish permission required to publish homepage.");
        }
        return doPublish(expectedVersion, actor.getEmail());
    }

    private Map<String, Object> doPublish(Integer expectedVersion, String actorEmail) {

        // 1. Lock and load current draft
        Optional<CmsPageDocument> draftOpt = cmsPageDocumentRepository.findByPageIdAndStatusForUpdate(HOMEPAGE_PAGE_ID, "draft");
        if (draftOpt.isEmpty()) {
            throw new CmsNotFoundException("No draft found to publish for homepage.");
        }

        CmsPageDocument draftEntity = draftOpt.get();
        int draftVersion = draftEntity.getVersion() != null ? draftEntity.getVersion() : 1;

        if (expectedVersion != null && draftVersion != expectedVersion) {
            throw new CmsConflictException("Publish conflict: the draft version (" + draftVersion
                    + ") does not match expected version (" + expectedVersion + ").");
        }

        // 2. Validate current draft
        PageDocumentDto draftDoc = deserializeDocument(draftEntity.getContentJson());
        List<CmsValidationIssue> issues = cmsDocumentValidator.validate(draftDoc);
        if (!issues.isEmpty()) {
            throw new CmsValidationException("Draft document failed schema validation and cannot be published", issues);
        }

        LocalDateTime now = LocalDateTime.now();
        String nowIso = LocalDateTime.now(ZoneOffset.UTC).format(ISO_FORMATTER);

        draftDoc.setId(HOMEPAGE_PAGE_ID);
        draftDoc.setSlug("/");
        draftDoc.setSchemaVersion(CmsDocumentValidator.CURRENT_CMS_SCHEMA_VERSION);
        draftDoc.setVersion(draftVersion);
        draftDoc.setUpdatedAt(nowIso);
        draftDoc.setPublishedAt(nowIso);

        String jsonString = serializeDocument(draftDoc);

        // 3. Atomically update published document in cms_page_documents
        Optional<CmsPageDocument> pubOpt = cmsPageDocumentRepository.findByPageIdAndStatusForUpdate(HOMEPAGE_PAGE_ID, "published");
        CmsPageDocument publishedEntity = pubOpt.orElseGet(() -> new CmsPageDocument(HOMEPAGE_PAGE_ID, "published", jsonString, 1, draftVersion));

        publishedEntity.setContentJson(jsonString);
        publishedEntity.setVersion(draftVersion);
        publishedEntity.setSchemaVersion(CmsDocumentValidator.CURRENT_CMS_SCHEMA_VERSION);
        publishedEntity.setPublishedAt(now);
        publishedEntity.setUpdatedAt(now);

        cmsPageDocumentRepository.save(publishedEntity);

        // 4. Record immutable revision snapshot in cms_page_revisions
        CmsPageRevision revision = new CmsPageRevision(
                HOMEPAGE_PAGE_ID,
                jsonString,
                CmsDocumentValidator.CURRENT_CMS_SCHEMA_VERSION,
                actorEmail
        );
        cmsPageRevisionRepository.save(revision);

        log.info("Homepage successfully published at version v{} by admin {}. Revision snapshot created.",
                draftVersion, actorEmail);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("version", draftVersion);
        response.put("publishedAt", nowIso);
        response.put("revisionRecorded", true);
        return response;
    }

    /**
     * Lists published revision history snapshots for the homepage.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> listHomepageRevisions(int limit, int offset) {
        int boundedLimit = Math.min(Math.max(1, limit), 50);
        int boundedOffset = Math.max(0, offset);
        int pageIndex = boundedOffset / boundedLimit;

        Pageable pageable = PageRequest.of(pageIndex, boundedLimit);
        List<CmsPageRevision> revisions = cmsPageRevisionRepository.findByPageIdOrderByIdDesc(HOMEPAGE_PAGE_ID, pageable);
        long total = cmsPageRevisionRepository.countByPageId(HOMEPAGE_PAGE_ID);

        List<RevisionSummaryDto> summaryList = new ArrayList<>();
        for (CmsPageRevision rev : revisions) {
            try {
                PageDocumentDto doc = deserializeDocument(rev.getContentJson());
                RevisionSummaryDto summary = buildRevisionSummary(rev, doc);
                summaryList.add(summary);
            } catch (Exception e) {
                log.warn("Skipping corrupted revision ID {}: {}", rev.getId(), e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("revisions", summaryList);
        result.put("total", total);
        return result;
    }

    @Transactional
    public Map<String, Object> restoreHomepageRevisionToDraft(Long revisionId, Integer expectedDraftVersion, User actor) {
        if (actor != null && !pageBuilderSecurityService.canPerform(actor, PageBuilderAction.EDIT)) {
            throw new CmsForbiddenException("Insufficient permissions: Edit permission required to restore revision.");
        }

        // 1. Fetch revision snapshot
        Optional<CmsPageRevision> revOpt = cmsPageRevisionRepository.findByIdAndPageId(revisionId, HOMEPAGE_PAGE_ID);
        if (revOpt.isEmpty()) {
            throw new CmsNotFoundException("Revision #" + revisionId + " not found for homepage.");
        }

        CmsPageRevision revision = revOpt.get();
        PageDocumentDto historicalDoc = deserializeDocument(revision.getContentJson());

        // 2. Validate historical document
        List<CmsValidationIssue> issues = cmsDocumentValidator.validate(historicalDoc);
        if (!issues.isEmpty()) {
            throw new CmsValidationException("Historical revision #" + revisionId + " failed schema validation and cannot be restored", issues);
        }

        // 3. Lock current draft row and verify optimistic concurrency
        Optional<CmsPageDocument> draftOpt = cmsPageDocumentRepository.findByPageIdAndStatusForUpdate(HOMEPAGE_PAGE_ID, "draft");
        int nextVersion = 1;
        CmsPageDocument draftEntity;

        if (draftOpt.isPresent()) {
            draftEntity = draftOpt.get();
            int currentVersion = draftEntity.getVersion() != null ? draftEntity.getVersion() : 1;
            if (expectedDraftVersion != null && currentVersion != expectedDraftVersion) {
                throw new CmsConflictException("Draft version conflict: current draft is at version "
                        + currentVersion + ", but expected version was " + expectedDraftVersion + ". Please reload.");
            }
            nextVersion = currentVersion + 1;
        } else {
            draftEntity = new CmsPageDocument(HOMEPAGE_PAGE_ID, "draft", "{}", 1, nextVersion);
        }

        // 4. Construct restored document for DRAFT state only
        String nowIso = LocalDateTime.now(ZoneOffset.UTC).format(ISO_FORMATTER);
        historicalDoc.setId(HOMEPAGE_PAGE_ID);
        historicalDoc.setSlug("/");
        historicalDoc.setSchemaVersion(CmsDocumentValidator.CURRENT_CMS_SCHEMA_VERSION);
        historicalDoc.setVersion(nextVersion);
        historicalDoc.setUpdatedAt(nowIso);
        historicalDoc.setPublishedAt(null); // Never retain publishedAt in draft!

        String jsonString = serializeDocument(historicalDoc);

        // 5. Update draft row only
        draftEntity.setContentJson(jsonString);
        draftEntity.setVersion(nextVersion);
        draftEntity.setSchemaVersion(CmsDocumentValidator.CURRENT_CMS_SCHEMA_VERSION);
        draftEntity.setUpdatedAt(LocalDateTime.now());
        draftEntity.setPublishedAt(null);

        cmsPageDocumentRepository.save(draftEntity);

        log.info("Restored historical revision #{} into homepage draft v{} by user {}. Published record remains untouched.",
                revisionId, nextVersion, actor != null ? actor.getEmail() : "system");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("version", nextVersion);
        response.put("updatedAt", nowIso);
        response.put("document", historicalDoc);
        return response;
    }

    private RevisionSummaryDto buildRevisionSummary(CmsPageRevision rev, PageDocumentDto doc) {
        RevisionSummaryDto summary = new RevisionSummaryDto();
        summary.setId(rev.getId());
        summary.setPageId(rev.getPageId());
        summary.setVersion(doc.getVersion());
        summary.setSchemaVersion(rev.getSchemaVersion());
        summary.setPublishedBy(rev.getPublishedBy());
        summary.setCreatedAt(rev.getCreatedAt() != null ? rev.getCreatedAt().toString() : null);

        if (doc.getSeo() != null) {
            summary.setMetaTitle(doc.getSeo().getMetaTitle());
            summary.setMetaDescription(doc.getSeo().getMetaDescription());
        }

        // Hero headline
        if (doc.getSections() != null) {
            summary.setSectionsCount(doc.getSections().size());
            List<String> enabled = new ArrayList<>();
            for (PageSectionDto s : doc.getSections()) {
                if (s.isEnabled()) {
                    enabled.add(s.getType());
                }
                if ("hero".equals(s.getType()) && s.getProps() != null) {
                    Object pri = s.getProps().get("titlePrimary");
                    Object hlt = s.getProps().get("titleHighlight");
                    String primary = pri != null ? pri.toString() : "";
                    String highlight = hlt != null ? hlt.toString() : "";
                    summary.setHeroTitlePrimary(primary);
                    summary.setHeroTitleHighlight(highlight);
                    summary.setHeroHeadline((primary + " " + highlight).trim());
                }
            }
            summary.setEnabledSections(enabled);
        }

        summary.setDocument(doc);
        return summary;
    }

    private PageDocumentDto deserializeDocument(String json) {
        try {
            return objectMapper.readValue(json, PageDocumentDto.class);
        } catch (Exception e) {
            throw new CmsValidationException("Corrupted JSON document stored in CMS table: " + e.getMessage(), null);
        }
    }

    private String serializeDocument(PageDocumentDto doc) {
        try {
            return objectMapper.writeValueAsString(doc);
        } catch (Exception e) {
            throw new CmsValidationException("Failed to serialize document to JSON: " + e.getMessage(), null);
        }
    }
}
