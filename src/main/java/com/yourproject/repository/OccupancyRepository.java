package com.yourproject.repository;

import com.yourproject.entity.Occupancy;
import com.yourproject.entity.Room;
import com.yourproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OccupancyRepository extends JpaRepository<Occupancy, Long>, JpaSpecificationExecutor<Occupancy> {

    Optional<Occupancy> findByStudentAndIsActiveTrue(User student);

    List<Occupancy> findByRoomAndIsActiveTrue(Room room);

    List<Occupancy> findByStudent(User student);

    List<Occupancy> findByRoom(Room room);

    boolean existsByStudentAndIsActiveTrue(User student);

    long countByRoomAndIsActiveTrue(Room room);

}
