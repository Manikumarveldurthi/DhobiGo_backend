package com.dhobigo.backend.controller;

import com.dhobigo.backend.dto.CatalogDtos.CatalogItemResponse;
import com.dhobigo.backend.model.ServiceType;
import com.dhobigo.backend.service.CatalogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /** Public — matches GET /api/catalog to replace the hardcoded CATALOG object in services-data.js */
    @GetMapping
    public List<CatalogItemResponse> getAll(@RequestParam(required = false) ServiceType service) {
        return service != null ? catalogService.getByService(service) : catalogService.getAll();
    }
}
