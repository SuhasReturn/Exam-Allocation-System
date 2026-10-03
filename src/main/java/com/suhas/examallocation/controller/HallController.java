package com.suhas.examallocation.controller;

import com.suhas.examallocation.model.Hall;
import com.suhas.examallocation.service.HallService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/halls")
public class HallController {

    private final HallService hallService;

    public HallController(HallService hallService) {
        this.hallService = hallService;
    }

    @GetMapping
    public ResponseEntity<List<Hall>> getAll() {
        return ResponseEntity.ok(hallService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hall> getById(@PathVariable Long id) {
        return ResponseEntity.ok(hallService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Hall> create(@Valid @RequestBody Hall hall) {
        Hall saved = hallService.save(hall);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Hall> update(@PathVariable Long id,
                                       @Valid @RequestBody Hall hall) {
        return ResponseEntity.ok(hallService.update(id, hall));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        hallService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
