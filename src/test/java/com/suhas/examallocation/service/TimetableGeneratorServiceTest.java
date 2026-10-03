package com.suhas.examallocation.service;

import com.suhas.examallocation.exception.NotEnoughSlotsException;
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

/**
 * Tests the timetable generation logic in isolation from the database.
 *
 * Mockito replaces the real repositories with fakes so we can control
 * exactly what data the service sees. This lets us test the algorithm
 * without needing MySQL running.
 *
 * @ExtendWith(MockitoExtension.class) tells JUnit to initialize
 * all @Mock and @InjectMocks fields before each test.
 */
@ExtendWith(MockitoExtension.class)
class TimetableGeneratorServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ExamSlotRepository examSlotRepository;

    @Mock
    private ExamRepository examRepository;

    @InjectMocks
    private TimetableGeneratorService timetableGeneratorService;

    private Faculty faculty;
    private Course courseA;
    private Course courseB;
    private Course courseC;

    @BeforeEach
    void setUp() {
        faculty = new Faculty("Dr. Test", "CSE");
        faculty.setId(1L);

        courseA = new Course("CS101", "Data Structures", 3, faculty);
        courseA.setId(1L);

        courseB = new Course("CS102", "Algorithms", 3, faculty);
        courseB.setId(2L);

        courseC = new Course("CS103", "Databases", 3, faculty);
        courseC.setId(3L);
    }

    @Test
    void buildClashMap_noSharedStudents_noEdges() {
        List<Course> courses = List.of(courseA, courseB);

        // student 1 enrolled only in courseA, student 2 only in courseB
        when(enrollmentRepository.findStudentIdsByCourseId(1L)).thenReturn(List.of(100L));
        when(enrollmentRepository.findStudentIdsByCourseId(2L)).thenReturn(List.of(200L));

        Map<Long, Set<Long>> clashMap = timetableGeneratorService.buildClashMap(courses);

        assertTrue(clashMap.get(1L).isEmpty(), "courseA should have no clashes");
        assertTrue(clashMap.get(2L).isEmpty(), "courseB should have no clashes");
    }

    @Test
    void buildClashMap_sharedStudent_createsEdge() {
        List<Course> courses = List.of(courseA, courseB);

        // student 100 is enrolled in BOTH courses
        when(enrollmentRepository.findStudentIdsByCourseId(1L)).thenReturn(List.of(100L));
        when(enrollmentRepository.findStudentIdsByCourseId(2L)).thenReturn(List.of(100L));

        Map<Long, Set<Long>> clashMap = timetableGeneratorService.buildClashMap(courses);

        assertTrue(clashMap.get(1L).contains(2L), "courseA should clash with courseB");
        assertTrue(clashMap.get(2L).contains(1L), "courseB should clash with courseA");
    }

    @Test
    void buildClashMap_threeCoursesWithTriangle_allConnected() {
        List<Course> courses = List.of(courseA, courseB, courseC);

        // student 100 enrolled in all three courses
        when(enrollmentRepository.findStudentIdsByCourseId(1L)).thenReturn(List.of(100L));
        when(enrollmentRepository.findStudentIdsByCourseId(2L)).thenReturn(List.of(100L));
        when(enrollmentRepository.findStudentIdsByCourseId(3L)).thenReturn(List.of(100L));

        Map<Long, Set<Long>> clashMap = timetableGeneratorService.buildClashMap(courses);

        assertEquals(2, clashMap.get(1L).size(), "courseA should clash with B and C");
        assertEquals(2, clashMap.get(2L).size(), "courseB should clash with A and C");
        assertEquals(2, clashMap.get(3L).size(), "courseC should clash with A and B");
    }

    @Test
    void generateTimetable_clashingCourses_getDifferentSlots() {
        List<Course> courses = List.of(courseA, courseB);
        when(courseRepository.findAll()).thenReturn(courses);

        // student 100 enrolled in both → they must get different slots
        when(enrollmentRepository.findStudentIdsByCourseId(1L)).thenReturn(List.of(100L));
        when(enrollmentRepository.findStudentIdsByCourseId(2L)).thenReturn(List.of(100L));

        // mock slot creation — return a new slot each time
        when(examSlotRepository.findByExamDateAndSession(any(), any()))
                .thenReturn(Optional.empty());
        when(examSlotRepository.save(any(ExamSlot.class))).thenAnswer(invocation -> {
            ExamSlot slot = invocation.getArgument(0);
            slot.setId((long) (slot.getExamDate().getDayOfMonth() * 10 + slot.getSession().ordinal()));
            return slot;
        });

        // capture saved exams to verify slot assignments
        List<Exam> savedExams = new ArrayList<>();
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> {
            Exam exam = invocation.getArgument(0);
            exam.setId((long) (savedExams.size() + 1));
            savedExams.add(exam);
            return exam;
        });

        List<Exam> result = timetableGeneratorService.generateTimetable(
                LocalDate.of(2025, 12, 1), 2);

        assertEquals(2, result.size());

        // the two clashing courses must NOT share the same slot
        ExamSlot slotOfA = findSlotForCourse(savedExams, courseA.getId());
        ExamSlot slotOfB = findSlotForCourse(savedExams, courseB.getId());
        assertNotEquals(slotOfA.getId(), slotOfB.getId(),
                "Clashing courses should be in different slots");
    }

    @Test
    void generateTimetable_nonClashingCourses_canShareSlot() {
        List<Course> courses = List.of(courseA, courseB);
        when(courseRepository.findAll()).thenReturn(courses);

        // different students → no clash → can share same slot
        when(enrollmentRepository.findStudentIdsByCourseId(1L)).thenReturn(List.of(100L));
        when(enrollmentRepository.findStudentIdsByCourseId(2L)).thenReturn(List.of(200L));

        when(examSlotRepository.findByExamDateAndSession(any(), any()))
                .thenReturn(Optional.empty());
        when(examSlotRepository.save(any(ExamSlot.class))).thenAnswer(invocation -> {
            ExamSlot slot = invocation.getArgument(0);
            slot.setId(1L);
            return slot;
        });

        List<Exam> savedExams = new ArrayList<>();
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> {
            Exam exam = invocation.getArgument(0);
            exam.setId((long) (savedExams.size() + 1));
            savedExams.add(exam);
            return exam;
        });

        List<Exam> result = timetableGeneratorService.generateTimetable(
                LocalDate.of(2025, 12, 1), 2);

        assertEquals(2, result.size());

        // non-clashing courses CAN share the same slot (slot index 0)
        ExamSlot slotOfA = findSlotForCourse(savedExams, courseA.getId());
        ExamSlot slotOfB = findSlotForCourse(savedExams, courseB.getId());
        assertEquals(slotOfA.getId(), slotOfB.getId(),
                "Non-clashing courses should share the same slot");
    }

    @Test
    void generateTimetable_noCourses_throwsException() {
        when(courseRepository.findAll()).thenReturn(Collections.emptyList());

        assertThrows(IllegalArgumentException.class, () ->
                timetableGeneratorService.generateTimetable(LocalDate.of(2025, 12, 1), 2));
    }

    private ExamSlot findSlotForCourse(List<Exam> exams, Long courseId) {
        return exams.stream()
                .filter(e -> e.getCourse().getId().equals(courseId))
                .findFirst()
                .orElseThrow()
                .getSlot();
    }
}
