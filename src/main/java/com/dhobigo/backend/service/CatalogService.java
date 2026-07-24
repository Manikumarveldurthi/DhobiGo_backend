package com.dhobigo.backend.service;

import com.dhobigo.backend.dto.CatalogDtos.CatalogItemRequest;
import com.dhobigo.backend.dto.CatalogDtos.CatalogItemResponse;
import com.dhobigo.backend.exception.ApiException;
import com.dhobigo.backend.model.CatalogItem;
import com.dhobigo.backend.model.ServiceType;
import com.dhobigo.backend.repository.CatalogItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private final CatalogItemRepository catalogItemRepository;

    public CatalogService(CatalogItemRepository catalogItemRepository) {
        this.catalogItemRepository = catalogItemRepository;
    }

    public List<CatalogItemResponse> getAll() {
        return catalogItemRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<CatalogItemResponse> getByService(ServiceType service) {
        return catalogItemRepository.findByService(service).stream().map(this::toResponse).toList();
    }

    /** Admin adds a brand-new laundry item/service combo — this is the "add new features" lever for the catalog. */
    public CatalogItemResponse create(CatalogItemRequest req) {
        CatalogItem item = CatalogItem.builder()
                .itemKey(req.itemKey())
                .name(req.name())
                .icon(req.icon())
                .service(req.service())
                .price(req.price())
                .ecoFriendly(req.ecoFriendly())
                .build();
        return toResponse(catalogItemRepository.save(item));
    }

    public CatalogItemResponse update(Long id, CatalogItemRequest req) {
        CatalogItem item = catalogItemRepository.findById(id)
                .orElseThrow(() -> new ApiException("Catalog item not found", HttpStatus.NOT_FOUND));
        item.setItemKey(req.itemKey());
        item.setName(req.name());
        item.setIcon(req.icon());
        item.setService(req.service());
        item.setPrice(req.price());
        item.setEcoFriendly(req.ecoFriendly());
        return toResponse(catalogItemRepository.save(item));
    }

    public void delete(Long id) {
        if (!catalogItemRepository.existsById(id)) {
            throw new ApiException("Catalog item not found", HttpStatus.NOT_FOUND);
        }
        catalogItemRepository.deleteById(id);
    }

    private CatalogItemResponse toResponse(CatalogItem item) {
    	return new CatalogItemResponse(item.getId(), item.getItemKey(), item.getName(), item.getIcon(), item.getService(), item.getPrice(), item.isEcoFriendly());
    }
}
