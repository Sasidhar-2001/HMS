package com.yourproject.controller;

import com.yourproject.dto.*;
import com.yourproject.service.UserService;
import com.yourproject.service.RoomService; // For room assignment
// import com.yourproject.service.ReportService; // For report generation
// import com.yourproject.service.DashboardService; // For dashboard stats
import com.yourproject.entity.Role; // For creating wardens

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // For role-based access
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // All endpoints in this controller require ADMIN role
public class AdminController {

    private final UserService userService;
    private final RoomService roomService;
    // private final ReportService reportService;
    // private final DashboardService dashboardService;

    @Autowired
    public AdminController(UserService userService, RoomService roomService /*, ReportService reportService, DashboardService dashboardService */) {
        this.userService = userService;
        this.roomService = roomService;
        // this.reportService = reportService;
        // this.dashboardService = dashboardService;
    }

    // @GetMapping("/dashboard")
    // public ResponseEntity<ApiResponse<AdminDashboardStatsDto>> getDashboardStats() {
    //     AdminDashboardStatsDto stats = dashboardService.getAdminDashboardStats();
    //     return ResponseEntity.ok(ApiResponse.success(stats, "Admin dashboard stats fetched successfully"));
    // }

    // Student Management
    @GetMapping("/students")
    public ResponseEntity<ApiResponse<PageResponseDto<UserDto>>> getAllStudents(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String searchTerm) {
        Page<UserDto> studentsPage = userService.getAllUsers(pageable, Role.STUDENT.name(), searchTerm);
        PageResponseDto<UserDto> pageResponseDto = new PageResponseDto<>(studentsPage);
        return ResponseEntity.ok(ApiResponse.success(pageResponseDto, "Students fetched successfully"));
    }

    @PostMapping("/students")
    public ResponseEntity<ApiResponse<UserDto>> createStudent(@Valid @RequestBody UserRegistrationRequestDto registrationRequest) {
        // Ensure role is STUDENT, or override if necessary
        registrationRequest.setRole(Role.STUDENT);
        UserDto createdStudent = userService.createUser(registrationRequest);
        return new ResponseEntity<>(ApiResponse.success(createdStudent, "Student created successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/students/{studentId}")
    public ResponseEntity<ApiResponse<UserDto>> updateStudent(@PathVariable Long studentId, @Valid @RequestBody ProfileUpdateRequestDto updateRequest) {
        // Admin might have more update capabilities than user's own profile update.
        // UserService.updateUser should handle this.
        UserDto updatedStudent = userService.updateUser(studentId, updateRequest);
        return ResponseEntity.ok(ApiResponse.success(updatedStudent, "Student updated successfully"));
    }

    @DeleteMapping("/students/{studentId}")
    public ResponseEntity<ApiResponse<String>> deleteStudent(@PathVariable Long studentId) {
        // This typically means deactivation
        userService.deactivateUser(studentId); // or userService.deleteUser(studentId) if hard delete
        return ResponseEntity.ok(ApiResponse.success("Student deactivated successfully"));
    }

    @PostMapping("/students/{studentId}/activate")
    public ResponseEntity<ApiResponse<String>> activateStudent(@PathVariable Long studentId) {
        userService.activateUser(studentId);
        return ResponseEntity.ok(ApiResponse.success("Student activated successfully"));
    }


    // Warden Management
    @GetMapping("/wardens")
    public ResponseEntity<ApiResponse<PageResponseDto<UserDto>>> getAllWardens(
             @PageableDefault(size = 10) Pageable pageable,
             @RequestParam(required = false) String searchTerm) {
        Page<UserDto> wardensPage = userService.getAllUsers(pageable, Role.WARDEN.name(), searchTerm);
        PageResponseDto<UserDto> pageResponseDto = new PageResponseDto<>(wardensPage);
        return ResponseEntity.ok(ApiResponse.success(pageResponseDto, "Wardens fetched successfully"));
    }

    @PostMapping("/wardens")
    public ResponseEntity<ApiResponse<UserDto>> createWarden(@Valid @RequestBody UserRegistrationRequestDto registrationRequest) {
        registrationRequest.setRole(Role.WARDEN);
        UserDto createdWarden = userService.createUser(registrationRequest);
        return new ResponseEntity<>(ApiResponse.success(createdWarden, "Warden created successfully"), HttpStatus.CREATED);
    }

    // PUT and DELETE for wardens would be similar to students


    // Room Assignment (from adminController.js - assignRoom)
    @PostMapping("/rooms/assign")
    public ResponseEntity<ApiResponse<OccupancyDto>> assignRoomToStudent(@Valid @RequestBody AssignRoomRequestDto assignRoomRequest) {
        OccupancyDto occupancyDto = roomService.assignStudentToRoom(
                assignRoomRequest.getStudentId(),
                assignRoomRequest.getRoomId(),
                assignRoomRequest.getBedNumber()
        );
        return ResponseEntity.ok(ApiResponse.success(occupancyDto, "Room assigned to student successfully"));
    }

    @PostMapping("/rooms/remove")
    public ResponseEntity<ApiResponse<String>> removeStudentFromRoom(@Valid @RequestBody RemoveStudentFromRoomRequestDto requestDto) {
        // Assuming studentId and roomId are in the DTO. RoomService needs to handle this.
        roomService.removeStudentFromRoom(requestDto.getStudentId(), requestDto.getRoomId());
        return ResponseEntity.ok(ApiResponse.success("Student removed from room successfully"));
    }


    // Reports (Placeholder - requires ReportService and DTOs for report generation request/response)
    // @GetMapping("/reports")
    // public ResponseEntity<ApiResponse<ReportResponseDto>> generateReport(@RequestParam String type, @RequestParam String format,
    //                                                                   @RequestParam(required = false) LocalDate startDate,
    //                                                                   @RequestParam(required = false) LocalDate endDate) {
    //     ReportRequestDto request = new ReportRequestDto(type, format, startDate, endDate);
    //     ReportResponseDto report = reportService.generateReport(request);
    //     return ResponseEntity.ok(ApiResponse.success(report, "Report generated successfully"));
    // }
}
