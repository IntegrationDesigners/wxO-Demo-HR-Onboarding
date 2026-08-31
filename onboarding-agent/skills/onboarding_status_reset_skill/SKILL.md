---
name: onboarding-status-reset-skill
description: Resets (clears) an employee's CV creation status or car provisioning status in the onboarding system. Use when asked to clear, reset, or undo a CV status or car provisioning status for an employee.
allowed-tools:
  - onboarding_updateEmployeeStatus
  - onboarding_getAllEmployees
---

# Onboarding Status Reset

## Rules

- `onboarding_updateEmployeeStatus` is used **exclusively** to reset (clear) a status field.
- Always pass exactly **one** flag set to `false`: either `cvStatus=false` **or** `carStatus=false` — never both at once, never `true`.

## Procedure — Reset CV status

When asked to clear, reset, or undo an employee's CV status:

1. If only an employee name was provided, call `onboarding_getAllEmployees` to resolve their ID.
2. Call `onboarding_updateEmployeeStatus` with the employee ID and `cvStatus=false`.
3. After a successful reset, use the `onboarding_slack_notification_skill` to send a Slack notification confirming that the CV status has been reset for the employee.

## Procedure — Reset car provisioning status

When asked to clear, reset, or undo an employee's car provisioning status:

1. If only an employee name was provided, call `onboarding_getAllEmployees` to resolve their ID.
2. Call `onboarding_updateEmployeeStatus` with the employee ID and `carStatus=false`.
3. After a successful reset, use the `onboarding_slack_notification_skill` to send a Slack notification confirming that the car provisioning status has been reset for the employee.
