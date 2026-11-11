package com.vapedealspot.email.campaign;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    private String id;

    @NotBlank(message = "Name is required")
    private String name;

    private String subject;
    private String senderName;
    private String senderEmail;
    private String replyTo;

    private String contentMode; // "blocks" or "html"

    @Column(columnDefinition = "LONGTEXT")
    private String contentHtml;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> contentDocument;

    private String status; // DRAFT or SENT

    private String audienceMode; // ALL or SPECIFIC

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> selectedContactIds;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = "draft";
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getReplyTo() { return replyTo; }
    public void setReplyTo(String replyTo) { this.replyTo = replyTo; }

    public String getContentMode() { return contentMode; }
    public void setContentMode(String contentMode) { this.contentMode = contentMode; }

    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }

    public Map<String, Object> getContentDocument() { return contentDocument; }
    public void setContentDocument(Map<String, Object> contentDocument) { this.contentDocument = contentDocument; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAudienceMode() { return audienceMode; }
    public void setAudienceMode(String audienceMode) { this.audienceMode = audienceMode; }

    public List<String> getSelectedContactIds() { return selectedContactIds; }
    public void setSelectedContactIds(List<String> selectedContactIds) { this.selectedContactIds = selectedContactIds; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
