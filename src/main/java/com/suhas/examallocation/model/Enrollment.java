package com.suhas.examallocation.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * An enrollment links one student to one course.
 * The unique constraint prevents a student from enrolling
 * in the same course twice.
 *
 * JPA relationships: ManyToOne on both sides because
 * many enrollments point to one student, and many enrollments
 * point to one course. This is a classic join table modeled
 * as its own entity so we can store the enrollment id and
 * query it directly.
 */
@Entity
@Table(name = "enrollment",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    public Enrollment() {
    }

    public Enrollment(Student student, Course course) {
        this.student = student;
        this.course = course;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }
}
