package com.example.fpm.service;

import com.example.fpm.dto.PageResponse;
import com.example.fpm.dto.PathConfigRequest;
import com.example.fpm.model.PathConfig;
import com.example.fpm.repository.PathConfigRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PathConfigService {
    private final PathConfigRepository repository;

    public PathConfigService(PathConfigRepository repository) {
        this.repository = repository;
    }

    public PageResponse<PathConfig> list(String search, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize);
        Page<PathConfig> result = (search == null || search.isBlank())
                ? repository.findAll(pageable)
                : repository.search(search, pageable);
        return new PageResponse<>(result.getContent(), result.getTotalElements(), page, pageSize);
    }

    @Transactional
    public PathConfig create(PathConfigRequest req) {
        validate(req);
        if (repository.existsByPrefixIgnoreCase(req.getPrefix())) {
            throw new IllegalArgumentException("Prefix already exists");
        }
        PathConfig pc = new PathConfig();
        pc.setPrefix(req.getPrefix().trim());
        pc.setSourcePath(req.getSourcePath().trim());
        pc.setOutputPath(req.getOutputPath().trim());
        pc.setStatus(req.getStatus());
        return repository.save(pc);
    }

    @Transactional
    public PathConfig update(Long id, PathConfigRequest req) {
        validate(req);
        PathConfig existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found"));
        if (!existing.getPrefix().equalsIgnoreCase(req.getPrefix()) && repository.existsByPrefixIgnoreCase(req.getPrefix())) {
            throw new IllegalArgumentException("Prefix already exists");
        }
        existing.setPrefix(req.getPrefix().trim());
        existing.setSourcePath(req.getSourcePath().trim());
        existing.setOutputPath(req.getOutputPath().trim());
        existing.setStatus(req.getStatus());
        return repository.save(existing);
    }

    public Optional<PathConfig> get(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void validate(PathConfigRequest req) {
        if (req == null) throw new IllegalArgumentException("Request is null");
        if (req.getPrefix() == null || req.getPrefix().trim().isEmpty())
            throw new IllegalArgumentException("prefix is required");
        if (req.getSourcePath() == null || req.getSourcePath().trim().isEmpty())
            throw new IllegalArgumentException("sourcePath is required");
        if (req.getOutputPath() == null || req.getOutputPath().trim().isEmpty())
            throw new IllegalArgumentException("outputPath is required");
        if (req.getStatus() == null || req.getStatus().trim().isEmpty())
            throw new IllegalArgumentException("status is required");
        String s = req.getStatus().trim();
        if (!"Active".equals(s) && !"Inactive".equals(s))
            throw new IllegalArgumentException("status must be Active or Inactive");
    }
}
