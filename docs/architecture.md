# Smart Solar Microgrid Trading System — Technical Architecture (v3: Spec-Feature-Aligned Model)

**Module:** SE4040 Enterprise Application Development — Assignment 1
**Team:** Shalon (Microgrid Node Management — Web; Dashboard & Maps — Mobile), Rukshan (Prosumer Management — Web; Prosumer Account Control — Mobile), Dinil (Energy Slot Reservation Management — Web; Operator Mode — Mobile), Migara (User Management — Web; Reservation & QR Dispatch — Mobile)
**Deadline:** 30 September 2026, 11:59 PM

## 0. What changed, and why

**v1** split work by **layer** (one backend person, one web person, two mobile people) — clean architecture story, weak GitHub story: commit history showed one person touching every `.cs` file.

**v2 (Equal Vertical-Slice Model)** split work by **vertical slice**: each person owned one invented functional module end-to-end (their own collection, controller, and client screens). This showed everyone touching all three layers, but the module boundaries didn't map onto anything the assignment brief or rubric actually names — Shalon's module, for instance, accumulated three separate backend reassignments during implementation (nearby stations, prosumer dashboard, reservations search) as gaps in the original split surfaced one at a time.

**v3 (this version, 2026-09-24)** re-splits work along the **assignment brief's own section (a) Web Application / (b) Mobile Application bullets** — each person owns exactly one named Web feature and one named Mobile feature, taken verbatim from the spec:

| | Web feature | Mobile feature |
|---|---|---|
| **Shalon** | Microgrid Node Management | Dashboard & Maps |
| **Rukshan** | Prosumer Management | Prosumer Account Control |
| **Dinil** | Energy Slot Reservation Management | Operator Mode |
| **Migara** | User Management | Reservation & QR Dispatch |

This resolves Rukshan's earlier no-Android-Studio hardware constraint (now resolved — he owns a real mobile feature) and gives every person one full vertical slice again, just remapped: **Login/User Management and both QR-lifecycle directions moved between people** (see §3–§8 for exactly what that touches). Shalon's Web feature is unchanged from v2; her Mobile feature ("Dashboard & Maps") is exactly what the three v2-era exception reassignments already built toward, so that earlier work carries forward rather than being redone.

**Honest caveat, stated up front so nobody is surprised in viva — read this one carefully:** mapping v3's feature list onto the *official* rubric Table 2 (which only names Login/User Management/Microgrid Node Management/Slot Booking Management as **Web Application Features'** sub-items — "Prosumer Management" and "Energy Slot Reservation Management" aren't separately scored there) has a serious side effect: **Dinil's individual Table 2 total drops from ≈22 (v2) to ≈4** — he loses both "Mobile Authentication and Account Management (9)" and "Reservation Workflow and Booking Management (9)" to Rukshan and Migara, and his new web page doesn't correspond to any distinctly-scored rubric line. See §7 for the full accounting. **This needs an explicit team conversation before it's treated as settled** — it doesn't look like anyone's intended outcome, and might mean "Energy Slot Reservation Management" should map onto an existing rubric line differently than assumed here, or that the split needs rebalancing. Table 1's group marks (35, awarded identically to all four) are unaffected regardless.

---

## 1. High-Level Architecture (FAT Service Pattern)

```
   React + Tailwind Web App              Native Android App (Kotlin)
   [Shalon's Hub screens]                [Shalon's Dashboard/Map screens]
   [Rukshan's Prosumer Mgmt screens]     [Rukshan's Prosumer Account screens]
   [Dinil's Reservation Mgmt screen]     [Dinil's Operator Mode screens]
   [Migara's Login/User Mgmt screens]    [Migara's Booking/QR screens]
              │  REST/JSON                          │  REST/JSON  (+ local SQLite cache)
              └───────────────┬──────────────────────┘
                               ▼
                 C# ASP.NET Core Web API (IIS)
                 ── FAT Service: ALL business rules live here ──
                 Controllers: Auth, Users (Migara)
                              AdminProsumers (Rukshan)
                              Stations, Slots, Nearby, Dashboard (Shalon)
                              ProsumerAuth (Rukshan)
                              Reservations, Verification (Dinil)
                               │
                               ▼
                         MongoDB (single database: SmartMicrogridDB)
                         Collections: Users, SolarStations, EnergyBookingSlots, Reservations
```

Both clients are **UI + local-cache layers only**. Neither computes a business rule, a role permission, or a QR signature — they call an endpoint and render what comes back. This is what Table 1's "Service Architecture and API Design (8)" and "Client Application Build and Architecture Compliance (12)" actually reward, and it's non-negotiable regardless of who owns which module.

---

## 2. MongoDB Schema — exact field specifications

Database: `SmartMicrogridDB`. Four collections, as required by the spec.

### 2.1 `Users` — owner: **Migara** (model + admin/auth endpoints, moved from Rukshan 2026-09-24 — see §0/§7); **Rukshan** writes to it from the prosumer self-registration endpoint (moved from Dinil, same date)

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

### 2.2 `SolarStations` — owner: **Shalon** (full CRUD; also reads it herself for the geospatial nearby-stations query, moved from Migara 2026-09-24 — see §0/§7)

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
Index: **2dsphere** on `location` (required for the `$geoNear`/`$geoWithin` query — now entirely Shalon's, both the index and the query that uses it, per §0/§7).

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

### 2.4 `Reservations` — owner: **Dinil** (writes: create/update/cancel + QR issuance, **and now also** `verify-qr`'s `Confirmed → Completed` flip, moved from Migara 2026-09-24 — see §0/§7, since Operator Mode is now his); **Shalon** reads it (dashboard summary, reservations search, station deactivation block — unchanged, see §4)

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

**Architectural decision worth defending in viva:** there is no separate "Pending → Approved" manual step. Because the FAT service already validates everything synchronously at creation time (slot availability, the 7-day window), a reservation is created directly as `Confirmed` and its QR token is issued in the same request. This removes an approval gate nobody in the new module split explicitly owns, and it matches the spec's own wording that a prosumer "reserves... slots" and "once approved, the app generates a... QR code" — here, "approved" = passed server-side validation at creation. Grid Operators still get real oversight: they monitor bookings through Dinil's Operator Dashboard (mobile, moved from Migara 2026-09-24 — see §0/§7) and a read-only reservations panel on Shalon's Station Detail screen (web) — see §6.

---

## 3. RESTful API Contract

Base URL once deployed: `https://<iis-host>/api`. JWT bearer auth on every route except registration/login. Claims: `sub`, `role`, and `nic` (Prosumers only).

### Owned by Migara — User Management (moved from Rukshan 2026-09-24 — see §0/§7)

| Verb & Path | Request Body | Response | Notes |
|---|---|---|---|
| `POST /auth/login` | `{ username, password }` | `{ token, role, fullName }` | Web login, Backoffice/GridOperator only; rejects `Deactivated` |
| `GET /users` | — | `[{ id, username, role, fullName, status }]` | Backoffice only |
| `POST /users` | `{ username, password, role, fullName, email }` | `{ id }` | Backoffice only; role ∈ {Backoffice, GridOperator} |
| `PUT /users/{id}` | `{ fullName?, email?, role? }` | `204` | Backoffice only |
| `PUT /users/{id}/deactivate` | — | `204` | Backoffice only |

Also owned by Migara (shared infrastructure, used by all controllers, moved from Rukshan 2026-09-24): `Services/JwtService.cs` (issue + validate), the `[Authorize(Roles="...")]` policy wiring in `Program.cs`, and `BCrypt` password hashing helper. Everyone still consumes it the same way — only the maintainer changed.

### Owned by Rukshan — Prosumer Management (web) + Prosumer Account Control (mobile)

| Verb & Path | Request Body | Response | Notes |
|---|---|---|---|
| `GET /admin/prosumers` | query: `status?` | `[{ nic, fullName, status, ... }]` | Backoffice only |
| `GET /admin/prosumers/pending` | — | `[{ nic, fullName, createdAt }]` | Backoffice only |
| `PUT /admin/prosumers/{nic}/activate` | — | `204` | `PendingActivation → Active` only |
| `PUT /admin/prosumers/{nic}/reactivate` | — | `204` | **Backoffice only — 403 for GridOperator**, enforced server-side |
| `PUT /admin/prosumers/{nic}/deactivate` | `{ reason? }` | `204` | Backoffice-initiated force deactivation |
| `POST /prosumers/register` | `{ nic, fullName, email, phone, address, password }` | `{ id }` | **Moved from Dinil 2026-09-24** (see §0/§7). Public; NIC uniqueness; creates with `PendingActivation` |
| `POST /auth/prosumer/login` | `{ nic, password }` | `{ token, status }` or `403` | **Moved from Dinil 2026-09-24.** Rejects `PendingActivation`/`Deactivated` with distinct codes |
| `GET /prosumers/{nic}` | — | profile | **Moved from Dinil 2026-09-24.** Self or Backoffice |
| `PUT /prosumers/{nic}` | `{ fullName?, email?, phone?, address? }` | `204` | **Moved from Dinil 2026-09-24.** Self only (JWT `nic` must match route) |
| `PUT /prosumers/{nic}/request-deactivation` | — | `204` | **Moved from Dinil 2026-09-24.** Self only |

### Owned by Shalon — Microgrid Node Management (web) + Dashboard & Maps (mobile)

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
| `GET /stations/nearby` | query: `lat, lng, radiusKm` | `[{ id, name, location, distanceKm, capacityKWh, availableSlots }]` | **Moved from Migara 2026-09-21** (see §4/§7). `$geoNear` aggregation on Shalon's own `SolarStations` collection, restricted to `Active` stations, closest first. `capacityKWh`/`availableSlots` added 2026-09-24 for the mobile map's info window (backward-compatible addition, not in the original spec) |
| `GET /reservations` | query: `nic?, stationId?, status?, from?, to?` | `[{ ...reservation }]` | **Moved from Migara 2026-09-21** (see §4/§7). Read-only over Dinil's `Reservations` collection. Prosumer restricted to own `nic` (JWT-enforced); GridOperator unrestricted — powers both dashboards' history/search. **Not yet enforced**: Rukshan's JWT auth isn't wired up, so the Prosumer-nic restriction is a documented `TODO(Rukshan)` in the code, not live yet |
| `GET /dashboard/prosumer/{nic}/summary` | — | `{ activeCount, pendingCount, approvedFutureCount, recentHistory: [...] }` | **Moved from Migara 2026-09-21** (see §4/§7). Self or Backoffice. Read-only over Dinil's `Reservations` collection. Field semantics (undefined until this implementation): `activeCount` = all `Confirmed`; `pendingCount` = `Confirmed` with `scheduledAt <= now` (awaiting operator check-in); `approvedFutureCount` = `Confirmed` with `scheduledAt > now` — `pendingCount + approvedFutureCount == activeCount` |

### Owned by Dinil — Energy Slot Reservation Management (web) + Operator Mode (mobile)

**2026-09-24 update:** the five `/prosumers/*` self-service rows that used to live here moved to Rukshan's table above (Prosumer Account Control). In exchange, `POST /reservations/verify-qr` and `GET /dashboard/operator/summary` moved here from Migara — both are now Dinil's, matching "Operator Mode: scan the QR code, verify server data, finalize the transfer." Dinil now owns the *entire* Reservations write surface (create/update/cancel *and* verify/complete) plus its one read-heavy dashboard, which is why `QrTokenService.cs` (§5) no longer needs to be a two-person pair-built file — both its callers are Dinil's own controllers now.

| Verb & Path | Request Body | Response | Notes |
|---|---|---|---|
| `POST /reservations` | `{ stationId, slotId, scheduledAt }` | `{ id, status, qrToken, qrTokenExpiresAt }` | **7-day rule**; slot must be `Available`; sets Slot → `Reserved`; issues QR immediately. Backs both Dinil's own web reservation-management page and Migara's mobile booking screens (Migara → Dinil dependency, see §4) |
| `PUT /reservations/{id}` | `{ slotId?, scheduledAt? }` | `204` / `409` | **12-hour rule**; re-validates 7-day window against new time |
| `PUT /reservations/{id}/cancel` | `{ reason? }` | `204` / `409` | **12-hour rule**; sets Slot back to `Available` |
| `GET /reservations/{id}` | — | full reservation | Owning Prosumer or GridOperator |
| `POST /reservations/verify-qr` | `{ qrToken }` | `{ reservationId, prosumerName, stationName, slotNumber, status }` / `4xx` | **Moved from Migara 2026-09-24.** GridOperator only; validates signature + expiry + `status == Confirmed`; flips to `Completed` |
| `GET /dashboard/operator/summary` | — | `{ confirmedTodayCount, completedTodayCount, pendingByStation: [...] }` | **Moved from Migara 2026-09-24.** GridOperator |

### Migara — no backend endpoints of her own

Migara's Web feature (User Management, above) and Mobile feature (Reservation & QR Dispatch) are both built entirely on **other people's** endpoints: her web screens call the User Management table above (which she also owns, so that's still a vertical slice), and her mobile booking/QR-display screens call Dinil's `POST/PUT /reservations*` table above (a genuine cross-module dependency — see §4). This is fine and expected under v3: not every person's Mobile feature needs its own dedicated controller, since "Reservation & QR Dispatch" is a *client* (booking UI + QR rendering), not a distinct backend capability — the backend for it was always going to be the same Reservations write surface Dinil already owns.

This table is the shared contract. If any owner needs to change their own route's shape, they update this file the same day and post it in the group chat — everyone else's client code depends on these exact shapes.

---

## 4. Cross-Module Dependency Map (so nobody gets silently blocked)

**Rebuilt clean for v3, 2026-09-24** — this table had accumulated enough "moved/updated" annotations across the v2 exceptions that it was no longer readable at a glance; the full history is still in git log / earlier revisions of this file if needed. What follows is where things stand now.

| Dependency | From → To | What's needed |
|---|---|---|
| Prosumer login (mobile) | Rukshan → Migara | `JwtService.Issue()` method signature; Rukshan's Prosumer Account Control screens call it, don't reimplement it |
| Reservation creation checks slot | Dinil → Shalon | Read access to `EnergyBookingSlots`; Dinil's `ReservationService.cs` calls a shared `SlotService.MarkReserved(slotId)` / `MarkAvailable(slotId)` method Shalon exposes internally — **unchanged from v2**, since Dinil kept reservation-write ownership throughout |
| Station deactivation block | Shalon → Dinil | Shalon's `StationsController` queries `Reservations` (read-only) before allowing deactivation — needs Dinil's collection/status names, already finalized per §2.4 |
| Web Station Detail "who's booked" panel | Shalon → Dinil | Read-only query into `Reservations` filtered by `stationId` |
| Reservations search + prosumer dashboard summary | Shalon → Dinil | `GET /reservations` and `GET /dashboard/prosumer/{nic}/summary` (Shalon's own endpoints, per §3) read-only query Dinil's `Reservations` |
| Mobile booking & QR-display screens | Migara → Dinil | **New in v3.** Migara's "Reservation & QR Dispatch" mobile screens (create/modify/cancel a booking, render the QR image) call Dinil's `POST/PUT /reservations*` endpoints (§3) — Dinil owns that backend end to end, Migara only builds the client UI on top of it |
| Web reservation-management page | *(none — fully vertical)* | Dinil's new web page (§3/§6) calls his own existing `ReservationsController.cs` — no new dependency, he already owned this backend |
| Prosumer Account Control | *(none — fully vertical)* | Rukshan's mobile screens + `ProsumerAuthController.cs` (§3) both moved to him together — no cross-module call needed |
| User Management | *(none — fully vertical)* | Migara's web screens + `AuthController.cs`/`UsersController.cs` (§3) both moved to her together — no cross-module call needed |
| Nearby stations map, Prosumer Dashboard | *(none — fully vertical)* | Both moved to Shalon in v2's exception process (backend + screen together) — carried forward unchanged into v3, see §3/§6 |
| Operator Mode (scan/verify/finalize) + Operator Dashboard | *(none — fully vertical)* | Both now Dinil's — backend (`POST /reservations/verify-qr`, `GET /dashboard/operator/summary`) and mobile screens together, per §3/§6 |
| QR issuance & verification | *(none — no longer cross-person)* | **Simplified in v3.** `Services/QrTokenService.cs`'s `Issue()` and `Verify()` are both called from Dinil's own controllers now (reservation creation and verify-qr are both his) — this used to require a Day-2 pair-build between two different owners; now it doesn't |
| SQLite: session/token cache | *(none — fully vertical)* | `ProsumerSessionDao.kt` moves with Prosumer Account Control to Rukshan (§7/§8) |
| SQLite: reservation cache | *(needs a file-ownership handoff)* | `ReservationCacheDao.kt` moves from Dinil to Migara (§7/§8) — since `AppDbHelper.kt` is jointly-maintained and each person's `CREATE_TABLE` constant lives in *their own* DAO file (§8), this specific file needs to physically move (or Migara creates her own equivalent and Dinil's is retired) — flag this to both of them before either edits it, so it isn't done twice or conflictingly |

**Practical takeaway:** every dependency above is now either a straightforward *read* of an already-agreed shape, or fully vertical (no cross-module call at all) — v3 actually has *fewer* genuine cross-person dependencies than v2, since several backend+screen pairs that used to be split across two people (nearby/dashboard/QR) are now owned end-to-end by one person each.

---

## 5. QR Token Design (Dinil owns both ends server-side, per v3)

**2026-09-24 update:** in v2 this was a two-person pair-built file (Dinil issues, Migara verifies). Under v3, both call sites are Dinil's own controllers — `Issue()` from his `POST /reservations` (Energy Slot Reservation Management) and `Verify()` from his `POST /reservations/verify-qr` (Operator Mode) — so `QrTokenService.cs` is now a single-owner file. The *client-side* rendering/scanning still splits across two different people, just swapped from v2: Migara renders the QR image (Reservation & QR Dispatch), Dinil scans it (Operator Mode).

- `QrTokenService.Issue(reservationId, expiryUtc)` → HMAC-SHA256 over `reservationId|expiryUtc` using a server-only secret from `appsettings.json`, base64url-encoded, returned as `qrToken`. Stored on the Reservation document, never regenerated.
- `QrTokenService.Verify(token)` → recomputes the HMAC, checks it matches, checks `qrTokenExpiresAt > now`, and — in `ReservationsService` — checks `status == Confirmed` (a token for an already-`Completed`/`Cancelled` reservation is rejected, which is what stops a QR being replayed).
- The Android app never generates or validates this token — it only renders `qrToken` as a QR **image** (Migara, via ZXing's `QRCodeWriter` — moved from Dinil 2026-09-24) and scans + forwards the decoded string (Dinil, via CameraX + ML Kit/ZXing — moved from Migara 2026-09-24) to `POST /reservations/verify-qr`.

---

## 6. Client Responsibilities & Screen Ownership

| Client | Screens | Owner |
|---|---|---|
| Web — Login + role redirect | `LoginPage.jsx` | Migara (moved from Rukshan 2026-09-24, see §0/§7) |
| Web — System Users CRUD | `UsersPage.jsx` | Migara (moved from Rukshan 2026-09-24) |
| Web — Prosumer Management + Pending Activations | `ProsumersPage.jsx`, `PendingProsumersPage.jsx` | Rukshan *(unchanged)* |
| Web — Energy Slot Reservation Management (new 2026-09-24) | `ReservationsPage.jsx` | Dinil — his first web screen; calls his own `POST/PUT /reservations*` (§3), no cross-module dependency |
| Web — Microgrid Hubs (table, search, create/edit) | `StationsPage.jsx` | Shalon *(unchanged)* |
| Web — Station Detail (live slots, battery status, schedule, **read-only reservations panel**) | `StationDetailPage.jsx` | Shalon *(unchanged)* |
| Android — Prosumer Register/Login/Profile | `RegisterActivity`, `LoginActivity`, `ProfileActivity` | Rukshan (moved from Dinil 2026-09-24, see §0/§7) |
| Android — Booking workflow (create/modify/cancel, summary, QR display) | `ui/prosumer/*` (`BookingsScreen.kt`, `ProfileScreen.kt` stubs currently say `TODO(Dinil)` — **need updating to `TODO(Migara)`/`TODO(Rukshan)` respectively, see the note below**) | Migara (moved from Dinil 2026-09-24) |
| Android — Operator mode, QR Scanner | `ui/operator/ScanQrScreen.kt` (currently a `TODO(Migara)` stub — **needs updating to `TODO(Dinil)`**) | Dinil (moved from Migara 2026-09-24) |
| Android — Nearby Stations Map (device location, markers, info window) | `ui/operator/MapScreen.kt` | Shalon (moved from Migara in v2's exception process, carried forward — see §4/§7) |
| Android — Prosumer Dashboard (stat tiles, booking history, pending list, search/filter) | `ui/dashboard/ProsumerDashboardScreen.kt` | Shalon (moved from Migara in v2's exception process, carried forward — see §4/§7) |
| Android — Operator Dashboard | `ui/dashboard/OperatorDashboardScreen.kt` (currently a `TODO(Migara)` stub — **needs updating to `TODO(Dinil)`**) | Dinil (moved from Migara 2026-09-24) |

**Stub files not yet updated to match this table:** `BookingsScreen.kt`, `ProfileScreen.kt`, `ScanQrScreen.kt`, and `OperatorDashboardScreen.kt` were all written as placeholders during the Home nav-graph shell task, each with a `TODO(<old owner>)` comment matching v2's ownership. This restructuring changes who each TODO should name — the comments themselves haven't been updated yet as of this doc edit, so they're currently stale. Fix them before anyone starts on those screens for real, so nobody reads a `TODO(Dinil)` and assumes that's still accurate.

Neither client computes a business rule anywhere in this table — every screen above calls one of the endpoints in §3 and renders the result, including error/rejection messages verbatim from the API.

---

## 7. Rubric Accounting (so everyone sees exactly where their 65 marks come from)

**Rebuilt for v3, 2026-09-24.** The table below maps each **official rubric Table 2 criterion** (not the spec's prose feature-bullets from §0, which don't share the same names) onto whoever now builds the functionality it describes. Two of the spec bullets that moved between people in §0 — Rukshan's "Prosumer Management" and Dinil's "Energy Slot Reservation Management" — **aren't separately named anywhere in Table 2**, so they don't add new individually-scored marks on their own; see the caveat below the table.

| Table 2 Criterion | Marks | Owner |
|---|---|---|
| Login and role-based access | 4 | Migara (moved from Rukshan) |
| User Management | 4 | Migara (moved from Rukshan) |
| Microgrid Node Management | 5 | Shalon *(unchanged)* |
| Slot Booking Management (slot/station administration) | 5 | Shalon *(unchanged)* |
| Mobile Authentication and Account Management | 9 | Rukshan (moved from Dinil) |
| Reservation Workflow and Booking Management | 9 | Migara (moved from Dinil) |
| Booking Views and Operational Dashboards | 10 | Shalon *(unchanged from the v2 exception process)* |
| Grid Operator Verification and Map Features (7) | split | Dinil (2) + Shalon (5) |
| Service Integration — Web app→API (2) | shared | All four — see note below |
| Service Integration — Mobile app→API (2) | shared | All four — see note below |
| Service Integration — SQLite persistence (3) | split | Rukshan (2, primary — session/token) + Migara (1 — reservation cache) |
| Service Integration — Google Maps (3) | 3 | Shalon (moved from Migara; **fixes a bug** — the previous doc revision moved the whole map screen to Shalon but left this specific row un-updated) |
| Service Integration — QR scanning (2) | 2 | Dinil (moved from Migara) |
| **Running totals (approx., excluding the two "shared" rows)** | 61 of 65 | **Shalon ≈ 28, Rukshan ≈ 11, Dinil ≈ 4, Migara ≈ 18** |

**"All four" rows explained:** under v3, every person now builds at least one web screen *and* one mobile screen that calls the API (§6) — which is exactly what v2's original design in §0 wanted but didn't fully achieve. Subdividing 2 marks four ways isn't meaningful, so these two rows are left as a qualitative "everyone satisfies this" note rather than forced into fractional splits.

**🔴 Read this before treating the table above as settled — the same caveat as §0, restated with numbers:** Dinil's individual total falls from ≈22 (v2) to ≈4. He loses both 9-mark mobile criteria (moved to Rukshan and Migara) and gains only the 2+2 QR-adjacent marks, because his two new v3 features — "Energy Slot Reservation Management" (web) and half of "Operator Mode" — don't map onto separately-named Table 2 web criteria the way v2's invented module names happened to. This is the single biggest number in this whole document and it needs the team's explicit sign-off, not just a doc update:
- If it's genuinely intended (e.g. Dinil's web reservation-management page and backend ownership are considered valuable enough to justify a lower Table 2 total, compensated some other way, or the team simply accepts this given how the marks fall) — fine, but say so explicitly rather than let it stand by default.
- If it's *not* intended, the likely fix is deciding that "Energy Slot Reservation Management" should count toward an existing line (most plausibly folding into "Slot Booking Management" or splitting "Reservation Workflow and Booking Management" between Migara's mobile half and Dinil's new web half, rather than giving that whole 9-mark line to Migara alone) — that would need this table reworked again, not just the totals nudged.

As with every split in this document: approximate bookkeeping for individual-contribution evidence, not a precise formula. Table 1's group marks (35, awarded identically to all four) are unaffected regardless of how §7 shakes out.

---

## 8. Monorepo Folder Structure (no merge chaos)

One repository, physically separated by owner wherever two people would otherwise touch the same file.

**v3 (2026-09-24):** file ownership below reflects the §0 restructuring. Physical file *locations* mostly haven't moved yet (that's implementation work, not a doc-only change) — this tree states where each file **should** end up / who should be editing it next, and flags the handful of stub files whose comments still say the old (v2) owner.

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
│       │   ├── AuthController.cs            # Migara (moved from Rukshan 2026-09-24)
│       │   ├── UsersController.cs           # Migara (moved from Rukshan 2026-09-24)
│       │   ├── AdminProsumersController.cs  # Rukshan (unchanged)
│       │   ├── StationsController.cs        # Shalon (nearby folded in as an action, not a separate file)
│       │   ├── SlotsController.cs           # Shalon (unchanged)
│       │   ├── DashboardController.cs       # Shalon — GET /dashboard/prosumer/{nic}/summary (unchanged from v2's exception process)
│       │   ├── ReservationsSearchController.cs # Shalon — GET /reservations search/filter (unchanged from v2's exception process)
│       │   ├── ProsumerAuthController.cs    # Rukshan (moved from Dinil 2026-09-24)
│       │   ├── ReservationsController.cs    # Dinil (unchanged — write side: create/update/cancel)
│       │   ├── VerificationController.cs    # Dinil (moved from Migara 2026-09-24)
│       │   └── OperatorDashboardController.cs # Dinil (moved from Migara 2026-09-24) — GET /dashboard/operator/summary
│       ├── Services/
│       │   ├── JwtService.cs                # Migara (moved from Rukshan 2026-09-24 — shared, everyone consumes, only Migara edits)
│       │   ├── StationService.cs            # Shalon (also GetNearbyAsync — unchanged)
│       │   ├── SlotService.cs               # Shalon (exposes MarkReserved/MarkAvailable for Dinil, unchanged)
│       │   ├── DashboardService.cs          # Shalon — prosumer summary + reservations search (unchanged)
│       │   ├── ReservationService.cs        # Dinil (unchanged)
│       │   ├── QrTokenService.cs            # Dinil (single owner now, moved from the Dinil+Migara pair-build — see §5)
│       │   └── DashboardAggregationService.cs # Dinil (moved from Migara 2026-09-24) — GET /dashboard/operator/summary
│       ├── Models/ (one file per entity — whoever owns the collection owns the model file; exception: Shalon's `Reservation.cs` reads Dinil's collection type-safely — Dinil still owns its write-side business rules and should be consulted before the schema changes)
│       ├── Data/MongoDbContext.cs           # built jointly Day 1, then frozen
│       ├── Middleware/ExceptionHandlingMiddleware.cs  # built jointly Day 1
│       ├── Program.cs                       # built jointly Day 1, then only touched together
│       └── appsettings.json
├── WebApp/                                  # React + Tailwind
│   └── src/
│       ├── pages/
│       │   ├── auth/LoginPage.jsx           # Migara (moved from Rukshan 2026-09-24)
│       │   ├── admin/UsersPage.jsx          # Migara (moved from Rukshan 2026-09-24)
│       │   ├── admin/ProsumersPage.jsx      # Rukshan (unchanged)
│       │   ├── admin/PendingProsumersPage.jsx # Rukshan (unchanged)
│       │   ├── reservations/ReservationsPage.jsx # Dinil — NEW 2026-09-24, doesn't exist yet, his first web screen
│       │   ├── stations/StationsPage.jsx    # Shalon (unchanged)
│       │   └── stations/StationDetailPage.jsx # Shalon (unchanged)
│       ├── components/ (shared — agree ownership per component, keep small)
│       ├── services/api.js                  # built jointly Day 1, then frozen
│       └── context/AuthContext.jsx          # built jointly Day 1
└── MobileApp/                               # Native Android, single project
    └── app/src/main/java/com/smartmicrogrid/
        ├── data/local/
        │   ├── AppDbHelper.kt               # built jointly Day 1 — see merge-conflict note below
        │   ├── ProsumerSessionDao.kt         # Rukshan (moved from Dinil 2026-09-24 — session/token cache)
        │   └── ReservationCacheDao.kt        # Migara (moved from Dinil 2026-09-24 — needs an actual file handoff, see §4)
        ├── data/remote/ApiClient.kt          # built jointly Day 1, then frozen (still doesn't exist yet as of 2026-09-24 — see below)
        ├── ui/prosumer/
        │   ├── RegisterActivity, LoginActivity, ProfileActivity   # Rukshan (moved from Dinil 2026-09-24)
        │   ├── ProfileScreen.kt              # Rukshan — currently a stub with a stale `TODO(Dinil)` comment, needs updating (see §6)
        │   └── BookingsScreen.kt             # Migara (moved from Dinil 2026-09-24) — currently a stub with a stale `TODO(Dinil)` comment, needs updating (see §6)
        ├── ui/operator/
        │   ├── ScanQrScreen.kt                # Dinil (moved from Migara 2026-09-24) — currently a stub with a stale `TODO(Migara)` comment, needs updating (see §6)
        │   └── MapScreen.kt                   # Shalon (unchanged from v2's exception process)
        ├── ui/dashboard/
        │   ├── ProsumerDashboardScreen.kt     # Shalon (unchanged from v2's exception process)
        │   └── OperatorDashboardScreen.kt     # Dinil (moved from Migara 2026-09-24) — currently a stub with a stale `TODO(Migara)` comment, needs updating (see §6)
        └── util/ (QR rendering helper, Maps helper)
```

**`data/remote/ApiClient.kt` still doesn't exist anywhere in the repo** as of this revision — every mobile screen built so far (dashboard, map) has had to fall back to a hardcoded fixture with a `TODO(swap-in-real-api)` comment for exactly this reason (see those files). Building the shared Retrofit client is still nobody's *specific* v3 feature — it remains joint Day-1-style infrastructure everyone needs, same as `AppDbHelper.kt`.

**Avoiding the single-file merge conflict on `AppDbHelper.kt`:** don't let both mobile developers edit the same `onCreate()` method body on different days. Structure it as:
```kotlin
// AppDbHelper.kt — built jointly Day 1, frozen after
override fun onCreate(db: SQLiteDatabase) {
    db.execSQL(ProsumerSessionDao.CREATE_TABLE)   // Rukshan owns this constant, in his own file (moved from Dinil 2026-09-24)
    db.execSQL(ReservationCacheDao.CREATE_TABLE)  // Migara owns this constant, in her own file (moved from Dinil 2026-09-24)
}
```
Each person's `CREATE TABLE` string lives as a constant in *their own* DAO file; `AppDbHelper.kt` itself is written once on Day 1 and essentially never touched again, which is what actually prevents the conflict — not the pattern above by itself. The 2026-09-24 ownership move means this file's two `db.execSQL(...)` calls now reference constants in two DIFFERENT people's files than before — a one-line, low-risk edit to `AppDbHelper.kt` itself, but touch it deliberately and once, not as a side effect of either DAO move.

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

**Branching:** one branch per person. Existing branch names (`shalon-stations`, `rukshan-auth-admin`, `dinil-reservations`, `migara-verification-maps`) predate v3 and don't all match the new split — rename or just start new branches as `<name>/<current-feature>` per person going forward rather than forcing the old names to fit (e.g. Migara's next branch is about User Management, not "verification-maps" anymore). Merged to `main` at each integration checkpoint in `plan.md`. Commit messages must be specific (`feat(reservations): enforce 12-hour cancellation rule`) — this commit log **is** the individual-contribution evidence a viva panel checks against the README.
