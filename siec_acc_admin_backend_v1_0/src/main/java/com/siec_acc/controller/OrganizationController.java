package com.siec_acc.controller;

import com.siec_acc.dto.request.OrganizationRequestDTO;
import com.siec_acc.dto.response.OrganizationResponseDTO;
import com.siec_acc.service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        Path directory = Paths.get("uploads", "organization");
        String[] extensions = {"png", "jpg", "jpeg", "webp"};

        try {
            for (String extension : extensions) {
                Path logoPath = directory.resolve(organizationId + "." + extension);
                if (Files.exists(logoPath)) {
                    String contentType = extension.equals("jpg") || extension.equals("jpeg")
                            ? MediaType.IMAGE_JPEG_VALUE
                            : extension.equals("webp") ? "image/webp" : MediaType.IMAGE_PNG_VALUE;
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_TYPE, contentType)
                            .body(Files.readAllBytes(logoPath));
                }
            }
        } catch (IOException e) {
            logger.error("Unable to read organization logo: {}", organizationId, e);
        }

        return ResponseEntity.notFound().build();
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