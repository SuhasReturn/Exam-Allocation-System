package com.suhas.examallocation.controller;

import com.suhas.examallocation.model.SeatAllocation;
import com.suhas.examallocation.repository.SeatAllocationRepository;
import com.suhas.examallocation.service.SeatingPlanGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/seating")
public class SeatingPlanController {

    private final SeatingPlanGeneratorService seatingPlanGeneratorService;
    private final SeatAllocationRepository seatAllocationRepository;

    public SeatingPlanController(SeatingPlanGeneratorService seatingPlanGeneratorService,
                                  SeatAllocationRepository seatAllocationRepository) {
        this.seatingPlanGeneratorService = seatingPlanGeneratorService;
        this.seatAllocationRepository = seatAllocationRepository;
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate() {
        List<String> degradedHalls = seatingPlanGeneratorService.generateSeatingForAllSlots();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Seating plan generated successfully");
        response.put("degradedHalls", degradedHalls);
        if (!degradedHalls.isEmpty()) {
            response.put("warning",
                    degradedHalls.size() + " hall(s) have imperfect interleaving: "
                    + String.join(", ", degradedHalls));
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Returns seating for a specific slot, grouped by hall.
     * Each hall entry includes its seats with student and course info.
     */
    @GetMapping("/{slotId}")
    public ResponseEntity<List<Map<String, Object>>> getSeatingBySlot(
            @PathVariable Long slotId) {

        List<SeatAllocation> allocations = seatAllocationRepository.findBySlotId(slotId);

        // group by hall
        Map<Long, List<SeatAllocation>> byHall = new HashMap<>();
        for (SeatAllocation alloc : allocations) {
            byHall.computeIfAbsent(alloc.getHall().getId(), k -> new ArrayList<>())
                   .add(alloc);
        }

        List<Map<String, Object>> hallSeatingList = new ArrayList<>();
        for (Map.Entry<Long, List<SeatAllocation>> entry : byHall.entrySet()) {
            List<SeatAllocation> hallAllocations = entry.getValue();
            hallAllocations.sort((a, b) -> Integer.compare(a.getSeatNo(), b.getSeatNo()));

            Map<String, Object> hallData = new HashMap<>();
            hallData.put("hallId", hallAllocations.get(0).getHall().getId());
            hallData.put("hallName", hallAllocations.get(0).getHall().getName());
            hallData.put("totalRows", hallAllocations.get(0).getHall().getTotalRows());
            hallData.put("totalColumns", hallAllocations.get(0).getHall().getTotalColumns());
            hallData.put("seatedCount", hallAllocations.size());

            List<Map<String, Object>> seats = new ArrayList<>();
            for (SeatAllocation alloc : hallAllocations) {
                Map<String, Object> seat = new HashMap<>();
                seat.put("seatNo", alloc.getSeatNo());
                seat.put("studentName", alloc.getStudent().getName());
                seat.put("studentRegNo", alloc.getStudent().getRegNo());
                seat.put("courseCode", alloc.getExam().getCourse().getCode());
                seat.put("courseTitle", alloc.getExam().getCourse().getTitle());
                seats.add(seat);
            }
            hallData.put("seats", seats);
            hallSeatingList.add(hallData);
        }

        return ResponseEntity.ok(hallSeatingList);
    }
}
