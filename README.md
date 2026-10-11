# FamilyConnect

FamilyConnect is a private family video-calling web application built for small-family groups, with support for up to 50 participants and a focus on trusted access, secure meeting links, and responsive mobile/desktop use.

## Project architecture

- Frontend: React + Vite in `frontend/`
- Backend: Java + Spring Boot REST API in `backend/`
- Database: PostgreSQL target with H2 in-memory prototype testing in `backend/`
- Deployment docs: `docs/`

## Environment setup

Copy `.env.example` to `.env` and replace the demo values before running the backend in a non-local environment:

```bash
cp .env.example .env
```

Required values:

- `FAMILYCONNECT_TOKEN_SECRET`: shared secret used to sign room join tokens
- `FAMILYCONNECT_ALLOWED_ORIGINS`: comma-separated list of browser origins allowed by CORS

## Phase plan

1. Phase 0: inspect and plan
2. Phase 1: scaffold the repository
3. Phase 2: authentication and family membership
4. Phase 3: rooms and secure LiveKit token issuance
5. Phase 4: React meeting interface
6. Phase 5: end-to-end integration
7. Phase 6: performance and 50-person preparation
8. Phase 7: deployment and security review
9. Phase 8: final verification and handover

## Current status

This repository is being built incrementally, one phase at a time, with verification after each phase.

## Deployment and security notes

- The backend token secret must come from an environment variable in real deployments.
- CORS origins are configured from environment variables instead of hard-coded localhost values.
- Room join tokens are short-lived and signed with HMAC-SHA256.
- The prototype intentionally uses H2 for local validation and should be replaced by PostgreSQL before production use.

## Free-tier deployment plan

This repository is prepared for a low-cost deployment setup:

- Frontend: Netlify free static hosting
- Backend: Render free web service via Docker

### Frontend deployment (Netlify)

1. Push the repository to GitHub.
2. Import the repo into Netlify.
3. Set the base directory to `frontend`.
4. Set build command to `npm install && npm run build`.
5. Set publish directory to `frontend/dist`.
6. Add a production environment variable:
   - `VITE_API_URL=https://your-render-app.onrender.com/api`

### Backend deployment (Render)

1. Push the repository to GitHub.
2. Create a new Render web service.
3. Choose the repository and set the root directory to `backend` if needed.
4. Use the included Docker configuration in `backend/Dockerfile`.
5. Add environment variables:
   - `FAMILYCONNECT_TOKEN_SECRET=<strong-random-value>`
   - `FAMILYCONNECT_ALLOWED_ORIGINS=https://your-netlify-site.netlify.app`

### Cost expectation

This setup is designed for free-tier usage as far as the hosting providers allow. The app is not production-hardened yet, so it should be treated as a prototype deployment, not a full production environment.

Render's free web service may sleep after inactivity and take about a minute to wake up. The current backend uses an in-memory H2 database, so family/member/room data is temporary and can be lost whenever the service restarts or sleeps. Do not store data that needs to persist until a persistent database is configured.
