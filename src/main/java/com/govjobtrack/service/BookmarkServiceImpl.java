package com.govjobtrack.service;

import com.govjobtrack.exception.BookmarkAlreadyExistsException;
import com.govjobtrack.exception.BookmarkNotFoundException;
import com.govjobtrack.exception.ResourceNotFoundException;
import com.govjobtrack.model.Bookmark;
import com.govjobtrack.model.Job;
import com.govjobtrack.model.User;
import com.govjobtrack.payload.response.BookmarkResponse;
import com.govjobtrack.payload.response.MessageResponse;
import com.govjobtrack.repository.BookmarkRepository;
import com.govjobtrack.repository.JobRepository;
import com.govjobtrack.repository.UserRepository;
import com.govjobtrack.security.services.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookmarkServiceImpl implements BookmarkService {

    private static final Logger logger = LoggerFactory.getLogger(BookmarkServiceImpl.class);

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    // --- Mapper method (can be moved to a dedicated mapper class later) ---
    private BookmarkResponse mapBookmarkToBookmarkResponse(Bookmark bookmark) {
        if (bookmark == null) {
            return null;
        }
        Job job = bookmark.getJob(); // Assuming Job is EAGER fetched or accessed within transaction
        return new BookmarkResponse(
                bookmark.getId(),
                bookmark.getUser().getId(),
                job.getId(),
                job.getTitle(),
                job.getDepartment(),
                bookmark.getBookmarkedDate(),
                job.getLastDateToApply()
        );
    }

    @Override
    @Transactional
    public BookmarkResponse addBookmark(Long jobId, UserDetailsImpl currentUserDetails) {
        User currentUser = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserDetails.getId()));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        if (bookmarkRepository.findByUserAndJob(currentUser, job).isPresent()) {
            throw new BookmarkAlreadyExistsException(currentUser.getId(), job.getId());
        }

        Bookmark bookmark = new Bookmark(currentUser, job);
        // bookmarkedDate is set by @CreatedDate

        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        logger.info("Bookmark created with ID: {} for User ID: {} and Job ID: {}", savedBookmark.getId(), currentUser.getId(), job.getId());
        return mapBookmarkToBookmarkResponse(savedBookmark);
    }

    @Override
    @Transactional
    public MessageResponse removeBookmark(Long jobId, UserDetailsImpl currentUserDetails) {
        User currentUser = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserDetails.getId()));

        Job job = jobRepository.findById(jobId) // Ensure job exists, though not strictly needed if bookmark implies job exists
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        Bookmark bookmark = bookmarkRepository.findByUserAndJob(currentUser, job)
                .orElseThrow(() -> new BookmarkNotFoundException(currentUser.getId(), job.getId()));

        bookmarkRepository.delete(bookmark);
        logger.info("Bookmark for User ID: {} and Job ID: {} deleted successfully", currentUser.getId(), job.getId());
        return new MessageResponse("Bookmark removed successfully!");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookmarkResponse> getUserBookmarks(UserDetailsImpl currentUserDetails, Pageable pageable) {
        User currentUser = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserDetails.getId()));

        Page<Bookmark> bookmarksPage = bookmarkRepository.findByUser(currentUser, pageable);

        // Need to adjust findByUser in BookmarkRepository to accept Pageable
        // Let's assume it's: List<Bookmark> findByUser(User user);
        // If so, pagination must be handled manually or repository method updated.
        // For now, let's update the repository method signature in mind.
        // If BookmarkRepository.findByUser returns List<Bookmark>, then:
        // List<Bookmark> bookmarks = bookmarkRepository.findByUser(currentUser);
        // Page<Bookmark> bookmarksPage = new PageImpl<>(bookmarks, pageable, bookmarks.size());
        // This is not efficient. The repository method should support Pageable.

        // Using the new repository method with JOIN FETCH
        Page<Bookmark> bookmarksPage = bookmarkRepository.findByUserWithJobAndUserEager(currentUser, pageable);

        return bookmarksPage.map(this::mapBookmarkToBookmarkResponse);
    }
}
