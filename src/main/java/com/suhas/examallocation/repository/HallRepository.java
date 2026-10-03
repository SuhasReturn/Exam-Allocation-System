package com.suhas.examallocation.repository;

import com.suhas.examallocation.model.Hall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HallRepository extends JpaRepository<Hall, Long> {

    Optional<Hall> findByName(String name);

    boolean existsByName(String name);
}
