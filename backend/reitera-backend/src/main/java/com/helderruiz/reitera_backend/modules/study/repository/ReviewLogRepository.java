package com.helderruiz.reitera_backend.modules.study.repository;

import com.helderruiz.reitera_backend.modules.study.model.ReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, Integer> {
}