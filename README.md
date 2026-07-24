# popn.gg server

popn.gg is a service for providing chart and playdata information of Japanese arcade game, pop'n music.

It is being serviced in [popn.gg](https://popn.gg).

## 📋 Project Structure

```
popngg/
├── popngg-api/         # API Layer (Controllers, DTOs, Configuration)
├── popngg-domain/      # Domain Layer (Services, Models, Repository Interfaces)
└── popngg-infra/       # Infrastructure Layer (Repository Implementations, DB Entities)
```

## 🚀 Getting Started

### Prerequisites

- JDK 21
- Docker and Docker Compose
- Gradle 8.13 (use the committed wrapper)

### Development Setup

1. Clone the repository
   ```bash
   git clone [repository-url]
   cd popngg
   ```

2. Run the application
   ```bash
   ./gradlew :popngg-api:bootRun --args='--spring.profiles.active=local'
   ```

## API Endpoints

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Technology Stack

- **Backend**: Spring Boot 3.5.16 / Spring Framework 6.2.x, Java 21
- **Build Tool**: Gradle
- **Database**: MySQL 8.0
- **Containerization**: Docker, Docker Compose
- **API Documentation**: SpringDoc OpenAPI 3.0
