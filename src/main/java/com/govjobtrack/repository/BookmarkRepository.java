package com.govjobtrack.repository;

import com.govjobtrack.model.Bookmark;
import com.govjobtrack.model.Job;
import com.govjobtrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByUserAndJob(User user, Job job);
    List<Bookmark> findByUser(User user);
    // We might also want a method to find Bookmarks by Job, e.g., to see how many users bookmarked a specific job
    // List<Bookmark> findByJob(Job job);
}
