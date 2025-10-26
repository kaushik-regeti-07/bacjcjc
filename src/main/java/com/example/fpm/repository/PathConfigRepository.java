package com.example.fpm.repository;

import com.example.fpm.model.PathConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PathConfigRepository extends JpaRepository<PathConfig, Long> {

    boolean existsByPrefixIgnoreCase(String prefix);

    @Query("select p from PathConfig p where lower(p.prefix) like lower(concat('%', :search, '%')) " +
            "or lower(p.sourcePath) like lower(concat('%', :search, '%')) " +
            "or lower(p.outputPath) like lower(concat('%', :search, '%'))")
    Page<PathConfig> search(String search, Pageable pageable);
}
