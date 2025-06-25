package com.govjobtrack.repository;

import com.govjobtrack.model.Job;
import com.govjobtrack.model.User;
import com.govjobtrack.model.Role;
import com.govjobtrack.model.RoleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class JobRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository; // To create the 'createdBy' user

    @Autowired
    private RoleRepository roleRepository; // To create roles for the user

    private User adminUser;

    @BeforeEach
    void setUp() {
        // Setup a user that can create jobs
        RoleEntity adminRole = roleRepository.findByName(Role.ROLE_ADMIN)
                .orElseGet(() -> {
                    RoleEntity newRole = new RoleEntity(Role.ROLE_ADMIN);
                    entityManager.persist(newRole); // Persist role if not found
                    return newRole;
                });

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(adminRole);

        adminUser = new User("Admin", "Poster", "admin.poster@example.com", "securepass");
        adminUser.setRoles(roles);
        entityManager.persist(adminUser); // Persist user
        entityManager.flush(); // Ensure user and role are in DB before job creation
    }

    @Test
    public void whenSaveJob_thenCanBeRetrieved() {
        // given
        Job newJob = new Job();
        newJob.setTitle("Software Engineer");
        newJob.setDepartment("Tech Department");
        newJob.setDescription("Develop amazing software.");
        newJob.setQualification("B.Sc. in CS or related field");
        newJob.setApplicationLink("https://apply.example.com/swe");
        newJob.setLastDateToApply(LocalDate.now().plusMonths(1));
        newJob.setCreatedBy(adminUser);
        // postedDate is @CreatedDate, should be set automatically

        Job savedJob = jobRepository.save(newJob);

        // when
        Optional<Job> foundJobOpt = jobRepository.findById(savedJob.getId());

        // then
        assertThat(foundJobOpt).isPresent();
        foundJobOpt.ifPresent(foundJob -> {
            assertThat(foundJob.getTitle()).isEqualTo("Software Engineer");
            assertThat(foundJob.getDepartment()).isEqualTo("Tech Department");
            assertThat(foundJob.getDescription()).isEqualTo("Develop amazing software.");
            assertThat(foundJob.getQualification()).isEqualTo("B.Sc. in CS or related field");
            assertThat(foundJob.getApplicationLink()).isEqualTo("https://apply.example.com/swe");
            assertThat(foundJob.getLastDateToApply()).isEqualTo(LocalDate.now().plusMonths(1));
            assertThat(foundJob.getPostedDate()).isNotNull(); // Auditing check
            assertThat(foundJob.getCreatedBy()).isNotNull();
            assertThat(foundJob.getCreatedBy().getId()).isEqualTo(adminUser.getId());
            assertThat(foundJob.getCreatedBy().getEmail()).isEqualTo("admin.poster@example.com");
        });
    }

    @Test
    public void whenFindById_withNonExistentId_thenReturnEmpty() {
        // when
        Optional<Job> foundJobOpt = jobRepository.findById(-99L); // A non-existent ID

        // then
        assertThat(foundJobOpt).isNotPresent();
    }
}
