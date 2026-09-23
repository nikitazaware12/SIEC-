package com.siec_acc.service;

import com.siec_acc.dto.request.OrganizationRequestDTO;
import com.siec_acc.dto.response.OrganizationResponseDTO;



public interface OrganizationService {

    OrganizationResponseDTO createOrganization(OrganizationRequestDTO request);

    OrganizationResponseDTO getOrganization(String organizationId);



    OrganizationResponseDTO updateOrganization(
            String organizationId,
            OrganizationRequestDTO request);

    OrganizationResponseDTO patchOrganization(
            String organizationId,
            OrganizationRequestDTO request);

    void deleteOrganization(String organizationId);
}