package com.suhas.examallocation.service;

import com.suhas.examallocation.exception.ResourceNotFoundException;
import com.suhas.examallocation.model.Student;
import com.suhas.examallocation.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    public Student findById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found with id " + id));
    }

    public Student findByRegNo(String regNo) {
        return studentRepository.findByRegNo(regNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found with regNo " + regNo));
    }

    public Student save(Student student) {
        if (studentRepository.existsByRegNo(student.getRegNo())) {
            throw new IllegalArgumentException(
                    "Student with regNo " + student.getRegNo() + " already exists");
        }
        return studentRepository.save(student);
    }

    public Student update(Long id, Student updated) {
        Student existing = findById(id);
        existing.setRegNo(updated.getRegNo());
        existing.setName(updated.getName());
        existing.setBranch(updated.getBranch());
        existing.setSemester(updated.getSemester());
        return studentRepository.save(existing);
    }

    public void delete(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student not found with id " + id);
        }
        studentRepository.deleteById(id);
    }
}
