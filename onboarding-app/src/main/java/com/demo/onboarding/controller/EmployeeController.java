package com.demo.onboarding.controller;

import com.demo.onboarding.model.Employee;
import com.demo.onboarding.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing employee onboarding operations.
 * Provides endpoints for creating, retrieving, and updating employee records and their onboarding status.
 */
@RestController
@RequestMapping("/api/employee")
@Tag(name = "Employee Management", description = "APIs for managing employee onboarding processes including CV creation and car provisioning status")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @PostMapping
    @Operation(
        summary = "Create a new employee",
        description = "Creates a new employee record in the onboarding system. The employee is initialized with a unique ID and default onboarding status (CV and car status set to false). The system will automatically track when both CV creation and car provisioning are completed to mark the overall onboarding as complete."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Employee successfully created",
            content = @Content(schema = @Schema(implementation = Employee.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data - missing required fields or invalid format"
        )
    })
    public ResponseEntity<Employee> createEmployee(
            @Parameter(description = "Employee details including given name, family name, start date, and gender", required = true)
            @Valid @RequestBody Employee employee) {
        Employee created = employeeService.createEmployee(employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get employee onboarding status",
        description = "Retrieves detailed information about an employee's onboarding status including their start date, CV creation status, car provisioning status, and overall onboarding completion status. The onboardingStatus field reflects one of three states: 'Completed' (both CV created and car provisioned), 'In Progress' (one or both steps not yet done), or 'Error' (carStatus is true but no car is actually assigned to the employee, indicating a data inconsistency that requires manual correction)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Employee found and returned successfully",
            content = @Content(schema = @Schema(implementation = Employee.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Employee not found with the specified ID"
        )
    })
    public ResponseEntity<Employee> getEmployee(
            @Parameter(description = "Unique identifier of the employee", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String id) {
        return employeeService.getEmployeeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/cv-status")
    @Operation(
        summary = "Update employee CV status",
        description = "Updates the CV creation status of a specific employee. Set cvStatus to true when the employee's CV has been created, or false to mark it as not yet done. The overall onboarding status is automatically recalculated after the update."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "CV status updated successfully",
            content = @Content(schema = @Schema(implementation = Employee.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Employee not found with the specified ID"
        )
    })
    public ResponseEntity<Employee> updateCvStatus(
            @Parameter(description = "Unique identifier of the employee", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String id,
            @Parameter(description = "CV creation status", required = true, example = "true")
            @RequestParam boolean cvStatus) {
        return employeeService.updateEmployeeStatus(id, cvStatus, null)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update employee onboarding status",
        description = "Updates exactly one onboarding status field for an employee. Provide either cvStatus OR carStatus — not both and not neither. The system automatically recalculates the overall onboarding completion status. Onboarding is marked as complete only when BOTH cvStatus=true AND carStatus=true."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Employee status updated successfully",
            content = @Content(schema = @Schema(implementation = Employee.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Employee not found with the specified ID"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Exactly one of cvStatus or carStatus must be provided"
        )
    })
    public ResponseEntity<?> updateEmployeeStatus(
            @Parameter(description = "Unique identifier of the employee", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String id,
            @Parameter(description = "CV creation status - set to true when CV is created", example = "true")
            @RequestParam(required = false) Boolean cvStatus,
            @Parameter(description = "Car provisioning status - set to true when car is assigned", example = "true")
            @RequestParam(required = false) Boolean carStatus) {
        int provided = (cvStatus != null ? 1 : 0) + (carStatus != null ? 1 : 0);
        if (provided != 1) {
            return ResponseEntity.badRequest()
                    .body("Exactly one of cvStatus or carStatus must be provided.");
        }
        return employeeService.updateEmployeeStatus(id, cvStatus, carStatus)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(
        summary = "List all employees",
        description = "Retrieves a list of all employees in the system with their ID, full name, and current onboarding status. This provides a quick overview of all employees and their onboarding progress. Use this endpoint to get a summary view before drilling down into specific employee details using GET /api/employee/{id}."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of employees retrieved successfully"
        )
    })
    public ResponseEntity<List<Map<String, String>>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @PostMapping("/{employeeId}/assign-car/{carId}")
    @Operation(
        summary = "Assign a car to an employee",
        description = "Assigns a specific car to an employee and automatically updates both the employee's car provisioning status and the car's assignment status. The employee's carStatus will be set to true, and the car will be marked as assigned with the employee's information. The system will validate that the car is available before assignment and will automatically recalculate the employee's onboarding status."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Car successfully assigned to employee",
            content = @Content(schema = @Schema(implementation = Employee.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Employee or car not found with the specified IDs"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Car is not available for assignment (already assigned or in maintenance)"
        )
    })
    public ResponseEntity<Employee> assignCarToEmployee(
            @Parameter(description = "Unique identifier of the employee", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String employeeId,
            @Parameter(description = "Unique identifier of the car to assign", required = true, example = "car-123e4567")
            @PathVariable String carId) {
        return employeeService.assignCarToEmployee(employeeId, carId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{employeeId}/unassign-car")
    @Operation(
        summary = "Unassign car from an employee",
        description = "Removes the car assignment from an employee and automatically updates both the employee's car provisioning status and the car's availability status. The employee's carStatus will be set to false, assignedCarId will be cleared, and the car will be marked as available again. The system will automatically recalculate the employee's onboarding status."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Car successfully unassigned from employee",
            content = @Content(schema = @Schema(implementation = Employee.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Employee not found or employee has no car assigned"
        )
    })
    public ResponseEntity<Employee> unassignCarFromEmployee(
            @Parameter(description = "Unique identifier of the employee", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String employeeId) {
        return employeeService.unassignCarFromEmployee(employeeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reload")
    @Operation(
        summary = "Reload employees from CSV",
        description = "Reloads all employee data from the CSV file (resources/data/employees.csv). This will clear all existing employees and load fresh data from the file. Useful for resetting the system to initial state or loading updated CSV data without restarting the application."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Employees successfully reloaded from CSV"
        )
    })
    public ResponseEntity<Map<String, String>> reloadEmployees() {
        employeeService.loadEmployeesFromCSV();
        Map<String, String> response = Map.of(
            "message", "Employees reloaded successfully from CSV",
            "status", "success"
        );
        return ResponseEntity.ok(response);
    }
}


