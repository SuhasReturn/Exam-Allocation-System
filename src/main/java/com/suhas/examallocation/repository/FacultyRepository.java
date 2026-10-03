package com.suhas.examallocation.repository;

import com.suhas.examallocation.model.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    Optional<Faculty> findByName(String name);
}
