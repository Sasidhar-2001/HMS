package com.yourproject.service;

import com.yourproject.dto.OccupancyDto;
import com.yourproject.dto.RoomDto;
import com.yourproject.dto.RoomRequestDto;
// import com.yourproject.dto.RoomStatsDto;
import com.yourproject.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RoomService {

    RoomDto createRoom(RoomRequestDto roomRequestDto);
    RoomDto getRoomById(Long roomId);
    RoomDto getRoomByNumber(String roomNumber);
    Page<RoomDto> getAllRooms(Pageable pageable, String block, Integer floor, String type, String status);
    List<RoomDto> getAvailableRooms(String type, String block);
    RoomDto updateRoom(Long roomId, RoomRequestDto roomRequestDto);
    void deleteRoom(Long roomId); // Typically deactivation

    OccupancyDto assignStudentToRoom(Long studentId, Long roomId, Integer bedNumber);
    void removeStudentFromRoom(Long studentId, Long roomId); // Or using Occupancy ID

    // RoomStatsDto getRoomStats();

    Room findRoomEntityById(Long roomId); // Helper for internal use
}
