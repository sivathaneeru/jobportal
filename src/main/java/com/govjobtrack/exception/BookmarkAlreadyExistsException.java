package com.govjobtrack.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // 409 Conflict
public class BookmarkAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BookmarkAlreadyExistsException(String message) {
        super(message);
    }

    public BookmarkAlreadyExistsException(Long userId, Long jobId) {
        super(String.format("Bookmark already exists for user ID '%s' and job ID '%s'", userId, jobId));
    }
}
