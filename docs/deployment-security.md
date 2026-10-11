# Deployment and security review

## Current prototype posture

This repository is intentionally a prototype-first implementation. It validates the core family-membership and room-access flow with an in-memory H2 database and a locally hosted Spring API. It is not yet a production deployment configuration.

## Required hardening before production

- Replace the H2 prototype database with PostgreSQL or another managed relational database.
- Store the `FAMILYCONNECT_TOKEN_SECRET` in a secure secret manager rather than a committed default value.
- Restrict CORS to the exact production origins used by the frontend deployment.
- Require signed, short-lived room tokens and expire them aggressively.
- Add explicit auth checks for room access and family membership enforcement at all API boundaries.
- Enable audit logging for admin actions such as invite creation, invite acceptance, and room creation.

## Secret management

Environment variables should be loaded from a secure runtime environment or secret manager. A sample file is included at `.env.example`.

## CORS and origin policy

The backend reads allowed origins from the `FAMILYCONNECT_ALLOWED_ORIGINS` environment variable. This keeps local development flexible without hard-coding hosts into the source code.

## Room token model

The current token payload includes:

- room code
- family ID
- participant email
- expiry timestamp

The token is signed with HMAC-SHA256 before being issued to the client. This is appropriate for a prototype, but production deployments should also validate the token server-side before accepting room joins.

## Recommended next deployment milestone

Before moving to a broader production rollout:

1. replace H2 with PostgreSQL
2. add managed secret storage
3. add HTTPS and a trusted reverse proxy
4. validate room access and invite lifecycle with integration tests
5. test load assumptions for up to 50 concurrent participants
