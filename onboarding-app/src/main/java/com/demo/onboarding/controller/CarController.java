package com.demo.onboarding.controller;

import com.demo.onboarding.model.Car;
import com.demo.onboarding.service.CarService;
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

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing company car fleet operations.
 * Provides endpoints for viewing car availability, details, and updating car status.
 */
@RestController
@RequestMapping("/api/car")
@Tag(name = "Car Fleet Management", description = "APIs for managing the company car fleet including availability tracking, assignment status, and car details across different price ranges")
public class CarController {

    @Autowired
    private CarService carService;

    @GetMapping
    @Operation(
        summary = "List all cars",
        description = "Retrieves a complete list of all cars in the company fleet regardless of their availability or assignment status. Each car includes details such as make/model, license plate, price range class (1-4), current status, and assignment information if applicable. Use this endpoint to get a comprehensive overview of the entire fleet."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of all cars retrieved successfully",
            content = @Content(schema = @Schema(implementation = Car.class))
        )
    })
    public ResponseEntity<List<Car>> getAllCars() {
        return ResponseEntity.ok(carService.getAllCars());
    }

    @GetMapping("/available")
    @Operation(
        summary = "List all available cars",
        description = "Retrieves a filtered list of cars that are currently available for assignment to employees. A car is considered available when its status is 'Available' and it is not assigned to any employee. This endpoint is useful for HR staff when provisioning cars during the employee onboarding process. Cars in maintenance, reserved, or already assigned are excluded from this list."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of available cars retrieved successfully",
            content = @Content(schema = @Schema(implementation = Car.class))
        )
    })
    public ResponseEntity<List<Car>> getAvailableCars() {
        return ResponseEntity.ok(carService.getAvailableCars());
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get car details",
        description = "Retrieves detailed information about a specific car including its name (make/model), license plate number, availability status, assigned employee information (if any), price range class, and current operational status. The price range indicates the car class: 1=Economy, 2=Mid-range, 3=Premium, 4=Luxury. This endpoint provides all information needed to understand a car's current state and assignment."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Car details retrieved successfully",
            content = @Content(schema = @Schema(implementation = Car.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Car not found with the specified ID"
        )
    })
    public ResponseEntity<Car> getCarById(
            @Parameter(description = "Unique identifier of the car", required = true, example = "car-123e4567")
            @PathVariable String id) {
        return carService.getCarById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}")
    @Operation(
        summary = "Update car status",
        description = "Updates the operational status of a specific car. Allowed status values are: 'Available' (ready for assignment), 'In Maintenance' (undergoing repairs or service), and 'Reserved' (held for future assignment). Setting status to 'Available' is only permitted if the car is not currently assigned to an employee."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Car status updated successfully",
            content = @Content(schema = @Schema(implementation = Car.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Car not found with the specified ID"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid status value provided - must be one of: Available, In Maintenance, Reserved. Setting Available is not allowed when the car is assigned to an employee."
        )
    })
    public ResponseEntity<?> updateCarStatus(
            @Parameter(description = "Unique identifier of the car", required = true, example = "car-123e4567")
            @PathVariable String id,
            @Parameter(description = "New status for the car", required = true, example = "In Maintenance",
                      schema = @Schema(allowableValues = {"Available", "In Maintenance", "Reserved"}))
            @RequestParam String status) {
        try {
            return carService.updateCarStatus(id, status)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reload")
    @Operation(
        summary = "Reload cars from CSV",
        description = "Reloads all car fleet data from the CSV file (resources/data/cars.csv). This will clear all existing cars and load fresh data from the file. If the CSV file is not found, it will fall back to loading default hardcoded car data. Useful for resetting the system to initial state or loading updated CSV data without restarting the application."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cars successfully reloaded from CSV"
        )
    })
    public ResponseEntity<Map<String, String>> reloadCars() {
        carService.initializeCarFleet();
        Map<String, String> response = Map.of(
            "message", "Cars reloaded successfully from CSV",
            "status", "success"
        );
        return ResponseEntity.ok(response);
    }
}


