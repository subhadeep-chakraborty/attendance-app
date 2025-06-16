package com.subhadeep.attendance.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReportPreview {
    private String reportType;
    private String className;
    private List<String> headers;
    private List<List<String>> rows;
}
