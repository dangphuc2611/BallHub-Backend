package com.ballhub.ballhub_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ballhub.ballhub_backend.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndStatusTrue(String email);

    /**
     * Đếm tổng số khách hàng (user) trong hệ thống
     */
    @Query("SELECT COUNT(u) FROM User u")
    Long countTotalCustomers();

    @Query("SELECT u FROM User u WHERE u.status = true " +
            "AND (LOWER(u.fullName) LIKE LOWER(:keyword) " +
            "OR u.phone LIKE :keyword " +
            "OR LOWER(u.email) LIKE LOWER(:keyword))")
    List<User> searchByKeyword(@Param("keyword") String keyword);
}
