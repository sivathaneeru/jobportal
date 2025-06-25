package com.govjobtrack.repository;

import com.govjobtrack.model.Role;
import com.govjobtrack.model.RoleEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException; // For unique constraint test

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@DataJpaTest
public class RoleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    public void whenSaveRole_thenCanBeRetrievedById() {
        // given
        RoleEntity roleUser = new RoleEntity(Role.ROLE_USER);
        RoleEntity savedRole = roleRepository.save(roleUser); // Use repository save directly

        // when
        Optional<RoleEntity> foundRoleOpt = roleRepository.findById(savedRole.getId());

        // then
        assertThat(foundRoleOpt).isPresent();
        foundRoleOpt.ifPresent(foundRole -> {
            assertThat(foundRole.getName()).isEqualTo(Role.ROLE_USER);
            assertThat(foundRole.getId()).isNotNull();
        });
    }

    @Test
    public void whenFindByName_thenReturnRoleEntity() {
        // given
        entityManager.persist(new RoleEntity(Role.ROLE_ADMIN));
        entityManager.flush();

        // when
        Optional<RoleEntity> foundRoleOpt = roleRepository.findByName(Role.ROLE_ADMIN);

        // then
        assertThat(foundRoleOpt).isPresent();
        assertThat(foundRoleOpt.get().getName()).isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    public void whenFindByName_withNonExistingRole_thenReturnEmpty() {
        // when
        Optional<RoleEntity> foundRoleOpt = roleRepository.findByName(Role.ROLE_USER); // Assuming DB is clean for this test or ROLE_USER not persisted yet

        // then
        assertThat(foundRoleOpt).isNotPresent();
    }

    @Test
    public void whenSaveDuplicateRoleName_thenThrowException() {
        // given
        RoleEntity role1 = new RoleEntity(Role.ROLE_USER);
        roleRepository.save(role1); // Persist first role

        // when & then
        RoleEntity role2 = new RoleEntity(Role.ROLE_USER);
        // The unique constraint on 'name' column in RoleEntity should cause this to fail
        // Note: TestEntityManager.persist might not trigger validation immediately,
        // but repository.save() followed by a flush or commit would.
        // @DataJpaTest usually wraps tests in transactions that are rolled back.
        // The actual exception might depend on the JPA provider and database (H2 in this case).
        assertThrows(DataIntegrityViolationException.class, () -> {
            roleRepository.saveAndFlush(role2); // saveAndFlush to ensure constraint violation is triggered
        });
    }
}
