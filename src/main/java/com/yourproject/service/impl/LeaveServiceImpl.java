package com.yourproject.service.impl;

import com.yourproject.dto.*;
import com.yourproject.entity.*;
import com.yourproject.entity.embeddable.*;
import com.yourproject.exception.AccessDeniedException;
import com.yourproject.exception.BadRequestException;
import com.yourproject.exception.ResourceNotFoundException;
import com.yourproject.repository.LeaveRepository;
import com.yourproject.repository.UserRepository;
import com.yourproject.service.EmailService;
import com.yourproject.service.FileUploadService; // If handling attachments
import com.yourproject.service.LeaveService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile; // If handling attachments
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
// import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map; // For stats

@Service
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRepository leaveRepository;
    private final UserRepository userRepository; // For approvedBy
    private final ModelMapper modelMapper;
    private final EmailService emailService;
    private final FileUploadService fileUploadService; // If handling attachments

    @Autowired
    public LeaveServiceImpl(LeaveRepository leaveRepository,
                            UserRepository userRepository,
                            ModelMapper modelMapper,
                            EmailService emailService,
                            FileUploadService fileUploadService ) { // If handling attachments
        this.leaveRepository = leaveRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.emailService = emailService;
        this.fileUploadService = fileUploadService; // If handling attachments
    }

    private LeaveDto convertToDto(Leave leave) {
        LeaveDto dto = modelMapper.map(leave, LeaveDto.class);
        if (leave.getStudent() != null) {
            dto.setStudent(modelMapper.map(leave.getStudent(), UserSlimDto.class));
        }
        if (leave.getApprovedBy() != null) {
            dto.setApprovedBy(modelMapper.map(leave.getApprovedBy(), UserSlimDto.class));
        }
        // Populate calculated fields
        dto.setDurationDays(calculateDurationDays(leave));
        dto.setCurrentOverallStatus(calculateCurrentOverallStatus(leave));
        dto.setOverdueDays(calculateOverdueDays(leave));
        return dto;
    }

    private Leave findLeaveEntityById(Long leaveId) {
        return leaveRepository.findById(leaveId)
            .orElseThrow(() -> new ResourceNotFoundException("Leave application not found with ID: " + leaveId));
    }

    private String generateLeaveIdString() {
        // LVYYYYMMDDXXXX
        String yearMonthDay = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%04d", new Random().nextInt(10000));
        return "LV" + yearMonthDay + randomSuffix;
    }

    private void addStatusHistory(Leave leave, LeaveStatus status, User updatedBy, String comment) {
        LeaveStatusHistoryItem historyItem = new LeaveStatusHistoryItem(
            status, updatedBy.getId(), LocalDateTime.now(), comment
        );
        leave.getStatusHistory().add(historyItem);
    }

    @Override
    @Transactional
    public LeaveDto createLeaveApplication(LeaveRequestDto requestDto, User currentUser) {
        if (currentUser.getRole() != Role.STUDENT) {
            throw new AccessDeniedException("Only students can apply for leave.");
        }
        if (requestDto.getStartDate().isAfter(requestDto.getEndDate())) {
            throw new BadRequestException("Leave start date must be before or same as end date.");
        }

        Leave leave = modelMapper.map(requestDto, Leave.class);
        leave.setStudent(currentUser);
        leave.setLeaveIdString(generateLeaveIdString());
        leave.setStatus(LeaveStatus.PENDING);
        leave.setAppliedDate(LocalDateTime.now());

        // Logic for medical cert and parent approval requirements based on type/duration
        if (leave.getLeaveType() == LeaveType.MEDICAL) {
            if (leave.getMedicalCertificateInfo() == null) leave.setMedicalCertificateInfo(new MedicalCertificateInfo());
            leave.getMedicalCertificateInfo().setRequired(true);
        }
        long duration = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
        if (duration > 7) { // Example: leaves longer than 7 days need parental approval
             if (leave.getParentalApprovalInfo() == null) leave.setParentalApprovalInfo(new ParentalApproval());
            leave.getParentalApprovalInfo().setRequired(true);
        }

        addStatusHistory(leave, LeaveStatus.PENDING, currentUser, "Leave application submitted.");

        Leave savedLeave = leaveRepository.save(leave);
        // Notify admin/warden about new leave application
        // emailService.sendLeaveApplicationNotification(savedLeave);
        return convertToDto(savedLeave);
    }

    @Override
    public LeaveDto getLeaveApplicationById(Long leaveId, User currentUser) {
        Leave leave = findLeaveEntityById(leaveId);
        if (currentUser.getRole() == Role.STUDENT && !leave.getStudent().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to view this leave application.");
        }
        return convertToDto(leave);
    }

    @Override
    public Page<LeaveDto> getAllLeaveApplications(Pageable pageable, User currentUser, String statusFilter, String leaveTypeFilter) {
         Specification<Leave> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (currentUser.getRole() == Role.STUDENT) {
                predicates.add(cb.equal(root.get("student"), currentUser));
            }
            if (StringUtils.hasText(statusFilter)) {
                try {
                    predicates.add(cb.equal(root.get("status"), LeaveStatus.valueOf(statusFilter.toUpperCase())));
                } catch (IllegalArgumentException e) {/*ignore*/}
            }
            if (StringUtils.hasText(leaveTypeFilter)) {
                 try {
                    predicates.add(cb.equal(root.get("leaveType"), LeaveType.valueOf(leaveTypeFilter.toUpperCase())));
                } catch (IllegalArgumentException e) {/*ignore*/}
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return leaveRepository.findAll(spec, pageable).map(this::convertToDto);
    }

    @Override
    @Transactional
    public LeaveDto updateLeaveApplication(Long leaveId, LeaveRequestDto requestDto, User currentUser) {
        Leave leave = findLeaveEntityById(leaveId);
        if (!leave.getStudent().getId().equals(currentUser.getId()) || leave.getStatus() != LeaveStatus.PENDING) {
            throw new AccessDeniedException("Cannot update this leave application. It may not be yours or is already processed.");
        }
         if (requestDto.getStartDate().isAfter(requestDto.getEndDate())) {
            throw new BadRequestException("Leave start date must be before or same as end date.");
        }

        // Update allowed fields for a pending leave application by student
        leave.setLeaveType(requestDto.getLeaveType());
        leave.setStartDate(requestDto.getStartDate());
        leave.setEndDate(requestDto.getEndDate());
        leave.setReason(requestDto.getReason());
        if(requestDto.getEmergencyContact() != null) leave.setEmergencyContact(requestDto.getEmergencyContact());
        if(requestDto.getDestination() != null) leave.setDestination(requestDto.getDestination());
        // Attachments update logic would be more complex (remove old, add new if URLs provided)
        if(requestDto.getAttachments() != null) leave.setAttachments(requestDto.getAttachments());

        // Re-evaluate requirements
        if (leave.getLeaveType() == LeaveType.MEDICAL) {
            if (leave.getMedicalCertificateInfo() == null) leave.setMedicalCertificateInfo(new MedicalCertificateInfo());
            leave.getMedicalCertificateInfo().setRequired(true);
        } else {
             if (leave.getMedicalCertificateInfo() != null) leave.getMedicalCertificateInfo().setRequired(false);
        }
        long duration = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
        if (duration > 7) {
             if (leave.getParentalApprovalInfo() == null) leave.setParentalApprovalInfo(new ParentalApproval());
            leave.getParentalApprovalInfo().setRequired(true);
        } else {
            if (leave.getParentalApprovalInfo() != null) leave.getParentalApprovalInfo().setRequired(false);
        }


        Leave updatedLeave = leaveRepository.save(leave);
        return convertToDto(updatedLeave);
    }

    @Override
    @Transactional
    public LeaveDto updateLeaveStatus(Long leaveId, LeaveStatusUpdateRequestDto statusRequestDto, User currentUser) {
        if (currentUser.getRole() == Role.STUDENT) {
            throw new AccessDeniedException("Students cannot approve or reject leave applications.");
        }
        Leave leave = findLeaveEntityById(leaveId);

        LeaveStatus newStatus = statusRequestDto.getStatus();
        if (newStatus == LeaveStatus.PENDING) throw new BadRequestException("Cannot set status back to PENDING through this action.");

        leave.setStatus(newStatus);
        leave.setApprovedBy(currentUser);
        leave.setApprovedDate(LocalDateTime.now());
        if (newStatus == LeaveStatus.REJECTED) {
            leave.setRejectionReason(statusRequestDto.getComment());
        } else {
            leave.setRejectionReason(null); // Clear rejection reason if approved
        }

        addStatusHistory(leave, newStatus, currentUser, statusRequestDto.getComment());
        Leave updatedLeave = leaveRepository.save(leave);

        // emailService.sendLeaveStatusUpdateEmail(leave.getStudent(), updatedLeave);
        return convertToDto(updatedLeave);
    }

    @Override
    @Transactional
    public LeaveDto cancelLeaveApplication(Long leaveId, String reason, User currentUser) {
        Leave leave = findLeaveEntityById(leaveId);
        boolean isAdminOrWarden = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.WARDEN;

        if (!isAdminOrWarden && !leave.getStudent().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to cancel this leave application.");
        }
        if (leave.getStatus() != LeaveStatus.PENDING && leave.getStatus() != LeaveStatus.APPROVED) {
            throw new BadRequestException("Only pending or approved leaves can be cancelled. Current status: " + leave.getStatus());
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        addStatusHistory(leave, LeaveStatus.CANCELLED, currentUser, reason);

        // If admin/warden cancelled, they are approvedBy for this action context
        if (isAdminOrWarden) {
            leave.setApprovedBy(currentUser);
            leave.setApprovedDate(LocalDateTime.now()); // Timestamp of cancellation action
        }

        Leave cancelledLeave = leaveRepository.save(leave);
        // emailService.sendLeaveCancellationEmail(leave.getStudent(), cancelledLeave);
        // If admin cancelled, maybe notify student. If student cancelled, maybe notify admin/warden.
        return convertToDto(cancelledLeave);
    }

    @Override
    @Transactional
    public LeaveDto requestExtension(Long leaveId, LeaveExtensionApiRequestDto extRequestDto, User currentUser) {
        Leave leave = findLeaveEntityById(leaveId);
        if (!leave.getStudent().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the student who applied can request an extension.");
        }
        if (leave.getStatus() != LeaveStatus.APPROVED) {
            throw new BadRequestException("Can only request extension for approved leaves.");
        }
        if (extRequestDto.getNewEndDate().isBefore(leave.getEndDate()) || extRequestDto.getNewEndDate().isEqual(leave.getEndDate())) {
            throw new BadRequestException("New end date must be after the current leave end date.");
        }

        LeaveExtensionRequest extension = new LeaveExtensionRequest();
        extension.setRequestedEndDate(extRequestDto.getNewEndDate());
        extension.setReason(extRequestDto.getReason());
        extension.setRequestedDate(LocalDateTime.now());
        extension.setStatus(LeaveExtensionStatus.PENDING);

        leave.getExtensionRequests().add(extension);
        leave.setExtended(true); // Mark that an extension has been requested/is active
        Leave updatedLeave = leaveRepository.save(leave);
        // Notify admin/warden about extension request
        // emailService.sendLeaveExtensionRequestEmail(updatedLeave, extension);
        return convertToDto(updatedLeave);
    }

    @Override
    @Transactional
    public LeaveDto updateExtensionStatus(Long leaveId, Long extensionId, LeaveExtensionStatusUpdateRequestDto statusRequestDto, User currentUser) {
        if (currentUser.getRole() == Role.STUDENT) {
            throw new AccessDeniedException("Students cannot approve or reject leave extensions.");
        }
        Leave leave = findLeaveEntityById(leaveId);
        LeaveExtensionRequest extension = leave.getExtensionRequests().stream()
            .filter(ext -> ext.hashCode() == extensionId) // Simplistic match; ideally use a persisted ID if embeddables had one
            .findFirst() // This matching is weak if LeaveExtensionRequest is just embeddable without its own ID.
                         // For a robust solution, LeaveExtensionRequest should be a separate @Entity.
                         // Assuming for now it's identifiable if it's the latest PENDING one.
            .orElseThrow(() -> new ResourceNotFoundException("Leave extension request not found."));

        if(extension.getStatus() != LeaveExtensionStatus.PENDING) {
            throw new BadRequestException("Leave extension request has already been processed.");
        }

        extension.setStatus(statusRequestDto.getStatus());
        extension.setApprovedById(currentUser.getId());
        extension.setApprovedDate(LocalDateTime.now());

        if (statusRequestDto.getStatus() == LeaveExtensionStatus.APPROVED) {
            leave.setEndDate(extension.getRequestedEndDate());
        }

        Leave updatedLeave = leaveRepository.save(leave);
        // emailService.sendLeaveExtensionStatusEmail(leave.getStudent(), updatedLeave, extension);
        return convertToDto(updatedLeave);
    }

    @Override
    @Transactional
    public LeaveDto markReturnFromLeave(Long leaveId, MarkReturnRequestDto returnRequestDto, User currentUser) {
         if (currentUser.getRole() == Role.STUDENT) { // Or only admin/warden can mark return
            throw new AccessDeniedException("Students cannot mark their own return. Please contact warden/admin.");
        }
        Leave leave = findLeaveEntityById(leaveId);
        if (leave.getStatus() != LeaveStatus.APPROVED) {
            throw new BadRequestException("Cannot mark return for a leave that was not approved.");
        }
        if (leave.getActualReturnDate() != null) {
            throw new BadRequestException("Return already marked for this leave.");
        }

        leave.setActualReturnDate(returnRequestDto.getActualReturnDate() != null ? returnRequestDto.getActualReturnDate() : LocalDate.now());
        // No status change, but actualReturnDate is now set.
        addStatusHistory(leave, leave.getStatus(), currentUser, "Student returned on " + leave.getActualReturnDate());

        Leave updatedLeave = leaveRepository.save(leave);
        return convertToDto(updatedLeave);
    }

    @Override
    @Transactional
    public LeaveDto addAttachmentsToLeave(Long leaveId, List<MultipartFile> files, String attachmentType, User currentUser) {
        Leave leave = findLeaveEntityById(leaveId);
        // Permission check: only student who applied or admin/warden
        if (!(currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.WARDEN || leave.getStudent().getId().equals(currentUser.getId()))) {
            throw new AccessDeniedException("You do not have permission to add attachments to this leave application.");
        }

        List<String> newAttachmentPaths = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // Store file, e.g., in "leaves/{leaveIdString}/"
            String relativePath = fileUploadService.storeFile(file, "leaves", leave.getLeaveIdString(), attachmentType); // attachmentType can be part of path
            newAttachmentPaths.add(relativePath);

            // If it's a medical certificate, update MedicalCertificateInfo
            if ("medical_certificate".equalsIgnoreCase(attachmentType)) {
                MedicalCertificateInfo mcInfo = leave.getMedicalCertificateInfo();
                if (mcInfo == null) {
                    mcInfo = new MedicalCertificateInfo();
                    leave.setMedicalCertificateInfo(mcInfo);
                }
                mcInfo.setUploaded(true);
                mcInfo.setFileName(StringUtils.cleanPath(file.getOriginalFilename()));
                // mcInfo.setFilePath(relativePath); // If you decide to store path in MedicalCertificateInfo as well
                mcInfo.setUploadDate(LocalDate.now());
            }
        }

        if (!"medical_certificate".equalsIgnoreCase(attachmentType)) {
            leave.getAttachments().addAll(newAttachmentPaths); // Add to general attachments list
        }

        Leave updatedLeave = leaveRepository.save(leave);
        return convertToDto(updatedLeave);
    }


    @Override
    public LeaveStatsDto getLeaveStats(User currentUser, Integer yearFilter) {
        int year = (yearFilter == null) ? LocalDate.now().getYear() : yearFilter;
        // Requires specific queries in repository for year-based stats
        long total = leaveRepository.count(); // Filter by year
        long pending = leaveRepository.countByStatus(LeaveStatus.PENDING);
        long approved = leaveRepository.countByStatus(LeaveStatus.APPROVED);
        long rejected = leaveRepository.countByStatus(LeaveStatus.REJECTED);
        long active = leaveRepository.findActiveLeavesOnDate(LeaveStatus.APPROVED, LocalDate.now()).size();
        long overdue = leaveRepository.findOverdueLeaves(LeaveStatus.APPROVED, LocalDate.now()).size();

        // Example for type counts
        List<Map<String, Object>> byType = new ArrayList<>();
        for(LeaveType type : LeaveType.values()){
            // byType.add(Map.of("type", type.name(), "count", leaveRepository.countByLeaveTypeAndYear(type, year)));
        }
        return new LeaveStatsDto(year, total, pending, approved, rejected, active, overdue, byType, new ArrayList<>());
    }

    // Helper methods for calculated DTO fields
    private Integer calculateDurationDays(Leave leave) {
        if (leave.getStartDate() != null && leave.getEndDate() != null) {
            return (int) (ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1);
        }
        return 0;
    }

    private String calculateCurrentOverallStatus(Leave leave) {
        if (leave.getStatus() != LeaveStatus.APPROVED) {
            return leave.getStatus().name();
        }
        LocalDate now = LocalDate.now();
        if (now.isBefore(leave.getStartDate())) {
            return "UPCOMING";
        } else if (!now.isAfter(leave.getEndDate())) { // On or before end date
            return "ACTIVE";
        } else { // After end date
            return leave.getActualReturnDate() != null ? "COMPLETED" : "OVERDUE_RETURN";
        }
    }

    private Integer calculateOverdueDays(Leave leave) {
        if (leave.getStatus() == LeaveStatus.APPROVED && leave.getActualReturnDate() == null) {
            LocalDate now = LocalDate.now();
            if (now.isAfter(leave.getEndDate())) {
                return (int) ChronoUnit.DAYS.between(leave.getEndDate(), now);
            }
        }
        return 0;
    }
}
