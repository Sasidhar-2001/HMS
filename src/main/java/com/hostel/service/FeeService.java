package com.hostel.service;

import com.hostel.repository.FeeRepository;
import com.hostel.model.Fee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FeeService {

    @Autowired
    private FeeRepository feeRepository;

    public Map<String, Object> getAllFees(String userId, String userRole, int page, int limit, String status, String feeType) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("dueDate").descending());
        
        // For now, return all fees - you can add filtering logic based on user role
        Page<Fee> feesPage = feeRepository.findAll(pageable);
        
        Map<String, Object> result = new HashMap<>();
        result.put("fees", feesPage.getContent());
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("current", page);
        pagination.put("pages", feesPage.getTotalPages());
        pagination.put("total", feesPage.getTotalElements());
        result.put("pagination", pagination);
        
        return result;
    }
}