package com.suhas.examallocation.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * An exam slot is a specific date + session (FN or AN).
 * This is the "time unit" of the timetable — every exam
 * is assigned to exactly one slot.
 *
 * The unique constraint on (exam_date, session) means we
 * can't accidentally create two identical slots.
 *
 * @Enumerated(EnumType.STRING) stores "FN" / "AN" as text
 * in the database rather than 0/1, which is much easier
 * to read when debugging with raw SQL.
 */
@Entity
@Table(name = "exam_slot",
       uniqueConstraints = @UniqueConstraint(columnNames = {"exam_date", "session"}))
public class ExamSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Session session;

    public ExamSlot() {
    }

    public ExamSlot(LocalDate examDate, Session session) {
        this.examDate = examDate;
        this.session = session;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }
}
