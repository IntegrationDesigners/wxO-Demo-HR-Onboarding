---
name: onboarding-employee-lookup-skill
description: Resolves an employee name to their system ID and retrieves full employee records. Use when a user provides an employee name but no employee ID, or when an employee record (including start date or salary) is needed before calling any other tool.
allowed-tools:
  - onboarding_getEmployee
  - onboarding_getAllEmployees
---

# Employee Lookup

## Procedure

1. When only an employee name is provided (no employee ID), call `onboarding_getAllEmployees` to retrieve the full list of employees.
2. Find the employee whose name matches the one provided. If no match is found, inform the user and stop.
3. Extract the employee ID from the matched record for use in all subsequent tool calls.
4. When a full employee record is needed (for example, to retrieve start date or salary), call `onboarding_getEmployee` with the resolved employee ID.
5. Never make up or guess employee IDs or any employee data — only use values returned directly by these tools.
