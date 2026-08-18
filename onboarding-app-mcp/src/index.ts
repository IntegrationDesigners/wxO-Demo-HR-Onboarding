#!/usr/bin/env node
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { isInitializeRequest } from "@modelcontextprotocol/sdk/types.js";
import express, { Request, Response } from "express";
import { randomUUID } from "crypto";
import { z } from "zod";

const BASE_URL =
  process.env.ONBOARDING_API_URL ??
  "http://127.0.0.1:8080";

const PORT = parseInt(process.env.PORT ?? "8888", 10);

// ─── Helpers ──────────────────────────────────────────────────────────────────

async function apiFetch(
  path: string,
  init: RequestInit = {}
): Promise<{ ok: boolean; status: number; body: unknown }> {
  const url = `${BASE_URL}${path}`;
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    ...init,
  });

  let body: unknown;
  const ct = res.headers.get("content-type") ?? "";
  if (ct.includes("application/json")) {
    body = await res.json();
  } else {
    body = await res.text();
  }

  return { ok: res.ok, status: res.status, body };
}

function toResult(result: {
  ok: boolean;
  status: number;
  body: unknown;
}): { content: [{ type: "text"; text: string }]; isError?: boolean } {
  console.log(result)
  const text =
    typeof result.body === "string"
      ? result.body
      : JSON.stringify(result.body, null, 2);

  if (!result.ok) {
    return {
      content: [{ type: "text", text: `Error ${result.status}: ${text}` }],
      isError: true,
    };
  }

  return { content: [{ type: "text", text }] };
}

// ─── Output schemas ───────────────────────────────────────────────────────────

const employeeSchema = z.object({
  id: z.string().describe("Unique identifier of the employee (UUID format)"),
  givenName: z.string().describe("Employee's first name"),
  name: z.string().describe("Employee's family name (last name)"),
  startDate: z.string().describe("Employee's start date (YYYY-MM-DD)"),
  gender: z.enum(["Male", "Female", "Other"]).describe("Employee's gender"),
  salary: z.number().describe("Employee's annual salary"),
  cvStatus: z.boolean().describe("Whether the employee's CV has been created"),
  carStatus: z.boolean().describe("Whether a car has been provisioned"),
  assignedCarId: z
    .string()
    .nullable()
    .describe("ID of the assigned car, or null if no car is assigned"),
  onboardingStatus: z
    .enum(["Completed", "In Progress", "Error"])
    .describe("Overall onboarding status"),
});

const carSchema = z.object({
  id: z.string().describe("Unique identifier of the car"),
  make: z.string().describe("Car manufacturer (e.g. Toyota, BMW)"),
  model: z.string().describe("Car model name"),
  licensePlate: z.string().describe("Car license plate number"),
  priceRangeClass: z
    .number()
    .int()
    .min(1)
    .max(4)
    .describe("Price range class: 1=Economy, 2=Mid-range, 3=Premium, 4=Luxury"),
  status: z
    .enum(["Available", "Assigned", "In Maintenance", "Reserved"])
    .describe("Current operational status of the car"),
  assignedEmployeeId: z
    .string()
    .nullable()
    .describe("ID of the employee the car is assigned to, or null"),
});

const healthSchema = z.object({
  status: z.enum(["UP", "DOWN"]).describe("Application health status"),
});

// ─── MCP server factory ───────────────────────────────────────────────────────

function createMcpServer(): McpServer {
  const server = new McpServer({ name: "onboarding-app-mcp", version: "0.1.0" });

  // ─── Employee Management tools ──────────────────────────────────────────────

  server.registerTool(
    "list_employees",
    {
      description:
        "List all employees with their ID, full name, and current onboarding status (Completed / In Progress / Error).",
      inputSchema: z.object({}),
      // outputSchema: z.array(employeeSchema),
    },
    async () => toResult(await apiFetch("/api/employee"))
  );

  server.registerTool(
    "get_employee",
    {
      description:
        "Get detailed onboarding status for a specific employee including start date, CV status, car provisioning status, and overall onboarding status.",
      inputSchema: z.object({
        id: z
          .string()
          .describe("Unique identifier of the employee (UUID format)"),
      }),
      outputSchema: employeeSchema,
    },
    async ({ id }) => toResult(await apiFetch(`/api/employee/${id}`))
  );

  server.registerTool(
    "create_employee",
    {
      description:
        "Create a new employee record in the onboarding system. The employee is initialized with CV and car status set to false.",
      inputSchema: z.object({
        givenName: z.string().describe("Employee's first name"),
        name: z.string().describe("Employee's family name (last name)"),
        startDate: z
          .string()
          .describe("Employee's start date in ISO format (YYYY-MM-DD)"),
        gender: z
          .enum(["Male", "Female", "Other"])
          .describe("Employee's gender"),
        salary: z
          .number()
          .positive()
          .describe("Employee's annual salary (must be > 0)"),
      }),
      outputSchema: employeeSchema,
    },
    async (body) => {
      const result = await apiFetch("/api/employee", {
        method: "POST",
        body: JSON.stringify(body),
      });
      return toResult(result);
    }
  );

  server.registerTool(
    "update_cv_status",
    {
      description:
        "Update the CV creation status of a specific employee. " +
        "Set cvStatus to true when the employee's CV has been created, or false to mark it as not yet done. " +
        "The overall onboarding status is automatically recalculated after the update.",
      inputSchema: z.object({
        id: z.string().describe("Unique identifier of the employee"),
        cvStatus: z
          .boolean()
          .describe("CV creation status: true when CV has been created, false otherwise"),
      }),
      outputSchema: employeeSchema,
    },
    async ({ id, cvStatus }) => {
      const result = await apiFetch(
        `/api/employee/${id}/cv-status?cvStatus=${cvStatus}`,
        { method: "PATCH" }
      );
      return toResult(result);
    }
  );

  server.registerTool(
    "update_employee_status",
    {
      description:
        "Update the onboarding status of an employee by setting cvStatus and/or carStatus. " +
        "Exactly one of cvStatus or carStatus must be provided. " +
        "The system recalculates overall status: Completed (both true + valid car assignment), " +
        "Error (carStatus=true but no car assigned), or In Progress.",
      inputSchema: z.object({
        id: z.string().describe("Unique identifier of the employee"),
        cvStatus: z
          .boolean()
          .optional()
          .describe("Set to true when CV has been created"),
        carStatus: z
          .boolean()
          .optional()
          .describe("Set to true when a car has been provisioned"),
      }),
      outputSchema: employeeSchema,
    },
    async ({ id, cvStatus, carStatus }) => {
      const params = new URLSearchParams();
      if (cvStatus !== undefined) params.set("cvStatus", String(cvStatus));
      if (carStatus !== undefined) params.set("carStatus", String(carStatus));
      const qs = params.toString() ? `?${params.toString()}` : "";
      return toResult(
        await apiFetch(`/api/employee/${id}${qs}`, { method: "PUT" })
      );
    }
  );

  server.registerTool(
    "assign_car_to_employee",
    {
      description:
        "Assign a specific car to an employee. Validates that the car is available, sets carStatus=true on the employee, " +
        "marks the car as Assigned, and recalculates the employee's onboarding status.",
      inputSchema: z.object({
        employeeId: z.string().describe("Unique identifier of the employee"),
        carId: z
          .string()
          .describe("Unique identifier of the car to assign (e.g. car-123e4567)"),
      }),
      outputSchema: employeeSchema,
    },
    async ({ employeeId, carId }) => {
      const result = await apiFetch(
        `/api/employee/${employeeId}/assign-car/${carId}`,
        { method: "POST" }
      );
      return toResult(result);
    }
  );

  server.registerTool(
    "unassign_car_from_employee",
    {
      description:
        "Remove the car assignment from an employee. Sets carStatus=false, clears assignedCarId, " +
        "and marks the car as Available again. Recalculates onboarding status.",
      inputSchema: z.object({
        employeeId: z.string().describe("Unique identifier of the employee"),
      }),
      outputSchema: employeeSchema,
    },
    async ({ employeeId }) => {
      const result = await apiFetch(
        `/api/employee/${employeeId}/unassign-car`,
        { method: "DELETE" }
      );
      return toResult(result);
    }
  );

  server.registerTool(
    "reload_employees",
    {
      description:
        "Reset all employee data by reloading from the CSV file (resources/data/employees.csv). " +
        "This clears all current employee records and restores the initial dataset.",
      inputSchema: z.object({}),
      // outputSchema: z.array(employeeSchema),
    },
    async () => {
      const result = await apiFetch("/api/employee/reload", { method: "POST" });
      return toResult(result);
    }
  );

  // ─── Car Fleet Management tools ─────────────────────────────────────────────

  server.registerTool(
    "list_cars",
    {
      description:
        "List all cars in the company fleet (all statuses: Available, Assigned, In Maintenance, Reserved). " +
        "Includes make/model, license plate, price range class (1=Economy, 2=Mid-range, 3=Premium, 4=Luxury), " +
        "status, and assignment information.",
      inputSchema: z.object({}),
      // outputSchema: z.array(carSchema),
    },
    async () => toResult(await apiFetch("/api/car"))
  );

  server.registerTool(
    "list_available_cars",
    {
      description:
        "List only cars that are currently available for assignment to employees. " +
        "Excludes cars that are Assigned, In Maintenance, or Reserved.",
      inputSchema: z.object({}),
      // outputSchema: z.array(carSchema),
    },
    async () => toResult(await apiFetch("/api/car/available"))
  );

  server.registerTool(
    "get_car",
    {
      description:
        "Get detailed information about a specific car including make/model, license plate, " +
        "availability status, assigned employee (if any), price range class, and operational status.",
      inputSchema: z.object({
        id: z.string().describe("Unique identifier of the car (e.g. car-123e4567)"),
      }),
      outputSchema: carSchema,
    },
    async ({ id }) => toResult(await apiFetch(`/api/car/${id}`))
  );

  server.registerTool(
    "update_car_status",
    {
      description:
        "Update the operational status of a specific car. " +
        "Allowed values: Available, In Maintenance, Reserved. " +
        "Setting status to Available is only permitted when the car is not currently assigned to an employee. " +
        "To mark a car as Assigned, use the assign_car_to_employee tool instead.",
      inputSchema: z.object({
        id: z.string().describe("Unique identifier of the car"),
        status: z
          .enum(["Available", "In Maintenance", "Reserved"])
          .describe("New operational status for the car"),
      }),
      outputSchema: carSchema,
    },
    async ({ id, status }) => {
      const result = await apiFetch(
        `/api/car/${id}?status=${encodeURIComponent(status)}`,
        { method: "POST" }
      );
      return toResult(result);
    }
  );

  server.registerTool(
    "reload_cars",
    {
      description:
        "Reset all car fleet data by reloading from the CSV file (resources/data/cars.csv). " +
        "Falls back to default hardcoded data if CSV is not found. " +
        "This clears all current car records and restores the initial fleet.",
      inputSchema: z.object({}),
      // outputSchema: z.array(carSchema),
    },
    async () => {
      const result = await apiFetch("/api/car/reload", { method: "POST" });
      return toResult(result);
    }
  );

  // ─── Health & Monitoring tools ───────────────────────────────────────────────

  server.registerTool(
    "health_check",
    {
      description:
        "Check the health status of the Employee Onboarding application.",
      inputSchema: z.object({}),
      outputSchema: healthSchema,
    },
    async () => toResult(await apiFetch("/actuator/health"))
  );

  return server;
}

// ─── Main ─────────────────────────────────────────────────────────────────────

async function main() {
  const app = express();
  app.use(express.json());

  // Map of session ID → transport (stateful mode)
  const transports = new Map<string, StreamableHTTPServerTransport>();

  app.post("/mcp", async (req: Request, res: Response) => {
    const sessionId = req.headers["mcp-session-id"] as string | undefined;

    // Reuse existing session transport
    if (sessionId && transports.has(sessionId)) {
      const transport = transports.get(sessionId)!;
      await transport.handleRequest(req, res, req.body);
      return;
    }

    // New session: only accept if this is an initialize request
    if (!isInitializeRequest(req.body)) {
      res.status(400).json({ error: "No active session. Send an initialize request first." });
      return;
    }

    const newSessionId = randomUUID();
    const transport = new StreamableHTTPServerTransport({
      sessionIdGenerator: () => newSessionId,
      onsessioninitialized: (id) => {
        transports.set(id, transport);
      },
    });

    transport.onclose = () => {
      transports.delete(newSessionId);
    };

    const server = createMcpServer();
    await server.connect(transport);
    await transport.handleRequest(req, res, req.body);
  });

  // GET /mcp — SSE stream for server-to-client notifications
  app.get("/mcp", async (req: Request, res: Response) => {
    const sessionId = req.headers["mcp-session-id"] as string | undefined;
    if (!sessionId || !transports.has(sessionId)) {
      res.status(400).json({ error: "No active session." });
      return;
    }
    const transport = transports.get(sessionId)!;
    await transport.handleRequest(req, res);
  });

  // DELETE /mcp — explicit session termination
  app.delete("/mcp", async (req: Request, res: Response) => {
    const sessionId = req.headers["mcp-session-id"] as string | undefined;
    if (!sessionId || !transports.has(sessionId)) {
      res.status(400).json({ error: "No active session." });
      return;
    }
    const transport = transports.get(sessionId)!;
    await transport.handleRequest(req, res);
    transports.delete(sessionId);
  });

  app.listen(PORT, () => {
    console.log(
      `onboarding-app-mcp-http listening on http://0.0.0.0:${PORT}/mcp (API: ${BASE_URL})`
    );
  });
}

main().catch((error) => {
  console.error("Fatal error:", error);
  process.exit(1);
});
