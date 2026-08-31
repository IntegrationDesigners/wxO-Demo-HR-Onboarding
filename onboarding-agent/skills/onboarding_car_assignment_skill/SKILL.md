---
name: onboarding-car-assignment-skill
description: Assigns a car to an employee during onboarding. Use when the user wants to assign a car to an employee.
allowed-tools:
  - onboarding_getAvailableCars
  - onboarding_assignCarToEmployee
---

# Car Assignment

## Procedure

1. Use the `onboarding_employee_lookup_skill` to resolve the employee name to an employee ID and retrieve their full record, including their salary. Do not proceed without a confirmed employee ID.
2. Query the `Onboarding_Car_Policy` knowledge base using the employee's salary to determine the **maximum car price range** the employee is entitled to. Retain this value internally — do not reveal the salary or the price bracket to the user.
3. Call `onboarding_getAvailableCars` to retrieve all currently available cars. Never present car options without calling this tool first.
4. Filter the result to cars whose price range is **at or below** the employee's allowed price range. Employees may choose a cheaper car than their bracket allows.
5. If no cars remain after filtering, inform the user that no eligible cars are currently available and stop.
6. Present the filtered cars as a numbered list showing: make/model, license plate, and price range. Ask the user to pick one.
7. Confirm the selection with the user: *"Shall I assign [make/model] (licence plate [plate]) to [employee name]?"* — wait for explicit approval before continuing.
8. On approval, call `onboarding_assignCarToEmployee` with the employee ID and the selected car ID.
9. On success, use the `onboarding_slack_notification_skill` to notify the team with the employee's full name, car make/model, and licence plate.

## Error handling

- If a tool call fails at any step, briefly describe what failed and suggest the user try again.
- If the user cancels or declines confirmation at step 7, acknowledge the cancellation and stop without calling `onboarding_assignCarToEmployee`.

## Constraints

- Never expose the employee's salary, price bracket, or any other internal financial data to the user.
- Never call `onboarding_assignCarToEmployee` without explicit user confirmation (step 7).
- Never present or suggest a car that was not returned by `onboarding_getAvailableCars`.
- Never select a car on the user's behalf.
