# wxO-Demo-HR-Onboarding

A demonstration project for **IBM watsonx Orchestrate** Integration Designers Enablement. It consists of three components:

| Component | Description | Port |
|---|---|---|
| [`onboarding-app`](onboarding-app/) | Spring Boot REST API + React dashboard for HR employee onboarding | `8080` |
| [`onboarding-app-mcp`](onboarding-app-mcp/) | MCP server (Streamable HTTP) that wraps the REST API for use with AI agents | `8888` |
| [`onboarding-agent`](onboarding-agent/) | watsonx Orchestrate agents, tools, and toolkits for the HR onboarding demo | — |

---

## Repository Structure

```
wxO-Demo-HR-Onboarding/
├── onboarding-app/               # Java 11 + Spring Boot backend & React frontend
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
├── onboarding-app-mcp/           # Node.js MCP server (TypeScript)
│   ├── src/index.ts               # MCP tools + Express HTTP transport
│   ├── Dockerfile
│   └── package.json
│
└── onboarding-agent/             # watsonx Orchestrate configuration
    ├── agents/                   # Native agent YAML specs
    │   ├── Onboarding_Car_Agent.yaml
    │   ├── Onboarding_CV_Agent.yaml
    │   └── Onboarding_Employee_Agent.yaml
    ├── toolkits/                 # MCP toolkit specs
    │   └── onboarding-app.local.yaml
    ├── tools/                    # Flow tools (JSON) and Python tools
    │   ├── employee_onboarding.json
    │   ├── onboarding_cv_converter.json
    │   ├── onboarding_send_slack_message.json
    │   ├── onboarding_send_slack_message.dummy.json
    │   ├── process_cv.json
    │   ├── process_identity_card.json
    │   └── populate_word_template/
    │       ├── populate_word_template.py
    │       └── requirements.txt
    ├── import-all.sh             # Bash deployment script
    └── import-all.ps1            # PowerShell deployment script
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

### 3 — Deploy agents to watsonx Orchestrate

Authenticate the `orchestrate` CLI first:

```bash
orchestrate env activate <your-env>
```

Then run the import script from inside the `onboarding-agent` directory.

**Linux / macOS (Bash):**

```bash
cd onboarding-agent
./import-all.sh
```

**Windows (PowerShell):**

```powershell
cd onboarding-agent
.\import-all.ps1
```

The scripts execute the following steps in order:

| Step | Action |
|---|---|
| 1 | Remove existing `onboarding-app` toolkit (if present), then re-import |
| 2 | Import flow tools: `employee_onboarding`, `onboarding_cv_converter`, `process_cv`, `process_identity_card` |
| 3 | Import dummy Slack flow tool (`onboarding_send_slack_message`) |
| 4 | Import Python tool: `populate_word_template` |
| 5 | Import agents: `Onboarding_Car_Agent`, `Onboarding_CV_Agent`, `Onboarding_Employee_Agent` |

---

## watsonx Orchestrate Agents

### Onboarding_Employee_Agent

Handles employee registration, listing, and onboarding status updates. After any status change it notifies Slack.

**Tools:** `employee_onboarding` · `onboarding-app:update_employee_status` · `onboarding-app:get_employee` · `onboarding-app:list_employees` · `onboarding_send_slack_message`

### Onboarding_Car_Agent

Handles car provisioning: listing, assignment/unassignment, and status updates. After a car is assigned it notifies Slack.

**Tools:** `onboarding-app:get_car` · `onboarding-app:list_cars` · `onboarding-app:list_available_cars` · `onboarding-app:assign_car_to_employee` · `onboarding-app:unassign_car_from_employee` · `onboarding-app:list_employees` · `onboarding-app:get_employee` · `onboarding_send_slack_message`

### Onboarding_CV_Agent

Handles CV conversion and status tracking. After a CV is created it notifies Slack.

**Tools:** `onboarding_cv_converter` · `onboarding-app:update_cv_status` · `onboarding-app:get_employee` · `onboarding-app:list_employees` · `onboarding_send_slack_message`

---

## watsonx Orchestrate Tools

| Tool | Kind | Description |
|---|---|---|
| `employee_onboarding` | Flow | End-to-end employee onboarding flow with user interaction steps |
| `onboarding_cv_converter` | Flow | Converts a CV document and stores the result |
| `process_cv` | Flow | Extracts structured data from a CV using document processing |
| `process_identity_card` | Flow | Extracts identity fields from an identity card document |
| `onboarding_send_slack_message` | Flow | No-op dummy Slack tool (simulates sending a message to the `#onboarding` channel) |
| `populate_word_template` | Python | Populates a `.docx` template with JSON data using Jinja2 |

---

## Docker

### Onboarding API

```bash
cd onboarding-app
docker build -t employee-onboarding-app:latest .
docker run -d -p 8082:8080 employee-onboarding-app:latest
```

### MCP Server

```bash
cd onboarding-app-mcp
docker build -t onboarding-app-mcp:latest .
docker run -d -p 8888:8888 \
  -e ONBOARDING_API_URL=http://host.docker.internal:8082 \
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
| `POST` | `/api/car/{id}?status=…` | Update car status (`Available` / `In Maintenance` / `Reserved`) |
| `POST` | `/api/car/reload` | Reset car fleet data from CSV |

Full request/response schemas are documented in [`onboarding-app/openapi.yaml`](onboarding-app/openapi.yaml) and the Swagger UI.

---

## MCP Tools

The MCP server exposes the following tools for use with AI agents:

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
| `update_car_status` | Set car status to `Available`, `In Maintenance`, or `Reserved` |
| `reload_cars` | Reset car fleet data from CSV |
| `health_check` | Check application health |

The MCP server uses the **Streamable HTTP** transport. Connect your client to `POST http://localhost:8888/mcp`.

---

## Prerequisites

| Tool | Version | Required for |
|---|---|---|
| Java | 11+ | Onboarding API |
| Maven | 3.6+ | Onboarding API |
| Node.js | 18+ | MCP server |
| `orchestrate` CLI | latest | Agent deployment |
| Docker | any recent | Optional containerisation |

---

> **Note:** This is a mock application built for demonstration purposes. In a production environment you would integrate with real HR systems, document management platforms, and fleet management databases.
