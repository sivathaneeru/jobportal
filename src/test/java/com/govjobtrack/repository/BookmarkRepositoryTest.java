package com.govjobtrack.repository;

import com.govjobtrack.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
public class BookmarkRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;
    private User jobCreator;
    private Job testJob1;
    private Job testJob2;

    @BeforeEach
    void setUp() {
        // Setup roles
        RoleEntity userRole = roleRepository.findByName(Role.ROLE_USER)
                .orElseGet(() -> entityManager.persist(new RoleEntity(Role.ROLE_USER)));
        RoleEntity adminRole = roleRepository.findByName(Role.ROLE_ADMIN)
                .orElseGet(() -> entityManager.persist(new RoleEntity(Role.ROLE_ADMIN)));
        entityManager.flush();


        // Setup users
        Set<RoleEntity> userRolesSet = new HashSet<>();
        userRolesSet.add(userRole);
        testUser = new User("Test", "User", "test.user@example.com", "password");
        testUser.setRoles(userRolesSet);
        entityManager.persist(testUser);

        Set<RoleEntity> adminRolesSet = new HashSet<>();
        adminRolesSet.add(adminRole);
        jobCreator = new User("Job", "Creator", "job.creator@example.com", "password");
        jobCreator.setRoles(adminRolesSet);
        entityManager.persist(jobCreator);
        entityManager.flush();

        // Setup jobs
        testJob1 = new Job();
        testJob1.setTitle("Software Developer");
        testJob1.setDepartment("Engineering");
        testJob1.setDescription("Develop cool apps.");
        testJob1.setQualification("Degree in CS");
        testJob1.setLastDateToApply(LocalDate.now().plusDays(30));
        testJob1.setCreatedBy(jobCreator);
        entityManager.persist(testJob1);

        testJob2 = new Job();
        testJob2.setTitle("Data Analyst");
        testJob2.setDepartment("Analytics");
        testJob2.setDescription("Analyze interesting data.");
        testJob2.setQualification("Degree in Stats");
        testJob2.setLastDateToApply(LocalDate.now().plusDays(60));
        testJob2.setCreatedBy(jobCreator);
        entityManager.persist(testJob2);
        entityManager.flush();
    }

    @Test
    public void whenSaveBookmark_thenCanBeRetrieved() {
        // given
        Bookmark bookmark = new Bookmark(testUser, testJob1);
        Bookmark savedBookmark = bookmarkRepository.save(bookmark);

        // when
        Optional<Bookmark> foundBookmarkOpt = bookmarkRepository.findById(savedBookmark.getId());

        // then
        assertThat(foundBookmarkOpt).isPresent();
        foundBookmarkOpt.ifPresent(foundBookmark -> {
            assertThat(foundBookmark.getUser().getId()).isEqualTo(testUser.getId());
            assertThat(foundBookmark.getJob().getId()).isEqualTo(testJob1.getId());
            assertThat(foundBookmark.getBookmarkedDate()).isNotNull(); // Auditing
        });
    }

    @Test
    public void whenFindByUserAndJob_thenReturnBookmark() {
        // given
        entityManager.persist(new Bookmark(testUser, testJob1));
        entityManager.flush();

        // when
        Optional<Bookmark> foundBookmarkOpt = bookmarkRepository.findByUserAndJob(testUser, testJob1);

        // then
        assertThat(foundBookmarkOpt).isPresent();
        assertThat(foundBookmarkOpt.get().getUser()).isEqualTo(testUser);
        assertThat(foundBookmarkOpt.get().getJob()).isEqualTo(testJob1);
    }

    @Test
    public void whenFindByUserAndJob_withNonExistentBookmark_thenReturnEmpty() {
        // when
        Optional<Bookmark> foundBookmarkOpt = bookmarkRepository.findByUserAndJob(testUser, testJob2); // testJob2 not bookmarked by testUser

        // then
        assertThat(foundBookmarkOpt).isNotPresent();
    }

    @Test
    public void whenFindByUser_thenReturnBookmarksList() {
        // given
        entityManager.persist(new Bookmark(testUser, testJob1));
        entityManager.persist(new Bookmark(testUser, testJob2)); // User bookmarks two jobs
        entityManager.flush();

        // when
        List<Bookmark> userBookmarks = bookmarkRepository.findByUser(testUser);

        // then
        assertThat(userBookmarks).hasSize(2);
        assertThat(userBookmarks).extracting(Bookmark::getJob).containsExactlyInAnyOrder(testJob1, testJob2);
    }

    @Test
    public void whenFindByUser_withUserHavingNoBookmarks_thenReturnEmptyList() {
        // User 'jobCreator' has no bookmarks in this setup
        // when
        List<Bookmark> userBookmarks = bookmarkRepository.findByUser(jobCreator);

        // then
        assertThat(userBookmarks).isEmpty();
    }

    @Test
    public void whenSaveDuplicateBookmark_thenThrowException() {
        // given
        Bookmark bookmark1 = new Bookmark(testUser, testJob1);
        bookmarkRepository.save(bookmark1); // Persist first bookmark

        // when & then
        Bookmark bookmark2 = new Bookmark(testUser, testJob1); // Same user, same job
        // The unique constraint on (user_id, job_id) in Bookmark entity should cause this to fail.
        assertThrows(DataIntegrityViolationException.class, () -> {
            bookmarkRepository.saveAndFlush(bookmark2);
        });
    }
}
