package com.yourproject.controller;

import com.yourproject.dto.*;
import com.yourproject.entity.User;
import com.yourproject.service.AnnouncementService;
import com.yourproject.service.UserService; // To get current user entity
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

import java.util.List; // Added
import java.util.Map; // For simple comment request

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final UserService userService; // To fetch User entity for service methods

    @Autowired
    public AnnouncementController(AnnouncementService announcementService, UserService userService) {
        this.announcementService = announcementService;
        this.userService = userService;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userService.findUserEntityByEmail(email); // Assuming UserService has this helper
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponseDto<AnnouncementDto>>> getAllAnnouncements(
            @PageableDefault(size = 10, sort = {"isSticky", "publishDate"}) Pageable pageable,
            // Default sort: sticky first, then by publishDate. Note: "isSticky" might need custom sort handling if not direct field.
            // For complex sorting like `isSticky DESC, publishDate DESC`, configure in Pageable.
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        User currentUser = getCurrentUser();
        Page<AnnouncementDto> announcementsPage = announcementService.getAllAnnouncements(pageable, currentUser, type, status, priority);
        PageResponseDto<AnnouncementDto> pageResponseDto = new PageResponseDto<>(announcementsPage);
        return ResponseEntity.ok(ApiResponse.success(pageResponseDto, "Announcements fetched successfully"));
    }

    // @GetMapping("/stats")
    // @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    // public ResponseEntity<ApiResponse<AnnouncementStatsDto>> getAnnouncementStats() {
    //     User currentUser = getCurrentUser();
    //     AnnouncementStatsDto stats = announcementService.getAnnouncementStats(currentUser);
    //     return ResponseEntity.ok(ApiResponse.success(stats, "Announcement statistics fetched successfully"));
    // }

    @GetMapping("/{announcementId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AnnouncementDto>> getAnnouncementById(@PathVariable Long announcementId) {
        User currentUser = getCurrentUser();
        AnnouncementDto announcementDto = announcementService.getAnnouncementById(announcementId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(announcementDto, "Announcement details fetched successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<AnnouncementDto>> createAnnouncement(@Valid @RequestBody AnnouncementRequestDto announcementRequestDto) {
        User currentUser = getCurrentUser();
        AnnouncementDto createdAnnouncement = announcementService.createAnnouncement(announcementRequestDto, currentUser);
        return new ResponseEntity<>(ApiResponse.success(createdAnnouncement, "Announcement created successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/{announcementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<AnnouncementDto>> updateAnnouncement(@PathVariable Long announcementId, @Valid @RequestBody AnnouncementRequestDto announcementRequestDto) {
        User currentUser = getCurrentUser();
        AnnouncementDto updatedAnnouncement = announcementService.updateAnnouncement(announcementId, announcementRequestDto, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedAnnouncement, "Announcement updated successfully"));
    }

    @DeleteMapping("/{announcementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<String>> deleteAnnouncement(@PathVariable Long announcementId) {
        User currentUser = getCurrentUser();
        announcementService.deleteAnnouncement(announcementId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Announcement deleted successfully"));
    }

    @PostMapping("/{announcementId}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ApiResponse<AnnouncementDto>> publishAnnouncement(@PathVariable Long announcementId) {
        User currentUser = getCurrentUser();
        AnnouncementDto publishedAnnouncement = announcementService.publishAnnouncement(announcementId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(publishedAnnouncement, "Announcement published successfully"));
    }

    @PostMapping("/{announcementId}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LikeDto>> toggleLike(@PathVariable Long announcementId) {
        User currentUser = getCurrentUser();
        LikeDto likeResult = announcementService.toggleLike(announcementId, currentUser);
        String message = likeResult != null ? "Announcement liked successfully" : "Announcement unliked successfully";
        return ResponseEntity.ok(ApiResponse.success(likeResult, message));
    }

    @PostMapping("/{announcementId}/comment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CommentDto>> addComment(
            @PathVariable Long announcementId,
            @Valid @RequestBody CommentRequestDto commentRequest) { // DTO for comment text
        User currentUser = getCurrentUser();
        CommentDto newComment = announcementService.addComment(announcementId, commentRequest.getText(), currentUser);
        return new ResponseEntity<>(ApiResponse.success(newComment, "Comment added successfully"), HttpStatus.CREATED);
    }

    @PostMapping("/{announcementId}/attachments")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')") // Or creator of announcement
    public ResponseEntity<ApiResponse<AnnouncementDto>> addAttachments(
            @PathVariable Long announcementId,
            @RequestParam("files") List<MultipartFile> files) {
        User currentUser = getCurrentUser();
        AnnouncementDto updatedAnnouncement = announcementService.addAttachmentsToAnnouncement(announcementId, files, currentUser);
        return ResponseEntity.ok(ApiResponse.success(updatedAnnouncement, "Attachments added successfully"));
    }
}
