package com.siec_acc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organization")
public class OrganizationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "organization_prime_id")
    private Long organizationPrimeId;

    @Column(name = "organization_id", unique = true, nullable = false, length = 40)
    private String organizationId;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "country")
    private String country;

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "website")
    private String website;

    @Lob
    @Column(name = "logo_blob", columnDefinition = "LONGBLOB")
    private byte[] logoBlob;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_ifsc_code")
    private String bankIfscCode;

    @Column(name = "gst_registered")
    private Boolean gstRegistered;

    @Column(name = "gst_number")
    private String gstNumber;

    @Column(name = "tax_registration_type")
    private String taxRegistrationType;

    @Column(name = "tax_state")
    private String taxState;

    @Column(name = "default_tax_rate")
    private String defaultTaxRate;

    @Column(name = "tax_inclusive")
    private Boolean taxInclusive;

    @Column(name = "default_unit")
    private String defaultUnit;

    @Column(name = "unit_system")
    private String unitSystem;

    @Column(name = "decimal_precision")
    private Integer decimalPrecision;

    @Column(name = "financial_year_start")
    private String financialYearStart;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "organization_status")
    private String organizationStatus;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrganizationDocumentEntity> documents = new ArrayList<>();

    @Lob
    @Column(name = "documents_blob", columnDefinition = "LONGBLOB")
    private byte[] legacyDocumentsBlob;

    @Column(name = "documents_url", columnDefinition = "LONGTEXT")
    private String legacyDocumentsUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public OrganizationEntity() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.organizationStatus == null) this.organizationStatus = "ACTIVE";
        if (this.gstRegistered == null) this.gstRegistered = false;
        if (this.taxInclusive == null) this.taxInclusive = false;
        if (this.decimalPrecision == null) this.decimalPrecision = 2;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getOrganizationPrimeId() { return organizationPrimeId; }
    public void setOrganizationPrimeId(Long organizationPrimeId) { this.organizationPrimeId = organizationPrimeId; }
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public byte[] getLogoBlob() { return logoBlob; }
    public void setLogoBlob(byte[] logoBlob) { this.logoBlob = logoBlob; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public String getBankIfscCode() { return bankIfscCode; }
    public void setBankIfscCode(String bankIfscCode) { this.bankIfscCode = bankIfscCode; }
    public Boolean getGstRegistered() { return gstRegistered; }
    public void setGstRegistered(Boolean gstRegistered) { this.gstRegistered = gstRegistered; }
    public String getGstNumber() { return gstNumber; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }
    public String getTaxRegistrationType() { return taxRegistrationType; }
    public void setTaxRegistrationType(String taxRegistrationType) { this.taxRegistrationType = taxRegistrationType; }
    public String getTaxState() { return taxState; }
    public void setTaxState(String taxState) { this.taxState = taxState; }
    public String getDefaultTaxRate() { return defaultTaxRate; }
    public void setDefaultTaxRate(String defaultTaxRate) { this.defaultTaxRate = defaultTaxRate; }
    public Boolean getTaxInclusive() { return taxInclusive; }
    public void setTaxInclusive(Boolean taxInclusive) { this.taxInclusive = taxInclusive; }
    public String getDefaultUnit() { return defaultUnit; }
    public void setDefaultUnit(String defaultUnit) { this.defaultUnit = defaultUnit; }
    public String getUnitSystem() { return unitSystem; }
    public void setUnitSystem(String unitSystem) { this.unitSystem = unitSystem; }
    public Integer getDecimalPrecision() { return decimalPrecision; }
    public void setDecimalPrecision(Integer decimalPrecision) { this.decimalPrecision = decimalPrecision; }
    public String getFinancialYearStart() { return financialYearStart; }
    public void setFinancialYearStart(String financialYearStart) { this.financialYearStart = financialYearStart; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getOrganizationStatus() { return organizationStatus; }
    public void setOrganizationStatus(String organizationStatus) { this.organizationStatus = organizationStatus; }
    public List<OrganizationDocumentEntity> getDocuments() { return documents; }
    public void setDocuments(List<OrganizationDocumentEntity> documents) { this.documents = documents; }
    public byte[] getLegacyDocumentsBlob() { return legacyDocumentsBlob; }
    public void setLegacyDocumentsBlob(byte[] legacyDocumentsBlob) { this.legacyDocumentsBlob = legacyDocumentsBlob; }
    public String getLegacyDocumentsUrl() { return legacyDocumentsUrl; }
    public void setLegacyDocumentsUrl(String legacyDocumentsUrl) { this.legacyDocumentsUrl = legacyDocumentsUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
