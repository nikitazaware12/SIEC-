package com.siec_acc.service;

import com.siec_acc.dto.request.CustomerPaymentRequestDTO;
import com.siec_acc.dto.response.CustomerPaymentResponseDTO;

import java.util.List;

public interface CustomerPaymentService {
    CustomerPaymentResponseDTO createPayment(CustomerPaymentRequestDTO request);
    CustomerPaymentResponseDTO getPayment(String paymentId);
    List<CustomerPaymentResponseDTO> getAllPayments();
    CustomerPaymentResponseDTO updatePayment(String paymentId, CustomerPaymentRequestDTO request);
    CustomerPaymentResponseDTO patchPayment(String paymentId, CustomerPaymentRequestDTO request);
    void deletePayment(String paymentId);
}
