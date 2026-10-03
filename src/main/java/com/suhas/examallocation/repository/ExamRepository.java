package com.suhas.examallocation.repository;

import com.suhas.examallocation.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    Optional<Exam> findByCourseId(Long courseId);

    List<Exam> findBySlotId(Long slotId);

    boolean existsByCourseId(Long courseId);
}
