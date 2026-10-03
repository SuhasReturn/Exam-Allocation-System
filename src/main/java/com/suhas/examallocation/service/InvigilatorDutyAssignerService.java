package com.suhas.examallocation.service;

import com.suhas.examallocation.exception.NotEnoughInvigilatorsException;
import com.suhas.examallocation.exception.ResourceNotFoundException;
import com.suhas.examallocation.model.*;
import com.suhas.examallocation.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Assigns faculty members as invigilators to exam halls.
 *
 * Rules:
 * - Invigilators per hall = ceil(students in hall / 30)
 * - A faculty is eligible if they are:
 *   (a) not on leave on that exam date
 *   (b) not already on duty in that slot
 *   (c) not the teacher of any course being examined in that hall
 * - Among eligible faculty, pick those with the fewest total duties (load balancing)
 * - The first assigned to a hall is CHIEF, the rest are ASSISTANT
 */
@Service
public class InvigilatorDutyAssignerService {

    private final SeatAllocationRepository seatAllocationRepository;
    private final InvigilatorDutyRepository invigilatorDutyRepository;
    private final FacultyRepository facultyRepository;
    private final FacultyLeaveRepository facultyLeaveRepository;
    private final ExamSlotRepository examSlotRepository;
    private final ExamRepository examRepository;

    public InvigilatorDutyAssignerService(SeatAllocationRepository seatAllocationRepository,
                                          InvigilatorDutyRepository invigilatorDutyRepository,
                                          FacultyRepository facultyRepository,
                                          FacultyLeaveRepository facultyLeaveRepository,
                                          ExamSlotRepository examSlotRepository,
                                          ExamRepository examRepository) {
        this.seatAllocationRepository = seatAllocationRepository;
        this.invigilatorDutyRepository = invigilatorDutyRepository;
        this.facultyRepository = facultyRepository;
        this.facultyLeaveRepository = facultyLeaveRepository;
        this.examSlotRepository = examSlotRepository;
        this.examRepository = examRepository;
    }

    @Transactional
    public void assignDutiesForAllSlots() {
        List<ExamSlot> slots = examSlotRepository.findAll();
        for (ExamSlot slot : slots) {
            assignDutiesForSlot(slot);
        }
    }

    @Transactional
    public void assignDutiesForSlot(ExamSlot slot) {
        // clear previous duties for this slot
        invigilatorDutyRepository.deleteBySlotId(slot.getId());

        List<SeatAllocation> allocationsInSlot = seatAllocationRepository.findBySlotId(slot.getId());
        if (allocationsInSlot.isEmpty()) {
            return;
        }

        // group allocations by hall to know which halls are used and how many students each has
        Map<Long, List<SeatAllocation>> allocationsByHall = new HashMap<>();
        for (SeatAllocation alloc : allocationsInSlot) {
            allocationsByHall.computeIfAbsent(alloc.getHall().getId(), k -> new ArrayList<>())
                             .add(alloc);
        }

        Set<Long> facultyOnLeave = new HashSet<>(
                facultyLeaveRepository.findFacultyIdsOnLeave(slot.getExamDate()));

        // track who's been assigned in this slot so far (across halls)
        Set<Long> facultyBusyInSlot = new HashSet<>(
                invigilatorDutyRepository.findFacultyIdsOnDutyInSlot(slot.getId()));

        for (Map.Entry<Long, List<SeatAllocation>> entry : allocationsByHall.entrySet()) {
            Long hallId = entry.getKey();
            List<SeatAllocation> hallAllocations = entry.getValue();
            Hall hall = hallAllocations.get(0).getHall();

            int studentCount = hallAllocations.size();
            int invigilatorsNeeded = (int) Math.ceil(studentCount / 30.0);

            Set<Long> courseOwnerIds = findCourseOwnerIdsInHall(hallAllocations);

            List<Faculty> eligible = findEligibleFaculty(
                    facultyOnLeave, facultyBusyInSlot, courseOwnerIds);

            if (eligible.size() < invigilatorsNeeded) {
                throw new NotEnoughInvigilatorsException(
                        "Not enough invigilators for hall " + hall.getName()
                        + " in slot " + slot.getExamDate() + " " + slot.getSession()
                        + ". Need " + invigilatorsNeeded
                        + " but only " + eligible.size() + " eligible faculty available");
            }

            sortByDutyCountAscending(eligible);

            for (int i = 0; i < invigilatorsNeeded; i++) {
                Faculty chosen = eligible.get(i);
                DutyRole role = (i == 0) ? DutyRole.CHIEF : DutyRole.ASSISTANT;

                InvigilatorDuty duty = new InvigilatorDuty(chosen, hall, slot, role);
                invigilatorDutyRepository.save(duty);

                facultyBusyInSlot.add(chosen.getId());
            }
        }
    }

    /**
     * Replaces the faculty member on an existing duty with the best
     * eligible alternative. Used when admin needs to swap someone out.
     */
    @Transactional
    public InvigilatorDuty replaceFacultyOnDuty(Long dutyId) {
        InvigilatorDuty existing = invigilatorDutyRepository.findById(dutyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invigilator duty not found with id " + dutyId));

        ExamSlot slot = existing.getSlot();
        Hall hall = existing.getHall();
        Long currentFacultyId = existing.getFaculty().getId();

        Set<Long> facultyOnLeave = new HashSet<>(
                facultyLeaveRepository.findFacultyIdsOnLeave(slot.getExamDate()));

        Set<Long> facultyBusyInSlot = new HashSet<>(
                invigilatorDutyRepository.findFacultyIdsOnDutyInSlot(slot.getId()));

        // the current faculty is "busy" but we're replacing them, so they're
        // still excluded (they might be legitimately unavailable)
        List<SeatAllocation> hallAllocations = seatAllocationRepository
                .findBySlotIdAndHallId(slot.getId(), hall.getId());
        Set<Long> courseOwnerIds = findCourseOwnerIdsInHall(hallAllocations);

        List<Faculty> eligible = findEligibleFaculty(
                facultyOnLeave, facultyBusyInSlot, courseOwnerIds);

        // also exclude the faculty being replaced
        eligible.removeIf(f -> f.getId().equals(currentFacultyId));

        if (eligible.isEmpty()) {
            throw new NotEnoughInvigilatorsException(
                    "No eligible replacement found for duty " + dutyId
                    + " in hall " + hall.getName() + " slot " + slot.getExamDate()
                    + " " + slot.getSession());
        }

        sortByDutyCountAscending(eligible);

        existing.setFaculty(eligible.get(0));
        return invigilatorDutyRepository.save(existing);
    }

    /**
     * Finds the IDs of faculty who teach courses being examined in this hall.
     * These faculty members cannot invigilate their own exam.
     */
    private Set<Long> findCourseOwnerIdsInHall(List<SeatAllocation> hallAllocations) {
        Set<Long> courseOwnerIds = new HashSet<>();

        Set<Long> examIds = new HashSet<>();
        for (SeatAllocation alloc : hallAllocations) {
            examIds.add(alloc.getExam().getId());
        }

        for (Long examId : examIds) {
            examRepository.findById(examId).ifPresent(exam ->
                    courseOwnerIds.add(exam.getCourse().getFaculty().getId()));
        }

        return courseOwnerIds;
    }

    private List<Faculty> findEligibleFaculty(Set<Long> onLeave,
                                               Set<Long> busyInSlot,
                                               Set<Long> courseOwners) {
        List<Faculty> allFaculty = facultyRepository.findAll();
        List<Faculty> eligible = new ArrayList<>();

        for (Faculty faculty : allFaculty) {
            Long id = faculty.getId();
            if (onLeave.contains(id)) continue;
            if (busyInSlot.contains(id)) continue;
            if (courseOwners.contains(id)) continue;
            eligible.add(faculty);
        }

        return eligible;
    }

    private void sortByDutyCountAscending(List<Faculty> faculty) {
        faculty.sort((a, b) -> {
            long countA = invigilatorDutyRepository.countDutiesByFacultyId(a.getId());
            long countB = invigilatorDutyRepository.countDutiesByFacultyId(b.getId());
            return Long.compare(countA, countB);
        });
    }
}
