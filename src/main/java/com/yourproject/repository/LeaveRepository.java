package com.yourproject.repository;

import com.yourproject.entity.Leave;
import com.yourproject.entity.LeaveStatus;
import com.yourproject.entity.LeaveType;
import com.yourproject.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long>, JpaSpecificationExecutor<Leave> {

    Optional<Leave> findByLeaveIdString(String leaveIdString);

    Page<Leave> findByStudent(User student, Pageable pageable);
    List<Leave> findByStudent(User student);

    Page<Leave> findByStatus(LeaveStatus status, Pageable pageable);
    List<Leave> findByStatus(LeaveStatus status);

    Page<Leave> findByLeaveType(LeaveType leaveType, Pageable pageable);
    List<Leave> findByLeaveType(LeaveType leaveType);

    Page<Leave> findByStudentAndStatus(User student, LeaveStatus status, Pageable pageable);
    List<Leave> findByStudentAndStatus(User student, LeaveStatus status);

    // For stats or specific checks
    long countByStatus(LeaveStatus status);
    long countByLeaveType(LeaveType leaveType);

    @Query("SELECT l FROM Leave l WHERE l.status = :status AND l.endDate < :date AND l.actualReturnDate IS NULL")
    List<Leave> findOverdueLeaves(@Param("status") LeaveStatus status, @Param("date") LocalDate date);

    @Query("SELECT l FROM Leave l WHERE l.status = :status AND l.startDate <= :date AND l.endDate >= :date")
    List<Leave> findActiveLeavesOnDate(@Param("status") LeaveStatus status, @Param("date") LocalDate date);

    // Find leaves within a date range for a particular student
    List<Leave> findByStudentAndStartDateBetweenOrEndDateBetween(User student, LocalDate rangeStart1, LocalDate rangeEnd1, LocalDate rangeStart2, LocalDate rangeEnd2);
}
