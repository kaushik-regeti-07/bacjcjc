package com.example.fpm.controller;

import com.example.fpm.service.RoutingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping("/incoming")
    public ResponseEntity<List<Map<String, Object>>> listIncoming() {
        return ResponseEntity.ok(routingService.listIncomingFiles());
    }

    @PostMapping("/route-one")
    public ResponseEntity<Map<String, Object>> routeOne(@RequestParam("fileName") String fileName) {
        return ResponseEntity.ok(routingService.routeSingle(fileName));
    }
}
