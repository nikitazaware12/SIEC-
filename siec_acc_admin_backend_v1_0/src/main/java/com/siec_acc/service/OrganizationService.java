package com.siec_acc.service;

import com.siec_acc.dto.request.OrganizationRequestDTO;
import com.siec_acc.dto.response.OrganizationResponseDTO;
import org.springframework.http.ResponseEntity;


public interface OrganizationService {

    OrganizationResponseDTO createOrganization(OrganizationRequestDTO request);

    OrganizationResponseDTO getOrganization(String organizationId);

    java.util.List<OrganizationResponseDTO> getAllOrganizations();



    OrganizationResponseDTO updateOrganization(
            String organizationId,
            OrganizationRequestDTO request);

    OrganizationResponseDTO patchOrganization(
            String organizationId,
            OrganizationRequestDTO request);

    void deleteOrganization(String organizationId);

    ResponseEntity<byte[]> getOrganizationLogo(String organizationId);
}