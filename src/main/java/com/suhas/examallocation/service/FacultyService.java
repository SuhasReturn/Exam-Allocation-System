package com.suhas.examallocation.service;

import com.suhas.examallocation.exception.ResourceNotFoundException;
import com.suhas.examallocation.model.Faculty;
import com.suhas.examallocation.repository.FacultyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FacultyService {

    private final FacultyRepository facultyRepository;

    public FacultyService(FacultyRepository facultyRepository) {
        this.facultyRepository = facultyRepository;
    }

    public List<Faculty> findAll() {
        return facultyRepository.findAll();
    }

    public Faculty findById(Long id) {
        return facultyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Faculty not found with id " + id));
    }

    public Faculty save(Faculty faculty) {
        return facultyRepository.save(faculty);
    }

    public Faculty update(Long id, Faculty updated) {
        Faculty existing = findById(id);
        existing.setName(updated.getName());
        existing.setDepartment(updated.getDepartment());
        return facultyRepository.save(existing);
    }

    public void delete(Long id) {
        if (!facultyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Faculty not found with id " + id);
        }
        facultyRepository.deleteById(id);
    }
}
