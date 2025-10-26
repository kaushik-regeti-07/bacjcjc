package com.example.fpm.service;

import com.example.fpm.model.PathConfig;
import com.example.fpm.model.RoutingLog;
import com.example.fpm.repository.PathConfigRepository;
import com.example.fpm.repository.RoutingLogRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Locale;

@Service
public class RoutingService {
    private final PathConfigRepository pathConfigRepository;
    private final RoutingLogRepository routingLogRepository;

    public RoutingService(PathConfigRepository pathConfigRepository, RoutingLogRepository routingLogRepository) {
        this.pathConfigRepository = pathConfigRepository;
        this.routingLogRepository = routingLogRepository;
    }

    // Stub: later integrate with Microsoft Graph to process incoming/ folder
    public Map<String, Object> runRoutingNow() {
        List<PathConfig> activeConfigs = pathConfigRepository.findAll();
        Map<String, Object> summary = new HashMap<>();
        summary.put("processed", 0);
        summary.put("moved", 0);
        summary.put("skipped", 0);
        summary.put("errors", 0);
        return summary;
    }

    public static class DryRunDecision {
        private String fileName;
        private String prefix;
        private boolean matched;
        private String outputPath; // reports/compliance/<Prefix>
        private String destinationPath; // outputPath + "/" + fileName
        private String reason; // if not matched

        public DryRunDecision() {}

        public DryRunDecision(String fileName, String prefix, boolean matched, String outputPath, String destinationPath, String reason) {
            this.fileName = fileName;
            this.prefix = prefix;
            this.matched = matched;
            this.outputPath = outputPath;
            this.destinationPath = destinationPath;
            this.reason = reason;
        }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
        public boolean isMatched() { return matched; }
        public void setMatched(boolean matched) { this.matched = matched; }
        public String getOutputPath() { return outputPath; }
        public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
        public String getDestinationPath() { return destinationPath; }
        public void setDestinationPath(String destinationPath) { this.destinationPath = destinationPath; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public List<DryRunDecision> dryRunDecisions(List<String> fileNames) {
        List<PathConfig> configs = pathConfigRepository.findAll();
        List<DryRunDecision> decisions = new ArrayList<>();
        for (String name : fileNames) {
            if (name == null || name.trim().isEmpty()) continue;
            String trimmed = name.trim();
            int sep = trimmed.indexOf("__");
            if (sep <= 0) {
                decisions.add(new DryRunDecision(trimmed, null, false, null, null, "Filename missing prefix delimiter '__'"));
                continue;
            }
            String prefix = trimmed.substring(0, sep);
            PathConfig match = null;
            for (PathConfig pc : configs) {
                if (pc.getPrefix() != null && pc.getStatus() != null
                        && pc.getPrefix().equalsIgnoreCase(prefix)
                        && pc.getStatus().equalsIgnoreCase("Active")) {
                    match = pc;
                    break;
                }
            }
            if (match == null) {
                decisions.add(new DryRunDecision(trimmed, prefix, false, null, null, "No active mapping for prefix"));
                continue;
            }
            String outputBase = match.getOutputPath();
            // If outputPath is reports/compliance/Finance we place file inside it
            String destination = (outputBase.endsWith("/")) ? outputBase + trimmed : outputBase + "/" + trimmed;
            decisions.add(new DryRunDecision(trimmed, prefix, true, outputBase, destination, null));
        }
        return decisions;
    }
}
