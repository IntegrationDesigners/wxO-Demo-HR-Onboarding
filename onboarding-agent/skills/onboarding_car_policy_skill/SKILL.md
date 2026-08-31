---
name: onboarding-car-policy-skill
description: Determines the car price range an employee is entitled to based on their salary bracket, using the company car policy. Use when assigning a car to an employee to filter the list of eligible cars before presenting options to the user.
allowed-tools:
  - onboarding_getAvailableCars
---

# Car Policy Lookup and Eligibility Filtering

## Procedure

1. After retrieving the employee record (including salary), query the `Onboarding_Car_Policy` knowledge base to determine the maximum car price range allowed for the employee's salary bracket. Do not reveal the salary or the internal bracket details to the user.
2. Call `onboarding_getAvailableCars` to retrieve the list of currently available cars. This step is mandatory — never present any car options without first calling this tool.
3. Filter the returned cars to those whose price range is at or below the employee's allowed price range. Employees may choose a car cheaper than their bracket allows.
4. If the filtered list is empty, inform the user that no eligible cars are available and stop the flow.
5. Present only the filtered cars as a numbered list showing: make/model, license plate, and price range. Do not invent or include any car not returned by `onboarding_getAvailableCars`.
6. Wait for the user to select a car before proceeding. Do not make a selection on the user's behalf.
