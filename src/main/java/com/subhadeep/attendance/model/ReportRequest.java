package com.subhadeep.attendance.model;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ReportRequest {
    private String reportType; // Attendance Summary or Grade Report
    private String className;
    private LocalDate dateFrom;
    private LocalDate dateTo;

    // Getters and setters
}