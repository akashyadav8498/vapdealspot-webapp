package com.vapedealspot.email.campaign;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignRepository campaignRepository;

    public CampaignController(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @GetMapping
    public List<Map<String, Object>> getAllCampaigns() {
        return campaignRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public Map<String, Object> getCampaign(@PathVariable String id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
        return mapToDto(campaign);
    }

    @PostMapping
    public Map<String, Object> createCampaign(@Valid @RequestBody CampaignRequest request) {
        Campaign campaign = new Campaign();
        mapToEntity(request, campaign);
        campaign = campaignRepository.save(campaign);
        return mapToDto(campaign);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateCampaign(@PathVariable String id, @Valid @RequestBody CampaignRequest request) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
        mapToEntity(request, campaign);
        campaign = campaignRepository.save(campaign);
        return mapToDto(campaign);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCampaign(@PathVariable String id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
        campaignRepository.delete(campaign);
    }

    private Map<String, Object> mapToDto(Campaign c) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", c.getId());
        dto.put("name", c.getName());
        dto.put("subject", c.getSubject());
        dto.put("senderName", c.getSenderName());
        dto.put("senderEmail", c.getSenderEmail());
        dto.put("replyTo", c.getReplyTo());
        dto.put("contentMode", c.getContentMode());
        dto.put("contentHtml", c.getContentHtml());
        dto.put("contentDocument", c.getContentDocument());
        dto.put("status", c.getStatus());
        dto.put("createdAt", c.getCreatedAt());
        dto.put("updatedAt", c.getUpdatedAt());

        Map<String, Object> audience = new HashMap<>();
        if ("specific".equalsIgnoreCase(c.getAudienceMode())) {
            audience.put("type", "specific");
            audience.put("contactIds", c.getSelectedContactIds());
        } else {
            audience.put("type", "all");
        }
        dto.put("audience", audience);

        return dto;
    }

    private void mapToEntity(CampaignRequest req, Campaign c) {
        c.setName(req.name());
        c.setSubject(req.subject());
        c.setSenderName(req.senderName());
        c.setSenderEmail(req.senderEmail());
        c.setReplyTo(req.replyTo());
        c.setContentMode(req.contentMode());
        c.setContentHtml(req.contentHtml());
        c.setContentDocument(req.contentDocument());
        
        if (req.audience() != null) {
            String type = (String) req.audience().get("type");
            if ("specific".equalsIgnoreCase(type)) {
                c.setAudienceMode("specific");
                c.setSelectedContactIds((List<String>) req.audience().get("contactIds"));
            } else {
                c.setAudienceMode("all");
                c.setSelectedContactIds(null);
            }
        }
    }

    public record CampaignRequest(
            @jakarta.validation.constraints.NotBlank(message = "Name is required")
            String name,
            String subject,
            String senderName,
            String senderEmail,
            String replyTo,
            String contentMode,
            String contentHtml,
            Map<String, Object> contentDocument,
            Map<String, Object> audience
    ) {}
}
