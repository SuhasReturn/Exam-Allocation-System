package com.suhas.examallocation.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * Assigns a faculty member to a hall for a specific exam slot,
 * with a role of CHIEF or ASSISTANT.
 *
 * UNIQUE(faculty_id, slot_id) means a faculty member can only
 * be in one hall per slot — they can't be in two places at once.
 */
@Entity
@Table(name = "invigilator_duty",
       uniqueConstraints = @UniqueConstraint(columnNames = {"faculty_id", "slot_id"}))
public class InvigilatorDuty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private ExamSlot slot;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "duty_role", nullable = false)
    private DutyRole dutyRole;

    public InvigilatorDuty() {
    }

    public InvigilatorDuty(Faculty faculty, Hall hall, ExamSlot slot, DutyRole dutyRole) {
        this.faculty = faculty;
        this.hall = hall;
        this.slot = slot;
        this.dutyRole = dutyRole;
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

    public Hall getHall() {
        return hall;
    }

    public void setHall(Hall hall) {
        this.hall = hall;
    }

    public ExamSlot getSlot() {
        return slot;
    }

    public void setSlot(ExamSlot slot) {
        this.slot = slot;
    }

    public DutyRole getDutyRole() {
        return dutyRole;
    }

    public void setDutyRole(DutyRole dutyRole) {
        this.dutyRole = dutyRole;
    }
}
