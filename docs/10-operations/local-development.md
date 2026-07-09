# Local Development

## Toolchain

- Java 21
- Maven 3.9+
- Node.js 24+
- npm 11+
- Docker Desktop or compatible Docker runtime

## Repository Layout

- `backend/` - Spring Boot API and worker codebase
- `frontend/` - React/Vite UI application
- `infra/` - local infrastructure definitions

## First-Time Setup

1. Start PostgreSQL with pgvector:
   `docker compose -f infra/docker-compose.yml up -d`
2. Create a frontend env file from `frontend/.env.example`.
3. Run the backend from `backend/`:
   `mvn spring-boot:run`
4. Run the frontend from `frontend/`:
   `npm install`
   `npm run dev`

## Default Local Endpoints

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- PostgreSQL: `localhost:5432`

## Notes

- The backend is configured to use Flyway migrations from `backend/src/main/resources/db/migration/`.
- The frontend expects the backend API base URL in `VITE_API_BASE_URL`.
- Early setup work creates scaffolding only; domain features and migrations come in later phases.
