package com.govjobtrack.controller;

import com.govjobtrack.payload.response.BookmarkResponse;
import com.govjobtrack.payload.response.MessageResponse;
import com.govjobtrack.security.services.UserDetailsImpl;
import com.govjobtrack.service.BookmarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    @Autowired
    private BookmarkService bookmarkService;

    @PostMapping("/job/{jobId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BookmarkResponse> addBookmark(@PathVariable Long jobId,
                                                        @AuthenticationPrincipal UserDetailsImpl currentUser) {
        BookmarkResponse bookmarkResponse = bookmarkService.addBookmark(jobId, currentUser);
        return new ResponseEntity<>(bookmarkResponse, HttpStatus.CREATED);
    }

    @DeleteMapping("/job/{jobId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MessageResponse> removeBookmark(@PathVariable Long jobId,
                                                          @AuthenticationPrincipal UserDetailsImpl currentUser) {
        MessageResponse messageResponse = bookmarkService.removeBookmark(jobId, currentUser);
        return ResponseEntity.ok(messageResponse);
    }

    @GetMapping("/mybookmarks")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<BookmarkResponse>> getUserBookmarks(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PageableDefault(size = 10, sort = "bookmarkedDate") Pageable pageable) {
        Page<BookmarkResponse> bookmarksPage = bookmarkService.getUserBookmarks(currentUser, pageable);
        return ResponseEntity.ok(bookmarksPage);
    }
}
