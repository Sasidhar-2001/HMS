package com.yourproject.controller;

import com.yourproject.dto.*;
import com.yourproject.entity.User;
import com.yourproject.service.FeeService;
import com.yourproject.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map; // For simple reminder request

@RestController
@RequestMapping("/api/fees")
public class FeeController {

    private final FeeService feeService;
    private final UserService userService;

    @Autowired
    public FeeController(FeeService feeService, UserService userService) {
        this.feeService = feeService;
        this.userService = userService;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userService.findUserEntityByEmail(authentication.getName());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<FeeDto>> createFee(@Valid @RequestBody FeeRequestDto feeRequestDto) {
        User currentUser = getCurrentUser();
        FeeDto createdFee = feeService.createFee(feeRequestDto, currentUser);
        return new ResponseEntity<>(ApiResponse.success(createdFee, "Fee record created successfully"), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponseDto<FeeDto>>> getAllFees(
            @PageableDefault(size = 10, sort = "dueDate") Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String feeType,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        User currentUser = getCurrentUser();
        Page<FeeDto> feesPage = feeService.getAllFees(pageable, currentUser, status, feeType, month, year);
        PageResponseDto<FeeDto> pageResponseDto = new PageResponseDto<>(feesPage);
        return ResponseEntity.ok(ApiResponse.success(pageResponseDto, "Fees fetched successfully"));
    }

    @GetMapping("/{feeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FeeDto>> getFeeById(@PathVariable Long feeId) {
        User currentUser = getCurrentUser();
        FeeDto feeDto = feeService.getFeeById(feeId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(feeDto, "Fee details fetched successfully"));
    }

    @PutMapping("/{feeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<FeeDto>> updateFee(
            @PathVariable Long feeId,
            @Valid @RequestBody FeeRequestDto feeRequestDto) {
        User currentUser = getCurrentUser();
        FeeDto updatedFee = feeService.updateFee(feeId, feeRequestDto, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedFee, "Fee record updated successfully"));
    }

    @PostMapping("/{feeId}/payment")
    @PreAuthorize("isAuthenticated()") // Student can pay for self, Admin/Warden can pay on behalf
    public ResponseEntity<ApiResponse<FeeDto>> addPayment(
            @PathVariable Long feeId,
            @Valid @RequestBody FeePaymentRequestDto paymentDto) {
        User currentUser = getCurrentUser();
        FeeDto updatedFee = feeService.addPayment(feeId, paymentDto, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedFee, "Payment added successfully"));
    }

    @PostMapping("/{feeId}/reminder")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<String>> sendFeeReminder(
            @PathVariable Long feeId,
            @RequestBody(required = false) Map<String, String> requestBody) { // Optional: specify reminder type e.g. {"type": "email"}
        User currentUser = getCurrentUser();
        String reminderType = (requestBody != null && requestBody.containsKey("type")) ? requestBody.get("type") : "email";
        feeService.sendFeeReminder(feeId, currentUser, reminderType);
        return ResponseEntity.ok(ApiResponse.success("Fee reminder sent successfully"));
    }

    @PostMapping("/bulk-reminders")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<String>> sendBulkReminders(
            @RequestBody(required = false) Map<String, String> requestBody) { // Optional: {"status": "OVERDUE", "type": "email"}
        User currentUser = getCurrentUser();
        String status = (requestBody != null && requestBody.containsKey("status")) ? requestBody.get("status") : "OVERDUE";
        String reminderType = (requestBody != null && requestBody.containsKey("type")) ? requestBody.get("type") : "email";
        feeService.sendBulkFeeReminders(currentUser, status, reminderType);
        return ResponseEntity.ok(ApiResponse.success("Bulk fee reminders processed"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<FeeStatsDto>> getFeeStats(@RequestParam(required = false) Integer year) {
        User currentUser = getCurrentUser();
        FeeStatsDto stats = feeService.getFeeStats(currentUser, year);
        return ResponseEntity.ok(ApiResponse.success(stats, "Fee statistics fetched successfully"));
    }

    @GetMapping("/defaulters")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<PageResponseDto<FeeDto>>> getDefaulters(
            @PageableDefault(size = 10, sort = "dueDate") Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<FeeDto> defaultersPage = feeService.getDefaulters(pageable, currentUser);
        PageResponseDto<FeeDto> pageResponseDto = new PageResponseDto<>(defaultersPage);
        return ResponseEntity.ok(ApiResponse.success(pageResponseDto, "Defaulters list fetched successfully"));
    }
}
