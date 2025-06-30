package com.hostel.service;

import com.hostel.repository.RoomRepository;
import com.hostel.model.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    public Map<String, Object> getAllRooms(int page, int limit, String status, String block) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("roomNumber"));
        
        Page<Room> roomsPage = roomRepository.findAll(pageable);
        
        Map<String, Object> result = new HashMap<>();
        result.put("rooms", roomsPage.getContent());
        
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("current", page);
        pagination.put("pages", roomsPage.getTotalPages());
        pagination.put("total", roomsPage.getTotalElements());
        result.put("pagination", pagination);
        
        return result;
    }

    public Map<String, Object> getRoomStats() {
        long total = roomRepository.countByIsActive(true);
        long occupied = roomRepository.countByStatusAndIsActive("occupied", true);
        long available = roomRepository.countByStatusAndIsActive("available", true);
        long maintenance = roomRepository.countByStatusAndIsActive("maintenance", true);
        
        double occupancyPercentage = total > 0 ? (double) occupied / total * 100 : 0;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("occupied", occupied);
        stats.put("available", available);
        stats.put("maintenance", maintenance);
        stats.put("occupancyPercentage", Math.round(occupancyPercentage * 100.0) / 100.0);
        
        return stats;
    }
}