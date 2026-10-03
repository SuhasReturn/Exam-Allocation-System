package com.suhas.examallocation.service;

import com.suhas.examallocation.dto.ClashReport;
import com.suhas.examallocation.model.*;
import com.suhas.examallocation.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Re-validates the timetable after generation or manual edits.
 * Checks every rule and returns a ClashReport listing every
 * violation found — it does NOT stop at the first one.
 */
@Service
public class ClashValidatorService {

    private final ExamRepository examRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SeatAllocationRepository seatAllocationRepository;
    private final InvigilatorDutyRepository invigilatorDutyRepository;

    public ClashValidatorService(ExamRepository examRepository,
                                  EnrollmentRepository enrollmentRepository,
                                  StudentRepository studentRepository,
                                  SeatAllocationRepository seatAllocationRepository,
                                  InvigilatorDutyRepository invigilatorDutyRepository) {
        this.examRepository = examRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.seatAllocationRepository = seatAllocationRepository;
        this.invigilatorDutyRepository = invigilatorDutyRepository;
    }

    /**
     * Validates the timetable: no student should have two exams
     * in the same slot. Returns all violations found.
     */
    public ClashReport validateTimetable() {
        ClashReport report = new ClashReport();
        List<Exam> allExams = examRepository.findAll();

        // group exams by slot
        Map<Long, List<Exam>> examsBySlot = new HashMap<>();
        for (Exam exam : allExams) {
            examsBySlot.computeIfAbsent(exam.getSlot().getId(), k -> new ArrayList<>())
                       .add(exam);
        }

        for (Map.Entry<Long, List<Exam>> entry : examsBySlot.entrySet()) {
            List<Exam> examsInSlot = entry.getValue();
            if (examsInSlot.size() < 2) {
                continue;
            }
            checkStudentClashesInSlot(examsInSlot, report);
        }

        return report;
    }

    /**
     * Validates seating: checks that no student is double-seated
     * in the same slot and no seat is assigned twice.
     */
    public ClashReport validateSeating() {
        ClashReport report = new ClashReport();
        List<SeatAllocation> allAllocations = seatAllocationRepository.findAll();

        // check unique(student_id, slot_id)
        Set<String> studentSlotPairs = new HashSet<>();
        for (SeatAllocation alloc : allAllocations) {
            String key = alloc.getStudent().getId() + "-" + alloc.getSlot().getId();
            if (!studentSlotPairs.add(key)) {
                Student student = alloc.getStudent();
                report.addViolation(
                        "Student " + student.getRegNo()
                        + " is assigned to multiple seats in slot " + alloc.getSlot().getId());
            }
        }

        // check unique(hall_id, slot_id, seat_no)
        Set<String> hallSlotSeatKeys = new HashSet<>();
        for (SeatAllocation alloc : allAllocations) {
            String key = alloc.getHall().getId() + "-" + alloc.getSlot().getId() + "-" + alloc.getSeatNo();
            if (!hallSlotSeatKeys.add(key)) {
                report.addViolation(
                        "Seat " + alloc.getSeatNo() + " in hall " + alloc.getHall().getName()
                        + " is assigned to multiple students in slot " + alloc.getSlot().getId());
            }
        }

        return report;
    }

    /**
     * Validates invigilation duties: no faculty should be assigned
     * to two halls in the same slot.
     */
    public ClashReport validateDuties() {
        ClashReport report = new ClashReport();
        List<InvigilatorDuty> allDuties = invigilatorDutyRepository.findAll();

        Set<String> facultySlotPairs = new HashSet<>();
        for (InvigilatorDuty duty : allDuties) {
            String key = duty.getFaculty().getId() + "-" + duty.getSlot().getId();
            if (!facultySlotPairs.add(key)) {
                report.addViolation(
                        "Faculty " + duty.getFaculty().getName()
                        + " is assigned to multiple halls in slot " + duty.getSlot().getId());
            }
        }

        return report;
    }

    /**
     * Runs all validations and merges the results.
     */
    public ClashReport validateAll() {
        ClashReport combined = new ClashReport();

        ClashReport timetableReport = validateTimetable();
        ClashReport seatingReport = validateSeating();
        ClashReport dutyReport = validateDuties();

        for (String violation : timetableReport.getViolations()) {
            combined.addViolation(violation);
        }
        for (String violation : seatingReport.getViolations()) {
            combined.addViolation(violation);
        }
        for (String violation : dutyReport.getViolations()) {
            combined.addViolation(violation);
        }

        return combined;
    }

    private void checkStudentClashesInSlot(List<Exam> examsInSlot, ClashReport report) {
        // for each pair of exams in the same slot, find shared students
        for (int i = 0; i < examsInSlot.size(); i++) {
            Set<Long> studentsInCourseA = new HashSet<>(
                    enrollmentRepository.findStudentIdsByCourseId(
                            examsInSlot.get(i).getCourse().getId()));

            for (int j = i + 1; j < examsInSlot.size(); j++) {
                List<Long> studentsInCourseB = enrollmentRepository.findStudentIdsByCourseId(
                        examsInSlot.get(j).getCourse().getId());

                for (Long studentId : studentsInCourseB) {
                    if (studentsInCourseA.contains(studentId)) {
                        Student student = studentRepository.findById(studentId).orElse(null);
                        String regNo = student != null ? student.getRegNo() : "id=" + studentId;

                        ExamSlot slot = examsInSlot.get(i).getSlot();
                        report.addViolation(
                                "Student " + regNo + " has two exams in slot "
                                + slot.getExamDate() + " " + slot.getSession()
                                + ": " + examsInSlot.get(i).getCourse().getCode()
                                + " and " + examsInSlot.get(j).getCourse().getCode());
                    }
                }
            }
        }
    }
}
