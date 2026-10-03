package com.suhas.examallocation.service;

import com.suhas.examallocation.exception.NotEnoughInvigilatorsException;
import com.suhas.examallocation.exception.ResourceNotFoundException;
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
class InvigilatorDutyAssignerServiceTest {

    @Mock
    private SeatAllocationRepository seatAllocationRepository;

    @Mock
    private InvigilatorDutyRepository invigilatorDutyRepository;

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private FacultyLeaveRepository facultyLeaveRepository;

    @Mock
    private ExamSlotRepository examSlotRepository;

    @Mock
    private ExamRepository examRepository;

    @InjectMocks
    private InvigilatorDutyAssignerService dutyAssignerService;

    private Faculty teacherFaculty;
    private Faculty invigilatorA;
    private Faculty invigilatorB;
    private ExamSlot slot;
    private Hall hall;
    private Course course;
    private Exam exam;

    @BeforeEach
    void setUp() {
        teacherFaculty = new Faculty("Dr. Teacher", "CSE");
        teacherFaculty.setId(1L);

        invigilatorA = new Faculty("Dr. Alpha", "ECE");
        invigilatorA.setId(2L);

        invigilatorB = new Faculty("Dr. Beta", "MECH");
        invigilatorB.setId(3L);

        slot = new ExamSlot(LocalDate.of(2025, 12, 1), Session.FN);
        slot.setId(1L);

        hall = new Hall("Main Hall", 5, 6);
        hall.setId(1L);

        course = new Course("CS101", "Data Structures", 3, teacherFaculty);
        course.setId(1L);

        exam = new Exam(course, slot);
        exam.setId(1L);
    }

    @Test
    void assignDuties_tenStudents_needsOneInvigilator() {
        List<SeatAllocation> allocations = createAllocations(10);

        when(seatAllocationRepository.findBySlotId(1L)).thenReturn(allocations);
        when(facultyLeaveRepository.findFacultyIdsOnLeave(slot.getExamDate()))
                .thenReturn(Collections.emptyList());
        when(invigilatorDutyRepository.findFacultyIdsOnDutyInSlot(1L))
                .thenReturn(Collections.emptyList());
        when(facultyRepository.findAll())
                .thenReturn(List.of(teacherFaculty, invigilatorA, invigilatorB));
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(invigilatorDutyRepository.countDutiesByFacultyId(any())).thenReturn(0L);
        when(invigilatorDutyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        dutyAssignerService.assignDutiesForSlot(slot);

        // ceil(10/30) = 1 invigilator needed
        verify(invigilatorDutyRepository, times(1)).save(any(InvigilatorDuty.class));
    }

    @Test
    void assignDuties_thirtyOneStudents_needsTwoInvigilators() {
        List<SeatAllocation> allocations = createAllocations(31);

        when(seatAllocationRepository.findBySlotId(1L)).thenReturn(allocations);
        when(facultyLeaveRepository.findFacultyIdsOnLeave(slot.getExamDate()))
                .thenReturn(Collections.emptyList());
        when(invigilatorDutyRepository.findFacultyIdsOnDutyInSlot(1L))
                .thenReturn(Collections.emptyList());
        when(facultyRepository.findAll())
                .thenReturn(List.of(teacherFaculty, invigilatorA, invigilatorB));
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(invigilatorDutyRepository.countDutiesByFacultyId(any())).thenReturn(0L);
        when(invigilatorDutyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        dutyAssignerService.assignDutiesForSlot(slot);

        // ceil(31/30) = 2 invigilators needed
        verify(invigilatorDutyRepository, times(2)).save(any(InvigilatorDuty.class));
    }

    @Test
    void assignDuties_courseTeacherExcluded() {
        List<SeatAllocation> allocations = createAllocations(5);

        when(seatAllocationRepository.findBySlotId(1L)).thenReturn(allocations);
        when(facultyLeaveRepository.findFacultyIdsOnLeave(slot.getExamDate()))
                .thenReturn(Collections.emptyList());
        when(invigilatorDutyRepository.findFacultyIdsOnDutyInSlot(1L))
                .thenReturn(Collections.emptyList());
        // only the teacher and one other faculty exist
        when(facultyRepository.findAll())
                .thenReturn(List.of(teacherFaculty, invigilatorA));
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(invigilatorDutyRepository.countDutiesByFacultyId(2L)).thenReturn(0L);
        when(invigilatorDutyRepository.save(any())).thenAnswer(inv -> {
            InvigilatorDuty duty = inv.getArgument(0);
            // the teacher (id=1) should NOT be assigned
            assertNotEquals(1L, duty.getFaculty().getId(),
                    "Course teacher should not invigilate their own exam");
            return duty;
        });

        dutyAssignerService.assignDutiesForSlot(slot);
    }

    @Test
    void assignDuties_facultyOnLeave_excluded() {
        List<SeatAllocation> allocations = createAllocations(5);

        when(seatAllocationRepository.findBySlotId(1L)).thenReturn(allocations);
        // invigilatorA is on leave
        when(facultyLeaveRepository.findFacultyIdsOnLeave(slot.getExamDate()))
                .thenReturn(List.of(2L));
        when(invigilatorDutyRepository.findFacultyIdsOnDutyInSlot(1L))
                .thenReturn(Collections.emptyList());
        when(facultyRepository.findAll())
                .thenReturn(List.of(teacherFaculty, invigilatorA, invigilatorB));
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(invigilatorDutyRepository.countDutiesByFacultyId(3L)).thenReturn(0L);
        when(invigilatorDutyRepository.save(any())).thenAnswer(inv -> {
            InvigilatorDuty duty = inv.getArgument(0);
            assertNotEquals(2L, duty.getFaculty().getId(),
                    "Faculty on leave should not be assigned");
            return duty;
        });

        dutyAssignerService.assignDutiesForSlot(slot);
    }

    @Test
    void assignDuties_notEnoughFaculty_throwsException() {
        List<SeatAllocation> allocations = createAllocations(5);

        when(seatAllocationRepository.findBySlotId(1L)).thenReturn(allocations);
        when(facultyLeaveRepository.findFacultyIdsOnLeave(slot.getExamDate()))
                .thenReturn(Collections.emptyList());
        when(invigilatorDutyRepository.findFacultyIdsOnDutyInSlot(1L))
                .thenReturn(Collections.emptyList());
        // only the teacher exists — no one eligible
        when(facultyRepository.findAll()).thenReturn(List.of(teacherFaculty));
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));

        assertThrows(NotEnoughInvigilatorsException.class, () ->
                dutyAssignerService.assignDutiesForSlot(slot));
    }

    @Test
    void replaceDuty_findsEligibleReplacement() {
        InvigilatorDuty existingDuty = new InvigilatorDuty(
                invigilatorA, hall, slot, DutyRole.CHIEF);
        existingDuty.setId(10L);

        when(invigilatorDutyRepository.findById(10L))
                .thenReturn(Optional.of(existingDuty));
        when(facultyLeaveRepository.findFacultyIdsOnLeave(slot.getExamDate()))
                .thenReturn(Collections.emptyList());
        when(invigilatorDutyRepository.findFacultyIdsOnDutyInSlot(1L))
                .thenReturn(List.of(2L)); // invigilatorA already on duty

        List<SeatAllocation> hallAllocations = createAllocations(5);
        when(seatAllocationRepository.findBySlotIdAndHallId(1L, 1L))
                .thenReturn(hallAllocations);
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));

        when(facultyRepository.findAll())
                .thenReturn(List.of(teacherFaculty, invigilatorA, invigilatorB));
        when(invigilatorDutyRepository.countDutiesByFacultyId(3L)).thenReturn(0L);
        when(invigilatorDutyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvigilatorDuty replaced = dutyAssignerService.replaceFacultyOnDuty(10L);

        assertEquals(invigilatorB.getId(), replaced.getFaculty().getId(),
                "Should pick invigilatorB as replacement");
    }

    @Test
    void replaceDuty_notFound_throwsException() {
        when(invigilatorDutyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                dutyAssignerService.replaceFacultyOnDuty(999L));
    }

    /**
     * Creates N seat allocations in one hall for one exam.
     * All allocations point to the same hall, slot, and exam.
     */
    private List<SeatAllocation> createAllocations(int count) {
        List<SeatAllocation> allocations = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Student student = new Student("REG" + i, "Student" + i, "CSE", 3);
            student.setId((long) i);
            SeatAllocation alloc = new SeatAllocation(student, exam, slot, hall, i);
            alloc.setId((long) i);
            allocations.add(alloc);
        }
        return allocations;
    }
}
