package com.yourproject.repository;

import com.yourproject.entity.Like;
import com.yourproject.entity.Announcement;
import com.yourproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByAnnouncementAndUser(Announcement announcement, User user);

    List<Like> findByAnnouncement(Announcement announcement);

    List<Like> findByUser(User user);

    long countByAnnouncement(Announcement announcement);

    boolean existsByAnnouncementAndUser(Announcement announcement, User user);
}
