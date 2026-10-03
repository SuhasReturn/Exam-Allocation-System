package com.suhas.examallocation.repository;

import com.suhas.examallocation.model.SeatAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatAllocationRepository extends JpaRepository<SeatAllocation, Long> {

    List<SeatAllocation> findBySlotId(Long slotId);

    List<SeatAllocation> findBySlotIdAndHallId(Long slotId, Long hallId);

    List<SeatAllocation> findByStudentId(Long studentId);

    void deleteBySlotId(Long slotId);
}
