package com.suhas.examallocation.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Assigns a specific seat to a student for a specific exam in a specific hall.
 *
 * Two hard constraints enforced at the DB level:
 * 1. UNIQUE(student_id, slot_id) — a student sits in only one hall per slot
 * 2. UNIQUE(hall_id, slot_id, seat_no) — no two students share a seat in the same hall and slot
 *
 * We store slot_id alongside exam_id even though exam already has a slot,
 * because the unique constraints need slot_id directly in this table.
 * This is intentional denormalization for constraint enforcement.
 */
@Entity
@Table(name = "seat_allocation",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"student_id", "slot_id"}),
           @UniqueConstraint(columnNames = {"hall_id", "slot_id", "seat_no"})
       })
public class SeatAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private ExamSlot slot;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    @Min(1)
    @Column(name = "seat_no", nullable = false)
    private int seatNo;

    public SeatAllocation() {
    }

    public SeatAllocation(Student student, Exam exam, ExamSlot slot, Hall hall, int seatNo) {
        this.student = student;
        this.exam = exam;
        this.slot = slot;
        this.hall = hall;
        this.seatNo = seatNo;
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

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public ExamSlot getSlot() {
        return slot;
    }

    public void setSlot(ExamSlot slot) {
        this.slot = slot;
    }

    public Hall getHall() {
        return hall;
    }

    public void setHall(Hall hall) {
        this.hall = hall;
    }

    public int getSeatNo() {
        return seatNo;
    }

    public void setSeatNo(int seatNo) {
        this.seatNo = seatNo;
    }
}
