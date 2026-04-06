package com.ballhub.ballhub_backend.repository;

import com.ballhub.ballhub_backend.entity.Style;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StyleRepository extends JpaRepository<Style, Integer> {
}
