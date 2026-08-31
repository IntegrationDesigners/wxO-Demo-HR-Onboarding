---
name: onboarding-car-unassignment-skill
description: Unassigns (removes) a car from an employee during onboarding. Use when the user wants to unassign, remove, or return a car from an employee.
allowed-tools:
  - onboarding_unassignCarFromEmployee
---

# Car Unassignment

## Procedure

1. Use the `onboarding_employee_lookup_skill` to resolve the employee name to an employee ID and retrieve their full record. Do not proceed without a confirmed employee ID.
2. Ask for explicit confirmation: "Shall I unassign the car from [employee name]?" and wait for the user's approval before proceeding.
3. On confirmation, call `onboarding_unassignCarFromEmployee` with the resolved employee ID.
4. If the unassignment succeeds, use the `onboarding_slack_notification_skill` to send a Slack notification confirming the car was unassigned from the employee.
5. If any step fails or the user cancels, inform the user and stop without making the unassignment call.

## Constraints

- Never call `onboarding_unassignCarFromEmployee` without prior explicit user confirmation in step 2.
- Never expose salary or other internal financial data to the user at any point.
