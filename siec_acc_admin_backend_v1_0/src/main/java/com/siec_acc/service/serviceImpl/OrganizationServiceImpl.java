package com.siec_acc.service.serviceImpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siec_acc.dto.request.OrganizationRequestDTO;
import com.siec_acc.dto.response.OrganizationResponseDTO;
import com.siec_acc.entity.OrganizationDocumentEntity;
import com.siec_acc.entity.OrganizationEntity;
import com.siec_acc.exceptions.ResourceNotFoundException;
import com.siec_acc.repository.OrganizationRepository;
import com.siec_acc.service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationServiceImpl.class);
    private static final long MAX_DOCUMENT_SIZE = 5L * 1024L * 1024L;
    private static final List<String> ALLOWED_DOCUMENT_TYPES = List.of(
            "application/pdf", "image/jpeg", "image/png", "image/webp");

    private final OrganizationRepository organizationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrganizationServiceImpl(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    @Transactional
    public OrganizationResponseDTO createOrganization(OrganizationRequestDTO request) {
        logger.info("Creating organization");
        OrganizationEntity organization = organizationRepository.findAll().stream().findFirst().orElseGet(() -> {
            OrganizationEntity newOrganization = new OrganizationEntity();
            newOrganization.setOrganizationId(generateOrganizationId());
            return newOrganization;
        });
        copyFields(organization, request, false);
        saveLogoBlob(organization, request);
        migrateLegacyDocuments(organization);
        OrganizationEntity saved = organizationRepository.save(organization);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public OrganizationResponseDTO getOrganization(String organizationId) {
        OrganizationEntity organization = findOrganization(organizationId);
        migrateLegacyDocuments(organization);
        return toResponse(organization);
    }

    @Override
    @Transactional
    public List<OrganizationResponseDTO> getAllOrganizations() {
        return organizationRepository.findAll().stream().map(organization -> {
            migrateLegacyDocuments(organization);
            return toResponse(organization);
        }).toList();
    }

    @Override
    @Transactional
    public OrganizationResponseDTO updateOrganization(String organizationId, OrganizationRequestDTO request) {
        logger.info("PUT organization: {}", organizationId);
        OrganizationEntity organization = organizationRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> organizationRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Organization not found")));
        copyFields(organization, request, false);
        saveLogoBlob(organization, request);
        migrateLegacyDocuments(organization);
        return toResponse(organizationRepository.save(organization));
    }

    @Override
    @Transactional
    public OrganizationResponseDTO patchOrganization(String organizationId, OrganizationRequestDTO request) {
        logger.info("PATCH organization: {}", organizationId);
        OrganizationEntity organization = findOrganization(organizationId);
        copyFields(organization, request, true);
        saveLogoBlob(organization, request);
        migrateLegacyDocuments(organization);
        return toResponse(organizationRepository.save(organization));
    }

    @Override
    @Transactional
    public void deleteOrganization(String organizationId) {
        OrganizationEntity organization = findOrganization(organizationId);
        organizationRepository.delete(organization);
        logger.info("Organization deleted: {}", organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getOrganizationLogo(String organizationId) {
        byte[] logo = findOrganization(organizationId).getLogoBlob();
        if (logo == null || logo.length == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(detectImageType(logo)).body(logo);
    }

    @Override
    @Transactional
    public OrganizationResponseDTO uploadDocument(String organizationId, String documentName, MultipartFile file) {
        OrganizationEntity organization = findOrganization(organizationId);
        validateDocument(documentName, file);
        migrateLegacyDocuments(organization);

        try {
            String documentId = generateDocumentId();
            OrganizationDocumentEntity document = new OrganizationDocumentEntity();
            document.setDocumentId(documentId);
            document.setOrganization(organization);
            document.setDocumentName(documentName.trim());
            document.setOriginalFileName(file.getOriginalFilename() == null ? "document" : file.getOriginalFilename());
            document.setContentType(file.getContentType().toLowerCase(Locale.ROOT));
            document.setFileSize(file.getSize());
            document.setDocumentBlob(file.getBytes());
            document.setDocumentUrl("/api/organization/" + organizationId + "/documents/" + documentId + "/content");
            organization.getDocuments().add(0, document);
            OrganizationEntity saved = organizationRepository.save(organization);
            logger.info("Document saved in child table: {}", documentId);
            return toResponse(saved);
        } catch (IOException exception) {
            logger.error("Unable to read organization document", exception);
            throw new IllegalStateException("Unable to save document");
        }
    }

    @Override
    @Transactional
    public List<Map<String, Object>> getDocuments(String organizationId) {
        OrganizationEntity organization = findOrganization(organizationId);
        migrateLegacyDocuments(organization);
        return organization.getDocuments().stream().map(this::documentMetadata).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getDocumentContent(String organizationId, String documentId) {
        OrganizationDocumentEntity document = findDocument(organizationId, documentId);
        byte[] data = document.getDocumentBlob();
        if (data == null || data.length == 0) throw new ResourceNotFoundException("Document content not found");

        String contentType = document.getContentType() == null ? "application/octet-stream" : document.getContentType();
        String fileName = document.getOriginalFileName() == null ? "document" : document.getOriginalFileName();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName.replace("\"", "") + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(data.length)
                .body(data);
    }

    @Override
    @Transactional
    public void deleteDocument(String organizationId, String documentId) {
        OrganizationEntity organization = findOrganization(organizationId);
        OrganizationDocumentEntity document = organization.getDocuments().stream()
                .filter(item -> documentId.equals(item.getDocumentId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));
        organization.getDocuments().remove(document);
        document.setOrganization(null);
        organizationRepository.save(organization);
        logger.info("Document deleted: {}", documentId);
    }

    private void validateDocument(String documentName, MultipartFile file) {
        if (documentName == null || documentName.isBlank()) throw new IllegalArgumentException("Document name is required");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Document file is required");
        if (file.getSize() > MAX_DOCUMENT_SIZE) throw new IllegalArgumentException("File size must not exceed 5 MB");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_DOCUMENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only PDF, JPG, JPEG, PNG or WEBP files are allowed");
        }
    }

    private OrganizationDocumentEntity findDocument(String organizationId, String documentId) {
        OrganizationEntity organization = findOrganization(organizationId);
        migrateLegacyDocuments(organization);
        return organization.getDocuments().stream()
                .filter(item -> documentId.equals(item.getDocumentId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));
    }

    private Map<String, Object> documentMetadata(OrganizationDocumentEntity document) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentId", document.getDocumentId());
        metadata.put("documentName", document.getDocumentName());
        metadata.put("originalFileName", document.getOriginalFileName());
        metadata.put("contentType", document.getContentType());
        metadata.put("fileSize", document.getFileSize());
        metadata.put("documentUrl", document.getDocumentUrl());
        metadata.put("createdAt", document.getCreatedAt());
        return metadata;
    }

    private void migrateLegacyDocuments(OrganizationEntity organization) {
        byte[] legacyBlob = organization.getLegacyDocumentsBlob();
        if (legacyBlob == null || legacyBlob.length == 0 || !organization.getDocuments().isEmpty()) return;

        try {
            List<Map<String, Object>> legacyDocuments = objectMapper.readValue(
                    legacyBlob, new TypeReference<List<Map<String, Object>>>() { });
            for (Map<String, Object> legacy : legacyDocuments) {
                String base64 = String.valueOf(legacy.getOrDefault("dataBase64", ""));
                if (base64.isBlank()) continue;
                OrganizationDocumentEntity document = new OrganizationDocumentEntity();
                document.setDocumentId(String.valueOf(legacy.getOrDefault("documentId", generateDocumentId())));
                document.setOrganization(organization);
                document.setDocumentName(String.valueOf(legacy.getOrDefault("documentName", "Document")));
                document.setOriginalFileName(String.valueOf(legacy.getOrDefault("originalFileName", "document")));
                document.setContentType(String.valueOf(legacy.getOrDefault("contentType", "application/octet-stream")));
                Object size = legacy.get("fileSize");
                document.setFileSize(size == null ? null : Long.valueOf(String.valueOf(size)));
                document.setDocumentBlob(Base64.getDecoder().decode(base64));
                document.setDocumentUrl(String.valueOf(legacy.getOrDefault("documentUrl", "/api/organization/" + organization.getOrganizationId() + "/documents/" + document.getDocumentId() + "/content")));
                Object createdAt = legacy.get("createdAt");
                if (createdAt != null) document.setCreatedAt(LocalDateTime.parse(String.valueOf(createdAt)));
                organization.getDocuments().add(document);
            }
            if (!organization.getDocuments().isEmpty()) {
                organization.setLegacyDocumentsBlob(null);
                organization.setLegacyDocumentsUrl(null);
                organizationRepository.save(organization);
                logger.info("Migrated legacy documents for organization: {}", organization.getOrganizationId());
            }
        } catch (Exception exception) {
            logger.warn("Legacy document migration skipped for organization: {}", organization.getOrganizationId(), exception);
        }
    }

    private void saveLogoBlob(OrganizationEntity organization, OrganizationRequestDTO request) {
        String logoValue = request.getLogoUrl();
        if (logoValue == null || logoValue.isBlank() || !logoValue.startsWith("data:image/")) return;
        try {
            String[] parts = logoValue.split(",", 2);
            if (parts.length != 2) return;
            organization.setLogoBlob(Base64.getDecoder().decode(parts[1]));
            organization.setLogoUrl("/api/organization/" + organization.getOrganizationId() + "/logo");
        } catch (IllegalArgumentException exception) {
            logger.error("Unable to decode organization logo", exception);
            throw new IllegalStateException("Unable to save organization logo");
        }
    }

    private MediaType detectImageType(byte[] bytes) {
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return MediaType.IMAGE_PNG;
        if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) return MediaType.IMAGE_JPEG;
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return MediaType.parseMediaType("image/webp");
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private OrganizationEntity findOrganization(String organizationId) {
        return organizationRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + organizationId));
    }

    private String generateOrganizationId() {
        String organizationId;
        do {
            organizationId = "ORG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (organizationRepository.existsByOrganizationId(organizationId));
        return organizationId;
    }

    private String generateDocumentId() {
        return "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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
        if (!skipNulls || r.getBankName() != null) o.setBankName(r.getBankName());
        if (!skipNulls || r.getBankAccountNumber() != null) o.setBankAccountNumber(r.getBankAccountNumber());
        if (!skipNulls || r.getBankIfscCode() != null) o.setBankIfscCode(r.getBankIfscCode());
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
        r.setBankName(o.getBankName());
        r.setBankAccountNumber(o.getBankAccountNumber());
        r.setBankIfscCode(o.getBankIfscCode());
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
        r.setDocuments(o.getDocuments().stream().map(this::documentMetadata).toList());
        r.setDocumentsUrl(o.getDocuments().stream().map(OrganizationDocumentEntity::getDocumentUrl)
                .filter(url -> url != null && !url.isBlank()).reduce((a, b) -> a + "\n" + b).orElse(""));
        r.setCreatedAt(o.getCreatedAt());
        r.setUpdatedAt(o.getUpdatedAt());
        return r;
    }
}
