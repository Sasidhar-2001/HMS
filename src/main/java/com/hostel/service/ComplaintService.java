package com.hostel.service;

import com.hostel.repository.ComplaintRepository;
import com.hostel.model.Complaint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ComplaintService {

    @Autowired
    private ComplaintRepository complaintRepository;

    public Map<String, Object> getAllComplaints(String userId, String userRole, int page, int limit, String status, String category, String priority) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        
        // For now, return all complaints - you can add filtering logic based on user role
        Page<Complaint> complaintsPage = complaintRepository.findAll(pageable);
        
        Map<String, Object> result = new HashMap<>();
        result.put("complaints", complaintsPage.getContent());
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("current", page);
        pagination.put("pages", complaintsPage.getTotalPages());
        pagination.put("total", complaintsPage.getTotalElements());
        result.put("pagination", pagination);
        
        return result;
    }
}