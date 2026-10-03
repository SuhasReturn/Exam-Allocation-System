package com.suhas.examallocation.service;

import com.suhas.examallocation.exception.ResourceNotFoundException;
import com.suhas.examallocation.model.Hall;
import com.suhas.examallocation.repository.HallRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HallService {

    private final HallRepository hallRepository;

    public HallService(HallRepository hallRepository) {
        this.hallRepository = hallRepository;
    }

    public List<Hall> findAll() {
        return hallRepository.findAll();
    }

    public Hall findById(Long id) {
        return hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hall not found with id " + id));
    }

    public Hall save(Hall hall) {
        if (hallRepository.existsByName(hall.getName())) {
            throw new IllegalArgumentException(
                    "Hall with name " + hall.getName() + " already exists");
        }
        return hallRepository.save(hall);
    }

    public Hall update(Long id, Hall updated) {
        Hall existing = findById(id);
        existing.setName(updated.getName());
        existing.setTotalRows(updated.getTotalRows());
        existing.setTotalColumns(updated.getTotalColumns());
        return hallRepository.save(existing);
    }

    public void delete(Long id) {
        if (!hallRepository.existsById(id)) {
            throw new ResourceNotFoundException("Hall not found with id " + id);
        }
        hallRepository.deleteById(id);
    }
}
