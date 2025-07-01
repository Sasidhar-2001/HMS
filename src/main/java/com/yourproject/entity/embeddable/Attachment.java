package com.yourproject.entity.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {

    @Size(max = 255)
    @Column(length = 255)
    private String fileName;

    @Size(max = 500) // Path or URL to the file
    @Column(length = 500)
    private String filePath; // Could also be a URL

    private Long fileSize; // In bytes

    @Column(updatable = false)
    private LocalDateTime uploadDate = LocalDateTime.now();
}
