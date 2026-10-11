# Phase plan

## Phase 0 — Inspect and plan

- Confirm architecture: React frontend, Java Spring Boot API, PostgreSQL, LiveKit WebRTC, managed free tier.
- Decide on a prototype-first approach that stays within a free-tier budget and accepts that limits may apply.
- Choose a simple, secure family workflow: invite-only membership and room-only access.
- Define the repository layout and validation workflow before implementation.

## Phase 1 — Scaffold repository

- Create frontend and backend projects.
- Add a shared root README and environment template.
- Verify both apps can start and basic tests pass.

## Phase 2 — Authentication and family membership

- Add family-admin and member flows.
- Implement invitation creation, acceptance, and member activation.
- Keep access restricted and auditable.

## Phase 3 — Rooms and secure LiveKit token issuance

- Add room creation and membership checks.
- Issue signed, short-lived LiveKit tokens for approved participants.
- Avoid exposing private credentials to the browser.

## Phase 4 — React meeting interface

- Build a responsive meeting screen with participant list and join controls.
- Support dial-in-friendly mobile views and desktop layouts.

## Phase 5 — End-to-end integration

- Connect frontend login and room flows to backend APIs.
- Validate room join and meeting data models end-to-end.

## Phase 6 — Performance and 50-participant preparation

- Measure join latency, room initialization, and network assumptions.
- Document scaling limits and recommended 50-person testing strategy.

## Phase 7 — Deployment and security review

- Add deployment and security guidance.
- Validate secrets management and route protections.

## Phase 8 — Final verification and handover

- Run repository tests and smoke checks.
- Explain the final state, known limitations, and next steps.
