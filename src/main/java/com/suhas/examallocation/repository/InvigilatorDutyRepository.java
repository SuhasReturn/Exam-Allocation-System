package com.suhas.examallocation.repository;

import com.suhas.examallocation.model.InvigilatorDuty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvigilatorDutyRepository extends JpaRepository<InvigilatorDuty, Long> {

    List<InvigilatorDuty> findByFacultyId(Long facultyId);

    List<InvigilatorDuty> findBySlotId(Long slotId);

    List<InvigilatorDuty> findBySlotIdAndHallId(Long slotId, Long hallId);

    /**
     * Returns faculty IDs already assigned to a duty in a given slot.
     * Used to avoid double-booking faculty.
     */
    @Query("SELECT d.faculty.id FROM InvigilatorDuty d WHERE d.slot.id = :slotId")
    List<Long> findFacultyIdsOnDutyInSlot(@Param("slotId") Long slotId);

    /**
     * Counts how many duties a faculty member has across all slots.
     * Used to balance duty load — pick the least-burdened faculty first.
     */
    @Query("SELECT COUNT(d) FROM InvigilatorDuty d WHERE d.faculty.id = :facultyId")
    long countDutiesByFacultyId(@Param("facultyId") Long facultyId);

    void deleteBySlotId(Long slotId);
}
