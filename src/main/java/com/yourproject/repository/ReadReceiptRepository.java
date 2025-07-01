package com.yourproject.repository;

import com.yourproject.entity.ReadReceipt;
import com.yourproject.entity.Announcement;
import com.yourproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ReadReceiptRepository extends JpaRepository<ReadReceipt, Long> {

    Optional<ReadReceipt> findByAnnouncementAndUser(Announcement announcement, User user);

    List<ReadReceipt> findByAnnouncement(Announcement announcement);

    List<ReadReceipt> findByUser(User user);

    long countByAnnouncement(Announcement announcement);

    boolean existsByAnnouncementAndUser(Announcement announcement, User user);
}
