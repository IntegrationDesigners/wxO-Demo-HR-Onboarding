package com.demo.onboarding.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Car entity representing a company car available for employee assignment.
 * Tracks availability, assignment status, and car details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Car entity with availability and assignment information")
public class Car {

    @Schema(description = "Unique identifier for the car", example = "car-123e4567")
    private String id;

    @NotBlank(message = "Car name is required")
    @Schema(description = "Car make and model", example = "BMW 3 Series", required = true)
    private String name;

    @NotBlank(message = "License plate is required")
    @Schema(description = "Car license plate number", example = "ABC-123", required = true)
    private String licensePlate;

    @Schema(description = "Car availability status", example = "true", defaultValue = "true")
    private boolean available;

    @Schema(description = "ID of the employee assigned to this car", example = "123e4567-e89b-12d3-a456-426614174000")
    private String assignedEmployeeId;

    @Schema(description = "Name of the employee assigned to this car", example = "John Doe")
    private String assignedEmployeeName;

    @NotNull(message = "Price range is required")
    @Min(value = 1, message = "Price range must be between 1 and 4")
    @Max(value = 4, message = "Price range must be between 1 and 4")
    @Schema(description = "Car price range class (1=Economy, 2=Mid-range, 3=Premium, 4=Luxury)", 
            example = "2", required = true, minimum = "1", maximum = "4")
    private Integer priceRange;

    @NotBlank(message = "Status is required")
    @Schema(description = "Current status of the car", 
            example = "Available", 
            required = true,
            allowableValues = {"Available", "Assigned", "In Maintenance", "Reserved"})
    private String status;

    /**
     * Initializes a new car with a generated UUID and default values.
     */
    public void initialize() {
        if (this.id == null) {
            this.id = "car-" + UUID.randomUUID().toString().substring(0, 8);
        }
        if (this.status == null) {
            this.status = "Available";
        }
        this.available = "Available".equals(this.status);
    }

    /**
     * Assigns this car to an employee.
     * 
     * @param employeeId The ID of the employee to assign
     * @param employeeName The name of the employee to assign
     */
    public void assignToEmployee(String employeeId, String employeeName) {
        this.assignedEmployeeId = employeeId;
        this.assignedEmployeeName = employeeName;
        this.status = "Assigned";
        this.available = false;
    }

    /**
     * Unassigns this car from its current employee.
     */
    public void unassign() {
        this.assignedEmployeeId = null;
        this.assignedEmployeeName = null;
        this.status = "Available";
        this.available = true;
    }
}


