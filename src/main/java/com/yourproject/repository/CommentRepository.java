package com.yourproject.repository;

import com.yourproject.entity.Comment;
import com.yourproject.entity.Announcement;
import com.yourproject.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByAnnouncement(Announcement announcement);
    Page<Comment> findByAnnouncement(Announcement announcement, Pageable pageable);

    List<Comment> findByUser(User user);
    Page<Comment> findByUser(User user, Pageable pageable);

    long countByAnnouncement(Announcement announcement);
}
