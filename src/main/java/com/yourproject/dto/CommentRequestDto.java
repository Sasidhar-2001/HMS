package com.yourproject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDto {
    @NotBlank(message = "Comment text cannot be blank")
    @Size(min = 1, max = 500, message = "Comment must be between 1 and 500 characters")
    private String text;
}
