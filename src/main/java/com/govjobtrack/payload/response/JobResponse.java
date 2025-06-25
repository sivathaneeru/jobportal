package com.govjobtrack.payload.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {
    private Long id;
    private String title;
    private String department;
    private String description;
    private String qualification;
    private String applicationLink;
    private LocalDate lastDateToApply;
    private LocalDateTime postedDate;
    private String createdByUsername; // e.g., email or full name of the admin who posted
    private Long createdByUserId;
}
