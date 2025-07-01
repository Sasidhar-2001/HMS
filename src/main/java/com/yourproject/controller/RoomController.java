package com.yourproject.controller;

import com.yourproject.dto.*;
import com.yourproject.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    @Autowired
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()") // All authenticated users can view rooms
    public ResponseEntity<ApiResponse<PageResponseDto<RoomDto>>> getAllRooms(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String block,
            @RequestParam(required = false) Integer floor,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        Page<RoomDto> roomsPage = roomService.getAllRooms(pageable, block, floor, type, status);
        PageResponseDto<RoomDto> pageResponseDto = new PageResponseDto<>(roomsPage);
        return ResponseEntity.ok(ApiResponse.success(pageResponseDto, "Rooms fetched successfully"));
    }

    @GetMapping("/available")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getAvailableRooms(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String block) {
        List<RoomDto> availableRooms = roomService.getAvailableRooms(type, block);
        return ResponseEntity.ok(ApiResponse.success(availableRooms, "Available rooms fetched successfully"));
    }

    // @GetMapping("/stats")
    // @PreAuthorize("isAuthenticated()") // Or specific roles like ADMIN, WARDEN
    // public ResponseEntity<ApiResponse<RoomStatsDto>> getRoomStats() {
    //     RoomStatsDto stats = roomService.getRoomStats();
    //     return ResponseEntity.ok(ApiResponse.success(stats, "Room statistics fetched successfully"));
    // }

    @GetMapping("/{roomId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RoomDto>> getRoomById(@PathVariable Long roomId) {
        RoomDto roomDto = roomService.getRoomById(roomId);
        return ResponseEntity.ok(ApiResponse.success(roomDto, "Room details fetched successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<RoomDto>> createRoom(@Valid @RequestBody RoomRequestDto roomRequestDto) {
        RoomDto createdRoom = roomService.createRoom(roomRequestDto);
        return new ResponseEntity<>(ApiResponse.success(createdRoom, "Room created successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/{roomId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<RoomDto>> updateRoom(@PathVariable Long roomId, @Valid @RequestBody RoomRequestDto roomRequestDto) {
        RoomDto updatedRoom = roomService.updateRoom(roomId, roomRequestDto);
        return ResponseEntity.ok(ApiResponse.success(updatedRoom, "Room updated successfully"));
    }

    @DeleteMapping("/{roomId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<String>> deleteRoom(@PathVariable Long roomId) {
        roomService.deleteRoom(roomId); // This is deactivation
        return ResponseEntity.ok(ApiResponse.success("Room deactivated successfully"));
    }

    // Assign student to a specific room (room ID from path)
    @PostMapping("/{roomId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<OccupancyDto>> assignStudentToSpecificRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody StudentToRoomAssignmentRequestDto requestDto) { // DTO with studentId and optional bedNumber
        OccupancyDto occupancyDto = roomService.assignStudentToRoom(requestDto.getStudentId(), roomId, requestDto.getBedNumber());
        return ResponseEntity.ok(ApiResponse.success(occupancyDto, "Student assigned to room successfully"));
    }

    // DTO for the above endpoint
    // @Data @NoArgsConstructor @AllArgsConstructor static class StudentToRoomAssignmentRequestDto {
    //     @NotNull private Long studentId;
    //     private Integer bedNumber;
    // }

    // Remove student from a specific room (room ID from path)
    @PostMapping("/{roomId}/remove")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<String>> removeStudentFromSpecificRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody StudentIdRequestDto requestDto) { // DTO with just studentId
        roomService.removeStudentFromRoom(requestDto.getStudentId(), roomId);
        return ResponseEntity.ok(ApiResponse.success("Student removed from room successfully"));
    }

    // DTO for the above endpoint
    // @Data @NoArgsConstructor @AllArgsConstructor static class StudentIdRequestDto {
    //    @NotNull private Long studentId;
    // }
}
