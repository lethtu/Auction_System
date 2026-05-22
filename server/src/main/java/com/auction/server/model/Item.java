package com.auction.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "items")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "category", discriminatorType = DiscriminatorType.STRING)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public abstract class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "uuid", unique = true, length = 36)
    private String uuid;

    @Column(name = "hidden", nullable = false)
    private Boolean hidden = false;

    private boolean isUuid(String str) {
        if (str == null || str.length() != 36) {
            return false;
        }
        try {
            java.util.UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.uuid == null) {
            this.uuid = java.util.UUID.randomUUID().toString();
        }
    }

    public abstract String getCategoryInfo();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return this.uuid;
    }

    public void setImagePath(String imagePath) {
        if (imagePath != null) {
            String trimmed = imagePath.trim();
            if (isUuid(trimmed)) {
                this.uuid = trimmed;
            } else {
                String extracted = extractUuidFromPath(trimmed);
                if (extracted != null) {
                    this.uuid = extracted;
                } else {
                    this.uuid = trimmed;
                }
            }
        }
    }

    private String extractUuidFromPath(String path) {
        if (path == null || path.isBlank()) return null;
        java.util.regex.Pattern uuidPattern = java.util.regex.Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
        java.util.regex.Matcher matcher = uuidPattern.matcher(path);
        return matcher.find() ? matcher.group() : null;
    }

    public boolean isHidden() {
        return Boolean.TRUE.equals(hidden);
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public String getUuid() {
        if (this.uuid == null) {
            this.uuid = java.util.UUID.randomUUID().toString();
        }
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}

