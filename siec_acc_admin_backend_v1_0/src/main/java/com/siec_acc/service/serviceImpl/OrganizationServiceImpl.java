package com.siec_acc.service.serviceImpl;

import com.siec_acc.dto.request.OrganizationRequestDTO;
import com.siec_acc.dto.response.OrganizationResponseDTO;
import com.siec_acc.entity.OrganizationEntity;
import com.siec_acc.exceptions.ResourceNotFoundException;
import com.siec_acc.repository.OrganizationRepository;
import com.siec_acc.service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import org.springframework.transaction.annotation.Transactional;


import java.util.UUID;


@Service
public class OrganizationServiceImpl implements OrganizationService {

    private static final Logger logger =
            LoggerFactory.getLogger(OrganizationServiceImpl.class);

    private final OrganizationRepository organizationRepository;

    public OrganizationServiceImpl(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    @Transactional
    public OrganizationResponseDTO createOrganization(
            OrganizationRequestDTO request) {

        logger.info("Creating organization");

        OrganizationEntity organization = organizationRepository.findAll()
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    OrganizationEntity newOrganization = new OrganizationEntity();
                    newOrganization.setOrganizationId(generateOrganizationId());
                    return newOrganization;
                });

        copyFields(organization, request, false);
        saveLogoFile(organization);

        OrganizationEntity saved = organizationRepository.save(organization);

        logger.info("Organization saved: {}", saved.getOrganizationId());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponseDTO getOrganization(String organizationId) {

        logger.info("Fetching organization: {}", organizationId);

        return toResponse(findOrganization(organizationId));
    }


    @Override
    @Transactional
    public OrganizationResponseDTO updateOrganization(
            String organizationId,
            OrganizationRequestDTO request) {

        logger.info("PUT organization: {}", organizationId);

        OrganizationEntity organization = organizationRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> organizationRepository.findAll()
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Organization not found")));

        copyFields(organization, request, false);
        saveLogoFile(organization);

        OrganizationEntity updated = organizationRepository.save(organization);

        return toResponse(updated);
    }

    @Override
    @Transactional
    public OrganizationResponseDTO patchOrganization(
            String organizationId,
            OrganizationRequestDTO request) {

        logger.info("PATCH organization: {}", organizationId);

        OrganizationEntity organization = findOrganization(organizationId);

        copyFields(organization, request, true);

        OrganizationEntity updated = organizationRepository.save(organization);

        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteOrganization(String organizationId) {

        logger.info("Deleting organization: {}", organizationId);

        OrganizationEntity organization = findOrganization(organizationId);
        deleteLogoFile(organizationId);

        organizationRepository.delete(organization);

        logger.info("Organization deleted: {}", organizationId);
    }
    private void deleteLogoFile(String organizationId) {
        Path directory = Paths.get("uploads", "organization");
        String[] extensions = {"png", "jpg", "jpeg", "webp"};

        for (String extension : extensions) {
            Path logoPath = directory.resolve(organizationId + "." + extension);

            try {
                if (Files.deleteIfExists(logoPath)) {
                    logger.info("Organization logo deleted: {}", logoPath);
                }
            } catch (IOException e) {
                logger.warn("Unable to delete organization logo: {}", logoPath, e);
            }
        }
    }
    private void saveLogoFile(OrganizationEntity organization) {
        String logoValue = organization.getLogoUrl();
        if (logoValue == null || logoValue.isBlank() || !logoValue.startsWith("data:image/")) {
            return;
        }

        try {
            String[] parts = logoValue.split(",", 2);
            if (parts.length != 2) {
                return;
            }

            String header = parts[0];
            String extension = header.contains("image/jpeg") || header.contains("image/jpg") ? "jpg"
                    : header.contains("image/webp") ? "webp" : "png";
            byte[] imageBytes = Base64.getDecoder().decode(parts[1]);

            Path directory = Paths.get("uploads", "organization");
            Files.createDirectories(directory);

            String fileName = organization.getOrganizationId() + "." + extension;
            Files.write(directory.resolve(fileName), imageBytes);
            organization.setLogoUrl("/api/organization/" + organization.getOrganizationId() + "/logo");

            Files.list(directory)
                    .filter(path -> path.getFileName().toString().startsWith(organization.getOrganizationId() + "."))
                    .filter(path -> !path.getFileName().toString().equals(fileName))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            logger.warn("Unable to remove old logo file: {}", path);
                        }
                    });
        } catch (IllegalArgumentException | IOException e) {
            logger.error("Unable to save organization logo", e);
            throw new IllegalStateException("Unable to save organization logo");
        }
    }

    private OrganizationEntity findOrganization(String organizationId) {

        return organizationRepository.findByOrganizationId(organizationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Organization not found with ID: " + organizationId));
    }

    private String generateOrganizationId() {

        String organizationId;

        do {
            organizationId =
                    "ORG-" +
                            UUID.randomUUID()
                                    .toString()
                                    .substring(0, 8)
                                    .toUpperCase();

        } while (organizationRepository.existsByOrganizationId(organizationId));

        return organizationId;
    }

    private void copyFields(OrganizationEntity o, OrganizationRequestDTO r, boolean skipNulls) {
        if (!skipNulls || r.getCompanyName() != null) o.setCompanyName(r.getCompanyName());
        if (!skipNulls || r.getLegalName() != null) o.setLegalName(r.getLegalName());
        if (!skipNulls || r.getBusinessType() != null) o.setBusinessType(r.getBusinessType());
        if (!skipNulls || r.getAddress() != null) o.setAddress(r.getAddress());
        if (!skipNulls || r.getCity() != null) o.setCity(r.getCity());
        if (!skipNulls || r.getState() != null) o.setState(r.getState());
        if (!skipNulls || r.getCountry() != null) o.setCountry(r.getCountry());
        if (!skipNulls || r.getPincode() != null) o.setPincode(r.getPincode());
        if (!skipNulls || r.getPhone() != null) o.setPhone(r.getPhone());
        if (!skipNulls || r.getEmail() != null) o.setEmail(r.getEmail());
        if (!skipNulls || r.getWebsite() != null) o.setWebsite(r.getWebsite());
        if (!skipNulls || r.getLogoUrl() != null) o.setLogoUrl(r.getLogoUrl());
        if (!skipNulls || r.getGstRegistered() != null) o.setGstRegistered(r.getGstRegistered());
        if (!skipNulls || r.getGstNumber() != null) o.setGstNumber(r.getGstNumber());
        if (!skipNulls || r.getTaxRegistrationType() != null) o.setTaxRegistrationType(r.getTaxRegistrationType());
        if (!skipNulls || r.getTaxState() != null) o.setTaxState(r.getTaxState());
        if (!skipNulls || r.getDefaultTaxRate() != null) o.setDefaultTaxRate(r.getDefaultTaxRate());
        if (!skipNulls || r.getTaxInclusive() != null) o.setTaxInclusive(r.getTaxInclusive());
        if (!skipNulls || r.getDefaultUnit() != null) o.setDefaultUnit(r.getDefaultUnit());
        if (!skipNulls || r.getUnitSystem() != null) o.setUnitSystem(r.getUnitSystem());
        if (!skipNulls || r.getDecimalPrecision() != null) o.setDecimalPrecision(r.getDecimalPrecision());
        if (!skipNulls || r.getFinancialYearStart() != null) o.setFinancialYearStart(r.getFinancialYearStart());
        if (!skipNulls || r.getTimezone() != null) o.setTimezone(r.getTimezone());
        if (!skipNulls || r.getOrganizationStatus() != null) o.setOrganizationStatus(r.getOrganizationStatus());
    }

    private OrganizationResponseDTO toResponse(OrganizationEntity o) {

        OrganizationResponseDTO r = new OrganizationResponseDTO();

        r.setOrganizationId(o.getOrganizationId());
        r.setCompanyName(o.getCompanyName());
        r.setLegalName(o.getLegalName());
        r.setBusinessType(o.getBusinessType());
        r.setAddress(o.getAddress());
        r.setCity(o.getCity());
        r.setState(o.getState());
        r.setCountry(o.getCountry());
        r.setPincode(o.getPincode());
        r.setPhone(o.getPhone());
        r.setEmail(o.getEmail());
        r.setWebsite(o.getWebsite());
        r.setLogoUrl(o.getLogoUrl());
        r.setGstRegistered(o.getGstRegistered());
        r.setGstNumber(o.getGstNumber());
        r.setTaxRegistrationType(o.getTaxRegistrationType());
        r.setTaxState(o.getTaxState());
        r.setDefaultTaxRate(o.getDefaultTaxRate());
        r.setTaxInclusive(o.getTaxInclusive());
        r.setDefaultUnit(o.getDefaultUnit());
        r.setUnitSystem(o.getUnitSystem());
        r.setDecimalPrecision(o.getDecimalPrecision());
        r.setFinancialYearStart(o.getFinancialYearStart());
        r.setTimezone(o.getTimezone());
        r.setOrganizationStatus(o.getOrganizationStatus());
        r.setCreatedAt(o.getCreatedAt());
        r.setUpdatedAt(o.getUpdatedAt());

        return r;
    }
}