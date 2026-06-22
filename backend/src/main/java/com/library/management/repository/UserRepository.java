package com.library.management.repository;

import com.library.management.dto.response.UserSummaryResponse;
import com.library.management.entity.User;
import com.library.management.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByStudentId(String studentId);

    boolean existsByEmail(String email);

    boolean existsByStudentId(String studentId);

    List<User> findAllByRole(UserRole role);

    List<User> findByIsActiveTrue();

    long countByRole(UserRole role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    List<User> findActiveUsersByRole(@Param("role") UserRole role);

    @Query("SELECT new com.library.management.dto.response.UserSummaryResponse(" +
            "u.userId, u.email, CONCAT(u.firstName, ' ', u.lastName), u.role, u.isActive, " +
            "COUNT(l.loanId), COALESCE(SUM(f.amount), 0)) " +
            "FROM User u " +
            "LEFT JOIN u.loans l ON l.status = 'ACTIVE' " +
            "LEFT JOIN u.fines f ON f.status = 'PENDING' " +
            "GROUP BY u.userId")
    List<UserSummaryResponse> findAllUsersWithSummary();

    @Query("SELECT CASE WHEN COALESCE(SUM(f.amount), 0) > :threshold THEN true ELSE false END " +
            "FROM Fine f WHERE f.user.userId = :userId AND f.status = 'PENDING'")
    boolean hasOutstandingFinesAbove(@Param("userId") Long userId, @Param("threshold") BigDecimal threshold);
}

