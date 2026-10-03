package com.suhas.examallocation.dto;

import java.time.LocalDate;

public class TimetableRow {

    private Long examId;
    private String courseCode;
    private String courseTitle;
    private int semester;
    private LocalDate examDate;
    private String session;
    private String facultyName;
    private long enrolledCount;

    public TimetableRow() {
    }

    public TimetableRow(Long examId, String courseCode, String courseTitle,
                        int semester, LocalDate examDate, String session,
                        String facultyName, long enrolledCount) {
        this.examId = examId;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.semester = semester;
        this.examDate = examDate;
        this.session = session;
        this.facultyName = facultyName;
        this.enrolledCount = enrolledCount;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
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

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
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

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public long getEnrolledCount() {
        return enrolledCount;
    }

    public void setEnrolledCount(long enrolledCount) {
        this.enrolledCount = enrolledCount;
    }
}
