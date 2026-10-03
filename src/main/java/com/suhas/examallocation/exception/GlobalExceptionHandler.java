package com.suhas.examallocation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Catches exceptions thrown anywhere in the controller layer and
 * turns them into clean JSON error responses instead of raw stack traces.
 *
 * @RestControllerAdvice is like @ControllerAdvice + @ResponseBody —
 * Spring scans for @ExceptionHandler methods here and routes matching
 * exceptions to them automatically.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ClashDetectedException.class)
    public ResponseEntity<Map<String, Object>> handleClashDetected(ClashDetectedException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NotEnoughSlotsException.class)
    public ResponseEntity<Map<String, Object>> handleNotEnoughSlots(NotEnoughSlotsException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(NotEnoughHallsException.class)
    public ResponseEntity<Map<String, Object>> handleNotEnoughHalls(NotEnoughHallsException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(NotEnoughInvigilatorsException.class)
    public ResponseEntity<Map<String, Object>> handleNotEnoughInvigilators(NotEnoughInvigilatorsException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles bean validation failures (@NotBlank, @Size, etc.).
     * Collects all field errors into a readable message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        StringBuilder message = new StringBuilder("Validation failed: ");
        ex.getBindingResult().getFieldErrors().forEach(error ->
            message.append(error.getField())
                   .append(" ")
                   .append(error.getDefaultMessage())
                   .append("; ")
        );
        return buildResponse(HttpStatus.BAD_REQUEST, message.toString().trim());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred: " + ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}
