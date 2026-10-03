package com.suhas.examallocation.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * Maps a course to an exam slot — this IS the timetable.
 * "Course X has its exam in slot Y."
 *
 * One course has exactly one exam (one row here).
 * One slot can have many exams (many courses examined at the same time).
 */
@Entity
@Table(name = "exam")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false, unique = true)
    private Course course;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private ExamSlot slot;

    public Exam() {
    }

    public Exam(Course course, ExamSlot slot) {
        this.course = course;
        this.slot = slot;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public ExamSlot getSlot() {
        return slot;
    }

    public void setSlot(ExamSlot slot) {
        this.slot = slot;
    }
}
