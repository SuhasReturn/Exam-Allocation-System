package com.suhas.examallocation.service;

import com.suhas.examallocation.exception.ResourceNotFoundException;
import com.suhas.examallocation.model.Course;
import com.suhas.examallocation.model.Enrollment;
import com.suhas.examallocation.model.Faculty;
import com.suhas.examallocation.model.Hall;
import com.suhas.examallocation.model.Student;
import com.suhas.examallocation.repository.CourseRepository;
import com.suhas.examallocation.repository.EnrollmentRepository;
import com.suhas.examallocation.repository.FacultyRepository;
import com.suhas.examallocation.repository.HallRepository;
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
    private final HallRepository hallRepository;
    private final EnrollmentRepository enrollmentRepository;

    public CsvImportService(StudentRepository studentRepository,
                            CourseRepository courseRepository,
                            FacultyRepository facultyRepository,
                            HallRepository hallRepository,
                            EnrollmentRepository enrollmentRepository) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.facultyRepository = facultyRepository;
        this.hallRepository = hallRepository;
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
     * Expected CSV format: employee_code,name,department
     * Example: F001,Dr. Chaitra Kamath,Computer Science
     *
     * Skips faculty whose employee code already exists.
     */
    @Transactional
    public int importFaculty(MultipartFile file) {
       List<String> lines = readLines(file);
       int imported = 0;

       for (int i = 0; i < lines.size(); i++) {
           String line = lines.get(i).trim();
           if (line.isEmpty()) {
               continue;
           }

           String[] columns = line.split(",");
           if (columns.length < 3) {
               throw new IllegalArgumentException(
                       "Invalid faculty CSV at line " + (i + 1)
                       + ": expected 3 columns (employee_code,name,department), got "
                       + columns.length);
           }

           String employeeCode = columns[0].trim();
           String name = columns[1].trim();
           String department = columns[2].trim();

           if (facultyRepository.existsByEmployeeCode(employeeCode)) {
               continue;
           }

           Faculty faculty = new Faculty(employeeCode, name, department);
           facultyRepository.save(faculty);
           imported++;
       }

       return imported;
    }

    /**
     * Expected CSV format: code,title,semester,faculty_code
     * Example: 21CS51,Operating Systems,5,F009
     *
     * Accepts either numeric faculty_id or faculty_code values.
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
                       + ": expected 4 columns (code,title,semester,faculty_code), got "
                       + columns.length);
           }

           String code = columns[0].trim();
           String title = columns[1].trim();
           int lineNumber = i + 1;
           int semester = parseIntColumn(columns[2].trim(), "semester", lineNumber);
           String facultyRef = columns[3].trim();

           if (courseRepository.existsByCode(code)) {
               continue;
           }

           Faculty faculty = resolveFacultyForCourse(facultyRef, code, lineNumber);

           Course course = new Course(code, title, semester, faculty);
           courseRepository.save(course);
           imported++;
       }

       return imported;
    }

    /**
     * Expected CSV format: hall_name,total_rows,total_columns
     * Example: A-01,8,8
     *
     * Skips halls whose name already exists.
     */
    @Transactional
    public int importHalls(MultipartFile file) {
       List<String> lines = readLines(file);
       int imported = 0;

       for (int i = 0; i < lines.size(); i++) {
           String line = lines.get(i).trim();
           if (line.isEmpty()) {
               continue;
           }

           String[] columns = line.split(",");
           if (columns.length < 3) {
               throw new IllegalArgumentException(
                       "Invalid hall CSV at line " + (i + 1)
                       + ": expected 3 columns (hall_name,total_rows,total_columns), got "
                       + columns.length);
           }

           String name = columns[0].trim();
           int totalRows = parseIntColumn(columns[1].trim(), "total_rows", i + 1);
           int totalColumns = parseIntColumn(columns[2].trim(), "total_columns", i + 1);

           if (hallRepository.existsByName(name)) {
               continue;
           }

           Hall hall = new Hall(name, totalRows, totalColumns);
           hallRepository.save(hall);
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

    private Faculty resolveFacultyForCourse(String facultyRef, String courseCode, int lineNumber) {
        String normalized = facultyRef.trim();

        try {
            long facultyId = Long.parseLong(normalized);
            return facultyRepository.findById(facultyId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Faculty with id " + facultyId + " not found (referenced in course " + courseCode + ")"));
        } catch (NumberFormatException ignored) {
            // support the real CSV data format, which stores faculty codes like F146
            String employeeCode = normalized.toUpperCase();
            return facultyRepository.findByEmployeeCode(employeeCode)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Faculty with code " + facultyRef + " not found (referenced in course " + courseCode
                            + ", line " + lineNumber + ")"));
        }
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
