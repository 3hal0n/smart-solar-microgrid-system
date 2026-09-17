# Rukshan — User Auth, Roles & Prosumer Admin Module (Full-Stack Web)

Hardware note: your constraint is "no Android Studio," not "no frontend." Your slice is now a full React + Tailwind + C# + MongoDB module, same shape as Shalon's — this is deliberate so your GitHub history shows the same end-to-end capability as everyone else's, not just backend commits.

Read `architecture.md` first — you own the `Users` collection (§2.1), your endpoint contract is §3 "Owned by Rukshan," and you own the shared `JwtService.cs` that Dinil's prosumer login depends on (§4).

## Your rubric ownership

| Rubric criterion (Table 2) | Marks | Your ownership |
|---|---|---|
| Login and role-based access | 4 | Full |
| User Management | 4 | Full |
| Service Integration — "Web app to Web API" sub-item | 1 of 2 (shared with Shalon) | Your half |

Roughly 9 of the 65 individual marks — see `architecture.md` §7 for why this is lower than Dinil/Migara's despite equal code volume. Your work underpins every other module's auth, though: nobody logs in, and no route is role-protected, without the infrastructure below — that's a real, defensible viva point even where it isn't the line item scoring the marks.

## Backend: `AuthController.cs`, `UsersController.cs`, `AdminProsumersController.cs`

### Setup
1. On Day 1 (jointly), you drive `MongoDbContext.cs` and the base `Program.cs` — you have the clearest need for the auth/role wiring to exist before anyone else's protected endpoints do.
2. Add `Models/User.cs`, `Services/JwtService.cs`, `Services/UserService.cs`, `Controllers/AuthController.cs`, `Controllers/UsersController.cs`, `Controllers/AdminProsumersController.cs`.

### Shared infrastructure you own (everyone else consumes, only you edit after Day 2)
- `JwtService.cs`: `Issue(userId, role, nic?)` → signed JWT with `sub`, `role`, `nic` claims and an expiry; `Validate(token)` used by ASP.NET's JWT bearer middleware.
- `Program.cs` policy wiring: `[Authorize(Roles = "Backoffice")]`, `[Authorize(Roles = "GridOperator")]`, etc. — Dinil's `ProsumerAuthController` issues its own tokens via the same `JwtService.Issue`, don't let a second JWT implementation appear anywhere in the codebase.
- BCrypt password hashing helper — used by both your `UsersController`/`AdminProsumersController` and Dinil's `ProsumersController.Register`.

### Endpoints to build (see `architecture.md` §3 for exact shapes)
- `POST /auth/login` — Backoffice/GridOperator web login; reject `Deactivated` accounts with a specific message.
- `GET/POST/PUT /users` — system account CRUD (Backoffice/GridOperator accounts only); enforce `username` uniqueness.
- `PUT /users/{id}/deactivate` — Backoffice only.
- `GET /admin/prosumers` (with `status` filter), `GET /admin/prosumers/pending` — Backoffice views over prosumer accounts (created by Dinil's registration endpoint, but administered here).
- `PUT /admin/prosumers/{nic}/activate` — flips `PendingActivation → Active` only; reject any other current status with a clear error.
- `PUT /admin/prosumers/{nic}/reactivate` — **the rule most likely to be probed directly in viva**: return `403` for any caller whose JWT role isn't `Backoffice`, even if they're a valid, authenticated `GridOperator`. Write a test that proves this, not just the happy path.
- `PUT /admin/prosumers/{nic}/deactivate` — Backoffice-initiated force deactivation with an optional reason.

## Frontend: React + Tailwind

### Screens
- `pages/auth/LoginPage.jsx` — calls `POST /auth/login`, stores JWT + role, redirects `Backoffice → /admin/...`, `GridOperator → /operator/...` (a landing page is enough for GridOperator on web since their main screens are mobile-first in this design — see `architecture.md` §6).
- `pages/admin/UsersPage.jsx` — table + create/edit form for Backoffice/GridOperator accounts.
- `pages/admin/PendingProsumersPage.jsx` — table of `PendingActivation` prosumers with an "Activate" button. This screen is what the rubric's Mobile Authentication criterion specifically credits under "pending activation view in web app" (marks land on Dinil's criterion, but you build the screen — coordinate so Dinil isn't blocked testing his registration flow end-to-end before this exists).
- `pages/admin/ProsumersPage.jsx` — full prosumer list, deactivate/reactivate actions (reactivate button rendered only for `Backoffice` role client-side, but always re-validated server-side — never trust the hidden button alone).

### Integration
- `AuthContext.jsx` (built jointly Day 1, you drive it) — the single place JWT + role live; every protected route/component reads from here.
- `ProtectedRoute` wrapper component, shared with Shalon's routes.
- Shared Tailwind component patterns with Shalon's module — same button/table/modal/toast style so the app doesn't look like two different products stitched together.

## Testing checklist
- Login as each of the three roles (including a `PendingActivation` and a `Deactivated` prosumer credential via Postman) — confirm correct redirect/rejection per case.
- Attempt `PUT /admin/prosumers/{nic}/reactivate` with a valid `GridOperator` token → expect `403`; repeat with a `Backoffice` token → expect success.
- Try to register two prosumers with the same NIC (via Dinil's endpoint, hitting your shared `Users` collection) → second attempt rejected.
- Activate a pending prosumer, then confirm their next login succeeds.

## Screenshot checklist
Login (each role's redirect), System Users list + create form, Pending Prosumers activation screen, Prosumers list, reactivate action (success as Backoffice, blocked attempt if you can capture the 403 response in dev tools for the report).
