package com.govjobtrack.repository;

import com.govjobtrack.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// We can add custom query methods here later if needed, e.g., findByDepartment, findByTitleContaining, etc.
@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
}
