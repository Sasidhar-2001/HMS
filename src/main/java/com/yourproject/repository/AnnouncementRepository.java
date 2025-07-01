package com.yourproject.repository;

import com.yourproject.entity.Announcement;
import com.yourproject.entity.AnnouncementStatus;
import com.yourproject.entity.AnnouncementType;
import com.yourproject.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long>, JpaSpecificationExecutor<Announcement> {

    List<Announcement> findByCreatedBy(User createdBy);

    List<Announcement> findByStatus(AnnouncementStatus status);

    List<Announcement> findByType(AnnouncementType type);

    // Find announcements for a student (all, students, or specific user) that are published and not expired
    @Query("SELECT a FROM Announcement a LEFT JOIN a.targetUsers tu WHERE a.status = :status " +
           "AND (a.expiryDate IS NULL OR a.expiryDate >= :now) " +
           "AND (a.targetAudience = com.yourproject.entity.AnnouncementTargetAudience.ALL " +
           "OR a.targetAudience = com.yourproject.entity.AnnouncementTargetAudience.STUDENTS " +
           "OR tu = :user)")
    Page<Announcement> findPublishedAnnouncementsForUser(@Param("status") AnnouncementStatus status,
                                                         @Param("now") LocalDateTime now,
                                                         @Param("user") User user,
                                                         Pageable pageable);

    Page<Announcement> findByStatusAndTargetAudienceInAndTargetUsersContainsOrTargetAudienceIn(
            AnnouncementStatus status,
            List<com.yourproject.entity.AnnouncementTargetAudience> generalAudiences,
            User targetUser,
            List<com.yourproject.entity.AnnouncementTargetAudience> userSpecificAudiences,
            Pageable pageable
    );


    // Find announcements by various criteria for admin/warden view
    Page<Announcement> findByStatusAndTypeAndPriority(AnnouncementStatus status, AnnouncementType type, com.yourproject.entity.AnnouncementPriority priority, Pageable pageable);
    // Add more specific finders or use Specifications as needed

    List<Announcement> findByStatusAndExpiryDateBefore(AnnouncementStatus status, LocalDateTime now);
}
