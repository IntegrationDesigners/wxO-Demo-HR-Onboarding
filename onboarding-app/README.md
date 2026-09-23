# Employee Onboarding Application

A comprehensive HR employee onboarding management system built with Java 11, Spring Boot, and React. This application tracks employee onboarding status including CV creation and car provisioning with error detection, featuring a complete REST API and real-time dashboard.

## 🎯 Features

### Backend (Java 11 + Spring Boot)
- **Employee Management**
  - Create new employee records
  - Track CV creation status
  - Track car provisioning status
  - Automatic onboarding status calculation (Completed/In Progress/Error)
  - Error detection for invalid car assignments
  - List all employees with status overview

- **Car Fleet Management**
  - Pre-populated fleet of 12 cars across 4 price ranges
  - Track car availability and assignments
  - Update car operational status
  - Filter available cars for assignment

- **Monitoring & Health**
  - Actuator health endpoint for readiness probes
  - Prometheus metrics endpoint
  - Comprehensive OpenAPI documentation

### Frontend (React)
- Real-time dashboard with auto-refresh
- Employee onboarding status overview
- Car fleet status visualization
- Statistics cards showing key metrics
- Responsive design with modern UI

## 📋 Prerequisites

- Java 11 or higher
- Maven 3.6+
- Docker (optional, for containerization)
- OpenShift/Kubernetes (optional, for deployment)

## 🚀 Quick Start

### Run the Application

```bash
cd onboarding-app
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Access Points

- **Web Dashboard**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/prometheus

## 📚 API Endpoints

### Employee Management

#### Create Employee
```bash
POST /api/employee
Content-Type: application/json

{
  "givenName": "John",
  "name": "Doe",
  "startDate": "2026-07-01",
  "gender": "Male"
}
```

#### Get Employee Status
```bash
GET /api/employee/{id}
```

#### Update Employee Onboarding Status
```bash
PUT /api/employee/{id}?cvStatus=true&carStatus=true
```

#### List All Employees
```bash
GET /api/employee
```

#### Assign Car to Employee
```bash
POST /api/employee/{employeeId}/assign-car/{carId}
```

This endpoint:
- Validates the car is available
- Assigns the car to the employee
- Updates employee's carStatus to true
- Updates car's assignment information
- Automatically recalculates onboarding status

#### Unassign Car from Employee
```bash
DELETE /api/employee/{employeeId}/unassign-car
```

This endpoint:
- Removes car assignment from employee
- Sets employee carStatus to false
- Clears employee's assignedCarId
- Marks car as available
- Automatically recalculates onboarding status

### Car Fleet Management

#### List All Cars
```bash
GET /api/car
```

#### List Available Cars
```bash
GET /api/car/available
```

#### Get Car Details
```bash
GET /api/car/{id}
```

#### Update Car Status
```bash
POST /api/car/{id}?status=Available
```

Valid status values: `Available`, `Assigned`, `In Maintenance`, `Reserved`

### Health & Monitoring

#### Health Check
```bash
GET /actuator/health
```

#### Prometheus Metrics
```bash
GET /actuator/prometheus
```

## 🏗️ Project Structure

```
onboarding-app/
├── src/
│   └── main/
│       ├── java/com/demo/onboarding/
│       │   ├── OnboardingApplication.java    # Main application class
│       │   ├── controller/                    # REST controllers
│       │   │   ├── EmployeeController.java
│       │   │   ├── CarController.java
│       │   │   └── ConfigController.java      # Exposes /api/config for frontend
│       │   ├── service/                       # Business logic
│       │   │   ├── EmployeeService.java
│       │   │   └── CarService.java
│       │   ├── model/                         # Domain models
│       │   │   ├── Employee.java
│       │   │   └── Car.java
│       │   └── config/                        # Configuration
│       │       └── OpenApiConfig.java
│       └── resources/
│           ├── static/                        # React frontend
│           │   └── index.html
│           └── application.properties         # App configuration
├── k8s/
│   └── deployment.yaml                        # Kubernetes/OpenShift manifest
├── pom.xml                                    # Maven configuration
├── Dockerfile                                 # Multi-stage Docker build
├── openapi.yaml                               # OpenAPI specification
└── README.md
```

## 🐳 Docker

### Build Docker Image

```bash
docker build -t employee-onboarding-app:latest .
```

### Run Docker Container

```bash
# Minimal (uses defaults from application.properties)
docker run -p 8082:8080 employee-onboarding-app:latest

# With wxO widget configured for a real environment
docker run -p 8082:8080 \
  -e WXO_HOST_URL=https://your-wxo-host \
  -e WXO_ORCHESTRATION_ID=<orchestration-id> \
  -e WXO_AGENT_ID=<agent-id> \
  -e WXO_AGENT_ENVIRONMENT_ID=<agent-environment-id> \
  employee-onboarding-app:latest
```

### Docker Compose

```bash
# Uses defaults — override any value with a shell env var before running
WXO_HOST_URL=https://your-wxo-host docker compose up -d
```

The Dockerfile uses:
- **Multi-stage build** for optimized image size
- **Distroless base image** for security
- **Non-root user** for enhanced security
- **Container-optimized JVM settings**

## ☸️ Kubernetes/OpenShift Deployment

### Deploy to OpenShift

```bash
# Set your container registry and image tag
export CONTAINER_REGISTRY=quay.io/your-org
export IMAGE_TAG=v1.0.0

# Build and push image
docker build -t ${CONTAINER_REGISTRY}/employee-onboarding-app:${IMAGE_TAG} .
docker push ${CONTAINER_REGISTRY}/employee-onboarding-app:${IMAGE_TAG}

# Deploy to OpenShift
oc apply -f k8s/deployment.yaml
```

### Deployment Features

- **High Availability**: 2 replicas with pod disruption budget
- **Auto-scaling**: HPA configured for CPU/memory based scaling (2-10 pods)
- **Health Checks**: Liveness and readiness probes
- **Security**: Non-root user, read-only filesystem, dropped capabilities
- **Monitoring**: Prometheus ServiceMonitor for metrics collection
- **TLS**: Edge-terminated route with automatic redirect

### Access the Application

```bash
# Get the route URL
oc get route employee-onboarding-route -n employee-onboarding

# Access the application
curl https://$(oc get route employee-onboarding-route -n employee-onboarding -o jsonpath='{.spec.host}')
```

## 📊 Business Logic

### Onboarding Status Rules

The system automatically calculates employee onboarding status based on three possible states:

#### ✅ Completed
- CV Status = `true` (CV has been created)
- Car Status = `true` (Car has been provisioned)
- Assigned Car ID is present (car is actually assigned)

#### ⚠️ In Progress
- CV Status = `false` OR Car Status = `false`
- Normal onboarding workflow state

#### ❌ Error
- Car Status = `true` BUT Assigned Car ID is `null` or empty
- Indicates data inconsistency: car marked as provisioned without actual assignment
- Requires manual intervention to resolve

The system validates data integrity by detecting when car provisioning is marked complete without an actual car assignment, preventing inconsistent states.

### Car Price Ranges

- **Class 1 (Economy)**: Toyota Corolla, Honda Civic, VW Golf
- **Class 2 (Mid-range)**: BMW 3 Series, Audi A4, Mercedes C-Class
- **Class 3 (Premium)**: BMW 5 Series, Audi A6, Mercedes E-Class
- **Class 4 (Luxury)**: BMW 7 Series, Audi A8, Mercedes S-Class

## 🔧 Configuration

### Application Properties

Key configuration options in `application.properties`:

```properties
# Server
server.port=8080

# Actuator
management.endpoints.web.exposure.include=health,prometheus,info,metrics
management.endpoint.health.show-details=always

# Metrics
management.metrics.export.prometheus.enabled=true

# API Documentation
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

### Environment Variables

For containerized deployments, the watsonx Orchestrate widget values can be overridden via environment variables (Spring Boot maps `WXO_HOST_URL` → `wxo.host-url`, etc.):

| Environment variable | Default | Description |
|---|---|---|
| `WXO_HOST_URL` | `http://localhost:3000` | Base URL of the wxO runtime that serves `wxoLoader.js` |
| `WXO_ORCHESTRATION_ID` | `906a1a1a-…` | wxO orchestration / tenant ID |
| `WXO_AGENT_ID` | `aab9725b-…` | wxO agent ID shown in the chat widget |
| `WXO_AGENT_ENVIRONMENT_ID` | _(empty)_ | Optional — wxO agent environment ID passed to `chatOptions.agentEnvironmentId` |

```bash
# Example — run with production values
docker run -p 8082:8080 \
  -e WXO_HOST_URL=https://your-wxo-host \
  -e WXO_ORCHESTRATION_ID=<prod-orchestration-id> \
  -e WXO_AGENT_ID=<prod-agent-id> \
  -e WXO_AGENT_ENVIRONMENT_ID=<agent-environment-id> \
  employee-onboarding-app:latest
```

Other useful variables:

```bash
JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
SPRING_PROFILES_ACTIVE=production
```

## 🧪 Testing the Application

### Create Sample Employee

```bash
curl -X POST http://localhost:8080/api/employee \
  -H "Content-Type: application/json" \
  -d '{
    "givenName": "Jane",
    "name": "Smith",
    "startDate": "2026-08-01",
    "gender": "Female"
  }'
```

### Update Onboarding Status

```bash
# Mark CV as created
curl -X PUT "http://localhost:8080/api/employee/{id}?cvStatus=true"

# Mark car as provisioned
curl -X PUT "http://localhost:8080/api/employee/{id}?carStatus=true"

# Update both at once
curl -X PUT "http://localhost:8080/api/employee/{id}?cvStatus=true&carStatus=true"
```

### Assign Car to Employee

```bash
# Assign a specific car to an employee
curl -X POST "http://localhost:8080/api/employee/{employeeId}/assign-car/{carId}"
```

This automatically:
- Sets employee carStatus to true
- Marks car as assigned
- Updates employee's assignedCarId
- Recalculates onboarding status

### Unassign Car from Employee

```bash
# Remove car assignment from an employee
curl -X DELETE "http://localhost:8080/api/employee/{employeeId}/unassign-car"
```

This automatically:
- Sets employee carStatus to false
- Clears assignedCarId
- Marks car as available
- Recalculates onboarding status

### Check Available Cars

```bash
curl http://localhost:8080/api/car/available
```

### Update Car Status

```bash
curl -X POST "http://localhost:8080/api/car/{carId}?status=Assigned"
```

## 📈 Monitoring

### Prometheus Metrics

The application exposes metrics at `/actuator/prometheus` including:
- JVM metrics (memory, threads, GC)
- HTTP request metrics
- System metrics (CPU, disk)
- Custom business metrics

### Health Checks

Health endpoint at `/actuator/health` provides:
- Application status
- Disk space
- Ping status

## 🔒 Security Features

- **Non-root container user** (UID 65532)
- **Read-only root filesystem**
- **Dropped Linux capabilities**
- **Distroless base image** (minimal attack surface)
- **No privilege escalation**
- **Security context constraints**

## 🛠️ Development

### Build the Project

```bash
mvn clean package
```

### Run Tests

```bash
mvn test
```

### Generate OpenAPI Documentation

The OpenAPI specification is automatically generated and available at:
- JSON: http://localhost:8080/v3/api-docs
- YAML: See `openapi.yaml` in project root

## 📝 API Documentation

Comprehensive API documentation with extended descriptions is available:
- **Interactive UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: `openapi.yaml` file in project root

Each endpoint includes:
- Detailed descriptions
- Request/response examples
- Parameter documentation
- Error responses

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is licensed under the Apache License 2.0.

## 👥 Support

For issues and questions:
- Create an issue in the repository
- Contact: hr-it@company.com

## 🎉 Acknowledgments

Built with:
- Spring Boot 2.7.18
- React 18
- OpenAPI 3.0
- Prometheus
- Kubernetes/OpenShift

---

**Note**: This is a mock application for demonstration purposes. In production, you would integrate with actual HR systems, document management systems, and fleet management databases.