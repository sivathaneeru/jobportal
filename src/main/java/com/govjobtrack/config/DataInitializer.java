package com.govjobtrack.config;

import com.govjobtrack.model.Role;
import com.govjobtrack.model.RoleEntity;
import com.govjobtrack.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional; // Import for @Transactional

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    @Transactional // Add Transactional to ensure operations are within a transaction
    public void run(String... args) throws Exception {
        // Check and create ROLE_USER
        if (roleRepository.findByName(Role.ROLE_USER).isEmpty()) {
            roleRepository.save(new RoleEntity(Role.ROLE_USER));
            System.out.println("DataInitializer: ROLE_USER created.");
        } else {
            System.out.println("DataInitializer: ROLE_USER already exists.");
        }

        // Check and create ROLE_ADMIN
        if (roleRepository.findByName(Role.ROLE_ADMIN).isEmpty()) {
            roleRepository.save(new RoleEntity(Role.ROLE_ADMIN));
            System.out.println("DataInitializer: ROLE_ADMIN created.");
        } else {
            System.out.println("DataInitializer: ROLE_ADMIN already exists.");
        }
    }
}
