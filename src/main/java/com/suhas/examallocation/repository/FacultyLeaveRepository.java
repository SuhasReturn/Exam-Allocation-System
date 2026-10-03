package com.suhas.examallocation.repository;

import com.suhas.examallocation.model.FacultyLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FacultyLeaveRepository extends JpaRepository<FacultyLeave, Long> {

    List<FacultyLeave> findByFacultyId(Long facultyId);

    /**
     * Returns IDs of all faculty members on leave on a given date.
     * Used by InvigilatorDutyAssignerService to exclude unavailable faculty.
     */
    @Query("SELECT fl.faculty.id FROM FacultyLeave fl WHERE fl.leaveDate = :date")
    List<Long> findFacultyIdsOnLeave(@Param("date") LocalDate date);

    boolean existsByFacultyIdAndLeaveDate(Long facultyId, LocalDate leaveDate);
}
