# Migara — Operator Field Verification, Maps & Analytics Module (Full-Stack Android) + Report Lead

Scope: a full-stack module in its own right (backend verification/geospatial/aggregation logic + the Android screens that consume it), plus overall responsibility for the final report and video. Read `architecture.md` first — you co-own the QR verification design (§5) with Dinil, your endpoint contract is §3 "Owned by Migara," and you're a *reader* of Shalon's `SolarStations` and Dinil's `Reservations` collections (§4) — never a writer, except for the one specific field flip described below.

## Your rubric ownership

| Rubric criterion (Table 2) | Marks | Your ownership |
|---|---|---|
| **Booking Views and Operational Dashboards** | 10 | Full |
| **Grid Operator Verification and Map Features** | 7 | Full |
| Service Integration — "Mobile app to Web API" sub-item | 1 of 2 (shared with Dinil) | Your half |
| Service Integration — "Google Maps API integration" | 3 | Full |
| Service Integration — "QR code scanning" | 2 | Full |

That's roughly 23 of the 65 individual marks — the largest single share alongside Dinil's, per `architecture.md` §7. Your report/video responsibility is separate, real, graded work on top of this (Table 1's "Documentation and Deployment," 5 group marks) — don't let it eat into the code slice above; start the report skeleton on Day 13 per `plan.md`, not earlier.

## Backend: `VerificationController.cs`, `NearbyController.cs`, `DashboardController.cs`

### Setup
Add `Services/QrTokenService.cs` (pair-built with Dinil on Day 2 — see `architecture.md` §5), `Services/DashboardAggregationService.cs`, `Controllers/VerificationController.cs`, `Controllers/NearbyController.cs`, `Controllers/DashboardController.cs`. You read Shalon's `SolarStations` and Dinil's `Reservations` collections directly via MongoDB queries — you do not touch their C# model/service files.

### Endpoints to build (see `architecture.md` §3 for exact shapes)
- `POST /reservations/verify-qr` — GridOperator only. `QrTokenService.Verify(token)` recomputes the HMAC signature and checks `qrTokenExpiresAt > now`; then check the reservation's current `status == Confirmed` (reject if already `Completed` — this is what stops a QR being scanned twice, and it's the single most important thing to demonstrate working correctly in viva). On success: set `status = Completed`, `completedAt = now`, `completedByUserId = <the operator's id>`, and call `SlotService.MarkAvailable(slotId)` (Shalon's exposed method, same discipline as Dinil's — don't touch the Slots collection directly).
- `GET /stations/nearby?lat&lng&radiusKm` — `$geoNear` (or `$geoWithin`/`$centerSphere`) aggregation against Shalon's `SolarStations.location` 2dsphere index. **Dependency:** confirm with Shalon that the index exists before writing this query (Day 1–4 per `plan.md`).
- `GET /reservations?nic=&stationId=&status=&from=&to=` — read-only list/search/filter over Dinil's `Reservations` collection; Prosumer callers restricted to their own `nic` (enforced from the JWT, ignore any client-supplied `nic` that doesn't match), GridOperator unrestricted. This single endpoint powers both dashboards' history and search.
- `GET /dashboard/prosumer/{nic}/summary` — aggregation pipeline: active/pending counts, count of approved (i.e., `Confirmed`) future reservations, recent history. Design the response shape so your own mobile dashboard screen is a near-direct render of it.
- `GET /dashboard/operator/summary` — counts of `Confirmed`/`Completed` today, pending-by-station breakdown.

## Mobile: Native Android (Kotlin), Camera, Maps

### Shared setup (Day 1, with Dinil — see `dinil.md` and `architecture.md` §8)
Your screens live in `ui/operator/` and `ui/dashboard/`, kept separate from Dinil's `ui/prosumer/` to avoid file conflicts.

### QR Scanning — "Read QR code and update the job as done" (2 of 7) + "QR code scanning" (2 of 12)
- CameraX for the live preview + ML Kit Barcode Scanning or ZXing Android Embedded for decoding.
- `ScanQrActivity`: on a successful decode, extract the token and call `POST /reservations/verify-qr`. Show the server's response clearly — reservation details on success, or the specific rejection reason (already-used/expired/invalid) verbatim on failure; the server is the authority on why it failed, don't reinterpret the reason client-side.
- Runtime camera-permission handling with a graceful denial message — a scanner that crashes without permission is an easy, avoidable deduction.

### Google Maps — "Show nearby stations on the map" (5 of 7) + "Google Maps API integration" (3 of 12)
- Maps SDK for Android, API key restricted to the app's package name + SHA-1 (get this issued Day 1 per `plan.md` — activation can take hours).
- `MapActivity`: center on device location (fallback to a sensible default if permission denied), call `GET /stations/nearby`, plot a marker per station, tap-to-show an info window (name, capacity, available slots). This 5-mark sub-item is the heaviest single line in your Grid Operator criterion — invest real polish: loading state, empty state, smooth camera-fit animation.

### Dashboards — full 10 marks
- `ui/dashboard/ProsumerDashboardFragment` and `OperatorDashboardFragment` — stat tiles from your two summary endpoints, a booking-history list and a pending-bookings view both backed by your `GET /reservations` search/filter endpoint, with visible filter controls (status, date range).
- Since you own both the aggregation backend and both dashboard screens, make sure the two screens (prosumer-facing vs operator-facing) are visibly distinct in purpose even though they share a query endpoint — a marker should immediately see which is which.

## Testing checklist
- Scan a valid, unexpired, `Confirmed` QR → success, reservation flips to `Completed`; re-scan the same QR → rejected with a clear "already completed" message, not a crash.
- Deny location permission → map still loads with a default center, no crash.
- Cross-check map markers against Shalon's live Stations data (create a station on the web app, confirm it appears on your map within the nearby-radius test).
- Confirm dashboard counts change correctly after a booking is created (Dinil), and again after it's completed (you) or cancelled (Dinil).

## Screenshot checklist (mobile)
Operator home, QR scan screen (camera live), scan result (success + rejected/already-used states), nearby stations map with markers + an open info window, Prosumer dashboard, Operator dashboard, booking history with a filter applied.

---

## Report & Submission Lead (separate from your code slice)

This is directly graded (Table 1 "Documentation and Deployment," 5 marks) and is what makes every other member's individual-contribution claim verifiable to a marker. Start on Day 13 per `plan.md`.

### Report structure
1. Cover page — module, assignment title, IT numbers, group members.
2. Introduction — the project scenario, in your own words.
3. High-level architecture diagram — from `architecture.md` §1, with the FAT service boundary clearly marked.
4. Use Case diagram — three actors (Backoffice, Grid Operator, Prosumer) against the major use cases.
5. DFD diagram — client ↔ API ↔ MongoDB collections, from `architecture.md` §2–3.
6. Database design — the four collections from `architecture.md` §2, formatted with real sample documents captured from the running database.
7. UI screenshots — collect every checklist from `shalon.md`, `rukshan.md`, `dinil.md`, and this file into one organized, clearly labeled section; every image unique.
8. Source code appendix — pasted as text (not screenshots); ask each member for their own 3–5 most representative files.
9. References — every tutorial/StackOverflow/doc source used by anyone, cross-checked against the in-code comments.
10. Individual contribution — one paragraph per member, written by each person themselves and compiled by you, matched against the Git commit history.
11. Challenges — one short note per member plus your own.

### README.md (repo root)
Git repo link, explicit per-person contributions, demo video link.

### Demo video (≤ 5 minutes) — suggested script
0:00–0:30 architecture recap · 0:30–1:30 Backoffice: create a station, create a system user, activate a pending prosumer (Shalon + Rukshan's work) · 1:30–2:30 Prosumer: register, log in, create a booking (Dinil's work) · 2:30–3:30 scan the QR, watch it complete, check the nearby-stations map (your work) · 3:30–4:30 both dashboards reflecting the completed transaction · 4:30–5:00 close.

### Final zip assembly
Follow `architecture.md` §8 exactly — no `node_modules`/`bin`/`obj`/`build`, opening-screen screenshot at the zip root, correct IT number in the filename.
