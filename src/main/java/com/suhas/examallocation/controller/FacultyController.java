package com.suhas.examallocation.controller;

import com.suhas.examallocation.dto.FacultyDutyView;
import com.suhas.examallocation.exception.ResourceNotFoundException;
import com.suhas.examallocation.model.Faculty;
import com.suhas.examallocation.model.FacultyLeave;
import com.suhas.examallocation.model.InvigilatorDuty;
import com.suhas.examallocation.model.UserAccount;
import com.suhas.examallocation.repository.FacultyLeaveRepository;
import com.suhas.examallocation.repository.InvigilatorDutyRepository;
import com.suhas.examallocation.repository.UserAccountRepository;
import com.suhas.examallocation.service.FacultyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles two sets of endpoints:
 * 1. Admin CRUD at /api/admin/faculty (requires ADMIN role)
 * 2. Faculty self-service at /api/faculty/* (requires FACULTY role):
 *    - View own duties
 *    - Mark unavailable dates
 */
@RestController
public class FacultyController {

    private final FacultyService facultyService;
    private final InvigilatorDutyRepository invigilatorDutyRepository;
    private final FacultyLeaveRepository facultyLeaveRepository;
    private final UserAccountRepository userAccountRepository;

    public FacultyController(FacultyService facultyService,
                              InvigilatorDutyRepository invigilatorDutyRepository,
                              FacultyLeaveRepository facultyLeaveRepository,
                              UserAccountRepository userAccountRepository) {
        this.facultyService = facultyService;
        this.invigilatorDutyRepository = invigilatorDutyRepository;
        this.facultyLeaveRepository = facultyLeaveRepository;
        this.userAccountRepository = userAccountRepository;
    }

    // ---- Admin CRUD endpoints ----

    @GetMapping("/api/admin/faculty")
    public ResponseEntity<List<Faculty>> getAll() {
        return ResponseEntity.ok(facultyService.findAll());
    }

    @GetMapping("/api/admin/faculty/{id}")
    public ResponseEntity<Faculty> getById(@PathVariable Long id) {
        return ResponseEntity.ok(facultyService.findById(id));
    }

    @PostMapping("/api/admin/faculty")
    public ResponseEntity<Faculty> create(@Valid @RequestBody Faculty faculty) {
        Faculty saved = facultyService.save(faculty);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/api/admin/faculty/{id}")
    public ResponseEntity<Faculty> update(@PathVariable Long id,
                                          @Valid @RequestBody Faculty faculty) {
        return ResponseEntity.ok(facultyService.update(id, faculty));
    }

    @DeleteMapping("/api/admin/faculty/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        facultyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Faculty-facing endpoints ----

    @GetMapping("/api/faculty/my-duties")
    public ResponseEntity<List<FacultyDutyView>> getMyDuties(Authentication authentication) {
        Faculty faculty = getLinkedFaculty(authentication.getName());

        List<InvigilatorDuty> duties = invigilatorDutyRepository
                .findByFacultyId(faculty.getId());

        List<FacultyDutyView> views = new ArrayList<>();
        for (InvigilatorDuty duty : duties) {
            FacultyDutyView view = new FacultyDutyView(
                    duty.getId(),
                    duty.getSlot().getExamDate(),
                    duty.getSlot().getSession().name(),
                    duty.getHall().getName(),
                    duty.getDutyRole().name());
            views.add(view);
        }

        views.sort((a, b) -> {
            int dateCompare = a.getExamDate().compareTo(b.getExamDate());
            if (dateCompare != 0) return dateCompare;
            return a.getSession().compareTo(b.getSession());
        });

        return ResponseEntity.ok(views);
    }

    /**
     * Accepts a list of dates the faculty member is unavailable.
     * Skips dates already marked.
     *
     * Request body: {"dates": ["2025-12-03", "2025-12-05"]}
     */
    @PostMapping("/api/faculty/unavailable-dates")
    public ResponseEntity<Map<String, Object>> markUnavailable(
            Authentication authentication,
            @RequestBody Map<String, List<String>> body) {

        Faculty faculty = getLinkedFaculty(authentication.getName());

        List<String> dateStrings = body.get("dates");
        if (dateStrings == null || dateStrings.isEmpty()) {
            throw new IllegalArgumentException("Request must include a 'dates' array");
        }

        int added = 0;
        for (String dateStr : dateStrings) {
            LocalDate date = LocalDate.parse(dateStr);
            if (!facultyLeaveRepository.existsByFacultyIdAndLeaveDate(faculty.getId(), date)) {
                FacultyLeave leave = new FacultyLeave(faculty, date);
                facultyLeaveRepository.save(leave);
                added++;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Marked " + added + " new unavailable date(s)");
        response.put("addedCount", added);
        return ResponseEntity.ok(response);
    }

    private Faculty getLinkedFaculty(String username) {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found for username " + username));

        if (account.getLinkedFaculty() == null) {
            throw new ResourceNotFoundException(
                    "No faculty profile linked to account " + username);
        }

        return account.getLinkedFaculty();
    }
}
