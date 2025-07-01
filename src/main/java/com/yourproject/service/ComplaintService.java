package com.yourproject.service;

import com.yourproject.dto.ComplaintDto;
import com.yourproject.dto.ComplaintRequestDto;
import com.yourproject.dto.ComplaintStatusUpdateRequestDto;
import com.yourproject.dto.ComplaintStatsDto;
import com.yourproject.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile; // For image uploads

import java.util.List;

public interface ComplaintService {

    ComplaintDto createComplaint(ComplaintRequestDto complaintRequestDto, User currentUser);
    ComplaintDto getComplaintById(Long complaintId, User currentUser);
    Page<ComplaintDto> getAllComplaints(Pageable pageable, User currentUser, String status, String category, String priority);
    ComplaintDto updateComplaint(Long complaintId, ComplaintRequestDto complaintRequestDto, User currentUser);
    ComplaintDto updateComplaintStatus(Long complaintId, ComplaintStatusUpdateRequestDto statusUpdateRequestDto, User currentUser);
    void deleteComplaint(Long complaintId, User currentUser);

    ComplaintDto uploadComplaintImages(Long complaintId, List<MultipartFile> files, User currentUser);
    ComplaintStatsDto getComplaintStats(User currentUser); // Permissions might restrict what stats are seen
}
