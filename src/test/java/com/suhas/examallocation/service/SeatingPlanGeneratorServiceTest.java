package com.suhas.examallocation.service;

import com.suhas.examallocation.exception.NotEnoughHallsException;
import com.suhas.examallocation.model.*;
import com.suhas.examallocation.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatingPlanGeneratorServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private HallRepository hallRepository;

    @Mock
    private SeatAllocationRepository seatAllocationRepository;

    @Mock
    private ExamSlotRepository examSlotRepository;

    @InjectMocks
    private SeatingPlanGeneratorService seatingPlanGeneratorService;

    private Faculty faculty;
    private ExamSlot slot;
    private Hall largeHall;
    private Hall smallHall;
    private Course courseA;
    private Course courseB;

    @BeforeEach
    void setUp() {
        faculty = new Faculty("Dr. Test", "CSE");
        faculty.setId(1L);

        slot = new ExamSlot(LocalDate.of(2025, 12, 1), Session.FN);
        slot.setId(1L);

        largeHall = new Hall("Main Hall", 5, 6);
        largeHall.setId(1L);

        smallHall = new Hall("Room 101", 3, 4);
        smallHall.setId(2L);

        courseA = new Course("CS101", "Data Structures", 3, faculty);
        courseA.setId(1L);

        courseB = new Course("CS102", "Algorithms", 3, faculty);
        courseB.setId(2L);
    }

    @Test
    void generateSeating_twoCoursesOneHall_interleavesCorrectly() {
        Exam examA = new Exam(courseA, slot);
        examA.setId(1L);
        Exam examB = new Exam(courseB, slot);
        examB.setId(2L);

        Student s1 = new Student("REG001", "Alice", "CSE", 3);
        s1.setId(101L);
        Student s2 = new Student("REG002", "Bob", "CSE", 3);
        s2.setId(102L);
        Student s3 = new Student("REG003", "Carol", "CSE", 3);
        s3.setId(103L);
        Student s4 = new Student("REG004", "Dave", "CSE", 3);
        s4.setId(104L);

        // courseA has s1, s2; courseB has s3, s4
        when(enrollmentRepository.findStudentIdsByCourseId(1L)).thenReturn(List.of(101L, 102L));
        when(enrollmentRepository.findStudentIdsByCourseId(2L)).thenReturn(List.of(103L, 104L));
        when(studentRepository.findById(101L)).thenReturn(Optional.of(s1));
        when(studentRepository.findById(102L)).thenReturn(Optional.of(s2));
        when(studentRepository.findById(103L)).thenReturn(Optional.of(s3));
        when(studentRepository.findById(104L)).thenReturn(Optional.of(s4));

        when(hallRepository.findAll()).thenReturn(List.of(largeHall));
        when(seatAllocationRepository.save(any(SeatAllocation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<String> degraded = seatingPlanGeneratorService
                .generateSeatingForSlot(slot, List.of(examA, examB));

        // 4 students, 2 courses, large hall (30 seats) — should NOT be degraded
        assertTrue(degraded.isEmpty(), "Should not be degraded with balanced courses");

        // verify 4 seat allocations saved
        verify(seatAllocationRepository, times(4)).save(any(SeatAllocation.class));
    }

    @Test
    void generateSeating_capacityExceeded_throwsException() {
        Exam examA = new Exam(courseA, slot);
        examA.setId(1L);

        // 50 students but only tiny hall
        List<Long> manyStudentIds = new ArrayList<>();
        for (long i = 1; i <= 50; i++) {
            manyStudentIds.add(i);
            Student s = new Student("REG" + i, "Student" + i, "CSE", 3);
            s.setId(i);
            lenient().when(studentRepository.findById(i)).thenReturn(Optional.of(s));
        }
        when(enrollmentRepository.findStudentIdsByCourseId(1L)).thenReturn(manyStudentIds);

        // only a 12-seat hall
        when(hallRepository.findAll()).thenReturn(List.of(smallHall));

        assertThrows(NotEnoughHallsException.class, () ->
                seatingPlanGeneratorService.generateSeatingForSlot(slot, List.of(examA)));
    }

    @Test
    void generateSeating_oneCourseOnly_markedDegraded() {
        Exam examA = new Exam(courseA, slot);
        examA.setId(1L);

        Student s1 = new Student("REG001", "Alice", "CSE", 3);
        s1.setId(101L);
        Student s2 = new Student("REG002", "Bob", "CSE", 3);
        s2.setId(102L);

        // only one course, so adjacent seats must be same course
        when(enrollmentRepository.findStudentIdsByCourseId(1L)).thenReturn(List.of(101L, 102L));
        when(studentRepository.findById(101L)).thenReturn(Optional.of(s1));
        when(studentRepository.findById(102L)).thenReturn(Optional.of(s2));

        when(hallRepository.findAll()).thenReturn(List.of(largeHall));
        when(seatAllocationRepository.save(any(SeatAllocation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<String> degraded = seatingPlanGeneratorService
                .generateSeatingForSlot(slot, List.of(examA));

        assertEquals(1, degraded.size(), "Single course in a hall should be flagged as degraded");
        assertEquals("Main Hall", degraded.get(0));
    }

    @Test
    void generateSeating_noStudents_nothingCreated() {
        Exam examA = new Exam(courseA, slot);
        examA.setId(1L);

        when(enrollmentRepository.findStudentIdsByCourseId(1L)).thenReturn(Collections.emptyList());

        List<String> degraded = seatingPlanGeneratorService
                .generateSeatingForSlot(slot, List.of(examA));

        assertTrue(degraded.isEmpty());
        verify(seatAllocationRepository, never()).save(any());
    }
}
