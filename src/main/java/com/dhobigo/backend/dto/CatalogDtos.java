package com.dhobigo.backend.dto;

import com.dhobigo.backend.model.ServiceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CatalogDtos {

    /**
     * "id" here is the item's slug (e.g. "shirt") — kept matching the
     * original frontend contract (services.js uses item.id as the slug).
     * "dbId" is the real database primary key, used only by the admin
     * catalog-management screen for edit/delete.
     */
    public record CatalogItemResponse(
            Long dbId,
            String id,
            String name,
            String icon,
            ServiceType service,
            int price,
            boolean ecoFriendly
    ) {}

    public record CatalogItemRequest(
            @NotBlank String itemKey,
            @NotBlank String name,
            @NotBlank String icon,
            @NotNull ServiceType service,
            @Min(0) int price,
            boolean ecoFriendly
    ) {}
}
