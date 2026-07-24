package com.dhobigo.backend.repository;

import com.dhobigo.backend.model.CatalogItem;
import com.dhobigo.backend.model.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {
    List<CatalogItem> findByService(ServiceType service);
}
