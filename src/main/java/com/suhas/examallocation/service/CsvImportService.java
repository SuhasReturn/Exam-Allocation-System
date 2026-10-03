package com.suhas.examallocation.service;

import com.suhas.examallocation.exception.ResourceNotFoundException;
import com.suhas.examallocation.model.Course;
import com.suhas.examallocation.model.Enrollment;
import com.suhas.examallocation.model.Faculty;
import com.suhas.examallocation.model.Student;
import com.suhas.examallocation.repository.CourseRepository;
import com.suhas.examallocation.repository.EnrollmentRepository;
import com.suhas.examallocation.repository.FacultyRepository;
import com.suhas.examallocation.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses CSV files uploaded by the admin and bulk-inserts records.
 *
 * We read CSVs manually with BufferedReader instead of pulling in
 * a library like OpenCSV — keeps dependencies minimal and the logic
 * transparent. The trade-off is we don't handle quoted fields with
 * commas inside them, but our data (reg numbers, course codes) won't
 * have commas.
 */
@Service
public class CsvImportService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;
    private final EnrollmentRepository enrollmentRepository;

    public CsvImportService(StudentRepository studentRepository,
                            CourseRepository courseRepository,
                            FacultyRepository facultyRepository,
                            EnrollmentRepository enrollmentRepository) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.facultyRepository = facultyRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * Expected CSV format: reg_no,name,branch,semester
     * Example: 1PU21IS045,Suhas Kumar,ISE,6
     *
     * Skips students whose reg_no already exists (idempotent re-upload).
     * Returns the count of newly imported students.
     */
    @Transactional
    public int importStudents(MultipartFile file) {
        List<String> lines = readLines(file);
        int imported = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] columns = line.split(",");
            if (columns.length < 4) {
                throw new IllegalArgumentException(
                        "Invalid student CSV at line " + (i + 1)
                        + ": expected 4 columns (reg_no,name,branch,semester), got "
                        + columns.length);
            }

            String regNo = columns[0].trim();
            String name = columns[1].trim();
            String branch = columns[2].trim();
            int semester = parseIntColumn(columns[3].trim(), "semester", i + 1);

            if (studentRepository.existsByRegNo(regNo)) {
                continue;
            }

            Student student = new Student(regNo, name, branch, semester);
            studentRepository.save(student);
            imported++;
        }

        return imported;
    }

    /**
     * Expected CSV format: code,title,semester,faculty_id
     * Example: 21CS51,Operating Systems,5,3
     *
     * The faculty_id must refer to an existing faculty record.
     * Skips courses whose code already exists.
     */
    @Transactional
    public int importCourses(MultipartFile file) {
        List<String> lines = readLines(file);
        int imported = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] columns = line.split(",");
            if (columns.length < 4) {
                throw new IllegalArgumentException(
                        "Invalid course CSV at line " + (i + 1)
                        + ": expected 4 columns (code,title,semester,faculty_id), got "
                        + columns.length);
            }

            String code = columns[0].trim();
            String title = columns[1].trim();
            int lineNumber = i + 1;
            int semester = parseIntColumn(columns[2].trim(), "semester", lineNumber);
            Long facultyId = parseLongColumn(columns[3].trim(), "faculty_id", lineNumber);

            if (courseRepository.existsByCode(code)) {
                continue;
            }

            Faculty faculty = facultyRepository.findById(facultyId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Faculty with id " + facultyId
                            + " not found (referenced in course " + code + ")"));

            Course course = new Course(code, title, semester, faculty);
            courseRepository.save(course);
            imported++;
        }

        return imported;
    }

    /**
     * Expected CSV format: student_reg_no,course_code
     * Example: 1PU21IS045,21CS51
     *
     * Both the student and course must already exist.
     * Skips duplicate enrollments.
     */
    @Transactional
    public int importEnrollments(MultipartFile file) {
        List<String> lines = readLines(file);
        int imported = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] columns = line.split(",");
            if (columns.length < 2) {
                throw new IllegalArgumentException(
                        "Invalid enrollment CSV at line " + (i + 1)
                        + ": expected 2 columns (student_reg_no,course_code), got "
                        + columns.length);
            }

            String regNo = columns[0].trim();
            String courseCode = columns[1].trim();
            int lineNumber = i + 1;

            Student student = studentRepository.findByRegNo(regNo)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Student " + regNo + " not found (enrollment line " + lineNumber + ")"));

            Course course = courseRepository.findByCode(courseCode)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Course " + courseCode + " not found (enrollment line " + lineNumber + ")"));

            if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
                continue;
            }

            Enrollment enrollment = new Enrollment(student, course);
            enrollmentRepository.save(enrollment);
            imported++;
        }

        return imported;
    }

    private List<String> readLines(MultipartFile file) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // skip header line
            String header = reader.readLine();
            if (header == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to read CSV file: " + ex.getMessage());
        }
        return lines;
    }

    private int parseIntColumn(String value, String columnName, int lineNumber) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Invalid " + columnName + " '" + value + "' at line " + lineNumber
                    + ": must be a whole number");
        }
    }

    private long parseLongColumn(String value, String columnName, int lineNumber) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Invalid " + columnName + " '" + value + "' at line " + lineNumber
                    + ": must be a whole number");
        }
    }
}
