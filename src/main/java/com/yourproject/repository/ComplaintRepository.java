package com.yourproject.repository;

import com.yourproject.entity.Complaint;
import com.yourproject.entity.ComplaintStatus;
import com.yourproject.entity.ComplaintCategory;
import com.yourproject.entity.User;
import com.yourproject.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

    Optional<Complaint> findByComplaintIdString(String complaintIdString);

    Page<Complaint> findByReportedBy(User reportedBy, Pageable pageable);
    List<Complaint> findByReportedBy(User reportedBy);

    Page<Complaint> findByAssignedTo(User assignedTo, Pageable pageable);
    List<Complaint> findByAssignedTo(User assignedTo);

    Page<Complaint> findByRoom(Room room, Pageable pageable);
    List<Complaint> findByRoom(Room room);

    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);
    List<Complaint> findByStatus(ComplaintStatus status);

    Page<Complaint> findByCategory(ComplaintCategory category, Pageable pageable);
    List<Complaint> findByCategory(ComplaintCategory category);

    // For dashboard/stats:
    long countByStatus(ComplaintStatus status);
    long countByCategory(ComplaintCategory category);
    long countByStatusAndExpectedResolutionDateBefore(ComplaintStatus status, LocalDate date);

    // Find complaints by multiple criteria (example, can be expanded or use Specifications)
    Page<Complaint> findByStatusAndCategoryAndPriority(
            ComplaintStatus status,
            ComplaintCategory category,
            com.yourproject.entity.ComplaintPriority priority, // FQCN to avoid import clash if any
            Pageable pageable
    );
}
