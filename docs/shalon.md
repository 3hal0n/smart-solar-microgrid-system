# Shalon — Microgrid Node & Capacity Management Module (Full-Stack Web)

Role note: you're Lead Developer/Architect *and* a full contributor — your commit history needs to show the same volume and shape (model + controller + UI) as everyone else's. Designing `architecture.md` doesn't count toward your code slice; the module below does.

Read `architecture.md` first — you own the `SolarStations` and `EnergyBookingSlots` collections (§2.2–2.3), your endpoint contract is §3 "Owned by Shalon," and the cross-module dependencies you're on both sides of are in §4.

## Your rubric ownership

| Rubric criterion (Table 2) | Marks | Your ownership |
|---|---|---|
| Microgrid Node Management | 5 | Full |
| Slot Booking Management (station/slot administration) | 5 | Full |
| Service Integration — "Web app to Web API" sub-item | 1 of 2 (shared with Rukshan) | Your half |

That's roughly 11 of the 65 individual marks — see `architecture.md` §7 for why this is lower than Dinil/Migara's totals despite equal code volume (the rubric simply grants mobile more distinct criteria). Your work also carries the two heaviest Table 1 group-mark drivers alongside Rukshan's: "Client Application Build and Architecture Compliance" and "User Interface and Experience Design" — those are awarded to all four equally, so polish here benefits everyone.

## Backend: `StationsController.cs` + `SlotsController.cs`

### Setup
1. `SmartMicrogrid.Api` is a shared solution (built jointly Day 1 — see `plan.md`). Add your own files without touching `Program.cs`/`MongoDbContext.cs` after Day 1 unless it's a joint call.
2. Add `Models/Station.cs`, `Models/Slot.cs`, `Services/StationService.cs`, `Services/SlotService.cs`, `Controllers/StationsController.cs`, `Controllers/SlotsController.cs`.

### Endpoints to build (see `architecture.md` §3 for exact shapes)
- `GET /stations` (search/status filter), `GET /stations/{id}` (station + its slots).
- `POST /stations`, `PUT /stations/{id}` — Backoffice only; validate GPS coordinates are numeric and in range, capacity > 0.
- `PUT /stations/{id}/deactivate` — **the business rule that matters most here**: before flipping `status` to `Inactive`, query `EnergyBookingSlots` for any `Reserved` status at this `stationId`, and query `Reservations` (Dinil's collection) for any `Confirmed` status at this `stationId`. If either exists, return `409` with a clear message. This is the rule a viva panel is most likely to ask you to demonstrate live — practice showing it fail, then succeed once cleared.
- `PUT /stations/{id}/schedule` — update `operatingSchedule`.
- `GET /stations/{id}/slots`, `POST /stations/{id}/slots`, `PUT /slots/{id}` — slot CRUD, capacity/status validation (a station's total reserved+maintenance slot count shouldn't exceed `totalBatterySlots`).
- `GET /stations/{id}/reservations-overview` — **read-only**, queries Dinil's `Reservations` collection filtered by `stationId`, returns just enough to show "slot X is booked by NIC Y at time Z" — this powers your web Station Detail panel and is your side of the "Grid Operators can access both apps to monitor bookings" scenario text in the spec.
- **2dsphere index on `SolarStations.location`** — create this in your `StationService`'s startup/seed logic on Day 1, even though you don't query it yourself; Migara's nearby-stations feature depends on it existing.

### Expose to other modules (don't let them re-implement this)
- `SlotService.MarkReserved(slotId)` / `MarkAvailable(slotId)` — internal methods, not new HTTP endpoints, called directly by Dinil's `ReservationService` when a booking is created/cancelled.

## Frontend: React + Tailwind

### Screens
- `pages/stations/StationsPage.jsx` — table (name, GPS, capacity, slot count, status), search box, "Create Station" button opening a modal (`StationForm.jsx`).
- `pages/stations/StationDetailPage.jsx` — station info, editable schedule, slot list (add/edit/status), and the **reservations-overview panel**: a simple read-only table (slot, prosumer NIC, scheduled time, status) fed by your new `GET /stations/{id}/reservations-overview` endpoint. This one panel is what closes the gap between your module and the original spec's "Grid Operators... monitor power trading bookings" via the web app.
- Deactivate button on both list and detail views: call the endpoint, and on `409` show the API's exact reason in a toast — don't pre-guess the block client-side.

### Integration
- Use the shared `services/api.js` (built jointly Day 1) — every call goes through it, no raw fetch URLs in components.
- Consistent Tailwind patterns (buttons, table, modal, toast) shared with Rukshan's screens — coordinate a shared `components/common/` folder so both your modules look like one product, not two.

## Testing checklist
- Create a station, add three slots, attempt to deactivate the station with no active reservations → succeeds.
- Once Dinil's reservation flow exists: create a reservation against one of your slots, attempt to deactivate the station → expect `409` with the correct reason; cancel/complete the reservation, retry → succeeds.
- Add a slot beyond the station's `totalBatterySlots` capacity → expect rejection.
- Confirm the reservations-overview panel updates within the same request cycle after Dinil's app creates/cancels a booking (test together on Day 7 per `plan.md`).

## Screenshot checklist
Stations list (search + create modal), Station Detail (info + schedule + slot list), slot create/edit form, deactivate confirmation (both success and blocked-with-reason states), reservations-overview panel with at least one live booking showing.
