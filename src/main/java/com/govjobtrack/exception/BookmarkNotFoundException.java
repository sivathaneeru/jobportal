package com.govjobtrack.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BookmarkNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BookmarkNotFoundException(String message) {
        super(message);
    }

    public BookmarkNotFoundException(Long userId, Long jobId) {
        super(String.format("Bookmark not found for user ID '%s' and job ID '%s'", userId, jobId));
    }

    public BookmarkNotFoundException(Long bookmarkId) {
        super(String.format("Bookmark not found with ID '%s'", bookmarkId));
    }
}
