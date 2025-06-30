package com.hostel.service;

import com.hostel.repository.LeaveRepository;
import com.hostel.model.Leave;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class LeaveService {

    @Autowired
    private LeaveRepository leaveRepository;

    public Map<String, Object> getAllLeaves(String userId, String userRole, int page, int limit, String status, String leaveType) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        
        // For now, return all leaves - you can add filtering logic based on user role
        Page<Leave> leavesPage = leaveRepository.findAll(pageable);
        
        Map<String, Object> result = new HashMap<>();
        result.put("leaves", leavesPage.getContent());
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("current", page);
        pagination.put("pages", leavesPage.getTotalPages());
        pagination.put("total", leavesPage.getTotalElements());
        result.put("pagination", pagination);
        
        return result;
    }
}