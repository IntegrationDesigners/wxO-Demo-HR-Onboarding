# wxO-Demo-HR-Onboarding

A demonstration project for **IBM watsonx Orchestrate** Integration Designers Enablement. It consists of two components:

| Component | Description | Port |
|---|---|---|
| [`onboarding-app`](onboarding-app/) | Spring Boot REST API + React dashboard for HR employee onboarding | `8080` |
| [`onboarding-app-mcp`](onboarding-app-mcp/) | MCP server (Streamable HTTP) that wraps the REST API for use with AI agents | `8888` |

---

## Repository Structure

```
wxO-Demo-HR-Onboarding/
├── onboarding-app/           # Java 11 + Spring Boot backend & React frontend
│   ├── src/
│   │   └── main/
│   │       ├── java/com/demo/onboarding/
│   │       │   ├── controller/       # EmployeeController, CarController
│   │       │   ├── service/          # EmployeeService, CarService
│   │       │   ├── model/            # Employee, Car
│   │       │   └── config/           # OpenApiConfig
│   │       └── resources/
│   │           ├── data/             # employees.csv, cars.csv (seed data)
│   │           └── static/           # React dashboard (index.html)
│   ├── openapi.yaml                  # OpenAPI 3.0 specification
│   ├── Dockerfile
│   └── pom.xml
│
└── onboarding-app-mcp/       # Node.js MCP server (TypeScript)
    ├── src/index.ts           # MCP tools + Express HTTP transport
    ├── Dockerfile
    └── package.json
```

---

## Quick Start

### 1 — Run the Onboarding API

```bash
cd onboarding-app
mvn spring-boot:run
```

| URL | Description |
|---|---|
| `http://localhost:8080` | React dashboard |
| `http://localhost:8080/swagger-ui.html` | Interactive API docs |
| `http://localhost:8080/v3/api-docs` | OpenAPI spec (JSON) |
| `http://localhost:8080/actuator/health` | Health check |

### 2 — Run the MCP Server

```bash
cd onboarding-app-mcp
npm install
npm run build
npm start
```

The MCP server listens on `http://localhost:8888/mcp` and proxies all calls to the API at `http://localhost:8080`.

---

## Docker

### Onboarding API

```bash
cd onboarding-app
docker build -t employee-onboarding-app:latest .
docker run -p 8080:8080 employee-onboarding-app:latest
```

### MCP Server

```bash
cd onboarding-app-mcp
docker build -t onboarding-app-mcp:latest .
docker run -p 8888:8888 \
  -e ONBOARDING_API_URL=http://host.docker.internal:8080 \
  onboarding-app-mcp:latest
```

| Environment variable | Default | Description |
|---|---|---|
| `PORT` | `8888` | Port the MCP server listens on |
| `ONBOARDING_API_URL` | `http://host.docker.internal:8080` | Base URL of the Onboarding API |

---

## REST API Overview

### Employee endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/employee` | List all employees |
| `POST` | `/api/employee` | Create a new employee |
| `GET` | `/api/employee/{id}` | Get employee details |
| `PUT` | `/api/employee/{id}` | Update `cvStatus` / `carStatus` |
| `PATCH` | `/api/employee/{id}/cv-status` | Update CV status only |
| `POST` | `/api/employee/{employeeId}/assign-car/{carId}` | Assign a car to an employee |
| `DELETE` | `/api/employee/{employeeId}/unassign-car` | Remove car assignment |
| `POST` | `/api/employee/reload` | Reset employee data from CSV |

### Car endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/car` | List all cars |
| `GET` | `/api/car/available` | List available cars only |
| `GET` | `/api/car/{id}` | Get car details |
| `POST` | `/api/car/{id}?status=…` | Update car status (`Available` / `In Maintenance` / `Reserved`); use assign endpoint to set `Assigned` |
| `POST` | `/api/car/reload` | Reset car fleet data from CSV |

Full request/response schemas are documented in [`onboarding-app/openapi.yaml`](onboarding-app/openapi.yaml) and the Swagger UI.

---

## MCP Tools

The MCP server exposes the following tools for use with AI agents (e.g. watsonx Orchestrate):

| Tool | Description |
|---|---|
| `list_employees` | List all employees with onboarding status |
| `get_employee` | Get details for a specific employee |
| `create_employee` | Create a new employee record |
| `update_cv_status` | Set `cvStatus` for a specific employee |
| `update_employee_status` | Set `cvStatus` and/or `carStatus` in one call |
| `assign_car_to_employee` | Assign an available car to an employee |
| `unassign_car_from_employee` | Remove a car assignment |
| `reload_employees` | Reset employee data from CSV |
| `list_cars` | List all cars in the fleet |
| `list_available_cars` | List only available cars |
| `get_car` | Get details for a specific car |
| `update_car_status` | Set car status to `Available`, `In Maintenance`, or `Reserved` (not `Assigned` — use `assign_car_to_employee`) |
| `reload_cars` | Reset car fleet data from CSV |
| `health_check` | Check application health |

The MCP server uses the **Streamable HTTP** transport. Connect your client to `POST http://localhost:8888/mcp`.

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 11+ |
| Maven | 3.6+ |
| Node.js | 18+ |
| Docker | any recent version (optional) |

---

> **Note:** This is a mock application built for demonstration purposes. In a production environment you would integrate with real HR systems, document management platforms, and fleet management databases.
