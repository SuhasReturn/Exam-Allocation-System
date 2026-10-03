package com.suhas.examallocation.controller;

import com.suhas.examallocation.model.InvigilatorDuty;
import com.suhas.examallocation.repository.InvigilatorDutyRepository;
import com.suhas.examallocation.service.InvigilatorDutyAssignerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/duties")
public class InvigilatorDutyController {

    private final InvigilatorDutyAssignerService dutyAssignerService;
    private final InvigilatorDutyRepository invigilatorDutyRepository;

    public InvigilatorDutyController(InvigilatorDutyAssignerService dutyAssignerService,
                                      InvigilatorDutyRepository invigilatorDutyRepository) {
        this.dutyAssignerService = dutyAssignerService;
        this.invigilatorDutyRepository = invigilatorDutyRepository;
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate() {
        dutyAssignerService.assignDutiesForAllSlots();

        long totalDuties = invigilatorDutyRepository.count();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Invigilator duties assigned successfully");
        response.put("totalDuties", totalDuties);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllDuties() {
        List<InvigilatorDuty> duties = invigilatorDutyRepository.findAll();
        return ResponseEntity.ok(mapDutiesToResponse(duties));
    }

    /**
     * Replaces the faculty member on an existing duty.
     * The system picks the best eligible replacement automatically.
     */
    @PutMapping("/{id}/replace")
    public ResponseEntity<Map<String, Object>> replaceDuty(@PathVariable Long id) {
        InvigilatorDuty updated = dutyAssignerService.replaceFacultyOnDuty(id);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Duty reassigned successfully");
        response.put("dutyId", updated.getId());
        response.put("newFacultyName", updated.getFaculty().getName());
        response.put("hallName", updated.getHall().getName());
        response.put("dutyRole", updated.getDutyRole().name());
        return ResponseEntity.ok(response);
    }

    private List<Map<String, Object>> mapDutiesToResponse(List<InvigilatorDuty> duties) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (InvigilatorDuty duty : duties) {
            Map<String, Object> dutyMap = new HashMap<>();
            dutyMap.put("dutyId", duty.getId());
            dutyMap.put("facultyName", duty.getFaculty().getName());
            dutyMap.put("facultyDepartment", duty.getFaculty().getDepartment());
            dutyMap.put("hallName", duty.getHall().getName());
            dutyMap.put("examDate", duty.getSlot().getExamDate().toString());
            dutyMap.put("session", duty.getSlot().getSession().name());
            dutyMap.put("dutyRole", duty.getDutyRole().name());
            result.add(dutyMap);
        }
        return result;
    }
}
