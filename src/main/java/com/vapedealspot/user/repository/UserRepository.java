package com.vapedealspot.user.repository;

import com.vapedealspot.user.model.User;
import com.vapedealspot.user.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, String id);

    List<User> findByRole(UserRole role);

    long countByRoleAndStatus(UserRole role, String status);

    List<User> findAllByOrderByCreatedAtDesc();
}

