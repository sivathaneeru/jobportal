package com.govjobtrack.payload.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkResponse {
    private Long id;
    private Long userId;
    private Long jobId;
    private String jobTitle; // For easier display on the frontend
    private String jobDepartment; // Added for more context
    private LocalDateTime bookmarkedDate;

    // We can add more Job details here if needed for the bookmark list view
    private LocalDate jobLastDateToApply;

}
