package com.suhas.examallocation.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Records a faculty member's unavailable date.
 * Used by InvigilatorDutyAssignerService to skip
 * faculty who are on leave for a given exam date.
 */
@Entity
@Table(name = "faculty_leave")
public class FacultyLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @NotNull
    @Column(name = "leave_date", nullable = false)
    private LocalDate leaveDate;

    public FacultyLeave() {
    }

    public FacultyLeave(Faculty faculty, LocalDate leaveDate) {
        this.faculty = faculty;
        this.leaveDate = leaveDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }

    public LocalDate getLeaveDate() {
        return leaveDate;
    }

    public void setLeaveDate(LocalDate leaveDate) {
        this.leaveDate = leaveDate;
    }
}
