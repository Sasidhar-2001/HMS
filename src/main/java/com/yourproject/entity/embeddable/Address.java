package com.yourproject.entity.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Size(max = 255)
    private String street;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 20)
    private String zipCode;

    @Size(max = 100)
    private String country = "India"; // Default value
}
