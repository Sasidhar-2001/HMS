package com.yourproject.repository;

import com.yourproject.entity.Room;
import com.yourproject.entity.RoomStatus;
import com.yourproject.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    Optional<Room> findByRoomNumber(String roomNumber);

    List<Room> findByBlockAndFloor(String block, int floor);

    List<Room> findByType(RoomType type);

    List<Room> findByStatus(RoomStatus status);

    List<Room> findByIsActiveTrue();

    // Find available rooms: status is AVAILABLE and current occupancy < capacity
    // This requires a more complex query logic, often handled in service layer or with @Query / Specifications
    // For example, a conceptual query:
    @Query("SELECT r FROM Room r WHERE r.isActive = true AND r.status = com.yourproject.entity.RoomStatus.AVAILABLE AND SIZE(r.occupancies) < r.capacity")
    List<Room> findAvailableRooms(); // Note: SIZE(r.occupancies) might need adjustment based on how active occupants are filtered

     List<Room> findByTypeAndIsActiveTrue(RoomType type);
     List<Room> findByBlockAndIsActiveTrue(String block);

}
