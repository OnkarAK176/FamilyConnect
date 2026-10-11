# FamilyConnect

FamilyConnect is a family video-calling web application with private invite links and rooms for up to 20 people.

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
4. Phase 3: rooms and SFU-backed call token issuance
5. Phase 4: multi-participant LiveKit calling interface
6. Phase 5: end-to-end integration
7. Phase 6: performance and 20-person call preparation
8. Phase 7: deployment and security review
9. Phase 8: final verification and handover

## Current status

Audio/video calls use LiveKit Cloud as a Selective Forwarding Unit (SFU). Media is forwarded by LiveKit to room participants; FamilyConnect does not record calls.

## Start a call

1. Create a LiveKit Cloud Build project (free tier, no credit card currently required).
2. In LiveKit Cloud, create an API key and copy its URL, key, and secret.
3. Add `LIVEKIT_URL`, `LIVEKIT_API_KEY`, and `LIVEKIT_API_SECRET` to the Render service environment. Keep the API secret on the backend; never put it in Netlify or browser code.
4. Open the FamilyConnect site in a modern browser on a secure `https://` page.
5. Enter your name and select **Start a call**. Allow camera and microphone access.
6. Select **Copy invite link** and send the link privately to your family.
7. Each family member opens the link, enters a name, selects **Join this call**, and allows camera and microphone access.
8. Use the mic and camera buttons to toggle them, and **Leave call** to hang up.

Each room is capped at 20 participants. The current LiveKit Cloud Build allowance includes 5,000 participant-minutes per month and up to 100 connected participants across the project; these are hard quotas, not per-room allowances. For example, a full 20-person, 60-minute call uses 1,200 participant-minutes. When the included quota is exhausted, new connections can be rejected until the quota resets. The free Render backend may sleep when idle, so the first call after inactivity can take a minute to set up.

## Deployment and security notes

- The backend token secret must come from an environment variable in real deployments.
- CORS origins are configured from environment variables instead of hard-coded localhost values.
- Existing family membership API tokens are short-lived and signed with HMAC-SHA256.
- Call links are bearer invitations: anyone who has the link can join, up to the room limit. Only share links with intended family members.
- LiveKit API credentials are required for calling and are used only by the backend to provision rooms and issue short-lived participant tokens.
- Configure `LIVEKIT_URL`, `LIVEKIT_API_KEY`, and `LIVEKIT_API_SECRET` on Render before calls can start; without them, the backend deliberately returns a setup error instead of an invalid token.
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
