package com.govjobtrack.payload.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import javax.validation.constraints.FutureOrPresent; // For lastDateToApply if we want to enforce it
import java.time.LocalDate;

@Data
public class JobRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 150)
    private String department;

    @NotBlank
    private String description; // @Lob in entity, so no specific size limit here, but validation can be added

    @NotBlank
    private String qualification; // @Lob in entity

    @Size(max = 500)
    private String applicationLink;

    // @FutureOrPresent // Consider if all jobs must have a future or present last date
    private LocalDate lastDateToApply;
}
