package com.suhas.examallocation.controller;

import com.suhas.examallocation.dto.StudentExamView;
import com.suhas.examallocation.exception.ResourceNotFoundException;
import com.suhas.examallocation.model.SeatAllocation;
import com.suhas.examallocation.model.Student;
import com.suhas.examallocation.model.UserAccount;
import com.suhas.examallocation.repository.SeatAllocationRepository;
import com.suhas.examallocation.repository.UserAccountRepository;
import com.suhas.examallocation.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles two sets of endpoints:
 * 1. Admin CRUD at /api/admin/students (requires ADMIN role)
 * 2. Student self-view at /api/student/my-exams (requires STUDENT role)
 *
 * Security is enforced by SecurityConfig's URL rules, not here.
 * No class-level @RequestMapping because the two groups have different prefixes.
 */
@RestController
public class StudentController {

    private final StudentService studentService;
    private final SeatAllocationRepository seatAllocationRepository;
    private final UserAccountRepository userAccountRepository;

    public StudentController(StudentService studentService,
                              SeatAllocationRepository seatAllocationRepository,
                              UserAccountRepository userAccountRepository) {
        this.studentService = studentService;
        this.seatAllocationRepository = seatAllocationRepository;
        this.userAccountRepository = userAccountRepository;
    }

    // ---- Admin CRUD endpoints ----

    @GetMapping("/api/admin/students")
    public ResponseEntity<List<Student>> getAll() {
        return ResponseEntity.ok(studentService.findAll());
    }

    @GetMapping("/api/admin/students/{id}")
    public ResponseEntity<Student> getById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.findById(id));
    }

    @PostMapping("/api/admin/students")
    public ResponseEntity<Student> create(@Valid @RequestBody Student student) {
        Student saved = studentService.save(student);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/api/admin/students/{id}")
    public ResponseEntity<Student> update(@PathVariable Long id,
                                          @Valid @RequestBody Student student) {
        return ResponseEntity.ok(studentService.update(id, student));
    }

    @DeleteMapping("/api/admin/students/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Student-facing endpoint ----

    /**
     * Returns the logged-in student's exam schedule with hall and seat info.
     *
     * Authentication.getName() gives us the username from the JWT.
     * We look up the UserAccount, then get the linked Student,
     * then find all their seat allocations.
     */
    @GetMapping("/api/student/my-exams")
    public ResponseEntity<List<StudentExamView>> getMyExams(Authentication authentication) {
        Student student = getLinkedStudent(authentication.getName());

        List<SeatAllocation> allocations = seatAllocationRepository
                .findByStudentId(student.getId());

        List<StudentExamView> views = new ArrayList<>();
        for (SeatAllocation alloc : allocations) {
            StudentExamView view = new StudentExamView(
                    alloc.getExam().getCourse().getCode(),
                    alloc.getExam().getCourse().getTitle(),
                    alloc.getSlot().getExamDate(),
                    alloc.getSlot().getSession().name(),
                    alloc.getHall().getName(),
                    alloc.getSeatNo());
            views.add(view);
        }

        // sort by exam date then session
        views.sort((a, b) -> {
            int dateCompare = a.getExamDate().compareTo(b.getExamDate());
            if (dateCompare != 0) return dateCompare;
            return a.getSession().compareTo(b.getSession());
        });

        return ResponseEntity.ok(views);
    }

    private Student getLinkedStudent(String username) {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found for username " + username));

        if (account.getLinkedStudent() == null) {
            throw new ResourceNotFoundException(
                    "No student profile linked to account " + username);
        }

        return account.getLinkedStudent();
    }
}
