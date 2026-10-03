package com.suhas.examallocation.repository;

import com.suhas.examallocation.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByCourseId(Long courseId);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    /**
     * Finds all course IDs that a given student is enrolled in.
     * Used by the timetable generator to build the clash graph —
     * two courses clash if any student is enrolled in both.
     */
    @Query("SELECT e.course.id FROM Enrollment e WHERE e.student.id = :studentId")
    List<Long> findCourseIdsByStudentId(@Param("studentId") Long studentId);

    /**
     * Finds all student IDs enrolled in a given course.
     * Used by the seating plan generator to know who needs a seat.
     */
    @Query("SELECT e.student.id FROM Enrollment e WHERE e.course.id = :courseId")
    List<Long> findStudentIdsByCourseId(@Param("courseId") Long courseId);
}
