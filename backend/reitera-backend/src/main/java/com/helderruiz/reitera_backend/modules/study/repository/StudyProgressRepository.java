package com.helderruiz.reitera_backend.modules.study.repository;

import com.helderruiz.reitera_backend.modules.study.model.StudyProgress;
import com.helderruiz.reitera_backend.modules.study.model.StudyProgressId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyProgressRepository extends JpaRepository<StudyProgress, StudyProgressId> {
}