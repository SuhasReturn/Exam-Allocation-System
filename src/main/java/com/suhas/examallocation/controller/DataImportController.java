package com.suhas.examallocation.controller;

import com.suhas.examallocation.service.CsvImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles CSV file uploads from the admin.
 *
 * @RequestParam("file") tells Spring to look for a multipart form field
 * named "file" in the request. The admin uploads via a form or Postman
 * with that field name.
 */
@RestController
@RequestMapping("/api/admin/import")
public class DataImportController {

    private final CsvImportService csvImportService;

    public DataImportController(CsvImportService csvImportService) {
        this.csvImportService = csvImportService;
    }

    @PostMapping("/students")
    public ResponseEntity<Map<String, Object>> importStudents(
            @RequestParam("file") MultipartFile file) {
        int count = csvImportService.importStudents(file);
        return ResponseEntity.ok(buildImportResponse("students", count));
    }

    @PostMapping("/faculty")
    public ResponseEntity<Map<String, Object>> importFaculty(
            @RequestParam("file") MultipartFile file) {
        int count = csvImportService.importFaculty(file);
        return ResponseEntity.ok(buildImportResponse("faculty", count));
    }

    @PostMapping("/courses")
    public ResponseEntity<Map<String, Object>> importCourses(
            @RequestParam("file") MultipartFile file) {
        int count = csvImportService.importCourses(file);
        return ResponseEntity.ok(buildImportResponse("courses", count));
    }

    @PostMapping("/halls")
    public ResponseEntity<Map<String, Object>> importHalls(
            @RequestParam("file") MultipartFile file) {
        int count = csvImportService.importHalls(file);
        return ResponseEntity.ok(buildImportResponse("halls", count));
    }

    @PostMapping("/enrollments")
    public ResponseEntity<Map<String, Object>> importEnrollments(
            @RequestParam("file") MultipartFile file) {
        int count = csvImportService.importEnrollments(file);
        return ResponseEntity.ok(buildImportResponse("enrollments", count));
    }

    private Map<String, Object> buildImportResponse(String entity, int count) {
        Map<String, Object> response = new HashMap<>();
        response.put("entity", entity);
        response.put("importedCount", count);
        response.put("message", "Successfully imported " + count + " " + entity);
        return response;
    }
}
