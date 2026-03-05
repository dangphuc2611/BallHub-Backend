package com.ballhub.ballhub_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
