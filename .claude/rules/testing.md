---
name: Testing Rules
description: Backend JUnit and frontend Vitest conventions
paths:
  - backend/src/test/**
  - frontend/src/__tests__/**
---

# Testing

## Backend

- JUnit 5 via Spring Boot test starter.
- Run: `./gradlew test`
- Location: `backend/src/test/java/com/stockreport/**`
- Integration tests touching Mongo run against the local `stockreport-dev` DB — do NOT mock the Mongo layer for those.

## Frontend

- Vitest + Testing Library (`@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`).
- Environment: `jsdom` (configured in `vitest.config.ts`).
- Run: `npm run test` (single run), `npm run test:watch`, `npm run test:ui`.
- Location: `frontend/src/__tests__/**`. Colocated `*.test.ts(x)` next to the component is also acceptable.

## Query preferences

- Prefer user-facing queries: `getByRole`, `getByText`, `getByLabelText`.
- Use `getByTestId` only as a last resort (e.g. for charts with no accessible name).
