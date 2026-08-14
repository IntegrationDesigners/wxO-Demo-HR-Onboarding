package com.demo.onboarding.service;

import com.demo.onboarding.model.Car;
import com.demo.onboarding.model.Employee;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service layer for managing car fleet operations.
 * Provides business logic for car availability, assignment, and status management.
 */
@Service
public class CarService {

    private final Map<String, Car> cars = new ConcurrentHashMap<>();

    /**
     * Initializes the car fleet from CSV file on application startup.
     * Loads car data from resources/data/cars.csv if it exists.
     * Falls back to hardcoded data if CSV file is not found.
     */
    @PostConstruct
    public void init() {
        initializeCarFleet();
    }

    /**
     * Loads or reloads the car fleet from CSV file.
     * Clears existing cars and loads fresh data from resources/data/cars.csv.
     * Falls back to hardcoded data if CSV file is not found.
     * Can be called at runtime to reload data.
     */
    public void initializeCarFleet() {
        cars.clear();
        try {
            ClassPathResource resource = new ClassPathResource("data/cars.csv");
            if (!resource.exists()) {
                System.out.println("No cars.csv found, using default car fleet");
                loadDefaultCars();
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                         .withFirstRecordAsHeader()
                         .withIgnoreHeaderCase()
                         .withTrim())) {

                for (CSVRecord record : csvParser) {
                    Car car = Car.builder()
                            .name(record.get("name"))
                            .licensePlate(record.get("licensePlate"))
                            .priceRange(Integer.parseInt(record.get("priceRange")))
                            .status(record.get("status"))
                            .build();
                    
                    car.initialize();
                    cars.put(car.getId(), car);
                }
                
                System.out.println("Loaded " + cars.size() + " cars from CSV");
            }
        } catch (Exception e) {
            System.err.println("Error loading cars from CSV: " + e.getMessage());
            e.printStackTrace();
            System.out.println("Falling back to default car fleet");
            loadDefaultCars();
        }
    }

    /**
     * Loads default car fleet when CSV is not available.
     */
    private void loadDefaultCars() {
        // Economy cars (Class 1)
        createCar("Toyota Corolla", "1-ABC-123", 1, "Available");
        createCar("Honda Civic", "1-DEF-456", 1, "Available");
        createCar("Volkswagen Golf", "1-GHI-789", 1, "Available");

        // Mid-range cars (Class 2)
        createCar("BMW 3 Series", "2-JKL-012", 2, "Available");
        createCar("Audi A4", "2-MNO-345", 2, "Available");
        createCar("Mercedes C-Class", "2-PQR-678", 2, "Available");

        // Premium cars (Class 3)
        createCar("BMW 5 Series", "3-STU-901", 3, "Available");
        createCar("Audi A6", "3-VWX-234", 3, "Available");
        createCar("Mercedes E-Class", "3-YZA-567", 3, "In Maintenance");

        // Luxury cars (Class 4)
        createCar("BMW 7 Series", "4-BCD-890", 4, "Available");
        createCar("Audi A8", "4-EFG-123", 4, "Available");
        createCar("Mercedes S-Class", "4-HIJ-456", 4, "Reserved");
    }

    /**
     * Creates and adds a new car to the fleet.
     * 
     * @param name Car make and model
     * @param licensePlate License plate number
     * @param priceRange Price range class (1-4)
     * @param status Initial status
     * @return The created car
     */
    private Car createCar(String name, String licensePlate, Integer priceRange, String status) {
        Car car = Car.builder()
                .name(name)
                .licensePlate(licensePlate)
                .priceRange(priceRange)
                .status(status)
                .build();
        car.initialize();
        cars.put(car.getId(), car);
        return car;
    }

    /**
     * Retrieves all cars in the fleet.
     * 
     * @return List of all cars
     */
    public List<Car> getAllCars() {
        return new ArrayList<>(cars.values());
    }

    /**
     * Retrieves all available cars (not assigned to any employee).
     * 
     * @return List of available cars
     */
    public List<Car> getAvailableCars() {
        return cars.values().stream()
                .filter(Car::isAvailable)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a car by its unique ID.
     * 
     * @param id The car ID
     * @return Optional containing the car if found
     */
    public Optional<Car> getCarById(String id) {
        return Optional.ofNullable(cars.get(id));
    }

    /**
     * Updates the status of a car.
     * Only "Available", "In Maintenance", and "Reserved" are accepted.
     * Setting status to "Available" is rejected if the car is currently assigned.
     *
     * @param id The car ID
     * @param status The new status
     * @return Optional containing the updated car if found
     * @throws IllegalArgumentException if the status is not allowed or the car is assigned
     */
    public Optional<Car> updateCarStatus(String id, String status) {
        if (!"Available".equals(status) && !"In Maintenance".equals(status) && !"Reserved".equals(status)) {
            throw new IllegalArgumentException("Invalid status '" + status + "'. Allowed values: Available, In Maintenance, Reserved");
        }

        Car car = cars.get(id);
        if (car == null) {
            return Optional.empty();
        }

        if ("Available".equals(status) && car.getAssignedEmployeeId() != null) {
            throw new IllegalArgumentException("Cannot set status to Available: car is currently assigned to an employee");
        }

        car.setStatus(status);
        car.setAvailable("Available".equals(status));
        return Optional.of(car);
    }

    /**
     * Assigns a car to an employee.
     * 
     * @param carId The car ID
     * @param employee The employee to assign the car to
     * @return Optional containing the updated car if found and available
     */
    public Optional<Car> assignCarToEmployee(String carId, Employee employee) {
        Car car = cars.get(carId);
        if (car == null || !car.isAvailable()) {
            return Optional.empty();
        }

        car.assignToEmployee(employee.getId(), employee.getGivenName() + " " + employee.getName());
        return Optional.of(car);
    }

    /**
     * Unassigns a car from its current employee.
     * 
     * @param carId The car ID
     * @return Optional containing the updated car if found
     */
    public Optional<Car> unassignCar(String carId) {
        Car car = cars.get(carId);
        if (car == null) {
            return Optional.empty();
        }

        car.unassign();
        return Optional.of(car);
    }

    /**
     * Finds a car assigned to a specific employee.
     * 
     * @param employeeId The employee ID
     * @return Optional containing the car if found
     */
    public Optional<Car> findCarByEmployeeId(String employeeId) {
        return cars.values().stream()
                .filter(car -> employeeId.equals(car.getAssignedEmployeeId()))
                .findFirst();
    }
}


