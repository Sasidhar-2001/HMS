package com.yourproject.controller;

import com.yourproject.dto.ApiResponse; // Though not directly used for file response
import com.yourproject.dto.ReportRequestDto;
import com.yourproject.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated; // Added
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')") // Only admins/wardens can generate reports
@Validated // Added for @Pattern on @RequestParam to work with GlobalExceptionHandler
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // Option 1: Using Request DTO (if preferred for validation or more params)
    // @GetMapping("/generate")
    // public ResponseEntity<InputStreamResource> generateReport(@Valid @ModelAttribute ReportRequestDto reportRequestDto) {
    //     try {
    //         ByteArrayInputStream bis = reportService.generateReport(
    //             reportRequestDto.getType(),
    //             reportRequestDto.getFormat(),
    //             reportRequestDto.getStartDate(),
    //             reportRequestDto.getEndDate()
    //         );
    //         String filename = reportService.getReportFilename(
    //             reportRequestDto.getType(),
    //             reportRequestDto.getFormat(),
    //             reportRequestDto.getStartDate(),
    //             reportRequestDto.getEndDate()
    //         );
    //         String contentType = reportService.getReportContentType(reportRequestDto.getFormat());

    //         HttpHeaders headers = new HttpHeaders();
    //         headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

    //         return ResponseEntity
    //                 .ok()
    //                 .headers(headers)
    //                 .contentType(MediaType.parseMediaType(contentType))
    //                 .body(new InputStreamResource(bis));

    //     } catch (Exception e) {
    //         // Log error properly
    //         // This should ideally be handled by GlobalExceptionHandler if it throws a specific exception
    //         return ResponseEntity.internalServerError().body(null); // Or an ApiResponse.error()
    //     }
    // }

    // Option 2: Using Request Parameters (more aligned with original Node.js GET /reports)
     @GetMapping
    public ResponseEntity<InputStreamResource> generateReport(
            @RequestParam @Pattern(regexp = "students|fees|complaints|rooms|leaves", message = "Invalid report type") String type,
            @RequestParam @Pattern(regexp = "pdf|excel", message = "Invalid report format") String format,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        try {
            ByteArrayInputStream bis = reportService.generateReport(type, format, startDate, endDate);
            String filename = reportService.getReportFilename(type, format, startDate, endDate);
            String contentType = reportService.getReportContentType(format);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            // headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            // headers.add("Pragma", "no-cache");
            // headers.add("Expires", "0");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new InputStreamResource(bis));

        } catch (IllegalArgumentException e) { // For invalid type/format before service call
             // This ideally should be handled by ConstraintViolationException via GlobalExceptionHandler if params are method-validated
            return ResponseEntity.badRequest().body(null); // Or an ApiResponse.error()
        }
        catch (Exception e) {
            // Log error properly
            // GlobalExceptionHandler should catch this if it throws a known exception type
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build(); // Build simple error response
        }
    }
}
