package com.suhas.examallocation.repository;

import com.suhas.examallocation.model.ExamSlot;
import com.suhas.examallocation.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ExamSlotRepository extends JpaRepository<ExamSlot, Long> {

    Optional<ExamSlot> findByExamDateAndSession(LocalDate examDate, Session session);

    boolean existsByExamDateAndSession(LocalDate examDate, Session session);
}
