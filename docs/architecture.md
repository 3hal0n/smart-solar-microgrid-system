# Smart Solar Microgrid Trading System — Technical Architecture (v2: Equal Vertical-Slice Model)

**Module:** SE4040 Enterprise Application Development — Assignment 1
**Team:** Shalon (Node & Capacity — Web), Rukshan (Auth/Roles/Prosumer Admin — Web), Dinil (Prosumer Mobile Lifecycle & Reservations — Android), Migara (Operator Verification, Maps & Analytics — Android + Report)
**Deadline:** 30 September 2026, 11:59 PM

## 0. What changed from v1, and why

The first pass split work by **layer** (one backend person, one web person, two mobile people). That's a clean architecture story but a weak GitHub story: a viva panel that reads commit history sees Rukshan touching every `.cs` file and everyone else touching only their own layer — it doesn't show four people each capable of shipping a complete feature.

This version splits work by **vertical slice**: every member owns one functional module **end-to-end** — their own MongoDB collection(s)/fields, their own C# controller(s), and their own client screen(s) (web or Android). Each person's GitHub history should show commits touching all three layers of their module in the same week. Rukshan's hardware constraint is respected the same way as before (no Android Studio), but he's no longer "backend only" — his module now includes a full React+Tailwind slice, same as Shalon.

**Honest caveat, stated up front so nobody is surprised in viva:** equal *code volume* does not translate into equal *rubric marks*, because the rubric (Table 2) simply allocates more distinct criteria to mobile functionality (35 of 65 marks: Mobile Auth 9 + Reservation Workflow 9 + Dashboards 10 + Grid Verification 7) than to web functionality (18 marks, shared across two people). Dinil and Migara will show higher individual-criterion totals than Shalon and Rukshan even though all four write a comparable amount of code. This is a property of the rubric, not a flaw in the split — see §7 for the full accounting. Shalon and Rukshan's work still counts fully toward the **Table 1 group marks** (35, awarded identically to all four), so nobody's total outcome is actually unequal — just where the marks are counted differs.

---

## 1. High-Level Architecture (FAT Service Pattern)

```
   React + Tailwind Web App              Native Android App (Kotlin)
   [Shalon's Hub screens]                [Dinil's Prosumer screens]
   [Rukshan's Auth/Admin screens]        [Migara's Operator/Map/Dashboard screens]
              │  REST/JSON                          │  REST/JSON  (+ local SQLite cache)
              └───────────────┬──────────────────────┘
                               ▼
                 C# ASP.NET Core Web API (IIS)
                 ── FAT Service: ALL business rules live here ──
                 Controllers: Auth, Users, AdminProsumers (Rukshan)
                              Stations, Slots (Shalon)
                              ProsumerAuth, Reservations (Dinil)
                              Verification, Nearby, Dashboard (Migara)
                               │
                               ▼
                         MongoDB (single database: SmartMicrogridDB)
                         Collections: Users, SolarStations, EnergyBookingSlots, Reservations
```

Both clients are **UI + local-cache layers only**. Neither computes a business rule, a role permission, or a QR signature — they call an endpoint and render what comes back. This is what Table 1's "Service Architecture and API Design (8)" and "Client Application Build and Architecture Compliance (12)" actually reward, and it's non-negotiable regardless of who owns which module.

---

## 2. MongoDB Schema — exact field specifications

Database: `SmartMicrogridDB`. Four collections, as required by the spec.

### 2.1 `Users` — owner: **Rukshan** (model + admin/auth endpoints); **Dinil** writes to it from the prosumer self-registration endpoint

```jsonc
{
  "_id": "ObjectId",
  "role": "Backoffice" | "GridOperator" | "Prosumer",

  // present only when role != "Prosumer"
  "username": "string, unique (partial index where role != 'Prosumer')",

  // present only when role == "Prosumer" — PRIMARY BUSINESS KEY
  "nic": "string, unique (partial index where role == 'Prosumer')",
  "phone": "string | null",
  "address": "string | null",

  "passwordHash": "string (BCrypt)",
  "fullName": "string",
  "email": "string",
  "status": "Active" | "PendingActivation" | "Deactivated",
  "deactivationRequestedAt": "ISODate | null",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

### 2.2 `SolarStations` — owner: **Shalon** (full CRUD); **Migara** reads it for the geospatial nearby-stations query

```jsonc
{
  "_id": "ObjectId",
  "name": "string",
  "location": {
    "type": "Point",
    "coordinates": ["lng: number", "lat: number"]   // GeoJSON order is [lng, lat]
  },
  "capacityKWh": "number",
  "totalBatterySlots": "number",
  "operatingSchedule": { "opensAt": "string HH:mm", "closesAt": "string HH:mm" },
  "status": "Active" | "Inactive",
  "createdByUserId": "ObjectId",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```
Index: **2dsphere** on `location` (required for Migara's `$geoNear`/`$geoWithin` query — create it in Shalon's module on day 1 since her collection owns the field, even though Migara is the consumer).

### 2.3 `EnergyBookingSlots` — owner: **Shalon** (schema + CRUD/definition); status field is flipped by **Dinil**'s reservation logic (internal service call, not a new public endpoint)

```jsonc
{
  "_id": "ObjectId",
  "stationId": "ObjectId  -- references SolarStations._id",
  "slotNumber": "number",
  "type": "Charging" | "Discharging",
  "capacityKWh": "number",
  "status": "Available" | "Reserved" | "Maintenance",
  "updatedAt": "ISODate"
}
```

### 2.4 `Reservations` — owner: **Dinil** (writes: create/update/cancel + QR issuance); **Migara** (reads: list/search/filter/aggregate; one write exception: `verify-qr` flips `Confirmed → Completed`)

```jsonc
{
  "_id": "ObjectId",
  "prosumerNic": "string  -- references Users.nic",
  "stationId": "ObjectId",
  "slotId": "ObjectId",
  "scheduledAt": "ISODate",
  "status": "Confirmed" | "Completed" | "Cancelled",
  "qrToken": "string      -- issued immediately at creation, see §5",
  "qrTokenExpiresAt": "ISODate",
  "createdAt": "ISODate",
  "updatedAt": "ISODate",
  "completedAt": "ISODate | null",
  "completedByUserId": "ObjectId | null   -- the Grid Operator who scanned it",
  "cancelReason": "string | null"
}
```

**Architectural decision worth defending in viva:** there is no separate "Pending → Approved" manual step. Because the FAT service already validates everything synchronously at creation time (slot availability, the 7-day window), a reservation is created directly as `Confirmed` and its QR token is issued in the same request. This removes an approval gate nobody in the new module split explicitly owns, and it matches the spec's own wording that a prosumer "reserves... slots" and "once approved, the app generates a... QR code" — here, "approved" = passed server-side validation at creation. Grid Operators still get real oversight: they monitor bookings through Migara's Operator Dashboard (mobile) and a read-only reservations panel on Shalon's Station Detail screen (web) — see §6.

---

## 3. RESTful API Contract

Base URL once deployed: `https://<iis-host>/api`. JWT bearer auth on every route except registration/login. Claims: `sub`, `role`, and `nic` (Prosumers only).

### Owned by Rukshan — Auth, Roles, System Users, Prosumer Admin

| Verb & Path | Request Body | Response | Notes |
|---|---|---|---|
| `POST /auth/login` | `{ username, password }` | `{ token, role, fullName }` | Web login, Backoffice/GridOperator only; rejects `Deactivated` |
| `GET /users` | — | `[{ id, username, role, fullName, status }]` | Backoffice only |
| `POST /users` | `{ username, password, role, fullName, email }` | `{ id }` | Backoffice only; role ∈ {Backoffice, GridOperator} |
| `PUT /users/{id}` | `{ fullName?, email?, role? }` | `204` | Backoffice only |
| `PUT /users/{id}/deactivate` | — | `204` | Backoffice only |
| `GET /admin/prosumers` | query: `status?` | `[{ nic, fullName, status, ... }]` | Backoffice only |
| `GET /admin/prosumers/pending` | — | `[{ nic, fullName, createdAt }]` | Backoffice only |
| `PUT /admin/prosumers/{nic}/activate` | — | `204` | `PendingActivation → Active` only |
| `PUT /admin/prosumers/{nic}/reactivate` | — | `204` | **Backoffice only — 403 for GridOperator**, enforced server-side |
| `PUT /admin/prosumers/{nic}/deactivate` | `{ reason? }` | `204` | Backoffice-initiated force deactivation |

Also owned by Rukshan (shared infrastructure, used by all controllers): `Services/JwtService.cs` (issue + validate), the `[Authorize(Roles="...")]` policy wiring in `Program.cs`, and `BCrypt` password hashing helper.

### Owned by Shalon — Microgrid Nodes & Capacity

| Verb & Path | Request Body | Response | Notes |
|---|---|---|---|
| `GET /stations` | query: `search?, status?` | `[{ id, name, location, capacityKWh, totalBatterySlots, status }]` | All authenticated roles. `totalBatterySlots` added 2026-09-20 for the web Stations table's "slot count" column (backward-compatible addition, not in the original spec) |
| `GET /stations/{id}` | — | full station + its slots | All authenticated roles |
| `POST /stations` | `{ name, lat, lng, capacityKWh, totalBatterySlots, operatingSchedule }` | `{ id }` | Backoffice only |
| `PUT /stations/{id}` | partial fields | `204` | Backoffice only |
| `PUT /stations/{id}/deactivate` | — | `204` / `409` | Backoffice only; **409 if any Slot is `Reserved` or any Reservation is `Confirmed` at this station** |
| `PUT /stations/{id}/schedule` | `{ opensAt, closesAt }` | `204` | Backoffice only |
| `GET /stations/{id}/slots` | — | `[{ id, slotNumber, type, capacityKWh, status }]` | All authenticated roles |
| `POST /stations/{id}/slots` | `{ slotNumber, type, capacityKWh }` | `{ id }` | Backoffice only |
| `PUT /slots/{id}` | partial fields | `204` | Backoffice only |
| `GET /stations/{id}/reservations-overview` | — | `[{ slotId, status, prosumerNic?, scheduledAt? }]` | **Read-only**, for the web Station Detail "who's booked what" panel — reads Dinil's `Reservations` collection, see §6 |

### Owned by Dinil — Prosumer Mobile Auth & Reservation Lifecycle

| Verb & Path | Request Body | Response | Notes |
|---|---|---|---|
| `POST /prosumers/register` | `{ nic, fullName, email, phone, address, password }` | `{ id }` | Public; NIC uniqueness; creates with `PendingActivation` |
| `POST /auth/prosumer/login` | `{ nic, password }` | `{ token, status }` or `403` | Rejects `PendingActivation`/`Deactivated` with distinct codes |
| `GET /prosumers/{nic}` | — | profile | Self or Backoffice |
| `PUT /prosumers/{nic}` | `{ fullName?, email?, phone?, address? }` | `204` | Self only (JWT `nic` must match route) |
| `PUT /prosumers/{nic}/request-deactivation` | — | `204` | Self only |
| `POST /reservations` | `{ stationId, slotId, scheduledAt }` | `{ id, status, qrToken, qrTokenExpiresAt }` | **7-day rule**; slot must be `Available`; sets Slot → `Reserved`; issues QR immediately |
| `PUT /reservations/{id}` | `{ slotId?, scheduledAt? }` | `204` / `409` | **12-hour rule**; re-validates 7-day window against new time |
| `PUT /reservations/{id}/cancel` | `{ reason? }` | `204` / `409` | **12-hour rule**; sets Slot back to `Available` |
| `GET /reservations/{id}` | — | full reservation | Owning Prosumer or GridOperator |

### Owned by Migara — Grid Operator Verification, Maps, Dashboards

| Verb & Path | Request Body | Response | Notes |
|---|---|---|---|
| `POST /reservations/verify-qr` | `{ qrToken }` | `{ reservationId, prosumerName, stationName, slotNumber, status }` / `4xx` | GridOperator only; validates signature + expiry + `status == Confirmed`; flips to `Completed` |
| `GET /stations/nearby` | query: `lat, lng, radiusKm` | `[{ id, name, location, distanceKm }]` | `$geoNear` aggregation on Shalon's `SolarStations` collection |
| `GET /reservations` | query: `nic?, stationId?, status?, from?, to?` | `[{ ...reservation }]` | Prosumer restricted to own `nic` (JWT-enforced); GridOperator unrestricted — powers both dashboards' history/search |
| `GET /dashboard/prosumer/{nic}/summary` | — | `{ activeCount, pendingCount, approvedFutureCount, recentHistory: [...] }` | Self or Backoffice |
| `GET /dashboard/operator/summary` | — | `{ confirmedTodayCount, completedTodayCount, pendingByStation: [...] }` | GridOperator |

This table is the shared contract. If any owner needs to change their own route's shape, they update this file the same day and post it in the group chat — everyone else's client code depends on these exact shapes.

---

## 4. Cross-Module Dependency Map (so nobody gets silently blocked)

| Dependency | From → To | What's needed |
|---|---|---|
| Prosumer login | Dinil → Rukshan | `JwtService.Issue()` method signature agreed Day 1; Dinil calls it, doesn't reimplement it |
| Reservation creation checks slot | Dinil → Shalon | Read access to `EnergyBookingSlots`; Dinil's service calls a shared `SlotService.MarkReserved(slotId)` / `MarkAvailable(slotId)` method that Shalon exposes internally |
| QR issuance & verification | Dinil ↔ Migara | Shared `Services/QrTokenService.cs` with `Issue(reservationId, expiry)` and `Verify(token)` methods — **pair-build this file on Day 2**, don't let two people edit it independently later |
| Station deactivation block | Shalon → Dinil | Shalon's `StationsController` queries `Reservations` (read-only) before allowing deactivation — needs Dinil's collection/status names finalized Day 1 |
| Nearby stations query | Migara → Shalon | 2dsphere index must exist on `SolarStations.location` before Migara can query it — Shalon creates this index Day 1–2 |
| Dashboard aggregations | Migara → Dinil | Reads `Reservations`; needs Dinil's status enum (`Confirmed/Completed/Cancelled`) finalized Day 1 |
| Web Station Detail "who's booked" panel | Shalon → Dinil | Read-only query into `Reservations` filtered by `stationId` |

**Practical takeaway:** lock §2 and §3 of this document in the Day 1 meeting, before anyone writes a model class. Every cross-module dependency above is a *read* of an already-agreed shape, or a *call* into a small shared service — nobody needs to wait for someone else's controller to be fully built, only for the shape to be agreed.

---

## 5. QR Token Design (Dinil issues, Migara verifies)

- `QrTokenService.Issue(reservationId, expiryUtc)` → HMAC-SHA256 over `reservationId|expiryUtc` using a server-only secret from `appsettings.json`, base64url-encoded, returned as `qrToken`. Stored on the Reservation document, never regenerated.
- `QrTokenService.Verify(token)` → recomputes the HMAC, checks it matches, checks `qrTokenExpiresAt > now`, and — in `ReservationsService` — checks `status == Confirmed` (a token for an already-`Completed`/`Cancelled` reservation is rejected, which is what stops a QR being replayed).
- The Android app never generates or validates this token — it only renders `qrToken` as a QR **image** (Dinil, via ZXing's `QRCodeWriter`) and scans + forwards the decoded string (Migara, via CameraX + ML Kit/ZXing) to `POST /reservations/verify-qr`.

---

## 6. Client Responsibilities & Screen Ownership

| Client | Screens | Owner |
|---|---|---|
| Web — Login + role redirect | `LoginPage.jsx` | Rukshan |
| Web — System Users CRUD | `UsersPage.jsx` | Rukshan |
| Web — Prosumer Management + Pending Activations | `ProsumersPage.jsx`, `PendingProsumersPage.jsx` | Rukshan |
| Web — Microgrid Hubs (table, search, create/edit) | `StationsPage.jsx` | Shalon |
| Web — Station Detail (live slots, battery status, schedule, **read-only reservations panel**) | `StationDetailPage.jsx` | Shalon |
| Android — Prosumer Register/Login/Profile | `RegisterActivity`, `LoginActivity`, `ProfileActivity` | Dinil |
| Android — Booking workflow (create/modify/cancel, summary, QR display) | `ui/prosumer/*` | Dinil |
| Android — Operator mode, QR Scanner | `ui/operator/ScanQrActivity` | Migara |
| Android — Nearby Stations Map | `ui/operator/MapActivity` | Migara |
| Android — Prosumer & Operator Dashboards | `ui/dashboard/*` | Migara |

Neither client computes a business rule anywhere in this table — every screen above calls one of the endpoints in §3 and renders the result, including error/rejection messages verbatim from the API.

---

## 7. Rubric Accounting (so everyone sees exactly where their 65 marks come from)

| Table 2 Criterion | Marks | Owner | 
|---|---|---|
| Login and role-based access | 4 | Rukshan |
| User Management | 4 | Rukshan |
| Microgrid Node Management | 5 | Shalon |
| Slot Booking Management (slot/station administration) | 5 | Shalon |
| Mobile Authentication and Account Management | 9 | Dinil |
| Reservation Workflow and Booking Management | 9 | Dinil |
| Booking Views and Operational Dashboards | 10 | Migara |
| Grid Operator Verification and Map Features | 7 | Migara |
| Service Integration — Web app→API (2) | split | Shalon (1) + Rukshan (1) |
| Service Integration — Mobile app→API (2) | split | Dinil (1) + Migara (1) |
| Service Integration — SQLite persistence (3) | 3 | Dinil (primary; session/token + reservation cache) |
| Service Integration — Google Maps (3) | 3 | Migara |
| Service Integration — QR scanning (2) | 2 | Migara |
| **Running totals (approx.)** | 65 | **Shalon ≈ 11, Rukshan ≈ 9, Dinil ≈ 22, Migara ≈ 23** |

This confirms the caveat in §0 numerically: Dinil and Migara's individual-criterion totals are roughly double Shalon and Rukshan's, purely because the rubric weights mobile criteria more heavily. All four still write one complete, defensible full-stack module each, and all four earn the same Table 1 group marks (35) regardless.

---

## 8. Monorepo Folder Structure (no merge chaos)

One repository, physically separated by owner wherever two people would otherwise touch the same file.

```
smart-microgrid/
├── README.md
├── opening-screen-screenshot.png
├── Report/
│   ├── IT_NUMBER_Report.pdf
│   └── diagrams/ (architecture, use-case, DFD)
├── WebService/                              # C# Web API — one project, files split by owner
│   └── SmartMicrogrid.Api/
│       ├── Controllers/
│       │   ├── AuthController.cs            # Rukshan
│       │   ├── UsersController.cs           # Rukshan
│       │   ├── AdminProsumersController.cs  # Rukshan
│       │   ├── StationsController.cs        # Shalon
│       │   ├── SlotsController.cs           # Shalon
│       │   ├── ProsumerAuthController.cs    # Dinil
│       │   ├── ReservationsController.cs    # Dinil
│       │   ├── VerificationController.cs    # Migara
│       │   ├── NearbyController.cs          # Migara
│       │   └── DashboardController.cs       # Migara
│       ├── Services/
│       │   ├── JwtService.cs                # Rukshan (shared — everyone consumes, only Rukshan edits)
│       │   ├── StationService.cs            # Shalon
│       │   ├── SlotService.cs               # Shalon (exposes MarkReserved/MarkAvailable for Dinil)
│       │   ├── ReservationService.cs        # Dinil
│       │   ├── QrTokenService.cs            # Dinil + Migara (pair-built Day 2, see §4)
│       │   └── DashboardAggregationService.cs # Migara
│       ├── Models/ (one file per entity — whoever owns the collection owns the model file)
│       ├── Data/MongoDbContext.cs           # built jointly Day 1, then frozen
│       ├── Middleware/ExceptionHandlingMiddleware.cs  # built jointly Day 1
│       ├── Program.cs                       # built jointly Day 1, then only touched together
│       └── appsettings.json
├── WebApp/                                  # React + Tailwind
│   └── src/
│       ├── pages/
│       │   ├── auth/LoginPage.jsx           # Rukshan
│       │   ├── admin/UsersPage.jsx          # Rukshan
│       │   ├── admin/ProsumersPage.jsx      # Rukshan
│       │   ├── admin/PendingProsumersPage.jsx # Rukshan
│       │   ├── stations/StationsPage.jsx    # Shalon
│       │   └── stations/StationDetailPage.jsx # Shalon
│       ├── components/ (shared — agree ownership per component, keep small)
│       ├── services/api.js                  # built jointly Day 1, then frozen
│       └── context/AuthContext.jsx          # built jointly Day 1
└── MobileApp/                               # Native Android, single project
    └── app/src/main/java/com/smartmicrogrid/
        ├── data/local/
        │   ├── AppDbHelper.kt               # built jointly Day 1 — see merge-conflict note below
        │   ├── ProsumerSessionDao.kt         # Dinil
        │   └── ReservationCacheDao.kt        # Dinil
        ├── data/remote/ApiClient.kt          # built jointly Day 1, then frozen
        ├── ui/prosumer/ (Register/Login/Profile/Booking/QR display) # Dinil
        ├── ui/operator/ (ScanQr/Map)          # Migara
        ├── ui/dashboard/ (Prosumer & Operator dashboards) # Migara
        └── util/ (QR rendering helper, Maps helper)
```

**Avoiding the single-file merge conflict on `AppDbHelper.kt`:** don't let both mobile developers edit the same `onCreate()` method body on different days. Structure it as:
```kotlin
// AppDbHelper.kt — built jointly Day 1, frozen after
override fun onCreate(db: SQLiteDatabase) {
    db.execSQL(ProsumerSessionDao.CREATE_TABLE)   // Dinil owns this constant, in his own file
    db.execSQL(ReservationCacheDao.CREATE_TABLE)  // Dinil owns this constant, in his own file
}
```
Each person's `CREATE TABLE` string lives as a constant in *their own* DAO file; `AppDbHelper.kt` itself is written once on Day 1 and essentially never touched again, which is what actually prevents the conflict — not the pattern above by itself.

**Excluded from the repo (`.gitignore` at each sub-project root):** `node_modules/`, `bin/`, `obj/`, `.gradle/`, `build/`.

**Comment header block — required on every `.cs` file, spec is explicit that files without one are not marked:**
```csharp
// ============================================================
// File: ReservationsController.cs
// Purpose: Handles reservation lifecycle (create/update/cancel)
//          and QR issuance at creation, per the FAT service
//          pattern — all validation happens here, not in clients.
// Author: Dinil
// ============================================================
```
Every method additionally needs its own opening inline comment (one line describing what the method does, before its logic). Apply the same discipline (header + per-method comment) to `.jsx` and `.kt` files even though the spec's hard "not marked" rule names `.cs` specifically — consistent commenting is what the Table 1 "Documentation" band and a viva panel actually respond well to.

**Branching:** one branch per person (`shalon-stations`, `rukshan-auth-admin`, `dinil-reservations`, `migara-verification-maps`), merged to `main` at each integration checkpoint in `plan.md`. Commit messages must be specific (`feat(reservations): enforce 12-hour cancellation rule`) — this commit log **is** the individual-contribution evidence a viva panel checks against the README.
