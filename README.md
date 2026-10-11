# FamilyConnect

FamilyConnect is a two-person family video-calling web application with private invite links and a responsive mobile/desktop interface.

## Project architecture

- Frontend: TypeScript + Vite in `frontend/`
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
4. Phase 3: rooms and WebRTC call signaling
5. Phase 4: two-person WebRTC calling interface
6. Phase 5: end-to-end integration
7. Phase 6: performance and 50-person preparation
8. Phase 7: deployment and security review
9. Phase 8: final verification and handover

## Current status

Two-person audio/video calls use browser WebRTC for media and the backend WebSocket endpoint for call signaling. Media is sent directly between the two browsers; the backend does not record or relay the audio/video.

## Start a call

1. Open the FamilyConnect site in a modern browser on a secure `https://` page.
2. Enter your name and select **Start a call**. Allow camera and microphone access.
3. Select **Copy invite link** and send the link privately to your brother.
4. Your brother opens the link, enters his name, selects **Join this call**, and allows camera and microphone access.
5. Use the mic and camera buttons to toggle them, and **Leave call** to hang up.

The free setup uses a public STUN server for direct browser connections and does not provide a TURN relay. Some restrictive networks may prevent a direct call; reliable calling on those networks requires configuring a TURN service. The free Render backend may sleep when idle, so the first call after inactivity can take a minute to connect. Calls support two participants, not 50.

## Deployment and security notes

- The backend token secret must come from an environment variable in real deployments.
- CORS origins are configured from environment variables instead of hard-coded localhost values.
- Existing family membership API tokens are short-lived and signed with HMAC-SHA256.
- WebRTC audio/video is peer-to-peer. Anyone with a call link can join its two-person room; only share the link with the intended caller.
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
