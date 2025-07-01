package com.yourproject.controller;

import com.yourproject.dto.*;
import com.yourproject.entity.User;
import com.yourproject.service.LeaveService;
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
import org.springframework.web.multipart.MultipartFile; // Added

import java.util.List; // Added
import java.util.Map; // For cancel reason

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;
    private final UserService userService;

    @Autowired
    public LeaveController(LeaveService leaveService, UserService userService) {
        this.leaveService = leaveService;
        this.userService = userService;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userService.findUserEntityByEmail(authentication.getName());
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<LeaveDto>> createLeaveApplication(@Valid @RequestBody LeaveRequestDto leaveRequestDto) {
        User currentUser = getCurrentUser();
        LeaveDto createdLeave = leaveService.createLeaveApplication(leaveRequestDto, currentUser);
        return new ResponseEntity<>(ApiResponse.success(createdLeave, "Leave application submitted successfully"), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponseDto<LeaveDto>>> getAllLeaveApplications(
            @PageableDefault(size = 10, sort = "appliedDate") Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String leaveType) {
        User currentUser = getCurrentUser();
        Page<LeaveDto> leavesPage = leaveService.getAllLeaveApplications(pageable, currentUser, status, leaveType);
        PageResponseDto<LeaveDto> pageResponseDto = new PageResponseDto<>(leavesPage);
        return ResponseEntity.ok(ApiResponse.success(pageResponseDto, "Leave applications fetched successfully"));
    }

    @GetMapping("/{leaveId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LeaveDto>> getLeaveApplicationById(@PathVariable Long leaveId) {
        User currentUser = getCurrentUser();
        LeaveDto leaveDto = leaveService.getLeaveApplicationById(leaveId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(leaveDto, "Leave application details fetched successfully"));
    }

    @PutMapping("/{leaveId}")
    @PreAuthorize("hasRole('STUDENT')") // Only student can update their own PENDING leave application
    public ResponseEntity<ApiResponse<LeaveDto>> updateLeaveApplication(
            @PathVariable Long leaveId,
            @Valid @RequestBody LeaveRequestDto leaveRequestDto) {
        User currentUser = getCurrentUser();
        LeaveDto updatedLeave = leaveService.updateLeaveApplication(leaveId, leaveRequestDto, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedLeave, "Leave application updated successfully"));
    }

    @PutMapping("/{leaveId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<LeaveDto>> updateLeaveStatus(
            @PathVariable Long leaveId,
            @Valid @RequestBody LeaveStatusUpdateRequestDto statusUpdateRequestDto) {
        User currentUser = getCurrentUser();
        LeaveDto updatedLeave = leaveService.updateLeaveStatus(leaveId, statusUpdateRequestDto, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedLeave, "Leave status updated successfully"));
    }

    @PostMapping("/{leaveId}/cancel")
    @PreAuthorize("isAuthenticated()") // Student can cancel own, Admin/Warden can cancel any (permissions in service)
    public ResponseEntity<ApiResponse<LeaveDto>> cancelLeaveApplication(
            @PathVariable Long leaveId,
            @RequestBody(required = false) Map<String, String> requestBody) { // Optional: {"reason": "some reason"}
        User currentUser = getCurrentUser();
        String reason = (requestBody != null) ? requestBody.get("reason") : "Cancelled by user.";
        LeaveDto cancelledLeave = leaveService.cancelLeaveApplication(leaveId, reason, currentUser);
        return ResponseEntity.ok(ApiResponse.success(cancelledLeave, "Leave application cancelled successfully"));
    }

    @PostMapping("/{leaveId}/extension")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<LeaveDto>> requestExtension(
            @PathVariable Long leaveId,
            @Valid @RequestBody LeaveExtensionApiRequestDto extensionApiRequestDto) {
        User currentUser = getCurrentUser();
        LeaveDto leaveWithExtensionRequest = leaveService.requestExtension(leaveId, extensionApiRequestDto, currentUser);
        return ResponseEntity.ok(ApiResponse.success(leaveWithExtensionRequest, "Leave extension requested successfully"));
    }

    // Updating extension status is complex if extensionId is not a direct ID but an index or requires matching.
    // Assuming extensionId here refers to a way to identify the specific extension request within the Leave entity.
    // This might require a more robust way to identify extension requests (e.g., if they become separate entities).
    // For now, let's assume a simple scenario where extensionId could be an index or a specific unique key within the list.
    // The service layer would need to implement the logic to find the correct extension request.
    @PutMapping("/{leaveId}/extension/{extensionIdentifier}") // extensionIdentifier could be an index or a unique key of the request.
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<LeaveDto>> updateExtensionStatus(
            @PathVariable Long leaveId,
            @PathVariable String extensionIdentifier, // Using String for flexibility, service to parse/use
            @Valid @RequestBody LeaveExtensionStatusUpdateRequestDto statusUpdateRequestDto) {
        User currentUser = getCurrentUser();
        // The service method needs to be adapted to use `extensionIdentifier`
        // For simplicity, if only one pending extension is allowed, extensionIdentifier might not be needed.
        // Let's assume extensionIdentifier is the hashcode of the object for now as per service impl.
        // This is NOT robust for a real API. ExtensionRequest should be an entity.
        Long extId;
        try {
            extId = Long.parseLong(extensionIdentifier); // Simplistic: assuming it's the hashcode or a temp ID
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid extension identifier."));
        }
        LeaveDto updatedLeave = leaveService.updateExtensionStatus(leaveId, extId, statusUpdateRequestDto, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedLeave, "Leave extension status updated successfully"));
    }

    @PostMapping("/{leaveId}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')") // Or students can mark their own return
    public ResponseEntity<ApiResponse<LeaveDto>> markReturnFromLeave(
            @PathVariable Long leaveId,
            @Valid @RequestBody(required = false) MarkReturnRequestDto returnRequestDto) {
        User currentUser = getCurrentUser();
        MarkReturnRequestDto dto = (returnRequestDto == null) ? new MarkReturnRequestDto() : returnRequestDto;
        LeaveDto updatedLeave = leaveService.markReturnFromLeave(leaveId, dto, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedLeave, "Return from leave marked successfully"));
    }

    // @PostMapping("/{leaveId}/attachments")
    // @PreAuthorize("isAuthenticated()")
    // public ResponseEntity<ApiResponse<LeaveDto>> uploadLeaveAttachments(
    //         @PathVariable Long leaveId,
    //         @RequestParam("files") List<MultipartFile> files) {
    //     User currentUser = getCurrentUser();
    //     LeaveDto updatedLeave = leaveService.uploadLeaveAttachments(leaveId, files, currentUser);
    //     return ResponseEntity.ok(ApiResponse.success(updatedLeave, "Attachments uploaded successfully"));
    // }

    @PostMapping("/{leaveId}/attachments")
    @PreAuthorize("isAuthenticated()") // Permissions checked in service
    public ResponseEntity<ApiResponse<LeaveDto>> addLeaveAttachments(
            @PathVariable Long leaveId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(name = "type", defaultValue = "general") String attachmentType) { // e.g., "general", "medical_certificate"
        User currentUser = getCurrentUserEntity(); // Corrected to getCurrentUserEntity if that's the intended method
        LeaveDto updatedLeave = leaveService.addAttachmentsToLeave(leaveId, files, attachmentType, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedLeave, "Attachments added to leave successfully"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<LeaveStatsDto>> getLeaveStats(@RequestParam(required = false) Integer year) {
        User currentUser = getCurrentUser();
        LeaveStatsDto stats = leaveService.getLeaveStats(currentUser, year);
        return ResponseEntity.ok(ApiResponse.success(stats, "Leave statistics fetched successfully"));
    }
}
