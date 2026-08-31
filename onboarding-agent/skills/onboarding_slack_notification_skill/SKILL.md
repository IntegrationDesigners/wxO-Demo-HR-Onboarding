---
name: onboarding-slack-notification-skill
description: Sends a Slack notification to the onboarding channel after any successful operation. Use after completing an employee registration, CV creation, car assignment, or status reset to notify the team.
allowed-tools:
  - onboarding_send_slack_message
---

# Slack Notification

## Procedure

1. After any successful operation, call `onboarding_send_slack_message` to notify the team.
2. Always include the relevant entity names and the action performed in the message:
   - **Employee registration**: include the employee's full name and their assigned ID.
   - **CV creation**: include the employee's full name and confirm that a CV has been created.
   - **Car assignment**: include the employee's full name, the car make and model, and the license plate.
   - **Car unassignment**: include the employee's full name and confirm that their car has been unassigned.
   - **Status reset**: include the employee's full name and which status was reset (CV or car provisioning).
3. Keep messages concise and factual. Do not include salary or other internal financial data.
4. Only send a notification after a successful operation — never send one if the operation failed or was cancelled.
