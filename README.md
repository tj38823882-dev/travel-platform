# TravelVibe

A full-stack travel itinerary and community platform built with Vue 3 and Spring Boot.

This repository is prepared as a public interview showcase. Sensitive credentials, third-party secrets, and personal service configuration have been removed and replaced with environment-based setup.

## Highlights

- Travel itinerary browsing and trip planning flows
- Community posting, comments, and social interaction
- Friend system, private chat, notifications, and collaborative drawing
- Shopping cart, points system, and LINE Pay top-up flow
- Google OAuth login, Cloudinary media upload, and admin management pages

## Tech Stack

- Frontend: Vue 3, Vite, Pinia, Vue Router, Axios, Bootstrap
- Backend: Spring Boot, Spring Security, Spring Data JPA, WebSocket, MSSQL
- Integrations: Google Maps, Google OAuth, LINE Pay, Cloudinary, Gmail SMTP

## Project Structure

```text
travel-itinerary-platform/
|- frontend/   Vue 3 client app
|- backend/    Spring Boot API and WebSocket server
```

## Local Setup

### Frontend

1. Copy `frontend/.env.example` to `frontend/.env`.
2. Fill in the required values.
3. Run:

```bash
cd frontend
npm install
npm run dev
```

### Backend

Set these environment variables before starting the Spring Boot server:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `LINE_PAY_CHANNEL_ID`
- `LINE_PAY_CHANNEL_SECRET`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`

Then run:

```bash
cd backend
./mvnw spring-boot:run
```

## Interview Notes

- This project focuses on product completeness across frontend, backend, authentication, payments, and real-time features.
- Secrets were intentionally removed for safe public sharing.
- Demo data and local database setup are expected for a full end-to-end run.
