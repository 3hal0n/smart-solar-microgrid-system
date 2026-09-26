# Smart Solar Microgrid Trading System

[![.NET](https://img.shields.io/badge/.NET-9.0-512BD4?style=flat-square)](https://dotnet.microsoft.com/)
[![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-47A248?style=flat-square)](https://www.mongodb.com/atlas)
[![React](https://img.shields.io/badge/React-19-149ECA?style=flat-square)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF?style=flat-square)](https://vitejs.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-4-38BDF8?style=flat-square)](https://tailwindcss.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=flat-square)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84?style=flat-square)](https://developer.android.com/jetpack/compose)

A client-server system for trading solar energy through a network of managed microgrid hubs. A web application handles backoffice administration and grid operator tools; a native Android application handles the prosumer side and on-site operator verification. Everything goes through one central API — neither client talks to the database directly.

## Contents

- [What this system does](#what-this-system-does)
- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [Project layout](#project-layout)
- [Running it locally](#running-it-locally)
- [Team and individual contributions](#team-and-individual-contributions)
- [Repository](#repository)
- [Demo video](#demo-video)

## What this system does

A solar prosumer is someone with their own solar panel array who wants to sell surplus energy back into the grid, or draw from it, through a physical charging/discharging point — a microgrid hub. The Backoffice team registers these hubs (location, capacity, battery slots) and keeps their schedules current. Grid operators keep an eye on slot availability and verify transfers in person. Prosumers do everything else from their phone: find a nearby hub, reserve a slot within a 7-day window, get a QR code once the reservation is confirmed, and hand that code to an operator when they show up.

None of the business rules — slot availability, the 7-day window, the 12-hour cancellation notice, whether a hub can be deactivated — are decided by either client. They all live in the API, and both the web app and the mobile app just render whatever the API tells them, including its rejection messages when a request isn't allowed.

## Architecture

```
        ┌─────────────┐          ┌─────────────┐
        │  Web App    │          │  Mobile App │
        │  (React)    │          │  (Android)  │
        └──────┬──────┘          └──────┬──────┘
               │      REST / JSON       │
               └───────────┬────────────┘
                            │
                    ┌───────▼────────┐
                    │   Web Service   │
                    │  (C# / ASP.NET) │
                    └───────┬────────┘
                            │
                    ┌───────▼────────┐
                    │    MongoDB      │
                    └────────────────┘
```

Both clients are interface layers only. The web app is a React single-page app; the mobile app is pure native Android — no Flutter, no React Native, no Xamarin. The mobile app keeps a small local SQLite cache for offline-first rendering (recent dashboard numbers, cached QR verification history), but that cache is never the source of truth — every write goes through the API, and the cache just holds the last thing the API said.

Authentication is JWT-based for Backoffice and Grid Operator staff on the web. The mobile app currently runs against fixture/test data for the prosumer identity until a prosumer login flow is wired up end to end.

## Tech stack

**Web Service**
- ASP.NET Core 9 Web API, deployed against Windows IIS in production
- MongoDB (Atlas in development), accessed through the official MongoDB.Driver
- JWT bearer authentication, BCrypt password hashing
- Swagger/OpenAPI for interactive API docs in development

**Web Application**
- React 19 with Vite
- Tailwind CSS 4
- React Router
- Axios, with a shared instance that attaches the signed-in user's JWT automatically

**Mobile Application**
- Pure native Android, Kotlin, Jetpack Compose
- SQLite via the platform's own `SQLiteOpenHelper` — no Room, no ORM
- Retrofit and OkHttp for API calls
- Google Maps SDK for Android (nearby-hub map)
- CameraX and ML Kit for QR scanning

## Project layout

```
WebService/     ASP.NET Core API — controllers, services, MongoDB models
WebApp/         React web application
MobileApp/      Native Android application
docs/           Architecture notes and this assignment's brief
```

## Running it locally

You'll need the .NET 9 SDK, Node.js, Android Studio, and a MongoDB connection string (Atlas or local).

**1. Web Service**

```
cd WebService/SmartMicrogrid.Api
dotnet run --launch-profile http
```

Add a `.env` file in that folder with your own `ConnectionStrings__MongoDb` and `Jwt__Key` — see `.env.example`. The API listens on `http://localhost:5128` by default.

Before anyone can sign in, create the first account directly against the API (this endpoint is deliberately open, precisely so there's a way to bootstrap the first user):

```
curl -X POST http://localhost:5128/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"ChangeMe123!","role":"Backoffice","fullName":"Admin User","email":"admin@example.com"}'
```

**2. Web Application**

```
cd WebApp
npm install
npm run dev
```

Runs on `http://localhost:5173`. Sign in at `/login` with the account you just created.

**3. Mobile Application**

Open the `MobileApp` folder in Android Studio (not the repository root) and let it sync. Run it on an emulator or, for a better test of the camera/GPS features, a physical device over USB with developer mode and USB debugging enabled.

If you're testing on a physical device rather than an emulator, add these to `MobileApp/local.properties`:

```
MAPS_API_KEY=your_google_maps_api_key
API_BASE_URL=http://<your-computer's-LAN-IP>:5128/
```

The emulator defaults to `10.0.2.2`, which only means anything to the emulator itself — a real phone needs your computer's actual IP address on the same Wi-Fi network.

## Team and individual contributions

This was a four-person group project. Ownership of each module shifted a few times over the course of the project as the team rebalanced who was doing what; what follows reflects the final split.

**Shalon Fernando** — Microgrid node management on the backend and web: creating and updating hubs, their battery slots and operating schedule, and the deactivation rule that blocks a hub from going offline while it still has active reservations. Also built the prosumer dashboard summary and reservation search endpoints, and the geolocation query behind the nearby-hubs map. On the web app: the Microgrid Hubs list and detail screens, the shared page layout used across the whole app, and the public landing page. On mobile: the prosumer dashboard (stat counts, booking history, filtering) and the nearby-hubs map screen built on the Google Maps SDK, plus the app's splash and onboarding screens.

**Dinil Dulneth** — The reservation lifecycle on the backend: creating, updating and cancelling a booking, with the 7-day scheduling window and 12-hour notice rule both enforced server-side, along with QR token issuance and verification. On the web app: the reservations oversight page used by Backoffice and Grid Operator staff to review and cancel bookings. On mobile: the Grid Operator QR scanner, built with CameraX and ML Kit, which reads a prosumer's transaction code and confirms the transfer against the server.

**Migara Wijesinghe** — User authentication and account management on the backend: JWT login, and CRUD for Backoffice and Grid Operator staff accounts with BCrypt password hashing. On the web app: the staff sign-in page and the user management screen.

**Rukshan** — Prosumer management on the web: prosumer profile administration and reviewing pending account activations. On mobile: prosumer account control — registration using NIC as the primary key, profile editing, and requesting account deactivation.

## Repository

https://github.com/3hal0n/smart-solar-microgrid-system

## Demo video

A short walkthrough of the application is available here: *add your video link before submission.*
