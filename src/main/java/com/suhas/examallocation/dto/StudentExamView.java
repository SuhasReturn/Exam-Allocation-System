package com.suhas.examallocation.dto;

import java.time.LocalDate;

public class StudentExamView {

    private String courseCode;
    private String courseTitle;
    private LocalDate examDate;
    private String session;
    private String hallName;
    private int seatNo;

    public StudentExamView() {
    }

    public StudentExamView(String courseCode, String courseTitle, LocalDate examDate,
                           String session, String hallName, int seatNo) {
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.examDate = examDate;
        this.session = session;
        this.hallName = hallName;
        this.seatNo = seatNo;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
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

    public int getSeatNo() {
        return seatNo;
    }

    public void setSeatNo(int seatNo) {
        this.seatNo = seatNo;
    }
}
