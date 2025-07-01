package com.yourproject.controller;

import com.yourproject.dto.*;
import com.yourproject.entity.Role;
import com.yourproject.entity.User; // For getCurrentUserEntity
import com.yourproject.service.*; // Import all relevant services
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

@RestController
@RequestMapping("/api/warden")
@PreAuthorize("hasAnyRole('WARDEN', 'ADMIN')") // Warden or Admin can access these
public class WardenController {

    private final UserService userService;
    private final ComplaintService complaintService;
    private final LeaveService leaveService;
    private final RoomService roomService;
    private final AnnouncementService announcementService;
    // private final WardenDashboardService dashboardService; // For dashboard

    @Autowired
    public WardenController(UserService userService, ComplaintService complaintService,
                            LeaveService leaveService, RoomService roomService,
                            AnnouncementService announcementService /*, WardenDashboardService dashboardService */) {
        this.userService = userService;
        this.complaintService = complaintService;
        this.leaveService = leaveService;
        this.roomService = roomService;
        this.announcementService = announcementService;
        // this.dashboardService = dashboardService;
    }

    private User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userService.findUserEntityByEmail(authentication.getName());
    }

    // @GetMapping("/dashboard")
    // public ResponseEntity<ApiResponse<WardenDashboardDto>> getDashboard() {
    //     User currentUser = getCurrentUserEntity();
    //     // WardenDashboardDto dashboardData = dashboardService.getWardenDashboard(currentUser);
    //     // return ResponseEntity.ok(ApiResponse.success(dashboardData, "Warden dashboard fetched."));
    //     return ResponseEntity.ok(ApiResponse.success(null, "Warden dashboard placeholder"));
    // }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<PageResponseDto<UserDto>>> getAllStudents(
            @PageableDefault(size = 10, sort = "firstName") Pageable pageable,
            @RequestParam(required = false) String searchTerm) {
        // Wardens see only students. Admins using this endpoint would also see students via role filter.
        Page<UserDto> studentsPage = userService.getAllUsers(pageable, Role.STUDENT.name(), searchTerm);
        PageResponseDto<UserDto> pageResponseDto = new PageResponseDto<>(studentsPage);
        return ResponseEntity.ok(ApiResponse.success(pageResponseDto, "Students fetched successfully"));
    }

    @GetMapping("/complaints")
    public ResponseEntity<ApiResponse<PageResponseDto<ComplaintDto>>> getAllComplaintsForWarden(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority) {
        User currentUser = getCurrentUserEntity(); // For context, though service might not filter by warden for GET all
        Page<ComplaintDto> complaintsPage = complaintService.getAllComplaints(pageable, currentUser, status, category, priority);
        PageResponseDto<ComplaintDto> pageResponseDto = new PageResponseDto<>(complaintsPage);
        return ResponseEntity.ok(ApiResponse.success(pageResponseDto, "Complaints fetched for warden"));
    }

    @PutMapping("/complaints/{complaintId}/status")
    public ResponseEntity<ApiResponse<ComplaintDto>> updateComplaintStatus(
            @PathVariable Long complaintId,
            @Valid @RequestBody ComplaintStatusUpdateRequestDto statusUpdateRequestDto) {
        User currentUser = getCurrentUserEntity();
        ComplaintDto updatedComplaint = complaintService.updateComplaintStatus(complaintId, statusUpdateRequestDto, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedComplaint, "Complaint status updated successfully"));
    }

    @GetMapping("/maintenance-requests")
     public ResponseEntity<ApiResponse<PageResponseDto<ComplaintDto>>> getMaintenanceRequests(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) String status, // e.g. PENDING, IN_PROGRESS
            @RequestParam(required = false) String priority) {
        User currentUser = getCurrentUserEntity();
        // Assuming category is fixed for maintenance type requests in service or a specific method.
        // For now, using the general getAllComplaints and filtering by a list of maintenance categories.
        // This might be better as a dedicated service method: complaintService.getMaintenanceRequests(...)
        String maintenanceCategories = "PLUMBING,ELECTRICAL,MAINTENANCE"; // Example, could be dynamic
        Page<ComplaintDto> complaintsPage = complaintService.getAllComplaints(pageable, currentUser, status, maintenanceCategories, priority);
        PageResponseDto<ComplaintDto> pageResponseDto = new PageResponseDto<>(complaintsPage);
        return ResponseEntity.ok(ApiResponse.success(pageResponseDto, "Maintenance requests fetched"));
    }


    @GetMapping("/leaves")
    public ResponseEntity<ApiResponse<PageResponseDto<LeaveDto>>> getAllLeaveApplicationsForWarden(
            @PageableDefault(size = 10, sort = "appliedDate") Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String leaveType) {
        User currentUser = getCurrentUserEntity();
        Page<LeaveDto> leavesPage = leaveService.getAllLeaveApplications(pageable, currentUser, status, leaveType);
        PageResponseDto<LeaveDto> pageResponseDto = new PageResponseDto<>(leavesPage);
        return ResponseEntity.ok(ApiResponse.success(pageResponseDto, "Leave applications fetched for warden"));
    }

    @PutMapping("/leaves/{leaveId}/status")
    public ResponseEntity<ApiResponse<LeaveDto>> updateLeaveStatus(
            @PathVariable Long leaveId,
            @Valid @RequestBody LeaveStatusUpdateRequestDto statusUpdateRequestDto) {
        User currentUser = getCurrentUserEntity();
        LeaveDto updatedLeave = leaveService.updateLeaveStatus(leaveId, statusUpdateRequestDto, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedLeave, "Leave status updated successfully"));
    }

    // @GetMapping("/rooms/occupancy")
    // public ResponseEntity<ApiResponse<List<RoomOccupancyViewDto>>> getRoomOccupancy() {
    //     // List<RoomOccupancyViewDto> occupancy = roomService.getRoomOccupancyView();
    //     // return ResponseEntity.ok(ApiResponse.success(occupancy, "Room occupancy fetched."));
    //     return ResponseEntity.ok(ApiResponse.success(null, "Room occupancy placeholder"));
    // }


    @PostMapping("/announcements")
    public ResponseEntity<ApiResponse<AnnouncementDto>> createWardenAnnouncement(@Valid @RequestBody AnnouncementRequestDto announcementRequestDto) {
        User currentUser = getCurrentUserEntity();
        AnnouncementDto createdAnnouncement = announcementService.createAnnouncement(announcementRequestDto, currentUser);
        return new ResponseEntity<>(ApiResponse.success(createdAnnouncement, "Announcement created successfully by warden"), HttpStatus.CREATED);
    }
}
