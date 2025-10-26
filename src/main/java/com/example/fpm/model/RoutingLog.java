package com.example.fpm.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "routing_logs")
public class RoutingLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String action; // MOVED | SKIPPED | ERROR

    @Column(nullable = false)
    private String fromPath;

    @Column(nullable = false)
    private String toPath;

    @Column(nullable = true, length = 1000)
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    public RoutingLog() {}

    public RoutingLog(Long id, String fileName, String action, String fromPath, String toPath, String message, Instant createdAt) {
        this.id = id;
        this.fileName = fileName;
        this.action = action;
        this.fromPath = fromPath;
        this.toPath = toPath;
        this.message = message;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getFromPath() { return fromPath; }
    public void setFromPath(String fromPath) { this.fromPath = fromPath; }
    public String getToPath() { return toPath; }
    public void setToPath(String toPath) { this.toPath = toPath; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
