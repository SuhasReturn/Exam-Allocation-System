package com.suhas.examallocation.service;

import com.suhas.examallocation.exception.ResourceNotFoundException;
import com.suhas.examallocation.model.Course;
import com.suhas.examallocation.model.Faculty;
import com.suhas.examallocation.repository.CourseRepository;
import com.suhas.examallocation.repository.FacultyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;

    public CourseService(CourseRepository courseRepository,
                         FacultyRepository facultyRepository) {
        this.courseRepository = courseRepository;
        this.facultyRepository = facultyRepository;
    }

    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    public Course findById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course not found with id " + id));
    }

    public Course findByCode(String code) {
        return courseRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course not found with code " + code));
    }

    public Course save(Course course) {
        if (courseRepository.existsByCode(course.getCode())) {
            throw new IllegalArgumentException(
                    "Course with code " + course.getCode() + " already exists");
        }
        return courseRepository.save(course);
    }

    /**
     * Saves a course by looking up the faculty by ID.
     * Used by CSV import where we get a faculty ID from the file.
     */
    public Course saveWithFacultyId(String code, String title, int semester, Long facultyId) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Faculty not found with id " + facultyId
                        + " (referenced by course " + code + ")"));

        Course course = new Course(code, title, semester, faculty);
        return save(course);
    }

    public Course update(Long id, Course updated) {
        Course existing = findById(id);
        existing.setCode(updated.getCode());
        existing.setTitle(updated.getTitle());
        existing.setSemester(updated.getSemester());
        existing.setFaculty(updated.getFaculty());
        return courseRepository.save(existing);
    }

    public void delete(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Course not found with id " + id);
        }
        courseRepository.deleteById(id);
    }
}
