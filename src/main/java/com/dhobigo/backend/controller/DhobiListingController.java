package com.dhobigo.backend.controller;

import com.dhobigo.backend.dto.DhobiDtos.DhobiResponse;
import com.dhobigo.backend.service.DhobiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public listing so customers can pick a dhobi on services.html. Pass
 * lat/lng (e.g. from the browser's geolocation API) to get results sorted
 * nearest-first with a distanceKm on each — same idea as "restaurants near
 * you" on Swiggy/Zomato. Without lat/lng, results come back unsorted.
 */
@RestController
@RequestMapping("/api/dhobis")
public class DhobiListingController {

    private final DhobiService dhobiService;

    public DhobiListingController(DhobiService dhobiService) {
        this.dhobiService = dhobiService;
    }

    @GetMapping
    public List<DhobiResponse> getAvailable(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng
    ) {
        return dhobiService.getAvailableDhobis(lat, lng);
    }
}
