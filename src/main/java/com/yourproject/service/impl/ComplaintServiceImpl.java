package com.yourproject.service.impl;

import com.yourproject.dto.*;
import com.yourproject.entity.*;
import com.yourproject.entity.embeddable.ComplaintStatusHistoryItem;
import com.yourproject.exception.AccessDeniedException;
import com.yourproject.exception.BadRequestException;
import com.yourproject.exception.ResourceNotFoundException;
import com.yourproject.repository.ComplaintRepository;
import com.yourproject.repository.RoomRepository;
import com.yourproject.repository.UserRepository;
import com.yourproject.service.ComplaintService;
import com.yourproject.service.EmailService;
import com.yourproject.service.FileUploadService; // For image uploads
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.Map; // For stats

@Service
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository; // For assignedTo, reportedBy
    private final RoomRepository roomRepository; // For room details
    private final ModelMapper modelMapper;
    private final EmailService emailService;
    private final FileUploadService fileUploadService; // Assuming this service exists

    @Autowired
    public ComplaintServiceImpl(ComplaintRepository complaintRepository,
                                UserRepository userRepository,
                                RoomRepository roomRepository,
                                ModelMapper modelMapper,
                                EmailService emailService,
                                FileUploadService fileUploadService) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.modelMapper = modelMapper;
        this.emailService = emailService;
        this.fileUploadService = fileUploadService;
    }

    private ComplaintDto convertToDto(Complaint complaint) {
        ComplaintDto dto = modelMapper.map(complaint, ComplaintDto.class);
        if (complaint.getReportedBy() != null) {
            dto.setReportedBy(modelMapper.map(complaint.getReportedBy(), UserSlimDto.class));
        }
        if (complaint.getAssignedTo() != null) {
            dto.setAssignedTo(modelMapper.map(complaint.getAssignedTo(), UserSlimDto.class));
        }
        if (complaint.getRoom() != null) {
            dto.setRoom(modelMapper.map(complaint.getRoom(), RoomSlimDto.class));
        }
        // Calculated fields for DTO
        if (complaint.getActualResolutionDate() != null && complaint.getCreatedAt() != null) {
            long diffHours = java.time.Duration.between(complaint.getCreatedAt(), complaint.getActualResolutionDate().atStartOfDay()).toHours();
            dto.setResolutionTimeHours((int) diffHours);
        }
        if (complaint.getExpectedResolutionDate() != null &&
            complaint.getStatus() != ComplaintStatus.RESOLVED &&
            complaint.getStatus() != ComplaintStatus.CLOSED &&
            LocalDate.now().isAfter(complaint.getExpectedResolutionDate())) {
            dto.setOverdue(true);
        } else {
            dto.setOverdue(false);
        }
        return dto;
    }

    private Complaint findComplaintEntityById(Long complaintId) {
        return complaintRepository.findById(complaintId)
            .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with ID: " + complaintId));
    }

    private String generateComplaintIdString() {
        // CMPYYYYMMDDXXXX (XXXX is random)
        String yearMonthDay = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%04d", new Random().nextInt(10000));
        return "CMP" + yearMonthDay + randomSuffix;
    }

    @Override
    @Transactional
    public ComplaintDto createComplaint(ComplaintRequestDto requestDto, User currentUser) {
        Complaint complaint = modelMapper.map(requestDto, Complaint.class);
        complaint.setReportedBy(currentUser);
        complaint.setComplaintIdString(generateComplaintIdString());
        complaint.setStatus(ComplaintStatus.PENDING); // Initial status

        if (currentUser.getRole() == Role.STUDENT && currentUser.getCurrentOccupancy().isPresent()) {
            complaint.setRoom(currentUser.getCurrentOccupancy().get().getRoom());
        } else if (requestDto.getRoomId() != null) {
            Room room = roomRepository.findById(requestDto.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + requestDto.getRoomId()));
            complaint.setRoom(room);
        }

        if (requestDto.getAssignedToId() != null && (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.WARDEN)) {
            User assignedTo = userRepository.findById(requestDto.getAssignedToId())
                .orElseThrow(() -> new ResourceNotFoundException("User to assign not found with ID: " + requestDto.getAssignedToId()));
            complaint.setAssignedTo(assignedTo);
        }

        if (complaint.getPriority() == ComplaintPriority.URGENT) {
            complaint.setUrgent(true);
        }

        // Add initial status to history
        ComplaintStatusHistoryItem initialHistory = new ComplaintStatusHistoryItem(
            ComplaintStatus.PENDING, currentUser.getId(), LocalDateTime.now(), "Complaint created."
        );
        complaint.getStatusHistory().add(initialHistory);

        Complaint savedComplaint = complaintRepository.save(complaint);
        return convertToDto(savedComplaint);
    }

    @Override
    public ComplaintDto getComplaintById(Long complaintId, User currentUser) {
        Complaint complaint = findComplaintEntityById(complaintId);
        if (currentUser.getRole() == Role.STUDENT && !complaint.getReportedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to view this complaint.");
        }
        return convertToDto(complaint);
    }

    @Override
    public Page<ComplaintDto> getAllComplaints(Pageable pageable, User currentUser, String statusFilter, String categoryFilter, String priorityFilter) {
        Specification<Complaint> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (currentUser.getRole() == Role.STUDENT) {
                predicates.add(cb.equal(root.get("reportedBy"), currentUser));
            }
            // Add other filters for admin/warden
            if (StringUtils.hasText(statusFilter)) {
                try {
                    predicates.add(cb.equal(root.get("status"), ComplaintStatus.valueOf(statusFilter.toUpperCase())));
                } catch (IllegalArgumentException e) {/* ignore */}
            }
            if (StringUtils.hasText(categoryFilter)) {
                 try {
                    predicates.add(cb.equal(root.get("category"), ComplaintCategory.valueOf(categoryFilter.toUpperCase())));
                } catch (IllegalArgumentException e) {/* ignore */}
            }
            if (StringUtils.hasText(priorityFilter)) {
                 try {
                    predicates.add(cb.equal(root.get("priority"), ComplaintPriority.valueOf(priorityFilter.toUpperCase())));
                } catch (IllegalArgumentException e) {/* ignore */}
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return complaintRepository.findAll(spec, pageable).map(this::convertToDto);
    }

    @Override
    @Transactional
    public ComplaintDto updateComplaint(Long complaintId, ComplaintRequestDto requestDto, User currentUser) {
        Complaint complaint = findComplaintEntityById(complaintId);
        // Students can only update their own PENDING complaints, limited fields
        if (currentUser.getRole() == Role.STUDENT) {
            if (!complaint.getReportedBy().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You cannot update this complaint.");
            }
            if (complaint.getStatus() != ComplaintStatus.PENDING) {
                throw new BadRequestException("Cannot update complaint that is already being processed.");
            }
            // Update only allowed fields for student
            if(StringUtils.hasText(requestDto.getTitle())) complaint.setTitle(requestDto.getTitle());
            if(StringUtils.hasText(requestDto.getDescription())) complaint.setDescription(requestDto.getDescription());
            if(StringUtils.hasText(requestDto.getLocation())) complaint.setLocation(requestDto.getLocation());
            // Category and Priority might be updatable by student if pending
            if(requestDto.getCategory() != null) complaint.setCategory(requestDto.getCategory());
            if(requestDto.getPriority() != null) {
                complaint.setPriority(requestDto.getPriority());
                complaint.setUrgent(requestDto.getPriority() == ComplaintPriority.URGENT);
            }

        } else { // Admin/Warden can update more
            modelMapper.map(requestDto, complaint); // Or selective mapping
            if(requestDto.getPriority() != null) complaint.setUrgent(requestDto.getPriority() == ComplaintPriority.URGENT);
            if (requestDto.getAssignedToId() != null) {
                 User assignedTo = userRepository.findById(requestDto.getAssignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User to assign not found with ID: " + requestDto.getAssignedToId()));
                complaint.setAssignedTo(assignedTo);
            }
             if (requestDto.getRoomId() != null) {
                Room room = roomRepository.findById(requestDto.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + requestDto.getRoomId()));
                complaint.setRoom(room);
            }
        }
        Complaint updatedComplaint = complaintRepository.save(complaint);
        return convertToDto(updatedComplaint);
    }

    @Override
    @Transactional
    public ComplaintDto updateComplaintStatus(Long complaintId, ComplaintStatusUpdateRequestDto statusRequestDto, User currentUser) {
        if (currentUser.getRole() == Role.STUDENT) {
            throw new AccessDeniedException("Students cannot update complaint status.");
        }
        Complaint complaint = findComplaintEntityById(complaintId);

        complaint.setStatus(statusRequestDto.getStatus());
        if (statusRequestDto.getAssignedToId() != null) {
             User assignedTo = userRepository.findById(statusRequestDto.getAssignedToId())
                .orElseThrow(() -> new ResourceNotFoundException("User to assign not found with ID: " + statusRequestDto.getAssignedToId()));
            complaint.setAssignedTo(assignedTo);
        }

        // Add to status history
        ComplaintStatusHistoryItem historyItem = new ComplaintStatusHistoryItem(
            statusRequestDto.getStatus(), currentUser.getId(), LocalDateTime.now(), statusRequestDto.getComment()
        );
        complaint.getStatusHistory().add(historyItem);

        if (statusRequestDto.getStatus() == ComplaintStatus.RESOLVED || statusRequestDto.getStatus() == ComplaintStatus.CLOSED) {
            complaint.setActualResolutionDate(LocalDate.now());
            if (statusRequestDto.getResolution() != null) {
                complaint.setResolution(statusRequestDto.getResolution());
                if(complaint.getResolution().getResolvedById() == null) { // If not set in DTO
                    complaint.getResolution().setResolvedById(currentUser.getId());
                }
                 if(complaint.getResolution().getResolvedAt() == null) {
                    complaint.getResolution().setResolvedAt(LocalDate.now());
                }
            }
        }

        Complaint updatedComplaint = complaintRepository.save(complaint);

        // Send notification email to student who reported it
        // emailService.sendComplaintUpdateEmail(updatedComplaint.getReportedBy(), updatedComplaint);

        return convertToDto(updatedComplaint);
    }

    @Override
    @Transactional
    public void deleteComplaint(Long complaintId, User currentUser) {
        Complaint complaint = findComplaintEntityById(complaintId);
        if (currentUser.getRole() == Role.STUDENT) {
            if (!complaint.getReportedBy().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You cannot delete this complaint.");
            }
            if (complaint.getStatus() != ComplaintStatus.PENDING) {
                 throw new BadRequestException("Cannot delete complaint that is already being processed.");
            }
        }
        // Add logic for cleaning up images if any from file storage
        // complaint.getImages().forEach(fileUploadService::deleteFile); // Assuming file paths are stored
        complaintRepository.delete(complaint);
    }

    @Override
    @Transactional
    public ComplaintDto uploadComplaintImages(Long complaintId, List<MultipartFile> files, User currentUser) {
        Complaint complaint = findComplaintEntityById(complaintId);
        if (currentUser.getRole() == Role.STUDENT && !complaint.getReportedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You cannot upload images for this complaint.");
        }

        List<String> uploadedFilePaths = new ArrayList<>();
        for (MultipartFile file : files) {
            // Define a subfolder for complaints, e.g., "complaints/{complaintIdString}"
            String filePath = fileUploadService.storeFile(file, "complaints/" + complaint.getComplaintIdString());
            uploadedFilePaths.add(filePath);
        }
        complaint.getImages().addAll(uploadedFilePaths);
        Complaint updatedComplaint = complaintRepository.save(complaint);
        return convertToDto(updatedComplaint);
    }

    @Override
    public ComplaintStatsDto getComplaintStats(User currentUser) {
        // Admin/Warden see all stats. Student might see their own stats if this endpoint is available to them.
        // This implementation assumes admin/warden context.
        long total = complaintRepository.count();
        long pending = complaintRepository.countByStatus(ComplaintStatus.PENDING);
        long inProgress = complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS);
        long resolved = complaintRepository.countByStatus(ComplaintStatus.RESOLVED);

        // Example for category counts. This would be better with a group by query.
        List<Map<String, Object>> byCategory = new ArrayList<>();
        for(ComplaintCategory cat : ComplaintCategory.values()){
            byCategory.add(Map.of("category", cat.name(), "count", complaintRepository.countByCategory(cat)));
        }

        List<Map<String, Object>> byPriority = new ArrayList<>();
         for(ComplaintPriority prio : ComplaintPriority.values()){
            // Need a countByPriority method or use Specifications
            // byPriority.add(Map.of("priority", prio.name(), "count", complaintRepository.countByPriority(prio)));
        }

        // Avg resolution time would require an aggregate query.
        // Double avgResTime = complaintRepository.getAverageResolutionTimeHours();

        return new ComplaintStatsDto(total, pending, inProgress, resolved, byCategory, byPriority, null);
    }
}
