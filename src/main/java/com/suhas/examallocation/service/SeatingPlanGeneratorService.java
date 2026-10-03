package com.suhas.examallocation.service;

import com.suhas.examallocation.exception.NotEnoughHallsException;
import com.suhas.examallocation.model.*;
import com.suhas.examallocation.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Assigns students to specific seats in specific halls for each exam slot.
 *
 * The algorithm per slot:
 * 1. Collect all students who have an exam in this slot, grouped by course.
 * 2. Sort halls by capacity (largest first) so we fill big rooms first.
 * 3. Check total capacity >= total students.
 * 4. Fill each hall using round-robin across course queues:
 *    seat 1 → course A student, seat 2 → course B student, seat 3 → course C student,
 *    seat 4 → course A student, ... This ensures adjacent seats hold different courses.
 * 5. If only one course queue has students left, adjacent seats will be same-course.
 *    We flag that hall as "degraded" instead of failing.
 */
@Service
public class SeatingPlanGeneratorService {

    private final ExamRepository examRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final HallRepository hallRepository;
    private final SeatAllocationRepository seatAllocationRepository;
    private final ExamSlotRepository examSlotRepository;

    public SeatingPlanGeneratorService(ExamRepository examRepository,
                                       EnrollmentRepository enrollmentRepository,
                                       StudentRepository studentRepository,
                                       HallRepository hallRepository,
                                       SeatAllocationRepository seatAllocationRepository,
                                       ExamSlotRepository examSlotRepository) {
        this.examRepository = examRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.hallRepository = hallRepository;
        this.seatAllocationRepository = seatAllocationRepository;
        this.examSlotRepository = examSlotRepository;
    }

    /**
     * Generates seating for all slots that have exams.
     * Returns the list of hall names that were degraded (imperfect interleaving).
     */
    @Transactional
    public List<String> generateSeatingForAllSlots() {
        List<ExamSlot> slots = examSlotRepository.findAll();
        List<String> allDegradedHalls = new ArrayList<>();

        for (ExamSlot slot : slots) {
            List<Exam> examsInSlot = examRepository.findBySlotId(slot.getId());
            if (examsInSlot.isEmpty()) {
                continue;
            }
            List<String> degraded = generateSeatingForSlot(slot, examsInSlot);
            allDegradedHalls.addAll(degraded);
        }

        return allDegradedHalls;
    }

    @Transactional
    public List<String> generateSeatingForSlot(ExamSlot slot, List<Exam> examsInSlot) {
        // clear previous seating for this slot
        seatAllocationRepository.deleteBySlotId(slot.getId());

        Map<Exam, LinkedList<Student>> studentQueues = buildStudentQueues(examsInSlot);
        int totalStudents = countTotalStudents(studentQueues);

        if (totalStudents == 0) {
            return Collections.emptyList();
        }

        List<Hall> halls = getSortedHallsByCapacity();
        int totalCapacity = halls.stream().mapToInt(Hall::getCapacity).sum();

        if (totalStudents > totalCapacity) {
            int shortage = totalStudents - totalCapacity;
            throw new NotEnoughHallsException(
                    "Not enough hall capacity for slot " + slot.getExamDate() + " "
                    + slot.getSession() + ". Need " + shortage
                    + " more seat(s). Total students: " + totalStudents
                    + ", total capacity: " + totalCapacity);
        }

        List<String> degradedHalls = new ArrayList<>();
        int studentsRemaining = totalStudents;

        for (Hall hall : halls) {
            if (studentsRemaining <= 0) {
                break;
            }

            int seatsToFill = Math.min(hall.getCapacity(), studentsRemaining);
            boolean isDegraded = fillHallWithRoundRobin(
                    hall, slot, studentQueues, seatsToFill);

            if (isDegraded) {
                degradedHalls.add(hall.getName());
            }

            studentsRemaining -= seatsToFill;
        }

        return degradedHalls;
    }

    /**
     * Fills a hall's seats using round-robin across course queues.
     * Returns true if the hall is "degraded" — meaning at some point
     * only one course had students left, breaking perfect interleaving.
     */
    private boolean fillHallWithRoundRobin(Hall hall, ExamSlot slot,
                                            Map<Exam, LinkedList<Student>> studentQueues,
                                            int seatsToFill) {
        // build a list of non-empty queues to rotate through
        List<Map.Entry<Exam, LinkedList<Student>>> activeQueues = new ArrayList<>();
        for (Map.Entry<Exam, LinkedList<Student>> entry : studentQueues.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                activeQueues.add(entry);
            }
        }

        boolean degraded = false;
        int seatNo = 1;
        int filled = 0;
        int queueIndex = 0;

        while (filled < seatsToFill) {
            // remove exhausted queues
            activeQueues.removeIf(entry -> entry.getValue().isEmpty());

            if (activeQueues.isEmpty()) {
                break;
            }

            if (activeQueues.size() == 1 && filled < seatsToFill - 1) {
                // only one course left with students — adjacent seats will match
                degraded = true;
            }

            if (queueIndex >= activeQueues.size()) {
                queueIndex = 0;
            }

            Map.Entry<Exam, LinkedList<Student>> current = activeQueues.get(queueIndex);
            Student student = current.getValue().poll();

            SeatAllocation allocation = new SeatAllocation(
                    student, current.getKey(), slot, hall, seatNo);
            seatAllocationRepository.save(allocation);

            seatNo++;
            filled++;
            queueIndex++;
        }

        return degraded;
    }

    private Map<Exam, LinkedList<Student>> buildStudentQueues(List<Exam> exams) {
        // LinkedHashMap preserves insertion order for consistent rotation
        Map<Exam, LinkedList<Student>> queues = new LinkedHashMap<>();

        for (Exam exam : exams) {
            List<Long> studentIds = enrollmentRepository.findStudentIdsByCourseId(
                    exam.getCourse().getId());

            LinkedList<Student> students = new LinkedList<>();
            for (Long studentId : studentIds) {
                studentRepository.findById(studentId).ifPresent(students::add);
            }

            queues.put(exam, students);
        }

        return queues;
    }

    private List<Hall> getSortedHallsByCapacity() {
        List<Hall> halls = hallRepository.findAll();
        halls.sort((a, b) -> Integer.compare(b.getCapacity(), a.getCapacity()));
        return halls;
    }

    private int countTotalStudents(Map<Exam, LinkedList<Student>> queues) {
        int total = 0;
        for (LinkedList<Student> queue : queues.values()) {
            total += queue.size();
        }
        return total;
    }
}
