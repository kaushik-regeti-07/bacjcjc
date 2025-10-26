package com.example.fpm.controller;

import com.example.fpm.service.RoutingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/routing")
public class RoutingController {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runNow() {
        return ResponseEntity.ok(routingService.runRoutingNow());
    }

    @PostMapping("/dry-run")
    public ResponseEntity<List<RoutingService.DryRunDecision>> dryRun(@RequestBody List<String> fileNames) {
        return ResponseEntity.ok(routingService.dryRunDecisions(fileNames));
    }
}
