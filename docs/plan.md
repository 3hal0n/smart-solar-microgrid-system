# Smart Solar Microgrid Trading System — Development & Integration Plan (v2)

**Today:** Wednesday, 16 September 2026
**Deadline:** Wednesday, 30 September 2026, 11:59 PM (no late submissions; viva compulsory)
**Target internal completion:** Saturday, 26 September — 26–30 Sep is buffer for integration, IIS deployment, report, and video, not new features.

Read `architecture.md` first — it is the frozen contract (§2 schema, §3 endpoints, §4 dependencies) every day below assumes is fixed. Because each person now owns a full vertical slice (their own backend + their own client screens), most days run **in parallel** rather than backend-then-frontend — the exceptions are the explicit dependency points from `architecture.md` §4, called out below wherever they land.

---

## Ground rules

1. **Day 1 is entirely joint work** — the shared scaffolding in `architecture.md` §8 (`MongoDbContext.cs`, `Program.cs`, `api.js`, `AuthContext.jsx`, `ApiClient.kt`, `AppDbHelper.kt`) gets built together, on a call, before anyone splits off. Getting this wrong on Day 1 is what causes merge chaos on Day 10.
2. **No client-side business logic, ever** — a 7-day/12-hour/uniqueness/role check duplicated in React or Kotlin is a rubric point lost, regardless of whose module it's in.
3. **Screenshot every screen the moment it works**, into `Report/screenshots/`, named by owner and screen (`shalon-stations-list.png`) — required, and "not unique" screenshots aren't marked.
4. **Every `.cs` file gets its header block and every method its inline comment from the first line written** — see `architecture.md` §8 for the exact template.
5. **Daily 15-minute sync, fixed time (suggest 8:00 PM Colombo)** — since work is parallel, this is where cross-module dependency blockers surface early, not on the day they'd otherwise block someone.
6. **API mocking rule:** if your screen needs another module's endpoint before that owner has built it, don't wait — hardcode the exact JSON shape from `architecture.md` §3 as a temporary local mock (a static JSON object, or `json-server` for the web app) and swap in the real call the moment the real endpoint exists. This is what lets four people work in parallel from Day 2 instead of queuing behind each other.

---

## Milestone overview

| Milestone | Date | What must be true |
|---|---|---|
| M0 — Contract frozen, scaffolding built | 16 Sep (Wed) | `architecture.md` agreed; repo created; shared files built jointly; each person's dev environment runs |
| M1 — Each module's skeleton runs | 18 Sep (Fri) | Every owner has their own controller returning real (not mocked) data for at least one endpoint, and their own screen calling it |
| M2 — Core CRUD complete per module | 22 Sep (Tue) | Shalon: stations/slots CRUD. Rukshan: auth+users+prosumer admin CRUD. Dinil: prosumer auth+profile done, reservation create/update in progress. Migara: map + dashboard skeletons pulling real data |
| M3 — Full reservation-to-QR-to-verification loop works | 24 Sep (Thu) | Dinil's create+QR issuance ↔ Migara's scan+verify tested together end-to-end |
| M4 — Feature freeze | 26 Sep (Sat) | Every rubric line item in every person's `.md` file is functionally complete |
| M5 — Integration freeze | 28 Sep (Mon) | IIS deployment done; full cross-testing pass; every screen screenshotted |
| M6 — Docs & video | 29 Sep (Tue) | Report, diagrams, README, ≤5 min video drafted |
| M7 — Submission | 30 Sep (Wed), before 11:59 PM | Zip built per `architecture.md` §8 structure |

---

## Day-by-day schedule

### Wed 16 Sep — Day 1: Joint scaffolding (everyone, together, same call)
- Walk through `architecture.md` end to end; freeze §2 (schema) and §3 (endpoints) — changing these later is expensive for all four people, not just one.
- Create the repo with the structure in `architecture.md` §8; add `.gitignore`s.
- **Jointly build and commit** (pair up, don't split this): `MongoDbContext.cs` + `Program.cs` skeleton (Rukshan drives, since it's his auth infra that plugs in first), `WebApp/src/services/api.js` + `AuthContext.jsx` (Shalon drives), `MobileApp` project skeleton + `ApiClient.kt` + `AppDbHelper.kt` empty shell (Dinil drives, Migara pairs).
- Install: Rukshan — .NET 8 SDK + MongoDB Atlas free-tier cluster (lighter than a local Mongo install on 6 GB RAM). Shalon — same .NET SDK (she also builds a C# slice) + Node/Vite. Dinil & Migara — Android Studio, Kotlin, get a Google Maps API key issued today (it can take a few hours to activate).
- End of day: everyone can `git pull` and build their own folder against the shared scaffolding.

### Thu 17 Sep — Day 2: First real endpoint + first real screen per person; QR service pair-built
- **Rukshan:** `Users` model, `POST /auth/login` returning a real JWT with role claims.
- **Shalon:** `SolarStations` model + `GET/POST /stations` (basic, no business rules yet); `StationsPage.jsx` listing real data.
- **Dinil:** `POST /prosumers/register`, `POST /auth/prosumer/login`; `RegisterActivity`/`LoginActivity` calling them for real.
- **Migara:** pairs with Dinil for 1–2 hours on `QrTokenService.cs` (Issue + Verify methods, per `architecture.md` §5) — build this together now, before either of you needs it, so it's not a Day 10 scramble. Rest of the day: Maps SDK smoke test (one hardcoded marker) to confirm the API key works.

### Fri 18 Sep — Day 3: M1 checkpoint
- **All:** confirm your own module's first endpoint + first screen work end-to-end against the real API (not a mock).
- **Dinil/Migara:** resolve emulator-to-API networking now (`10.0.2.2` vs `localhost`) — always costs more time the later it's left.
- **Rukshan:** add `[Authorize(Roles=...)]` policy wiring + CORS policy for the web app's origin — everyone else's protected endpoints depend on this existing.

### Sat 19 Sep — Day 4
- **Rukshan:** `GET/POST/PUT /users` (system accounts) complete; `UsersPage.jsx`.
- **Shalon:** `POST /stations/{id}/slots`, `PUT /slots/{id}`; slot management UI nested in `StationDetailPage.jsx`.
- **Dinil:** `GET/PUT /prosumers/{nic}`; `ProfileActivity`.
- **Migara:** `GET /stations/nearby` against Shalon's real (if still basic) station data — confirm the 2dsphere index Shalon set up on Day 1 actually works; real markers on the map.

### Sun 20 Sep — Day 5 (catch-up day, no new features scheduled)

### Mon 21 Sep — Day 6
- **Rukshan:** `GET /admin/prosumers`, `GET /admin/prosumers/pending`, `PUT /admin/prosumers/{nic}/activate`, `.../reactivate` (with the Backoffice-only 403 check), `.../deactivate`; `ProsumersPage.jsx` + `PendingProsumersPage.jsx`.
- **Shalon:** `PUT /stations/{id}/deactivate` — the blocked-while-reservations-active rule. **Dependency:** this needs Dinil's `Reservations` collection status enum finalized (it was, on Day 1) — confirm field names match exactly before writing the query.
- **Dinil:** `POST /reservations` — 7-day rule + slot-availability check + QR issuance (calling the shared `QrTokenService.Issue`) + flipping the Slot to `Reserved` (calling Shalon's `SlotService.MarkReserved`). This is the single most cross-dependent endpoint in the system — expect to spend real time here.
- **Migara:** `GET /reservations` (list/filter, read-only) + start `DashboardAggregationService.cs`.

### Tue 22 Sep — Day 7: M2 checkpoint
- **Shalon:** finish the read-only "reservations overview" panel on `StationDetailPage.jsx` (reads Dinil's data) — first real cross-module UI dependency, test it together.
- **Rukshan:** polish + edge-case testing on everything built so far (uniqueness, role checks, pending-activation blocking login).
- **Dinil:** `PUT /reservations/{id}` (12-hour rule) + `PUT /reservations/{id}/cancel`; mobile Create/Edit/Cancel booking screens + the post-action summary screen.
- **Migara:** finish `GET /dashboard/prosumer/{nic}/summary` and `GET /dashboard/operator/summary`; start the mobile dashboard screens against real data.

### Wed 23 Sep — Day 8
- **Dinil:** QR display screen (render `qrToken` as a QR image via ZXing) once a reservation exists — this is pure client-side rendering of a server-issued string, no logic.
- **Migara:** `POST /reservations/verify-qr` — signature/expiry/status validation, flips to `Completed`; wire the mobile `ScanQrActivity` (CameraX + barcode decode) to call it.
- **Shalon/Rukshan:** UI polish pass on everything shipped so far; make sure Tailwind usage is consistent across both of your modules (shared component patterns, not two different visual styles).

### Thu 24 Sep — Day 9: M3 checkpoint — the full loop, tested live on a call
- **All, together:** Dinil creates a booking on his device → QR renders → Migara scans it on her device → server flips to `Completed` → Shalon's Station Detail panel and Migara's dashboard both reflect it within the same minute.
- This is the highest-value integration test in the project — it touches four people's code in one pass. Fix whatever breaks before moving on.

### Fri 25 Sep — Day 10
- **Rukshan:** SQLite isn't his slice, but his web polish continues — full manual pass on every Rukshan-owned screen against every edge case in `rukshan.md`.
- **Shalon:** same polish pass against `shalon.md`'s edge cases.
- **Dinil:** SQLite persistence — session/token caching + local reservation cache read-through, so the booking/history screens survive a dropped connection.
- **Migara:** map polish (marker info windows, empty/loading states), dashboard search/filter UI.

### Sat 26 Sep — Day 11: M4 checkpoint — feature freeze
- **All:** re-read your own `.md` file's rubric mapping and self-check every line item as functionally done. From tomorrow: bug fixes and polish only, no new features.

### Sun 27 Sep — Day 12: IIS deployment + cross-testing
- **Rukshan (with Shalon, since she also has backend deploy familiarity from her own C# slice):** deploy to IIS per `architecture.md` deployment notes; re-point both clients' base URL at the IIS host, re-test everything against it, not `localhost`.
- **All:** full manual test pass against the deployed API using each person's edge-case checklist (7-day/12-hour boundaries, NIC uniqueness, deactivation-block, reactivate-403, expired/reused QR).
- **All:** finish collecting every unique screenshot, correctly named.

### Mon 28 Sep — Day 13: Integration freeze — bugs only
- **All:** fix whatever Day 12 testing turned up. No new UI, no new endpoints.
- **Migara:** start assembling the report skeleton (structure + diagram placeholders) — see `migara.md`.

### Tue 29 Sep — Day 14: Report, README, video
- **Migara:** finalize diagrams (architecture/use-case/DFD from `architecture.md`), database design section, references, individual-contribution + challenges sections (collect each person's own paragraph, don't author their claims).
- **All:** send Migara final code snippets for the report's source-code appendix + your own challenges note.
- **Rukshan or Migara:** record the ≤5-minute demo video; upload (YouTube unlisted or OneDrive), get a shareable link.
- **All:** finalize `README.md` — Git repo link, explicit per-person contributions, video link.

### Wed 30 Sep — Day 15: Submission day (buffer, not a work day)
- **All:** final read-through of the report against `architecture.md` §8's folder structure.
- **Migara or Shalon:** build the final zip (correct IT number filename, no `node_modules`/`bin`/`obj`/`build`, opening-screen screenshot at the zip root).
- **All:** submit well before 11:59 PM. Confirm the viva date once announced — attendance is compulsory.

---

## Risk register

| Risk | Mitigation |
|---|---|
| Rukshan's 6 GB RAM struggles running .NET + a Node dev server simultaneously (he now also builds React) | Use MongoDB Atlas (not local), run only one heavy dev server at a time, use VS Code instead of full Visual Studio |
| `QrTokenService.cs` becomes a two-person merge conflict since Dinil and Migara both depend on it | Pair-build it once on Day 2 (`architecture.md` §4/§5), then treat it as frozen — changes go through a call, not a solo edit |
| Cross-module field-name drift (e.g. Dinil renames a `Reservations` status value after Shalon/Migara have already queried it) | `architecture.md` §2/§3 is the single source of truth — any change gets posted in the group chat same day |
| Google Maps API key billing/quota surprises | Set up key restrictions + a budget alert on Day 1 |
| Emulator-to-API networking issues | Solved explicitly on Day 3 (M1), before it can block Day 6's higher-stakes work |
| IIS deployment surfacing bugs invisible on `localhost` (CORS, connection strings) | Deploy on Day 12, not Day 14, leaving time to react |
| Report/video left to the last day | Report skeleton starts Day 13; screenshots collected continuously from Day 1 |

---

## Final "what do you have to hand in" checklist

- [ ] Single zip, correctly named with the IT number, containing all project directories/files
- [ ] Screenshot of the main opening menu/screen (zip root)
- [ ] Every `.cs` file has a comment header block; every method has an opening inline comment
- [ ] Every UI screenshot in the report is unique
- [ ] Report includes: all UI screenshots, architecture/use-case/DFD diagrams, database design, source code pasted as text, full reference list, individual contribution section, challenges section
- [ ] `README.md`: Git repo link (individual contributions clearly stated) + video link (≤5 min)
- [ ] Any non-original code snippet referenced **and** commented in-line
- [ ] No plagiarism — confirmed by the group
