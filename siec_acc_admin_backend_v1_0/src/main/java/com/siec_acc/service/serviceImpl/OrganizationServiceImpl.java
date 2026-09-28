package com.siec_acc.service.serviceImpl;

import com.siec_acc.dto.request.OrganizationRequestDTO;
import com.siec_acc.dto.response.OrganizationResponseDTO;
import com.siec_acc.entity.OrganizationEntity;
import com.siec_acc.exceptions.ResourceNotFoundException;
import com.siec_acc.repository.OrganizationRepository;
import com.siec_acc.service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
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
    public OrganizationResponseDTO createOrganization(OrganizationRequestDTO request) {
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
        saveLogoBlob(organization, request);

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
    @Transactional(readOnly = true)
    public java.util.List<OrganizationResponseDTO> getAllOrganizations() {
        logger.info("Fetching all organizations");
        return organizationRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public OrganizationResponseDTO updateOrganization(
            String organizationId, OrganizationRequestDTO request) {

        logger.info("PUT organization: {}", organizationId);

        OrganizationEntity organization = organizationRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> organizationRepository.findAll()
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Organization not found")));

        copyFields(organization, request, false);
        saveLogoBlob(organization, request);

        OrganizationEntity updated = organizationRepository.save(organization);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public OrganizationResponseDTO patchOrganization(
            String organizationId, OrganizationRequestDTO request) {

        logger.info("PATCH organization: {}", organizationId);

        OrganizationEntity organization = findOrganization(organizationId);
        copyFields(organization, request, true);
        saveLogoBlob(organization, request);

        OrganizationEntity updated = organizationRepository.save(organization);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteOrganization(String organizationId) {
        logger.info("Deleting organization: {}", organizationId);

        OrganizationEntity organization = findOrganization(organizationId);
        organizationRepository.delete(organization);

        logger.info("Organization deleted: {}", organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getOrganizationLogo(String organizationId) {
        logger.info("Fetching organization logo: {}", organizationId);

        OrganizationEntity organization = findOrganization(organizationId);
        byte[] logo = organization.getLogoBlob();

        if (logo == null || logo.length == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(detectImageType(logo))
                .body(logo);
    }

    private void saveLogoBlob(OrganizationEntity organization, OrganizationRequestDTO request) {
        String logoValue = request.getLogoUrl();

        if (logoValue == null || logoValue.isBlank()) {
            return;
        }

        if (!logoValue.startsWith("data:image/")) {
            return;
        }

        try {
            String[] parts = logoValue.split(",", 2);
            if (parts.length != 2) {
                return;
            }

            byte[] imageBytes = Base64.getDecoder().decode(parts[1]);
            organization.setLogoBlob(imageBytes);
            organization.setLogoUrl(
                    "/api/organization/" + organization.getOrganizationId() + "/logo");

            logger.info("Organization logo saved in database: {}",
                    organization.getOrganizationId());
        } catch (IllegalArgumentException e) {
            logger.error("Unable to decode organization logo", e);
            throw new IllegalStateException("Unable to save organization logo");
        }
    }

    private MediaType detectImageType(byte[] bytes) {
        if (bytes.length >= 8
                && bytes[0] == (byte) 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47) {
            return MediaType.IMAGE_PNG;
        }

        if (bytes.length >= 3
                && bytes[0] == (byte) 0xFF
                && bytes[1] == (byte) 0xD8
                && bytes[2] == (byte) 0xFF) {
            return MediaType.IMAGE_JPEG;
        }

        if (bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P') {
            return MediaType.parseMediaType("image/webp");
        }

        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private OrganizationEntity findOrganization(String organizationId) {
        return organizationRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found with ID: " + organizationId));
    }

    private String generateOrganizationId() {
        String organizationId;
        do {
            organizationId = "ORG-" + UUID.randomUUID()
                    .toString().substring(0, 8).toUpperCase();
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
