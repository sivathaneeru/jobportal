package com.govjobtrack.service;

import com.govjobtrack.payload.response.BookmarkResponse;
import com.govjobtrack.payload.response.MessageResponse;
import com.govjobtrack.security.services.UserDetailsImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookmarkService {
    BookmarkResponse addBookmark(Long jobId, UserDetailsImpl currentUser);
    MessageResponse removeBookmark(Long jobId, UserDetailsImpl currentUser);
    Page<BookmarkResponse> getUserBookmarks(UserDetailsImpl currentUser, Pageable pageable);
}
