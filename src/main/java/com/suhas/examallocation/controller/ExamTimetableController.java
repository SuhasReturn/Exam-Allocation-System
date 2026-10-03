package com.suhas.examallocation.controller;

import com.suhas.examallocation.dto.ClashReport;
import com.suhas.examallocation.dto.TimetableRow;
import com.suhas.examallocation.model.Exam;
import com.suhas.examallocation.repository.EnrollmentRepository;
import com.suhas.examallocation.repository.ExamRepository;
import com.suhas.examallocation.service.ClashValidatorService;
import com.suhas.examallocation.service.TimetableGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/timetable")
public class ExamTimetableController {

    private final TimetableGeneratorService timetableGeneratorService;
    private final ClashValidatorService clashValidatorService;
    private final ExamRepository examRepository;
    private final EnrollmentRepository enrollmentRepository;

    public ExamTimetableController(TimetableGeneratorService timetableGeneratorService,
                                    ClashValidatorService clashValidatorService,
                                    ExamRepository examRepository,
                                    EnrollmentRepository enrollmentRepository) {
        this.timetableGeneratorService = timetableGeneratorService;
        this.clashValidatorService = clashValidatorService;
        this.examRepository = examRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * Generates the timetable from scratch.
     *
     * Query params:
     *   startDate — first exam date (ISO format, e.g. 2025-12-01)
     *   slotsPerDay — 1 (FN only) or 2 (FN + AN), defaults to 2
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate(
            @RequestParam("startDate") String startDateStr,
            @RequestParam(value = "slotsPerDay", defaultValue = "2") int slotsPerDay) {

        LocalDate startDate = LocalDate.parse(startDateStr);
        List<Exam> exams = timetableGeneratorService.generateTimetable(startDate, slotsPerDay);

        ClashReport report = clashValidatorService.validateTimetable();

        Map<String, Object> response = new HashMap<>();
        response.put("examCount", exams.size());
        response.put("message", "Timetable generated for " + exams.size() + " courses");
        response.put("clashReport", report);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TimetableRow>> getTimetable() {
        List<Exam> exams = examRepository.findAll();
        List<TimetableRow> rows = new ArrayList<>();

        for (Exam exam : exams) {
            long enrolledCount = enrollmentRepository.findStudentIdsByCourseId(
                    exam.getCourse().getId()).size();

            TimetableRow row = new TimetableRow(
                    exam.getId(),
                    exam.getCourse().getCode(),
                    exam.getCourse().getTitle(),
                    exam.getCourse().getSemester(),
                    exam.getSlot().getExamDate(),
                    exam.getSlot().getSession().name(),
                    exam.getCourse().getFaculty().getName(),
                    enrolledCount);
            rows.add(row);
        }

        rows.sort((a, b) -> {
            int dateCompare = a.getExamDate().compareTo(b.getExamDate());
            if (dateCompare != 0) return dateCompare;
            return a.getSession().compareTo(b.getSession());
        });

        return ResponseEntity.ok(rows);
    }

    @GetMapping("/validate")
    public ResponseEntity<ClashReport> validate() {
        return ResponseEntity.ok(clashValidatorService.validateTimetable());
    }
}
