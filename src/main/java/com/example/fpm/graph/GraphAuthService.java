package com.example.fpm.graph;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Service
public class GraphAuthService {
    @Value("${app.msgraph.tenantId:}")
    private String tenantId;
    @Value("${app.msgraph.clientId:}")
    private String clientId;
    @Value("${app.msgraph.clientSecret:}")
    private String clientSecret;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getAccessToken() {
        if (cachedToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry.minusSeconds(60))) {
            return cachedToken;
        }
        if (tenantId == null || tenantId.isBlank() || clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("Graph credentials not configured");
        }
        String url = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("scope", "https://graph.microsoft.com/.default");
        body.add("grant_type", "client_credentials");
        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(body, headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, req, Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("Failed to obtain access token");
        }
        Map<String, Object> m = resp.getBody();
        cachedToken = (String) m.get("access_token");
        Number expiresIn = (Number) m.get("expires_in");
        tokenExpiry = Instant.now().plusSeconds(expiresIn != null ? expiresIn.longValue() : 3000L);
        return cachedToken;
    }
}
