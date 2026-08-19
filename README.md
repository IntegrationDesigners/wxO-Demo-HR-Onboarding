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
    │   ├── Onboarding_Employee_Agent.yaml
    │   └── Onboarding_Orchestrator_Agent.yaml
    ├── knowledge-bases/          # Knowledge base specs and source documents
    │   ├── car_policy.yaml
    │   └── documents/
    │       └── car_policy.txt
    ├── toolkits/                 # MCP toolkit specs (reference / optional)
    │   └── onboarding-app.local.yaml
    ├── tools/                    # Flow tools (JSON), Python tools, OpenAPI spec
    │   ├── onboarding_api.local.yaml     # OpenAPI spec imported as tool set
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
| 1 | Import OpenAPI tool set from `onboarding_api.local.yaml` (generates individual `onboarding_*` tools) |
| 2 | Import flow tools: `employee_onboarding`, `onboarding_cv_converter`, `process_cv`, `process_identity_card` |
| 3 | Import dummy Slack flow tool (`onboarding_send_slack_message`) |
| 4 | Import Python tool: `populate_word_template` |
| 5 | Import knowledge base: `Onboarding_Car_Policy` |
| 6 | Import agents: `Onboarding_Car_Agent`, `Onboarding_CV_Agent`, `Onboarding_Employee_Agent` |
| 7 | Import orchestrator agent: `Onboarding_Orchestrator_Agent` (must be imported after all sub-agents) |

---

## watsonx Orchestrate Agents

The agent layer follows an orchestrator / sub-agent pattern. HR staff interact exclusively with the **Onboarding Orchestrator**, which delegates tasks to the three specialist sub-agents.

```
Onboarding_Orchestrator_Agent
├── Onboarding_Employee_Agent
├── Onboarding_CV_Agent
└── Onboarding_Car_Agent
```

### Onboarding_Orchestrator_Agent

The top-level agent used by **HR staff**. It routes requests to the correct sub-agent and coordinates the full onboarding journey in logical order: Employee → CV → Car. It carries no direct tools — all work is done via collaborators.

**Collaborators:** `Onboarding_Employee_Agent` · `Onboarding_CV_Agent` · `Onboarding_Car_Agent`

### Onboarding_Employee_Agent

Handles employee registration, listing, and onboarding status updates. After any status change it notifies Slack.

**Tools:** `employee_onboarding` · `onboarding_updateEmployeeStatus` · `onboarding_getEmployee` · `onboarding_getAllEmployees` · `onboarding_send_slack_message`

### Onboarding_Car_Agent

Handles car provisioning: listing, assignment/unassignment, and status updates. After a car is assigned it notifies Slack. Uses the `Onboarding_Car_Policy` knowledge base to answer policy questions about car eligibility and entitlements.

**Tools:** `onboarding_getCar` · `onboarding_getAllCars` · `onboarding_getAvailableCars` · `onboarding_assignCarToEmployee` · `onboarding_unassignCarFromEmployee` · `onboarding_getAllEmployees` · `onboarding_getEmployee` · `onboarding_send_slack_message`

**Knowledge base:** `Onboarding_Car_Policy`

### Onboarding_CV_Agent

Handles CV conversion and status tracking. After a CV is created it notifies Slack.

**Tools:** `onboarding_cv_converter` · `onboarding_updateCvStatus` · `onboarding_getEmployee` · `onboarding_getAllEmployees` · `onboarding_send_slack_message`

---

## watsonx Orchestrate Knowledge Bases

| Knowledge Base | Source document | Used by |
|---|---|---|
| `Onboarding_Car_Policy` | [`knowledge-bases/documents/car_policy.txt`](onboarding-agent/knowledge-bases/documents/car_policy.txt) | `Onboarding_Car_Agent` |

The knowledge base is imported with `orchestrate knowledge-bases import` using the spec file [`knowledge-bases/car_policy.yaml`](onboarding-agent/knowledge-bases/car_policy.yaml). It allows the Car Agent to answer natural-language questions about the company car policy (eligibility criteria, allowed models, fuel card rules, etc.) without hard-coding that information in the agent instructions.

---

## watsonx Orchestrate Tools

### OpenAPI tools (`onboarding_api.local.yaml`)

Imported from the OpenAPI spec via `orchestrate tools import -k openapi`. Each `operationId` becomes a standalone tool.

| Tool | Method | Description |
|---|---|---|
| `onboarding_getAllEmployees` | `GET /api/employee` | List all employees |
| `onboarding_createEmployee` | `POST /api/employee` | Create a new employee record |
| `onboarding_getEmployee` | `GET /api/employee/{id}` | Get details for a specific employee |
| `onboarding_updateEmployeeStatus` | `PUT /api/employee/{id}` | Update `cvStatus` and/or `carStatus` |
| `onboarding_updateCvStatus` | `PATCH /api/employee/{id}/cv-status` | Update CV status only |
| `onboarding_assignCarToEmployee` | `POST /api/employee/{employeeId}/assign-car/{carId}` | Assign an available car to an employee |
| `onboarding_unassignCarFromEmployee` | `DELETE /api/employee/{employeeId}/unassign-car` | Remove a car assignment |
| `onboarding_reloadEmployees` | `POST /api/employee/reload` | Reset employee data from CSV |
| `onboarding_getAllCars` | `GET /api/car` | List all cars in the fleet |
| `onboarding_getAvailableCars` | `GET /api/car/available` | List only available cars |
| `onboarding_getCar` | `GET /api/car/{id}` | Get details for a specific car |
| `onboarding_updateCarStatus` | `POST /api/car/{id}` | Set car status (`Available` / `In Maintenance` / `Reserved`) |
| `onboarding_reloadCars` | `POST /api/car/reload` | Reset car fleet data from CSV |

### Flow & Python tools

| Tool | Kind | Description |
|---|---|---|
| `employee_onboarding` | Flow | End-to-end employee onboarding flow with user interaction steps |
| `onboarding_cv_converter` | Flow | Converts a CV document and returns the result |
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

## MCP Server

The `onboarding-app-mcp` server wraps the same REST API as an **MCP (Model Context Protocol)** server using the Streamable HTTP transport. It exposes the same operations as the OpenAPI tools but under a different naming convention and is available as an alternative integration path (e.g. for MCP-native clients).

Connect your client to `POST http://localhost:8888/mcp`.

| MCP Tool | REST equivalent |
|---|---|
| `list_employees` | `GET /api/employee` |
| `get_employee` | `GET /api/employee/{id}` |
| `create_employee` | `POST /api/employee` |
| `update_cv_status` | `PATCH /api/employee/{id}/cv-status` |
| `update_employee_status` | `PUT /api/employee/{id}` |
| `assign_car_to_employee` | `POST /api/employee/{employeeId}/assign-car/{carId}` |
| `unassign_car_from_employee` | `DELETE /api/employee/{employeeId}/unassign-car` |
| `reload_employees` | `POST /api/employee/reload` |
| `list_cars` | `GET /api/car` |
| `list_available_cars` | `GET /api/car/available` |
| `get_car` | `GET /api/car/{id}` |
| `update_car_status` | `POST /api/car/{id}` |
| `reload_cars` | `POST /api/car/reload` |
| `health_check` | — |

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
