package model;

import java.time.LocalDateTime;

public class Announcement {

    private int announcementId;
    private String title;
    private String content;
    private int authorId;
    private TargetAudience targetAudience;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;

    public enum TargetAudience {
        ALL, STUDENTS, FACULTY
    }

    public Announcement() {}

    public Announcement(String title, String content, int authorId, TargetAudience targetAudience) {
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.targetAudience = targetAudience;
    }

    // Getters and Setters
    public int getAnnouncementId() { return announcementId; }
    public void setAnnouncementId(int announcementId) { this.announcementId = announcementId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }

    public TargetAudience getTargetAudience() { return targetAudience; }
    public void setTargetAudience(TargetAudience targetAudience) { this.targetAudience = targetAudience; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    /**
     * Returns true if this announcement is currently active —
     * i.e. it has no expiry date, or its expiry is in the future.
     */
    public boolean isActive() {
        return expiresAt == null || expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Returns true if this announcement is visible to the given audience role.
     * ALL announcements are visible to everyone; STUDENTS-only or FACULTY-only
     * announcements are restricted accordingly.
     */
    public boolean isVisibleTo(String role) {
        if (targetAudience == TargetAudience.ALL) return true;
        return targetAudience.name().equalsIgnoreCase(role);
    }
}
