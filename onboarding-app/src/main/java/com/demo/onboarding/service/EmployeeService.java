package com.demo.onboarding.service;

import com.demo.onboarding.model.Car;
import com.demo.onboarding.model.Employee;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service layer for managing employee onboarding operations.
 * Provides business logic for creating, retrieving, and updating employee records.
 */
@Service
public class EmployeeService {

    private final Map<String, Employee> employees = new ConcurrentHashMap<>();
    
    @Autowired
    private CarService carService;

    /**
     * Initializes employees from CSV file on application startup.
     * Loads employee data from resources/data/employees.csv if it exists.
     */
    @PostConstruct
    public void init() {
        loadEmployeesFromCSV();
    }

    /**
     * Loads or reloads employees from CSV file.
     * Clears existing employees and loads fresh data from resources/data/employees.csv.
     * Can be called at runtime to reload data.
     */
    public void loadEmployeesFromCSV() {
        employees.clear();
        try {
            ClassPathResource resource = new ClassPathResource("data/employees.csv");
            if (!resource.exists()) {
                System.out.println("No employees.csv found, skipping CSV import");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                         .withFirstRecordAsHeader()
                         .withIgnoreHeaderCase()
                         .withTrim())) {

                for (CSVRecord record : csvParser) {
                    Employee employee = Employee.builder()
                            .givenName(record.get("givenName"))
                            .name(record.get("name"))
                            .startDate(LocalDate.parse(record.get("startDate")))
                            .gender(record.get("gender"))
                            .salary(new BigDecimal(record.get("salary")))
                            .cvStatus(Boolean.parseBoolean(record.get("cvStatus")))
                            .carStatus(Boolean.parseBoolean(record.get("carStatus")))
                            .build();
                    
                    employee.initialize();
                    employees.put(employee.getId(), employee);
                }
                
                System.out.println("Loaded " + employees.size() + " employees from CSV");
            }
        } catch (Exception e) {
            System.err.println("Error loading employees from CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates a new employee in the system.
     * Initializes the employee with a unique ID and default onboarding status.
     * 
     * @param employee The employee to create
     * @return The created employee with generated ID
     */
    public Employee createEmployee(Employee employee) {
        employee.initialize();
        employees.put(employee.getId(), employee);
        return employee;
    }

    /**
     * Retrieves an employee by their unique ID.
     * 
     * @param id The employee ID
     * @return Optional containing the employee if found
     */
    public Optional<Employee> getEmployeeById(String id) {
        return Optional.ofNullable(employees.get(id));
    }

    /**
     * Retrieves all employees in the system.
     * Returns a simplified list containing only ID and name for overview purposes.
     *
     * @return List of all employees
     */
    public List<Map<String, String>> getAllEmployees() {
        return employees.values().stream()
                .map(emp -> {
                    Map<String, String> summary = new HashMap<>();
                    summary.put("id", emp.getId());
                    summary.put("name", emp.getGivenName() + " " + emp.getName());
                    summary.put("onboardingStatus", emp.getOnboardingStatus());
                    return summary;
                })
                .collect(Collectors.toList());
    }

    /**
     * Updates an employee's onboarding status.
     * Automatically recalculates the overall onboarding completion status.
     * 
     * @param id The employee ID
     * @param cvStatus The CV creation status
     * @param carStatus The car provisioning status
     * @return Optional containing the updated employee if found
     */
    public Optional<Employee> updateEmployeeStatus(String id, Boolean cvStatus, Boolean carStatus) {
        Employee employee = employees.get(id);
        if (employee == null) {
            return Optional.empty();
        }

        if (cvStatus != null) {
            employee.setCvStatus(cvStatus);
        }
        if (carStatus != null) {
            employee.setCarStatus(carStatus);
        }

        employee.updateOnboardingStatus();
        return Optional.of(employee);
    }

    /**
     * Assigns a car to an employee and updates their car provisioning status.
     * Coordinates with CarService to ensure the car is available and properly assigned.
     *
     * @param employeeId The employee ID
     * @param carId The car ID to assign
     * @return Optional containing the updated employee if found and car is available
     */
    public Optional<Employee> assignCarToEmployee(String employeeId, String carId) {
        Employee employee = employees.get(employeeId);
        if (employee == null) {
            return Optional.empty();
        }

        // Attempt to assign the car through CarService
        Optional<Car> assignedCar = carService.assignCarToEmployee(carId, employee);
        if (assignedCar.isEmpty()) {
            // Car not found or not available
            return Optional.empty();
        }

        // Update employee with car assignment
        employee.setAssignedCarId(carId);
        employee.setCarStatus(true);
        employee.updateOnboardingStatus();
        return Optional.of(employee);
    }

    /**
     * Removes car assignment from an employee.
     * Coordinates with CarService to ensure the car is properly unassigned and made available.
     *
     * @param employeeId The employee ID
     * @return Optional containing the updated employee if found and has a car assigned
     */
    public Optional<Employee> unassignCarFromEmployee(String employeeId) {
        Employee employee = employees.get(employeeId);
        if (employee == null || employee.getAssignedCarId() == null) {
            return Optional.empty();
        }

        // Unassign the car through CarService
        String carId = employee.getAssignedCarId();
        carService.unassignCar(carId);

        // Update employee to remove car assignment
        employee.setAssignedCarId(null);
        employee.setCarStatus(false);
        employee.updateOnboardingStatus();
        return Optional.of(employee);
    }

    /**
     * Checks if an employee exists in the system.
     * 
     * @param id The employee ID
     * @return true if employee exists, false otherwise
     */
    public boolean employeeExists(String id) {
        return employees.containsKey(id);
    }
}


