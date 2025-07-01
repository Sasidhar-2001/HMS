package com.yourproject.service;

import com.yourproject.dto.AnnouncementDto;
import com.yourproject.dto.AnnouncementRequestDto;
import com.yourproject.dto.CommentDto; // For adding comment
// import com.yourproject.dto.AnnouncementStatsDto;
import com.yourproject.entity.User; // For createdBy and interaction user
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AnnouncementService {

    AnnouncementDto createAnnouncement(AnnouncementRequestDto announcementRequestDto, User currentUser);
    AnnouncementDto getAnnouncementById(Long announcementId, User currentUser); // currentUser to mark as read
    Page<AnnouncementDto> getAllAnnouncements(Pageable pageable, User currentUser, String type, String status, String priority);
    AnnouncementDto updateAnnouncement(Long announcementId, AnnouncementRequestDto announcementRequestDto, User currentUser);
    void deleteAnnouncement(Long announcementId, User currentUser);

    AnnouncementDto publishAnnouncement(Long announcementId, User currentUser);
    LikeDto toggleLike(Long announcementId, User currentUser);
    CommentDto addComment(Long announcementId, String commentText, User currentUser);

    // AnnouncementStatsDto getAnnouncementStats(User currentUser);
    AnnouncementDto addAttachmentsToAnnouncement(Long announcementId, List<MultipartFile> files, User currentUser);


    // Consider methods for managing target users/rooms if more complex than just IDs in request DTO
}
