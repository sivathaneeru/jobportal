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

    // Page<Bookmark> findByUser(User user, Pageable pageable); // Replaced by version with JOIN FETCH

    // Fetches Bookmarks with their associated Job and User eagerly to avoid N+1 issues when mapping
    @Query("SELECT b FROM Bookmark b JOIN FETCH b.job j JOIN FETCH b.user u WHERE u = :user")
    Page<Bookmark> findByUserWithJobAndUserEager(User user, Pageable pageable);

    // We might also want a method to find Bookmarks by Job, e.g., to see how many users bookmarked a specific job
    // List<Bookmark> findByJob(Job job);
}
