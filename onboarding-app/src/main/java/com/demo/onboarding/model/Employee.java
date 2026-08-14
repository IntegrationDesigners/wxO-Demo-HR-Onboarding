package com.demo.onboarding.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Employee entity representing an employee in the onboarding process.
 * Tracks CV creation status, car provisioning status, and overall onboarding completion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Employee entity with onboarding status information")
public class Employee {

    @Schema(description = "Unique identifier for the employee", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;

    @NotBlank(message = "Given name is required")
    @Schema(description = "Employee's given name (first name)", example = "John", required = true)
    private String givenName;

    @NotBlank(message = "Name is required")
    @Schema(description = "Employee's family name (last name)", example = "Doe", required = true)
    private String name;

    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Employee's start date", example = "2026-07-01", required = true)
    private LocalDate startDate;

    @NotBlank(message = "Gender is required")
    @Schema(description = "Employee's gender", example = "Male", required = true, allowableValues = {"Male", "Female", "Other"})
    private String gender;

    @Positive(message = "Salary must be positive")
    @Schema(description = "Employee's annual salary", example = "75000.00", required = true)
    private BigDecimal salary;

    @Schema(description = "CV creation status", example = "false", defaultValue = "false")
    private boolean cvStatus;

    @Schema(description = "Car provisioning status", example = "false", defaultValue = "false")
    private boolean carStatus;

    @Schema(description = "ID of the car assigned to this employee", example = "car-123")
    private String assignedCarId;

    @Schema(description = "Onboarding status: 'Completed', 'In Progress', or 'Error'", example = "In Progress")
    private String onboardingStatus;

    /**
     * Initializes a new employee with a generated UUID and default status values.
     */
    public void initialize() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        updateOnboardingStatus();
    }

    /**
     * Updates the onboarding completion status based on CV and car status.
     * Onboarding is complete only when both CV is created AND car is provisioned.
     * If carStatus is true but no car is assigned, status is set to "Error".
     */
    public void updateOnboardingStatus() {
        // Check for error condition: car status is true but no car assigned
        if (this.carStatus && (this.assignedCarId == null || this.assignedCarId.trim().isEmpty())) {
            this.onboardingStatus = "Error";
        } else if (this.cvStatus && this.carStatus) {
            this.onboardingStatus = "Completed";
        } else {
            this.onboardingStatus = "In Progress";
        }
    }
}


