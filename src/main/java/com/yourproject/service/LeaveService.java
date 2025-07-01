package com.yourproject.service;

import com.yourproject.dto.*;
import com.yourproject.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile; // If attachments are handled as file uploads

import java.util.List; // If attachments are handled as file uploads

public interface LeaveService {

    LeaveDto createLeaveApplication(LeaveRequestDto leaveRequestDto, User currentUser);
    LeaveDto getLeaveApplicationById(Long leaveId, User currentUser);
    Page<LeaveDto> getAllLeaveApplications(Pageable pageable, User currentUser, String status, String leaveType);
    LeaveDto updateLeaveApplication(Long leaveId, LeaveRequestDto leaveRequestDto, User currentUser); // For student updating their own pending leave
    LeaveDto updateLeaveStatus(Long leaveId, LeaveStatusUpdateRequestDto statusUpdateRequestDto, User currentUser); // For admin/warden approving/rejecting
    LeaveDto cancelLeaveApplication(Long leaveId, String reason, User currentUser); // Student or admin cancelling

    LeaveDto requestExtension(Long leaveId, LeaveExtensionApiRequestDto extensionApiRequestDto, User currentUser);
    LeaveDto updateExtensionStatus(Long leaveId, Long extensionId, LeaveExtensionStatusUpdateRequestDto statusUpdateRequestDto, User currentUser);

    LeaveDto markReturnFromLeave(Long leaveId, MarkReturnRequestDto returnRequestDto, User currentUser);

    LeaveDto addAttachmentsToLeave(Long leaveId, List<MultipartFile> files, String attachmentType, User currentUser);
    LeaveStatsDto getLeaveStats(User currentUser, Integer year);
}
