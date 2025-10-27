package com.example.fpm.sharepoint;

import com.example.fpm.graph.GraphAuthService;
import com.example.fpm.model.PathConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SharePointService {

    public interface LogWriter {
        void write(String fileName, String action, String from, String to, String message);
    }

    private final GraphAuthService auth;
    private final RestTemplate http = new RestTemplate();

    @Value("${app.storage.incoming.shareLink:}")
    private String incomingShareLink;
    @Value("${app.storage.reports.shareLink:}")
    private String reportsShareLink;

    public SharePointService(GraphAuthService auth) {
        this.auth = auth;
    }

    public Map<String, Object> runLiveRouting(List<PathConfig> configs, LogWriter logger) {
        Map<String, Object> summary = new HashMap<>();
        int processed = 0, moved = 0, skipped = 0, errors = 0;
        if (incomingShareLink == null || incomingShareLink.isBlank() || reportsShareLink == null || reportsShareLink.isBlank()) {
            summary.put("processed", 0); summary.put("moved", 0); summary.put("skipped", 0); summary.put("errors", 0);
            return summary;
        }
        try {
            // Resolve share links
            DriveItemRef incoming = resolveShare(incomingShareLink);
            DriveItemRef reportsRoot = resolveShare(reportsShareLink);

            // Build active mapping by prefix (case-insensitive)
            Map<String, PathConfig> active = configs.stream()
                    .filter(pc -> pc.getPrefix() != null && pc.getStatus() != null && pc.getStatus().equalsIgnoreCase("Active"))
                    .collect(Collectors.toMap(pc -> pc.getPrefix().toLowerCase(Locale.ROOT), pc -> pc, (a,b)->a));

            // List files under incoming
            List<Map<String, Object>> children = listChildren(incoming.driveId, incoming.itemId);
            for (Map<String,Object> item : children) {
                String name = (String) item.get("name");
                Map<String, Object> fileFacet = (Map<String, Object>) item.get("file");
                if (fileFacet == null) {
                    // skip folders
                    continue;
                }
                processed++;
                if (name == null || !name.contains("__")) {
                    skipped++;
                    logger.write(name, "SKIPPED", "incoming", "", "Missing prefix delimiter");
                    continue;
                }
                String prefix = name.substring(0, name.indexOf("__"));
                PathConfig pc = active.getOrDefault(prefix.toLowerCase(Locale.ROOT), null);
                if (pc == null) {
                    skipped++;
                    logger.write(name, "SKIPPED", "incoming", "", "No active mapping for prefix");
                    continue;
                }
                String relOut = normalizeRelativeToReports(pc.getOutputPath());
                // Ensure destination folder exists under reports root
                String destFolderId = ensureFolderPath(reportsRoot.driveId, reportsRoot.itemId, relOut);
                try {
                    // Move file; note incoming drive may differ from reports drive
                    moveItem(item, incoming.driveId, destFolderId, reportsRoot.driveId, name);
                    moved++;
                    logger.write(name, "MOVED", "incoming", relOut, null);
                } catch (Exception ex) {
                    errors++;
                    logger.write(name, "ERROR", "incoming", relOut, ex.getMessage());
                }
            }
        } catch (Exception e) {
            // overall error; count as errors but keep response
        }
        summary.put("processed", processed);
        summary.put("moved", moved);
        summary.put("skipped", skipped);
        summary.put("errors", errors);
        return summary;
    }

    private static class DriveItemRef {
        String driveId;
        String itemId;
    }

    private DriveItemRef resolveShare(String shareLink) {
        String encoded = encodeSharingUrl(shareLink);
        String url = "https://graph.microsoft.com/v1.0/shares/" + encoded + "/driveItem?$select=id,remoteItem,driveId,parentReference";
        ResponseEntity<Map> resp = http.exchange(url, HttpMethod.GET, authHeaders(), Map.class);
        Map<String, Object> body = resp.getBody();
        if (body == null) throw new RuntimeException("Failed to resolve share link");
        Map<String, Object> remoteItem = (Map<String, Object>) body.get("remoteItem");
        Map<String, Object> parentRef = (Map<String, Object>) body.get("parentReference");
        DriveItemRef ref = new DriveItemRef();
        // When using shares, the resolved item may be a remoteItem; prefer remote driveId and id
        if (remoteItem != null) {
            Map<String, Object> remoteParent = (Map<String, Object>) remoteItem.get("parentReference");
            ref.driveId = (String) (remoteParent != null ? remoteParent.get("driveId") : body.get("driveId"));
            ref.itemId = (String) remoteItem.get("id");
        } else {
            ref.driveId = (String) body.get("driveId");
            ref.itemId = (String) body.get("id");
        }
        if (ref.driveId == null || ref.itemId == null) {
            throw new RuntimeException("Missing driveId or itemId from share resolution");
        }
        return ref;
    }

    private String normalizeRelativeToReports(String outputPath) {
        if (outputPath == null) return "";
        String p = outputPath.replace("\\", "/");
        if (p.startsWith("/")) p = p.substring(1);
        if (p.toLowerCase(Locale.ROOT).startsWith("reports/")) {
            p = p.substring("reports/".length());
        }
        return p;
    }

    private List<Map<String, Object>> listChildren(String driveId, String itemId) {
        String url = "https://graph.microsoft.com/v1.0/drives/"+driveId+"/items/"+itemId+"/children?$select=id,name,folder,file,parentReference";
        ResponseEntity<Map> resp = http.exchange(url, HttpMethod.GET, authHeaders(), Map.class);
        Map<String, Object> body = resp.getBody();
        if (body == null) return Collections.emptyList();
        List<Map<String, Object>> value = (List<Map<String, Object>>) body.get("value");
        return value != null ? value : Collections.emptyList();
    }

    private String ensureFolderPath(String driveId, String rootId, String relativePath) {
        String currentId = rootId;
        if (relativePath == null || relativePath.isBlank()) return currentId;
        String[] parts = Arrays.stream(relativePath.split("/"))
                .filter(s -> s != null && !s.isBlank())
                .toArray(String[]::new);
        for (String part : parts) {
            String childId = findChildFolderId(driveId, currentId, part);
            if (childId == null) {
                childId = createFolder(driveId, currentId, part);
            }
            currentId = childId;
        }
        return currentId;
    }

    private String findChildFolderId(String driveId, String parentId, String name) {
        List<Map<String, Object>> children = listChildren(driveId, parentId);
        for (Map<String, Object> item : children) {
            if (name.equals(item.get("name")) && item.get("folder") != null) {
                return (String) item.get("id");
            }
        }
        return null;
    }

    private String createFolder(String driveId, String parentId, String name) {
        String url = "https://graph.microsoft.com/v1.0/drives/"+driveId+"/items/"+parentId+"/children";
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("folder", new HashMap<>());
        body.put("@microsoft.graph.conflictBehavior", "replace");
        ResponseEntity<Map> resp = http.exchange(url, HttpMethod.POST, authHeaders(body), Map.class);
        Map<String, Object> created = resp.getBody();
        if (created == null) throw new RuntimeException("Failed to create folder: " + name);
        return (String) created.get("id");
    }

    private void moveItem(Map<String, Object> item, String fromDriveId, String toParentId, String toDriveId, String name) {
        String itemId = (String) item.get("id");
        String url = "https://graph.microsoft.com/v1.0/drives/"+fromDriveId+"/items/"+itemId;
        Map<String, Object> parentRef = new HashMap<>();
        parentRef.put("id", toParentId);
        if (toDriveId != null && !toDriveId.equals(fromDriveId)) {
            parentRef.put("driveId", toDriveId);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("parentReference", parentRef);
        body.put("name", name);
        http.exchange(url, HttpMethod.PATCH, authHeaders(body), Map.class);
    }

    private HttpEntity<Map<String, Object>> authHeaders(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(auth.getAccessToken());
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(auth.getAccessToken());
        return new HttpEntity<>(headers);
    }

    private String encodeSharingUrl(String url) {
        String b64 = Base64.getEncoder().encodeToString(url.getBytes(StandardCharsets.UTF_8))
                .replace('+', '-')
                .replace('/', '_')
                .replaceAll("=+$", "");
        return "u!" + b64;
    }
}
