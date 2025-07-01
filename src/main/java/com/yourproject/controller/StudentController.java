package com.yourproject.controller;

import com.yourproject.dto.*;
import com.yourproject.entity.User;
import com.yourproject.service.*; // Import all services
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')") // All endpoints require STUDENT role
public class StudentController {

    private final UserService userService;
    private final ComplaintService complaintService;
    private final FeeService feeService;
    private final LeaveService leaveService;
    private final AnnouncementService announcementService;
    private final RoomService roomService; // For room details

    @Autowired
    public StudentController(UserService userService, ComplaintService complaintService,
                             FeeService feeService, LeaveService leaveService,
                             AnnouncementService announcementService, RoomService roomService) {
        this.userService = userService;
        this.complaintService = complaintService;
        this.feeService = feeService;
        this.leaveService = leaveService;
        this.announcementService = announcementService;
        this.roomService = roomService;
    }

    private User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userService.findUserEntityByEmail(authentication.getName());
    }

    private UserDto getCurrentUserDto() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userService.getUserByEmail(authentication.getName());
    }


    // @GetMapping("/dashboard")
    // public ResponseEntity<ApiResponse<StudentDashboardDto>> getDashboard() {
    //     User currentUser = getCurrentUserEntity();
    //     // StudentDashboardDto dashboardData = studentDashboardService.getStudentDashboard(currentUser);
    //     // return ResponseEntity.ok(ApiResponse.success(dashboardData, "Dashboard data fetched."));
    //     return ResponseEntity.ok(ApiResponse.success(null, "Dashboard endpoint placeholder"));
    // }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> updateStudentProfile(@Valid @RequestBody ProfileUpdateRequestDto profileUpdateRequest) {
        User currentUser = getCurrentUserEntity();
        UserDto updatedUser = userService.updateUserProfile(currentUser.getEmail(), profileUpdateRequest);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Profile updated successfully"));
    }

    @GetMapping("/complaints")
    public ResponseEntity<ApiResponse<PageResponseDto<ComplaintDto>>> getMyComplaints(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) String status) {
        User currentUser = getCurrentUserEntity();
        // Service method already filters by current user if role is student
        Page<ComplaintDto> complaintsPage = complaintService.getAllComplaints(pageable, currentUser, status, null, null);
        PageResponseDto<ComplaintDto> pageResponseDto = new PageResponseDto<>(complaintsPage);
        return ResponseEntity.ok(ApiResponse.success(pageResponseDto, "My complaints fetched successfully"));
    }

    @GetMapping("/fees")
    public ResponseEntity<ApiResponse<PageResponseDto<FeeDto>>> getMyFees(
            @PageableDefault(size = 10, sort = "dueDate") Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer year) {
        User currentUser = getCurrentUserEntity();
        Page<FeeDto> feesPage = feeService.getAllFees(pageable, currentUser, status, null, null, year);
        PageResponseDto<FeeDto> pageResponseDto = new PageResponseDto<>(feesPage);
        return ResponseEntity.ok(ApiResponse.success(pageResponseDto, "My fees fetched successfully"));
    }

    @GetMapping("/leaves")
    public ResponseEntity<ApiResponse<PageResponseDto<LeaveDto>>> getMyLeaves(
            @PageableDefault(size = 10, sort = "appliedDate") Pageable pageable,
            @RequestParam(required = false) String status) {
        User currentUser = getCurrentUserEntity();
        Page<LeaveDto> leavesPage = leaveService.getAllLeaveApplications(pageable, currentUser, status, null);
        PageResponseDto<LeaveDto> pageResponseDto = new PageResponseDto<>(leavesPage);
        return ResponseEntity.ok(ApiResponse.success(pageResponseDto, "My leaves fetched successfully"));
    }

    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<PageResponseDto<AnnouncementDto>>> getStudentAnnouncements(
            @PageableDefault(size = 10, sort = "publishDate") Pageable pageable,
             @RequestParam(required = false) String type) {
        User currentUser = getCurrentUserEntity();
        // Service method filters for student-relevant announcements
        Page<AnnouncementDto> announcementsPage = announcementService.getAllAnnouncements(pageable, currentUser, type, "PUBLISHED", null);
        PageResponseDto<AnnouncementDto> pageResponseDto = new PageResponseDto<>(announcementsPage);
        return ResponseEntity.ok(ApiResponse.success(pageResponseDto, "Announcements fetched successfully"));
    }

    @PostMapping("/announcements/{announcementId}/read")
    public ResponseEntity<ApiResponse<String>> markAnnouncementRead(@PathVariable Long announcementId) {
        User currentUser = getCurrentUserEntity();
        // The getAnnouncementById service method handles marking as read for students
        announcementService.getAnnouncementById(announcementId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Announcement marked as read (if applicable)"));
    }

    @GetMapping("/room")
    public ResponseEntity<ApiResponse<RoomDto>> getMyRoomDetails() {
        UserDto currentUserDto = getCurrentUserDto();
        if (currentUserDto.getCurrentRoom() == null || currentUserDto.getCurrentRoom().getId() == null) {
             return ResponseEntity.ok(ApiResponse.success(null, "No room assigned to student."));
        }
        RoomDto roomDetails = roomService.getRoomById(currentUserDto.getCurrentRoom().getId());
        return ResponseEntity.ok(ApiResponse.success(roomDetails, "Room details fetched successfully"));
    }
}
