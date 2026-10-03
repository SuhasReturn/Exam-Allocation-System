package com.suhas.examallocation.service;

import com.suhas.examallocation.dto.ClashReport;
import com.suhas.examallocation.model.*;
import com.suhas.examallocation.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClashValidatorServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SeatAllocationRepository seatAllocationRepository;

    @Mock
    private InvigilatorDutyRepository invigilatorDutyRepository;

    @InjectMocks
    private ClashValidatorService clashValidatorService;

    private Faculty faculty;
    private ExamSlot slot;
    private Course courseA;
    private Course courseB;

    @BeforeEach
    void setUp() {
        faculty = new Faculty("Dr. Test", "CSE");
        faculty.setId(1L);

        slot = new ExamSlot(LocalDate.of(2025, 12, 1), Session.FN);
        slot.setId(1L);

        courseA = new Course("CS101", "Data Structures", 3, faculty);
        courseA.setId(1L);

        courseB = new Course("CS102", "Algorithms", 3, faculty);
        courseB.setId(2L);
    }

    @Test
    void validateTimetable_noClash_reportClean() {
        Exam examA = new Exam(courseA, slot);
        examA.setId(1L);
        Exam examB = new Exam(courseB, slot);
        examB.setId(2L);

        when(examRepository.findAll()).thenReturn(List.of(examA, examB));
        // different students in each course → no clash
        when(enrollmentRepository.findStudentIdsByCourseId(1L)).thenReturn(List.of(100L));
        when(enrollmentRepository.findStudentIdsByCourseId(2L)).thenReturn(List.of(200L));

        ClashReport report = clashValidatorService.validateTimetable();

        assertFalse(report.isHasClashes());
        assertTrue(report.getViolations().isEmpty());
    }

    @Test
    void validateTimetable_studentInTwoExamsSameSlot_reportsClash() {
        Exam examA = new Exam(courseA, slot);
        examA.setId(1L);
        Exam examB = new Exam(courseB, slot);
        examB.setId(2L);

        when(examRepository.findAll()).thenReturn(List.of(examA, examB));
        // student 100 is in BOTH courses in the same slot
        when(enrollmentRepository.findStudentIdsByCourseId(1L)).thenReturn(List.of(100L));
        when(enrollmentRepository.findStudentIdsByCourseId(2L)).thenReturn(List.of(100L));

        Student student = new Student("1PU21IS045", "Suhas", "ISE", 6);
        student.setId(100L);
        when(studentRepository.findById(100L)).thenReturn(Optional.of(student));

        ClashReport report = clashValidatorService.validateTimetable();

        assertTrue(report.isHasClashes());
        assertEquals(1, report.getViolations().size());
        assertTrue(report.getViolations().get(0).contains("1PU21IS045"),
                "Violation should mention the student's regNo");
        assertTrue(report.getViolations().get(0).contains("CS101"),
                "Violation should mention course A");
        assertTrue(report.getViolations().get(0).contains("CS102"),
                "Violation should mention course B");
    }

    @Test
    void validateTimetable_coursesInDifferentSlots_noClash() {
        ExamSlot slot2 = new ExamSlot(LocalDate.of(2025, 12, 1), Session.AN);
        slot2.setId(2L);

        Exam examA = new Exam(courseA, slot);
        examA.setId(1L);
        Exam examB = new Exam(courseB, slot2);
        examB.setId(2L);

        when(examRepository.findAll()).thenReturn(List.of(examA, examB));
        // same student but different slots → no clash
        when(enrollmentRepository.findStudentIdsByCourseId(1L)).thenReturn(List.of(100L));
        when(enrollmentRepository.findStudentIdsByCourseId(2L)).thenReturn(List.of(100L));

        ClashReport report = clashValidatorService.validateTimetable();

        assertFalse(report.isHasClashes());
    }

    @Test
    void validateDuties_facultyInTwoHallsSameSlot_reportsClash() {
        Hall hallA = new Hall("Hall A", 5, 6);
        hallA.setId(1L);
        Hall hallB = new Hall("Hall B", 5, 6);
        hallB.setId(2L);

        InvigilatorDuty dutyA = new InvigilatorDuty(faculty, hallA, slot, DutyRole.CHIEF);
        dutyA.setId(1L);
        InvigilatorDuty dutyB = new InvigilatorDuty(faculty, hallB, slot, DutyRole.ASSISTANT);
        dutyB.setId(2L);

        when(invigilatorDutyRepository.findAll()).thenReturn(List.of(dutyA, dutyB));

        ClashReport report = clashValidatorService.validateDuties();

        assertTrue(report.isHasClashes());
        assertEquals(1, report.getViolations().size());
        assertTrue(report.getViolations().get(0).contains("Dr. Test"));
    }

    @Test
    void validateSeating_noAllocations_reportClean() {
        when(seatAllocationRepository.findAll()).thenReturn(List.of());

        ClashReport report = clashValidatorService.validateSeating();

        assertFalse(report.isHasClashes());
    }
}
