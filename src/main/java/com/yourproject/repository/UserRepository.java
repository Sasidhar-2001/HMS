package com.yourproject.repository;

import com.yourproject.entity.User;
import com.yourproject.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // For complex queries
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByStudentId(String studentId);

    Optional<User> findByEmployeeId(String employeeId);

    boolean existsByEmail(String email);

    boolean existsByStudentId(String studentId);

    boolean existsByEmployeeId(String employeeId);

    List<User> findByRole(Role role);

    Optional<User> findByPasswordResetToken(String token);

    // Example for searching users (can be expanded or use Specifications)
    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String lastName, String email);
}
