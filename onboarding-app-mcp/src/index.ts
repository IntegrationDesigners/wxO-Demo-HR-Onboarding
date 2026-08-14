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

// ─── helpers ─────────────────────────────────────────────────────────────────

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

function toText(result: { ok: boolean; status: number; body: unknown }): {
  content: [{ type: "text"; text: string }];
  isError?: boolean;
} {
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
    },
    async () => toText(await apiFetch("/api/employee"))
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
    },
    async ({ id }) => toText(await apiFetch(`/api/employee/${id}`))
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
    },
    async (body) =>
      toText(
        await apiFetch("/api/employee", {
          method: "POST",
          body: JSON.stringify(body),
        })
      )
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
    },
    async ({ id, cvStatus }) =>
      toText(
        await apiFetch(
          `/api/employee/${id}/cv-status?cvStatus=${cvStatus}`,
          { method: "PATCH" }
        )
      )
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
    },
    async ({ id, cvStatus, carStatus }) => {
      const params = new URLSearchParams();
      if (cvStatus !== undefined) params.set("cvStatus", String(cvStatus));
      if (carStatus !== undefined) params.set("carStatus", String(carStatus));
      const qs = params.toString() ? `?${params.toString()}` : "";
      return toText(
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
    },
    async ({ employeeId, carId }) =>
      toText(
        await apiFetch(`/api/employee/${employeeId}/assign-car/${carId}`, {
          method: "POST",
        })
      )
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
    },
    async ({ employeeId }) =>
      toText(
        await apiFetch(`/api/employee/${employeeId}/unassign-car`, {
          method: "DELETE",
        })
      )
  );

  server.registerTool(
    "reload_employees",
    {
      description:
        "Reset all employee data by reloading from the CSV file (resources/data/employees.csv). " +
        "This clears all current employee records and restores the initial dataset.",
      inputSchema: z.object({}),
    },
    async () =>
      toText(await apiFetch("/api/employee/reload", { method: "POST" }))
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
    },
    async () => toText(await apiFetch("/api/car"))
  );

  server.registerTool(
    "list_available_cars",
    {
      description:
        "List only cars that are currently available for assignment to employees. " +
        "Excludes cars that are Assigned, In Maintenance, or Reserved.",
      inputSchema: z.object({}),
    },
    async () => toText(await apiFetch("/api/car/available"))
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
    },
    async ({ id }) => toText(await apiFetch(`/api/car/${id}`))
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
    },
    async ({ id, status }) =>
      toText(
        await apiFetch(`/api/car/${id}?status=${encodeURIComponent(status)}`, {
          method: "POST",
        })
      )
  );

  server.registerTool(
    "reload_cars",
    {
      description:
        "Reset all car fleet data by reloading from the CSV file (resources/data/cars.csv). " +
        "Falls back to default hardcoded data if CSV is not found. " +
        "This clears all current car records and restores the initial fleet.",
      inputSchema: z.object({}),
    },
    async () => toText(await apiFetch("/api/car/reload", { method: "POST" }))
  );

  // ─── Health & Monitoring tools ───────────────────────────────────────────────

  server.registerTool(
    "health_check",
    {
      description:
        "Check the health status of the Employee Onboarding application. " +
        "Returns UP when the application is healthy, 503 when unhealthy.",
      inputSchema: z.object({}),
    },
    async () => toText(await apiFetch("/actuator/health"))
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
