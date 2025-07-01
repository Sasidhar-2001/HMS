package com.yourproject.controller;

import com.yourproject.dto.*;
import com.yourproject.entity.User;
import com.yourproject.service.ComplaintService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UserService userService;

    @Autowired
    public ComplaintController(ComplaintService complaintService, UserService userService) {
        this.complaintService = complaintService;
        this.userService = userService;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userService.findUserEntityByEmail(authentication.getName());
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()") // Students, Wardens, Admins can create
    public ResponseEntity<ApiResponse<ComplaintDto>> createComplaint(@Valid @RequestBody ComplaintRequestDto complaintRequestDto) {
        User currentUser = getCurrentUser();
        ComplaintDto createdComplaint = complaintService.createComplaint(complaintRequestDto, currentUser);
        return new ResponseEntity<>(ApiResponse.success(createdComplaint, "Complaint created successfully"), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponseDto<ComplaintDto>>> getAllComplaints(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority) {
        User currentUser = getCurrentUser();
        Page<ComplaintDto> complaintsPage = complaintService.getAllComplaints(pageable, currentUser, status, category, priority);
        PageResponseDto<ComplaintDto> pageResponseDto = new PageResponseDto<>(complaintsPage);
        return ResponseEntity.ok(ApiResponse.success(pageResponseDto, "Complaints fetched successfully"));
    }

    @GetMapping("/{complaintId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ComplaintDto>> getComplaintById(@PathVariable Long complaintId) {
        User currentUser = getCurrentUser();
        ComplaintDto complaintDto = complaintService.getComplaintById(complaintId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(complaintDto, "Complaint details fetched successfully"));
    }

    @PutMapping("/{complaintId}")
    @PreAuthorize("isAuthenticated()") // Permissions handled in service
    public ResponseEntity<ApiResponse<ComplaintDto>> updateComplaint(
            @PathVariable Long complaintId,
            @Valid @RequestBody ComplaintRequestDto complaintRequestDto) {
        User currentUser = getCurrentUser();
        ComplaintDto updatedComplaint = complaintService.updateComplaint(complaintId, complaintRequestDto, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedComplaint, "Complaint updated successfully"));
    }

    @PutMapping("/{complaintId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<ComplaintDto>> updateComplaintStatus(
            @PathVariable Long complaintId,
            @Valid @RequestBody ComplaintStatusUpdateRequestDto statusUpdateRequestDto) {
        User currentUser = getCurrentUser();
        ComplaintDto updatedComplaint = complaintService.updateComplaintStatus(complaintId, statusUpdateRequestDto, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedComplaint, "Complaint status updated successfully"));
    }

    @DeleteMapping("/{complaintId}")
    @PreAuthorize("isAuthenticated()") // Permissions handled in service
    public ResponseEntity<ApiResponse<String>> deleteComplaint(@PathVariable Long complaintId) {
        User currentUser = getCurrentUser();
        complaintService.deleteComplaint(complaintId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Complaint deleted successfully"));
    }

    @PostMapping("/{complaintId}/images")
    @PreAuthorize("isAuthenticated()") // Permissions handled in service
    public ResponseEntity<ApiResponse<ComplaintDto>> uploadComplaintImages(
            @PathVariable Long complaintId,
            @RequestParam("images") List<MultipartFile> files) { // "images" should match the form field name
        User currentUser = getCurrentUser();
        ComplaintDto updatedComplaint = complaintService.uploadComplaintImages(complaintId, files, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedComplaint, "Images uploaded successfully"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<ComplaintStatsDto>> getComplaintStats() {
        User currentUser = getCurrentUser(); // For potential future filtering based on user
        ComplaintStatsDto stats = complaintService.getComplaintStats(currentUser);
        return ResponseEntity.ok(ApiResponse.success(stats, "Complaint statistics fetched successfully"));
    }
}
