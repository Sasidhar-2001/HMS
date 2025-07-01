package com.yourproject.service;

import com.yourproject.dto.ReportRequestDto; // To be created
import com.yourproject.dto.ReportResponseDto; // To be created
// Or directly return byte[] or Resource for the file

import java.io.ByteArrayInputStream; // For returning file as stream
import java.time.LocalDate;

public interface ReportService {

    // Option 1: Return a DTO with file info (path/URL)
    // ReportResponseDto generateReport(ReportRequestDto reportRequest);

    // Option 2: Return the file content directly as a stream or resource
    // This is often more flexible for controller to then serve as download.

    /**
     * Generates a report based on the specified parameters.
     * @param reportType Type of report (e.g., "students", "fees")
     * @param format Format of report ("pdf" or "excel")
     * @param startDate Optional start date for report data
     * @param endDate Optional end date for report data
     * @return ByteArrayInputStream containing the report file.
     * @throws Exception if report generation fails.
     */
    ByteArrayInputStream generateReport(String reportType, String format, LocalDate startDate, LocalDate endDate) throws Exception;

    String getReportFilename(String reportType, String format, LocalDate startDate, LocalDate endDate);
    String getReportContentType(String format);


    // Potentially more specific methods if needed:
    // ByteArrayInputStream generateStudentListPdf();
    // ByteArrayInputStream generateFeeCollectionExcel(LocalDate from, LocalDate to);
}
