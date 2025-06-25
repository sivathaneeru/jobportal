package com.govjobtrack.repository;

import com.govjobtrack.model.User;
import com.govjobtrack.model.Role;
import com.govjobtrack.model.RoleEntity; // Assuming RoleEntity is what we use
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles; // If you have test-specific profiles

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
// @ActiveProfiles("test") // Example if you have a specific test profile in application-test.properties
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired // We need RoleRepository to save RoleEntity instances first
    private RoleRepository roleRepository;

    @Test
    public void whenFindByEmail_thenReturnUser() {
        // given
        RoleEntity userRole = roleRepository.findByName(Role.ROLE_USER)
                .orElseGet(() -> roleRepository.save(new RoleEntity(Role.ROLE_USER)));

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(userRole);

        User testUser = new User("John", "Doe", "john.doe@example.com", "password123");
        testUser.setRoles(roles);
        entityManager.persist(testUser);
        entityManager.flush(); // Persist to DB

        // when
        Optional<User> foundUserOpt = userRepository.findByEmail(testUser.getEmail());

        // then
        assertThat(foundUserOpt).isPresent();
        foundUserOpt.ifPresent(foundUser -> {
            assertThat(foundUser.getEmail()).isEqualTo(testUser.getEmail());
            assertThat(foundUser.getFirstName()).isEqualTo("John");
            assertThat(foundUser.getRoles()).contains(userRole);
        });
    }

    @Test
    public void whenExistsByEmail_withExistingEmail_thenReturnTrue() {
        // given
        RoleEntity userRole = roleRepository.findByName(Role.ROLE_USER)
                .orElseGet(() -> roleRepository.save(new RoleEntity(Role.ROLE_USER)));
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(userRole);

        User testUser = new User("Jane", "Doe", "jane.doe@example.com", "password123");
        testUser.setRoles(roles);
        entityManager.persist(testUser);
        entityManager.flush();

        // when
        Boolean exists = userRepository.existsByEmail("jane.doe@example.com");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    public void whenExistsByEmail_withNonExistingEmail_thenReturnFalse() {
        // when
        Boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    public void testUserSaveAndRetrieval() {
        RoleEntity adminRole = roleRepository.findByName(Role.ROLE_ADMIN)
                                 .orElseGet(() -> roleRepository.save(new RoleEntity(Role.ROLE_ADMIN)));
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(adminRole);

        User user = new User("Admin", "User", "admin@test.com", "securepassword");
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("admin@test.com");
        assertThat(savedUser.getCreatedAt()).isNotNull(); // Auditing check

        Optional<User> retrievedUserOpt = userRepository.findById(savedUser.getId());
        assertThat(retrievedUserOpt).isPresent();
        retrievedUserOpt.ifPresent(retrievedUser -> {
            assertThat(retrievedUser.getRoles()).hasSize(1);
            assertThat(retrievedUser.getRoles().iterator().next().getName()).isEqualTo(Role.ROLE_ADMIN);
        });
    }
}
