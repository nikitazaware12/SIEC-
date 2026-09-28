package com.siec_acc.controller;

import com.siec_acc.dto.request.OrganizationRequestDTO;
import com.siec_acc.dto.response.OrganizationResponseDTO;
import com.siec_acc.service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationController.class);
    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    public ResponseEntity<OrganizationResponseDTO> createOrganization(
            @RequestBody OrganizationRequestDTO request) {
        logger.info("POST /api/organization");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.createOrganization(request));
    }

    @GetMapping
    public ResponseEntity<java.util.List<OrganizationResponseDTO>> getAllOrganizations() {
        logger.info("GET /api/organization");
        return ResponseEntity.ok(organizationService.getAllOrganizations());
    }

    @GetMapping("/{organizationId}")
    public ResponseEntity<OrganizationResponseDTO> getOrganization(
            @PathVariable String organizationId) {
        logger.info("GET /api/organization/{}", organizationId);
        return ResponseEntity.ok(organizationService.getOrganization(organizationId));
    }

    @GetMapping("/{organizationId}/logo")
    public ResponseEntity<byte[]> getOrganizationLogo(
            @PathVariable String organizationId) {
        logger.info("GET /api/organization/{}/logo", organizationId);
        return organizationService.getOrganizationLogo(organizationId);
    }

    @PutMapping("/{organizationId}")
    public ResponseEntity<OrganizationResponseDTO> updateOrganization(
            @PathVariable String organizationId,
            @RequestBody OrganizationRequestDTO request) {
        logger.info("PUT /api/organization/{}", organizationId);
        return ResponseEntity.ok(
                organizationService.updateOrganization(organizationId, request));
    }

    @PatchMapping("/{organizationId}")
    public ResponseEntity<OrganizationResponseDTO> patchOrganization(
            @PathVariable String organizationId,
            @RequestBody OrganizationRequestDTO request) {
        logger.info("PATCH /api/organization/{}", organizationId);
        return ResponseEntity.ok(
                organizationService.patchOrganization(organizationId, request));
    }

    @DeleteMapping("/{organizationId}")
    public ResponseEntity<String> deleteOrganization(
            @PathVariable String organizationId) {

        logger.info("DELETE /api/organization/{}", organizationId);

        organizationService.deleteOrganization(organizationId);

        return ResponseEntity.ok("Organization deleted successfully");
    }
}