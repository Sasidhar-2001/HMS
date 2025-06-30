package com.hostel.service;

import com.hostel.repository.AnnouncementRepository;
import com.hostel.model.Announcement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AnnouncementService {

    @Autowired
    private AnnouncementRepository announcementRepository;

    public Map<String, Object> getAllAnnouncements(String userId, String userRole, int page, int limit, String type, String priority) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        
        // For now, return all announcements - you can add filtering logic
        Page<Announcement> announcementsPage = announcementRepository.findAll(pageable);
        
        Map<String, Object> result = new HashMap<>();
        result.put("announcements", announcementsPage.getContent());
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("current", page);
        pagination.put("pages", announcementsPage.getTotalPages());
        pagination.put("total", announcementsPage.getTotalElements());
        result.put("pagination", pagination);
        
        return result;
    }

    public Map<String, Object> toggleLike(String announcementId, String userId) {
        Optional<Announcement> announcementOpt = announcementRepository.findById(announcementId);
        if (announcementOpt.isPresent()) {
            Announcement announcement = announcementOpt.get();
            // Implement like toggle logic here
            // For now, just return the announcement
            return Map.of("announcement", announcement, "liked", true);
        }
        throw new RuntimeException("Announcement not found");
    }
}