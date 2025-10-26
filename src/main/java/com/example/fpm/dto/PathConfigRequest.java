package com.example.fpm.dto;

public class PathConfigRequest {
    private String prefix;
    private String sourcePath; // should start with incoming/
    private String outputPath; // should start with reports/
    private String status; // Active | Inactive

    public PathConfigRequest() {}

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
}
