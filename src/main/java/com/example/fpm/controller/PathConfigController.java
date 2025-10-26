package com.example.fpm.controller;

import com.example.fpm.dto.PageResponse;
import com.example.fpm.dto.PathConfigRequest;
import com.example.fpm.model.PathConfig;
import com.example.fpm.service.PathConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paths")
public class PathConfigController {

    private final PathConfigService service;

    public PathConfigController(PathConfigService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<PathConfig> list(@RequestParam(required = false) String search,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        return service.list(search, page, pageSize);
    }

    @PostMapping
    public ResponseEntity<PathConfig> create(@RequestBody PathConfigRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PathConfig> update(@PathVariable Long id, @RequestBody PathConfigRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PathConfig> get(@PathVariable Long id) {
        return service.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
