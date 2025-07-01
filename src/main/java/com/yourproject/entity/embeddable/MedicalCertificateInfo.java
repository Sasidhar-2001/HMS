package com.yourproject.entity.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate; // Mongoose used Date

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalCertificateInfo {

    @Column(nullable = false)
    private boolean isRequired = false;

    @Column(nullable = false)
    private boolean isUploaded = false;

    @Size(max = 255)
    @Column(length = 255)
    private String fileName; // Name of the uploaded certificate file

    // We might store a path or a reference to a file storage service ID here instead of just filename
    // For now, keeping it simple as per Mongoose model.
    // private String filePath;

    private LocalDate uploadDate;
}
