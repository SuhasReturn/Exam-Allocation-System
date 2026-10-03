package com.suhas.examallocation.dto;

import java.time.LocalDate;

public class FacultyDutyView {

    private Long dutyId;
    private LocalDate examDate;
    private String session;
    private String hallName;
    private String dutyRole;

    public FacultyDutyView() {
    }

    public FacultyDutyView(Long dutyId, LocalDate examDate, String session,
                           String hallName, String dutyRole) {
        this.dutyId = dutyId;
        this.examDate = examDate;
        this.session = session;
        this.hallName = hallName;
        this.dutyRole = dutyRole;
    }

    public Long getDutyId() {
        return dutyId;
    }

    public void setDutyId(Long dutyId) {
        this.dutyId = dutyId;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public String getHallName() {
        return hallName;
    }

    public void setHallName(String hallName) {
        this.hallName = hallName;
    }

    public String getDutyRole() {
        return dutyRole;
    }

    public void setDutyRole(String dutyRole) {
        this.dutyRole = dutyRole;
    }
}
