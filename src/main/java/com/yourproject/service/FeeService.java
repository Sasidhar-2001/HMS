package com.yourproject.service;

import com.yourproject.dto.FeeDto;
import com.yourproject.dto.FeeRequestDto;
import com.yourproject.dto.FeePaymentRequestDto;
import com.yourproject.dto.FeeStatsDto;
import com.yourproject.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeeService {

    FeeDto createFee(FeeRequestDto feeRequestDto, User currentUser);
    FeeDto getFeeById(Long feeId, User currentUser);
    Page<FeeDto> getAllFees(Pageable pageable, User currentUser, String status, String feeType, Integer month, Integer year);
    FeeDto updateFee(Long feeId, FeeRequestDto feeRequestDto, User currentUser);
    // No direct delete for fees usually, maybe a cancel/waive status.

    FeeDto addPayment(Long feeId, FeePaymentRequestDto paymentDto, User currentUser);
    void sendFeeReminder(Long feeId, User currentUser, String reminderType); // reminderType e.g. "email"
    void sendBulkFeeReminders(User currentUser, String status, String reminderType);

    FeeStatsDto getFeeStats(User currentUser, Integer year);
    Page<FeeDto> getDefaulters(Pageable pageable, User currentUser);
}
