package com.siec_acc.service.serviceImpl;

import com.siec_acc.dto.request.CustomerPaymentRequestDTO;
import com.siec_acc.dto.response.CustomerPaymentResponseDTO;
import com.siec_acc.entity.CustomerPaymentEntity;
import com.siec_acc.exceptions.DuplicateResourceException;
import com.siec_acc.exceptions.ResourceNotFoundException;
import com.siec_acc.repository.CustomerPaymentRepository;
import com.siec_acc.service.CustomerPaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerPaymentServiceImpl implements CustomerPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerPaymentServiceImpl.class);
    private final CustomerPaymentRepository customerPaymentRepository;

    public CustomerPaymentServiceImpl(CustomerPaymentRepository customerPaymentRepository) {
        this.customerPaymentRepository = customerPaymentRepository;
    }

    @Override
    @Transactional
    public CustomerPaymentResponseDTO createPayment(CustomerPaymentRequestDTO request) {
        validateRequest(request, false);

        if (request.getReceiptNumber() != null && !request.getReceiptNumber().isBlank()
                && customerPaymentRepository.existsByReceiptNumberIgnoreCase(request.getReceiptNumber().trim())) {
            throw new DuplicateResourceException("Payment with receipt number '" + request.getReceiptNumber() + "' already exists.");
        }

        CustomerPaymentEntity payment = new CustomerPaymentEntity();
        payment.setPaymentId(generatePaymentId());
        applyFields(payment, request, false);
        calculateAmounts(payment);

        CustomerPaymentEntity saved = customerPaymentRepository.save(payment);
        logger.info("Customer payment created successfully: {}", saved.getPaymentId());
        return toResponse(saved);
    }

    @Override
    public CustomerPaymentResponseDTO getPayment(String paymentId) {
        return toResponse(findPayment(paymentId));
    }

    @Override
    public List<CustomerPaymentResponseDTO> getAllPayments() {
        return customerPaymentRepository.findAllByOrderByPaymentDateDescPaymentPrimeIdDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomerPaymentResponseDTO updatePayment(String paymentId, CustomerPaymentRequestDTO request) {
        validateRequest(request, false);
        CustomerPaymentEntity payment = findPayment(paymentId);
        checkReceiptDuplicate(payment, request.getReceiptNumber());
        applyFields(payment, request, false);
        calculateAmounts(payment);
        CustomerPaymentEntity saved = customerPaymentRepository.save(payment);
        logger.info("Customer payment updated successfully: {}", paymentId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CustomerPaymentResponseDTO patchPayment(String paymentId, CustomerPaymentRequestDTO request) {
        CustomerPaymentEntity payment = findPayment(paymentId);
        if (request.getReceiptNumber() != null && !request.getReceiptNumber().isBlank()) {
            checkReceiptDuplicate(payment, request.getReceiptNumber());
        }
        applyFields(payment, request, true);
        validateSavedPayment(payment);
        calculateAmounts(payment);
        CustomerPaymentEntity saved = customerPaymentRepository.save(payment);
        logger.info("Customer payment partially updated successfully: {}", paymentId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deletePayment(String paymentId) {
        CustomerPaymentEntity payment = findPayment(paymentId);
        customerPaymentRepository.delete(payment);
        logger.info("Customer payment deleted successfully: {}", paymentId);
    }

    private CustomerPaymentEntity findPayment(String paymentId) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("Payment ID is required.");
        }
        return customerPaymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer payment not found: " + paymentId));
    }

    private void validateRequest(CustomerPaymentRequestDTO request, boolean partial) {
        if (request == null) throw new IllegalArgumentException("Payment request is required.");
        if (partial) return;
        if (request.getPaymentDate() == null) throw new IllegalArgumentException("Payment date is required.");
        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) throw new IllegalArgumentException("Customer name is required.");
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        if (request.getPaymentMode() == null || request.getPaymentMode().isBlank()) throw new IllegalArgumentException("Payment mode is required.");
    }

    private void validateSavedPayment(CustomerPaymentEntity payment) {
        if (payment.getPaymentDate() == null) throw new IllegalArgumentException("Payment date is required.");
        if (payment.getCustomerName() == null || payment.getCustomerName().isBlank()) throw new IllegalArgumentException("Customer name is required.");
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        if (payment.getPaymentMode() == null || payment.getPaymentMode().isBlank()) throw new IllegalArgumentException("Payment mode is required.");
    }

    private void applyFields(CustomerPaymentEntity payment, CustomerPaymentRequestDTO request, boolean partial) {
        if (!partial || request.getReceiptNumber() != null) payment.setReceiptNumber(trim(request.getReceiptNumber()));
        if (!partial || request.getPaymentDate() != null) payment.setPaymentDate(request.getPaymentDate());
        if (!partial || request.getCustomerName() != null) payment.setCustomerName(trim(request.getCustomerName()));
        if (!partial || request.getAmount() != null) payment.setAmount(request.getAmount());
        if (!partial || request.getPaymentMode() != null) payment.setPaymentMode(trim(request.getPaymentMode()));
        if (!partial || request.getReferenceNumber() != null) payment.setReferenceNumber(trim(request.getReferenceNumber()));
        if (!partial || request.getBankAccount() != null) payment.setBankAccount(trim(request.getBankAccount()));
        if (!partial || request.getInvoiceReference() != null) payment.setInvoiceReference(trim(request.getInvoiceReference()));
        if (!partial || request.getAllocatedAmount() != null) payment.setAllocatedAmount(request.getAllocatedAmount());
        if (!partial || request.getStatus() != null) payment.setStatus(trim(request.getStatus()));
        if (!partial || request.getNotes() != null) payment.setNotes(trim(request.getNotes()));
    }

    private void calculateAmounts(CustomerPaymentEntity payment) {
        BigDecimal amount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
        BigDecimal allocated = payment.getAllocatedAmount() == null ? BigDecimal.ZERO : payment.getAllocatedAmount();
        if (allocated.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Allocated amount cannot be negative.");
        if (allocated.compareTo(amount) > 0) throw new IllegalArgumentException("Allocated amount cannot be greater than payment amount.");
        payment.setAllocatedAmount(allocated);
        payment.setUnallocatedAmount(amount.subtract(allocated));
        if (payment.getStatus() == null || payment.getStatus().isBlank()) payment.setStatus("RECEIVED");
    }

    private void checkReceiptDuplicate(CustomerPaymentEntity payment, String receiptNumber) {
        if (receiptNumber == null || receiptNumber.isBlank()) return;
        String requested = receiptNumber.trim();
        if (requested.equalsIgnoreCase(payment.getReceiptNumber())) return;
        if (customerPaymentRepository.existsByReceiptNumberIgnoreCase(requested)) {
            throw new DuplicateResourceException("Payment with receipt number '" + requested + "' already exists.");
        }
    }

    private String generatePaymentId() {
        return "CP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private CustomerPaymentResponseDTO toResponse(CustomerPaymentEntity payment) {
        CustomerPaymentResponseDTO response = new CustomerPaymentResponseDTO();
        response.setPaymentId(payment.getPaymentId());
        response.setReceiptNumber(payment.getReceiptNumber());
        response.setPaymentDate(payment.getPaymentDate());
        response.setCustomerName(payment.getCustomerName());
        response.setAmount(payment.getAmount());
        response.setPaymentMode(payment.getPaymentMode());
        response.setReferenceNumber(payment.getReferenceNumber());
        response.setBankAccount(payment.getBankAccount());
        response.setInvoiceReference(payment.getInvoiceReference());
        response.setAllocatedAmount(payment.getAllocatedAmount());
        response.setUnallocatedAmount(payment.getUnallocatedAmount());
        response.setStatus(payment.getStatus());
        response.setNotes(payment.getNotes());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }
}
