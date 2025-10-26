package com.example.fpm.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "path_configs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_path_configs_prefix", columnNames = {"prefix"})
})
public class PathConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String prefix; // e.g., Finance

    @Column(nullable = false, length = 512)
    private String sourcePath; // e.g., incoming/Finance__

    @Column(nullable = false, length = 512)
    private String outputPath; // e.g., reports/compliance/Finance

    @Column(nullable = false, length = 16)
    private String status; // Active | Inactive

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PathConfig() {
    }

    public PathConfig(Long id, String prefix, String sourcePath, String outputPath, String status, Instant createdAt) {
        this.id = id;
        this.prefix = prefix;
        this.sourcePath = sourcePath;
        this.outputPath = outputPath;
        this.status = status;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
