# Dinil — Prosumer Mobile Lifecycle & Slot Reservation Module (Full-Stack Android)

Scope: the single largest individual-marks slice in the project, and a genuinely full-stack one — your own C# controller(s) enforcing the trickiest business rules in the whole system (7-day window, 12-hour notice), plus the native Android screens that call them. Read `architecture.md` first — you own the `Reservations` collection (§2.4), your endpoint contract is §3 "Owned by Dinil," and §5 covers the QR design you co-build with Migara.

## Your rubric ownership

| Rubric criterion (Table 2) | Marks | Your ownership |
|---|---|---|
| **Mobile Authentication and Account Management** | 9 | Full |
| **Reservation Workflow and Booking Management** | 9 | Full |
| Service Integration — "Mobile app to Web API" sub-item | 1 of 2 (shared with Migara) | Your half |
| Service Integration — "SQLite local persistence" | 3 | Primary |

That's roughly 22 of the 65 individual marks — the largest single share alongside Migara's, per `architecture.md` §7.

## Backend: `ProsumerAuthController.cs` + `ReservationsController.cs`

### Setup
Add `Models/Reservation.cs`, `Services/ReservationService.cs`, `Controllers/ProsumerAuthController.cs`, `Controllers/ReservationsController.cs`. Your registration/login endpoints write into the *shared* `Users` collection (Rukshan's model) — don't create a second user model, just target the same collection with `role = "Prosumer"`.

### Endpoints to build (see `architecture.md` §3 for exact shapes)
- `POST /prosumers/register` — validate NIC uniqueness (query `Users` where `nic` matches), hash the password (use Rukshan's shared BCrypt helper, don't reimplement it), create with `status = PendingActivation`.
- `POST /auth/prosumer/login` — call Rukshan's shared `JwtService.Issue(userId, "Prosumer", nic)`, don't write a second JWT implementation. Reject `PendingActivation` and `Deactivated` with **distinct** error codes so the mobile app can show the right message for each.
- `GET/PUT /prosumers/{nic}` — self-service profile view/edit; the JWT's `nic` claim must match the route's `nic`, reject otherwise.
- `PUT /prosumers/{nic}/request-deactivation` — self-service; sets `deactivationRequestedAt` (actual deactivation is Rukshan's admin action, this just flags the request).
- `POST /reservations` — **the 7-day rule**: `scheduledAt` must be between `now` and `now + 7 days`. Also check the target Slot's `status == Available` (query Shalon's `EnergyBookingSlots`). On success: call `SlotService.MarkReserved(slotId)` (Shalon's exposed method — don't touch the Slots collection directly, go through her service so the two of you don't collide on the same collection from two different files), call `QrTokenService.Issue(reservationId, expiryUtc)` (built jointly with Migara on Day 2), store the reservation as `status = Confirmed` with the QR token already attached.
- `PUT /reservations/{id}` — **the 12-hour rule**: reject with `409` if `scheduledAt - now < 12 hours` (checked against the *current* stored time, before applying the update); if a `slotId`/`scheduledAt` change is requested, re-validate the 7-day window against the new time too.
- `PUT /reservations/{id}/cancel` — same 12-hour guard; on success call `SlotService.MarkAvailable(slotId)`.
- `GET /reservations/{id}` — owning Prosumer or GridOperator.

## Mobile: Native Android (Kotlin), SQLite, QR display

### Shared setup (Day 1, with Migara — see `architecture.md` §8)
- Project skeleton, `ApiClient.kt` (Retrofit + OkHttp + Gson), `AppDbHelper.kt` empty shell.
- Package split: your screens live in `ui/prosumer/`, Migara's in `ui/operator/` — keep it that way to avoid file-level merge conflicts for the rest of the project.

### SQLite (raw `SQLiteOpenHelper`, not Room — matches the spec's literal wording)
```kotlin
// ProsumerSessionDao.kt — you own this file
const val CREATE_TABLE = """
    CREATE TABLE local_prosumer_session (
        nic TEXT PRIMARY KEY,
        auth_token TEXT,
        full_name TEXT, email TEXT, phone TEXT, address TEXT,
        status TEXT, last_synced_at INTEGER
    )"""

// ReservationCacheDao.kt — you own this file
const val CREATE_TABLE = """
    CREATE TABLE local_reservations (
        reservation_id TEXT PRIMARY KEY,
        station_id TEXT, station_name TEXT, slot_id TEXT,
        scheduled_at INTEGER, status TEXT, qr_token TEXT,
        last_synced_at INTEGER
    )"""
```
`AppDbHelper.onCreate()` just calls both `CREATE_TABLE` constants (built once on Day 1, then frozen — see `architecture.md` §8's merge-conflict note). Use a read-through-cache pattern: booking screens read SQLite first for instant display, then refresh from the API and upsert the cache — an unused local table won't earn the persistence marks even if it technically exists.

### Screens
- `RegisterActivity` — NIC, full name, email, phone, address, password → `POST /prosumers/register`. On success, show "pending activation by a Backoffice officer" — don't attempt to log the user in immediately.
- `LoginActivity` — handle three distinct API outcomes: success (store JWT + session in SQLite, route to Prosumer home), pending activation (informational message), deactivated (distinct message, no retry path implied).
- `ProfileActivity` — view/edit own profile (`GET/PUT /prosumers/{nic}`), deactivation-request button with a confirmation dialog (`PUT /prosumers/{nic}/request-deactivation`).
- `CreateBookingActivity` — station/slot picker, date/time picker capped at `today+7d` (client-side cap is a UX nicety only; the real 7-day check is server-side and you must handle its rejection gracefully if the client cap is ever bypassed, e.g. via device clock changes).
- `EditBookingActivity` — modify date/slot on a `Confirmed` reservation; surface the 12-hour rejection message verbatim if the API returns one.
- Cancel flow (from a booking list/detail view) — confirmation dialog → `PUT /reservations/{id}/cancel`.
- `BookingSummaryFragment`/dialog — shown after **every** create/update/cancel action (station, slot, date/time, resulting status). The rubric credits this as its own line item — don't fold it into a toast.
- QR display screen — once a reservation exists (it's `Confirmed` immediately, per `architecture.md` §2.4's design decision), render `qrToken` as a QR image via ZXing's `QRCodeWriter`. Purely a rendering step — you never validate the token yourself, that's Migara's job on scan.

## Testing checklist
- Register, confirm login is blocked with the correct message until Rukshan's admin screen activates it.
- Create a booking for now+6 days (succeeds) and attempt now+8 days (rejected) — confirm the UI shows the rejection cleanly.
- Edit/cancel a booking at 11 hours out (rejected) vs 20 hours out (succeeds).
- Turn off network and open the booking list → confirm cached SQLite data still renders with a "last synced" indicator.
- Confirm the QR appears immediately after creating a booking (not gated behind a separate approval step, per the architecture decision).
- Once Migara's scan flow exists (Day 9 checkpoint in `plan.md`): confirm a scanned/completed reservation's QR is rejected if scanned again.

## Screenshot checklist
Register, pending-activation message, login (success/pending/deactivated states), Profile view/edit, Deactivate-request confirmation, Create Booking, Edit Booking, Cancel confirmation, Booking Summary screen, QR display screen, offline/cached view of the booking list.
