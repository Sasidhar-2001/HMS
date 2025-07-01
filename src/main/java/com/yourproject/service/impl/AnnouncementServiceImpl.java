package com.yourproject.service.impl;

import com.yourproject.dto.*;
import com.yourproject.entity.*;
import com.yourproject.exception.AccessDeniedException;
import com.yourproject.exception.BadRequestException;
import com.yourproject.exception.ResourceNotFoundException;
import com.yourproject.repository.*;
import com.yourproject.service.AnnouncementService;
import com.yourproject.service.EmailService; // For notifications
import com.yourproject.service.FileUploadService; // Added
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile; // Added
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository; // For targetRooms
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final ReadReceiptRepository readReceiptRepository;
    private final ModelMapper modelMapper;
    private final EmailService emailService;
    private final FileUploadService fileUploadService; // Added

    @Autowired
    public AnnouncementServiceImpl(AnnouncementRepository announcementRepository,
                                 UserRepository userRepository,
                                 RoomRepository roomRepository,
                                 LikeRepository likeRepository,
                                 CommentRepository commentRepository,
                                 ReadReceiptRepository readReceiptRepository,
                                 ModelMapper modelMapper,
                                 EmailService emailService,
                                 FileUploadService fileUploadService) { // Added
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.readReceiptRepository = readReceiptRepository;
        this.modelMapper = modelMapper;
        this.emailService = emailService;
        this.fileUploadService = fileUploadService; // Added
    }

    private AnnouncementDto convertToDto(Announcement announcement, User currentUser) {
        AnnouncementDto dto = modelMapper.map(announcement, AnnouncementDto.class);
        dto.setCreatedBy(modelMapper.map(announcement.getCreatedBy(), UserSlimDto.class));

        dto.setTargetUsers(announcement.getTargetUsers().stream()
            .map(user -> modelMapper.map(user, UserSlimDto.class))
            .collect(Collectors.toSet()));
        dto.setTargetRooms(announcement.getTargetRooms().stream()
            .map(room -> modelMapper.map(room, RoomSlimDto.class))
            .collect(Collectors.toSet()));

        dto.setLikes(announcement.getLikes().stream()
            .map(like -> {
                LikeDto likeDto = modelMapper.map(like, LikeDto.class);
                likeDto.setUser(modelMapper.map(like.getUser(), UserSlimDto.class));
                return likeDto;
            })
            .collect(Collectors.toSet()));
        dto.setLikeCount(dto.getLikes().size());

        dto.setCommentEntries(announcement.getCommentEntries().stream()
            .map(comment -> {
                CommentDto commentDto = modelMapper.map(comment, CommentDto.class);
                commentDto.setUser(modelMapper.map(comment.getUser(), UserSlimDto.class));
                return commentDto;
            })
            .collect(Collectors.toList()));
        dto.setCommentCount(dto.getCommentEntries().size());

        dto.setReadReceipts(announcement.getReadReceipts().stream()
            .map(rr -> {
                ReadReceiptDto rrDto = modelMapper.map(rr, ReadReceiptDto.class);
                rrDto.setUser(modelMapper.map(rr.getUser(), UserSlimDto.class));
                return rrDto;
            })
            .collect(Collectors.toSet()));

        dto.setActive(announcement.getStatus() == AnnouncementStatus.PUBLISHED &&
                      (announcement.getExpiryDate() == null || announcement.getExpiryDate().isAfter(LocalDateTime.now())));

        // Mark as read if current user is student and viewing a published announcement
        if (currentUser != null && currentUser.getRole() == Role.STUDENT &&
            announcement.getStatus() == AnnouncementStatus.PUBLISHED &&
            !readReceiptRepository.existsByAnnouncementAndUser(announcement, currentUser)) {
                // This marking logic might be better placed in getAnnouncementById after fetching
        }

        return dto;
    }

    private Announcement findAnnouncementEntityById(Long announcementId) {
        return announcementRepository.findById(announcementId)
            .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with ID: " + announcementId));
    }


    @Override
    @Transactional
    public AnnouncementDto createAnnouncement(AnnouncementRequestDto requestDto, User currentUser) {
        Announcement announcement = modelMapper.map(requestDto, Announcement.class);
        announcement.setCreatedBy(currentUser);

        if (requestDto.getTargetUserIds() != null && !requestDto.getTargetUserIds().isEmpty()) {
            Set<User> targetUsers = new HashSet<>(userRepository.findAllById(requestDto.getTargetUserIds()));
            announcement.setTargetUsers(targetUsers);
        }
        if (requestDto.getTargetRoomIds() != null && !requestDto.getTargetRoomIds().isEmpty()) {
            Set<Room> targetRooms = new HashSet<>(roomRepository.findAllById(requestDto.getTargetRoomIds()));
            announcement.setTargetRooms(targetRooms);
        }

        if (announcement.getStatus() == AnnouncementStatus.PUBLISHED && announcement.getPublishDate() == null) {
            announcement.setPublishDate(LocalDateTime.now());
        }

        // Handle pre-save logic for expiry (from Mongoose model)
        if (announcement.getExpiryDate() != null && LocalDateTime.now().isAfter(announcement.getExpiryDate()) && announcement.getStatus() == AnnouncementStatus.PUBLISHED) {
            announcement.setStatus(AnnouncementStatus.EXPIRED);
        }

        Announcement savedAnnouncement = announcementRepository.save(announcement);

        if (savedAnnouncement.getStatus() == AnnouncementStatus.PUBLISHED && !savedAnnouncement.isEmailSent()) {
            sendNotificationEmails(savedAnnouncement);
            savedAnnouncement.setEmailSent(true); // Mark as sent
            savedAnnouncement.setNotificationSent(true);
            announcementRepository.save(savedAnnouncement); // Save again after updating sent status
        }

        return convertToDto(savedAnnouncement, currentUser);
    }

    @Override
    @Transactional
    public AnnouncementDto getAnnouncementById(Long announcementId, User currentUser) {
        Announcement announcement = findAnnouncementEntityById(announcementId);

        // Student viewing a published announcement marks it as read
        if (currentUser != null && currentUser.getRole() == Role.STUDENT &&
            announcement.getStatus() == AnnouncementStatus.PUBLISHED &&
            !readReceiptRepository.existsByAnnouncementAndUser(announcement, currentUser)) {

            ReadReceipt readReceipt = new ReadReceipt();
            readReceipt.setAnnouncement(announcement);
            readReceipt.setUser(currentUser);
            readReceiptRepository.save(readReceipt);

            announcement.setViewCount(announcement.getViewCount() + 1);
            // Add the new receipt to the announcement's collection for immediate DTO update
            announcement.getReadReceipts().add(readReceipt);
            announcementRepository.save(announcement); // Save view count and potentially new receipt if cascaded
        }
        return convertToDto(announcement, currentUser);
    }

    @Override
    public Page<AnnouncementDto> getAllAnnouncements(Pageable pageable, User currentUser, String typeFilter, String statusFilter, String priorityFilter) {
        Specification<Announcement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (currentUser.getRole() == Role.STUDENT) {
                predicates.add(cb.equal(root.get("status"), AnnouncementStatus.PUBLISHED));
                predicates.add(
                    cb.or(
                        cb.isNull(root.get("expiryDate")),
                        cb.greaterThanOrEqualTo(root.get("expiryDate"), LocalDateTime.now())
                    )
                );
                // Target audience logic for students
                Predicate targetAll = cb.equal(root.get("targetAudience"), AnnouncementTargetAudience.ALL);
                Predicate targetStudents = cb.equal(root.get("targetAudience"), AnnouncementTargetAudience.STUDENTS);
                // Correctly join and check if currentUser is in targetUsers set
                Predicate targetSpecificUser = cb.isMember(currentUser, root.get("targetUsers"));
                // If targetRooms is relevant for students (e.g., announcements for their room block)
                // Predicate targetSpecificRoom = cb.isMember(currentUser.getCurrentRoom().orElse(null), root.get("targetRooms"));
                // This part would need currentUser to have currentRoom populated and that room to be checked against targetRooms.
                // For simplicity, let's assume targetUsers covers specific student targeting.

                predicates.add(cb.or(targetAll, targetStudents, targetSpecificUser));

            } else { // Admin/Warden can see more
                if (StringUtils.hasText(statusFilter)) {
                    try {
                        predicates.add(cb.equal(root.get("status"), AnnouncementStatus.valueOf(statusFilter.toUpperCase())));
                    } catch (IllegalArgumentException e) {/* ignore */}
                }
            }

            if (StringUtils.hasText(typeFilter)) {
                 try {
                    predicates.add(cb.equal(root.get("type"), AnnouncementType.valueOf(typeFilter.toUpperCase())));
                } catch (IllegalArgumentException e) {/* ignore */}
            }
            if (StringUtils.hasText(priorityFilter)) {
                 try {
                    predicates.add(cb.equal(root.get("priority"), AnnouncementPriority.valueOf(priorityFilter.toUpperCase())));
                } catch (IllegalArgumentException e) {/* ignore */}
            }
            // Ensure distinct results if joins cause duplicates, especially with targetUsers/targetRooms
            query.distinct(true);

            // Add sorting for sticky posts first, then by creation date or publish date
            // This is better handled by Pageable's sort parameter. Default can be set in controller.
            // Example: query.orderBy(cb.desc(root.get("isSticky")), cb.desc(root.get("publishDate")));


            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return announcementRepository.findAll(spec, pageable).map(ann -> convertToDto(ann, currentUser));
    }

    @Override
    @Transactional
    public AnnouncementDto updateAnnouncement(Long announcementId, AnnouncementRequestDto requestDto, User currentUser) {
        Announcement announcement = findAnnouncementEntityById(announcementId);
        checkPermission(announcement, currentUser, "update");

        // Update fields from DTO
        modelMapper.map(requestDto, announcement); // Be careful with nulls if strategy is not SKIP_NULL

        // Explicitly set fields that modelMapper might miss or that need logic
        announcement.setTitle(requestDto.getTitle());
        announcement.setContent(requestDto.getContent());
        announcement.setType(requestDto.getType());
        announcement.setPriority(requestDto.getPriority());
        announcement.setTargetAudience(requestDto.getTargetAudience());
        if(requestDto.getExpiryDate() != null) announcement.setExpiryDate(requestDto.getExpiryDate());
        if(requestDto.getPublishDate() != null) announcement.setPublishDate(requestDto.getPublishDate()); // Allow changing publish date
        if(requestDto.getStatus() != null) announcement.setStatus(requestDto.getStatus());
        if(requestDto.getIsSticky() != null) announcement.setSticky(requestDto.getIsSticky());
        if(requestDto.getTags() != null) announcement.setTags(requestDto.getTags());
        // Attachments might need more complex handling (removing old, adding new)

        if (requestDto.getTargetUserIds() != null) {
            Set<User> targetUsers = new HashSet<>(userRepository.findAllById(requestDto.getTargetUserIds()));
            announcement.setTargetUsers(targetUsers);
        }
         if (requestDto.getTargetRoomIds() != null) {
            Set<Room> targetRooms = new HashSet<>(roomRepository.findAllById(requestDto.getTargetRoomIds()));
            announcement.setTargetRooms(targetRooms);
        }

        boolean wasPublished = announcement.isEmailSent(); // Check if it was already published and sent
        if (announcement.getStatus() == AnnouncementStatus.PUBLISHED && announcement.getPublishDate() == null) {
            announcement.setPublishDate(LocalDateTime.now());
        }

        if (announcement.getExpiryDate() != null && LocalDateTime.now().isAfter(announcement.getExpiryDate()) && announcement.getStatus() == AnnouncementStatus.PUBLISHED) {
            announcement.setStatus(AnnouncementStatus.EXPIRED);
        }

        Announcement updatedAnnouncement = announcementRepository.save(announcement);

        if (updatedAnnouncement.getStatus() == AnnouncementStatus.PUBLISHED && !wasPublished && !updatedAnnouncement.isEmailSent()) {
            sendNotificationEmails(updatedAnnouncement);
            updatedAnnouncement.setEmailSent(true);
            updatedAnnouncement.setNotificationSent(true);
            announcementRepository.save(updatedAnnouncement);
        }

        return convertToDto(updatedAnnouncement, currentUser);
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long announcementId, User currentUser) {
        Announcement announcement = findAnnouncementEntityById(announcementId);
        checkPermission(announcement, currentUser, "delete");
        announcementRepository.delete(announcement);
    }

    @Override
    @Transactional
    public AnnouncementDto publishAnnouncement(Long announcementId, User currentUser) {
        Announcement announcement = findAnnouncementEntityById(announcementId);
        checkPermission(announcement, currentUser, "publish");

        if (announcement.getStatus() == AnnouncementStatus.PUBLISHED) {
            throw new BadRequestException("Announcement is already published.");
        }
        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        announcement.setPublishDate(LocalDateTime.now());

        if (announcement.getExpiryDate() != null && LocalDateTime.now().isAfter(announcement.getExpiryDate())) {
            announcement.setStatus(AnnouncementStatus.EXPIRED); // Should not happen if just publishing
        }

        Announcement publishedAnnouncement = announcementRepository.save(announcement);

        if (!publishedAnnouncement.isEmailSent()) { // Send emails only if not already sent
            sendNotificationEmails(publishedAnnouncement);
            publishedAnnouncement.setEmailSent(true);
            publishedAnnouncement.setNotificationSent(true);
            announcementRepository.save(publishedAnnouncement);
        }
        return convertToDto(publishedAnnouncement, currentUser);
    }

    @Override
    @Transactional
    public LikeDto toggleLike(Long announcementId, User currentUser) {
        Announcement announcement = findAnnouncementEntityById(announcementId);
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new BadRequestException("Cannot like an unpublished announcement.");
        }

        Optional<Like> existingLike = likeRepository.findByAnnouncementAndUser(announcement, currentUser);
        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            // Update announcement's likes collection for DTO conversion consistency (if not using refresh)
            announcement.getLikes().remove(existingLike.get());
            // We don't return a LikeDto on unlike, or return null/specific response
            return null; // Or a DTO indicating unliked
        } else {
            Like newLike = new Like();
            newLike.setAnnouncement(announcement);
            newLike.setUser(currentUser);
            Like savedLike = likeRepository.save(newLike);
            // Update announcement's likes collection
            announcement.getLikes().add(savedLike);

            LikeDto likeDto = modelMapper.map(savedLike, LikeDto.class);
            likeDto.setUser(modelMapper.map(currentUser, UserSlimDto.class));
            return likeDto;
        }
    }

    @Override
    @Transactional
    public CommentDto addComment(Long announcementId, String commentText, User currentUser) {
        Announcement announcement = findAnnouncementEntityById(announcementId);
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new BadRequestException("Cannot comment on an unpublished announcement.");
        }

        Comment newComment = new Comment();
        newComment.setAnnouncement(announcement);
        newComment.setUser(currentUser);
        newComment.setText(commentText);
        Comment savedComment = commentRepository.save(newComment);

        // Update announcement's comments collection
        announcement.getCommentEntries().add(savedComment);

        CommentDto commentDto = modelMapper.map(savedComment, CommentDto.class);
        commentDto.setUser(modelMapper.map(currentUser, UserSlimDto.class));
        return commentDto;
    }

    // @Override
    // public AnnouncementStatsDto getAnnouncementStats(User currentUser) {
    //     // Implementation for stats
    //     return new AnnouncementStatsDto();
    // }

    @Override
    @Transactional
    public AnnouncementDto addAttachmentsToAnnouncement(Long announcementId, List<MultipartFile> files, User currentUser) {
        Announcement announcement = findAnnouncementEntityById(announcementId);
        checkPermission(announcement, currentUser, "add attachments to");

        List<com.yourproject.entity.embeddable.Attachment> newAttachments = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            // Store file, e.g., in "announcements/{announcementId}/"
            String relativePath = fileUploadService.storeFile(file, "announcements", announcement.getId().toString());

            com.yourproject.entity.embeddable.Attachment attachment = new com.yourproject.entity.embeddable.Attachment();
            attachment.setFileName(StringUtils.cleanPath(file.getOriginalFilename()));
            attachment.setFilePath(relativePath); // Store relative path
            attachment.setFileSize(file.getSize());
            attachment.setUploadDate(LocalDateTime.now());
            newAttachments.add(attachment);
        }

        announcement.getAttachments().addAll(newAttachments);
        Announcement updatedAnnouncement = announcementRepository.save(announcement);
        return convertToDto(updatedAnnouncement, currentUser);
    }


    private void checkPermission(Announcement announcement, User user, String action) {
        if (user.getRole() != Role.ADMIN && !announcement.getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to " + action + " this announcement.");
        }
    }

    private void sendNotificationEmails(Announcement announcement) {
        // Determine recipients based on targetAudience
        List<User> recipients = new ArrayList<>();
        switch (announcement.getTargetAudience()) {
            case ALL:
                recipients.addAll(userRepository.findAll()); // Consider filtering for active users
                break;
            case STUDENTS:
                recipients.addAll(userRepository.findByRole(Role.STUDENT));
                break;
            case WARDENS:
                recipients.addAll(userRepository.findByRole(Role.WARDEN));
                break;
            case ADMINS:
                recipients.addAll(userRepository.findByRole(Role.ADMIN));
                break;
            case SPECIFIC_USERS:
                recipients.addAll(announcement.getTargetUsers());
                break;
            case SPECIFIC_ROOMS:
                // This requires fetching users in those rooms.
                // For each room in announcement.getTargetRooms(), get occupants.
                // This can be complex and lead to N+1 if not careful.
                // List<User> roomOccupants = announcement.getTargetRooms().stream()
                //     .flatMap(room -> room.getOccupancies().stream())
                //     .filter(Occupancy::isActive)
                //     .map(Occupancy::getStudent)
                //     .distinct()
                //     .collect(Collectors.toList());
                // recipients.addAll(roomOccupants);
                break;
        }

        // Remove duplicates and filter for active users with valid emails
        List<User> finalRecipients = recipients.stream()
            .filter(u -> u.isActive() && StringUtils.hasText(u.getEmail()))
            .distinct() // Based on User's equals/hashCode, ensure they are implemented or rely on object identity
            .collect(Collectors.toList());

        if (!finalRecipients.isEmpty()) {
            // Simplified email sending, in reality, might use a template
            // emailService.sendAnnouncementEmail(finalRecipients, announcement);
             finalRecipients.forEach(recipient ->
                emailService.sendHtmlMessage(
                    recipient.getEmail(),
                    announcement.getTitle(),
                    String.format("<h3>%s</h3><p>%s</p>", announcement.getTitle(), announcement.getContent())
                )
            );
        }
    }
}
