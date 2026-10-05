# Meeting Room Reservation — Backend API Handoff for the Frontend

This document describes everything the frontend needs to know about the backend: how to run it, authentication, every endpoint with its request/response shapes, permissions, business rules, error formats, and remaining limitations. It reflects the backend on branch `fix/security-hardening`. If the backend is checked out on `main` before that branch is merged, much of what is described here will not be present.

---

## 1. Running the backend

- Stack: Spring Boot 4, Java 26, PostgreSQL 17 (Docker).
- Start the database: `docker compose up -d` in the backend folder.
- Start the app: run `MeetingRoomReservationApplication` from IntelliJ, or `./mvnw spring-boot:run`.
- **Base URL:** `http://localhost:8080/api`
- **CORS:** only the origin `http://localhost:3000` is allowed (methods GET, POST, PUT, PATCH, DELETE, OPTIONS; all request headers). If the frontend dev server runs on another port (e.g. Vite's default `5173`), either run it on port 3000 or ask for the backend CORS config to be changed. Requests from any other origin fail in the browser with a CORS error.
- No cookies are used; authentication is a bearer token in the `Authorization` header, so `credentials: "include"` / `withCredentials` is **not** needed.

### Seeded login accounts

Created automatically on startup when the user table is empty:

| Username | Password | Role | Linked employee |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | none |
| `mate` | `mate123` | EMPLOYEE | Máté Horváth (`mate@example.com`) |
| `anna` | `anna123` | EMPLOYEE | Anna Kovács (`anna@example.com`) — only created if that employee exists, i.e. on a fresh database |

On a completely empty database the seeder also creates 3 employees, 3 rooms ("Room A", "Room B", "Conference Room") and 3 reservations.

The seeded passwords are shorter than the 8-character minimum that now applies to new passwords. They still work for logging in, but a user can't change their password *back* to one of them.

---

## 2. Concepts and data model

- **Employee** — a person in the company (name, email, department, job title). Managed by admins. Not the same thing as a login.
- **User (login account)** — username + password + role (`ADMIN` or `EMPLOYEE`). An `EMPLOYEE` user is always linked to exactly one employee; an `ADMIN` user may or may not be linked. One employee can have at most one user account.
- **Room** — bookable meeting room (name, capacity, location, has projector).
- **Reservation** — an employee booking a room for a time range.

**Two different "role" fields — do not confuse them:**
- `Employee.role` is a free-text **job title** (e.g. "Developer", "HR Manager").
- `User.role` is the **permission role**: `"ADMIN"` or `"EMPLOYEE"`.

**Soft deletes:** the API only physically deletes user accounts.
- `DELETE` on a room or employee sets `active: false` (deactivate). `PATCH .../activate` reverses it.
- `DELETE` on a reservation sets `archived: true` (archive). `PATCH .../restore` reverses it (admin only).

**Reservation status** (`status` field), exact string values: `"PLANNED"`, `"APPROVED"`, `"CANCELLED"`, `"COMPLETED"`. Only admins can change status, and only along these transitions:

```
PLANNED  ──► APPROVED ──► COMPLETED
   │            │
   └──► CANCELLED ◄──┘
```

| From | Allowed next statuses |
|---|---|
| `PLANNED` | `APPROVED`, `CANCELLED` |
| `APPROVED` | `CANCELLED`, `COMPLETED` |
| `CANCELLED` | none (final) |
| `COMPLETED` | none (final) |

- New reservations always start as `PLANNED`.
- For booking conflicts, every non-archived reservation **except `CANCELLED`** occupies its time slot (see the overlap rule in 7.5). Cancelling or archiving a reservation frees the slot.
- `CANCELLED` and `COMPLETED` reservations can no longer be edited.
- `archived` and `status` are independent: a reservation can be archived in any status.

---

## 3. Authentication

### Login

`POST /api/auth/login` (public)

Request:
```json
{ "username": "mate", "password": "mate123" }
```

Response `200`:
```json
{ "token": "<JWT>", "username": "mate", "role": "EMPLOYEE" }
```

`role` here is `"ADMIN"` or `"EMPLOYEE"` (no `ROLE_` prefix).

Failure responses:
- `401` `{ "error": "UNAUTHORIZED", "message": "Invalid username or password.", ... }` — wrong username or password (same message for both).
- `401` `{ "error": "UNAUTHORIZED", "message": "Account is disabled.", ... }` — the linked employee has been deactivated.
- `400` `VALIDATION_ERROR` if username or password is blank.

### Using the token

Send it on every other request:
```
Authorization: Bearer <token>
```

- The token is valid for **2 hours**. There is **no refresh token** and **no logout endpoint** — logout means deleting the token on the client.
- The backend re-checks the user on every request. If the account is deleted or its employee is deactivated, existing tokens immediately start returning `401`. A role change also takes effect immediately.

### Current user: `GET /api/me`

Any logged-in user. Returns the same shape as an entry in the admin user list:

```json
{ "id": 2, "username": "mate", "role": "EMPLOYEE", "employeeId": 1, "employeeName": "Máté Horváth" }
```

For an admin without a linked employee, `employeeId` and `employeeName` are `null`.

Call this right after login (and on app start, if a token is stored) to get `employeeId` for "my reservations" and to validate the stored token. The JWT payload also contains `sub` (username), `role` (`"ROLE_ADMIN"`/`"ROLE_EMPLOYEE"`), `userId`, `employeeId` (absent for admins without an employee), `iat` and `exp` (seconds since epoch) if you want to read expiry client-side. `/api/me` is the preferred source for everything else.

### Change own password: `PUT /api/me/password`

Any logged-in user.

```json
{ "currentPassword": "mate123", "newPassword": "newpass123" }
```

- `200`, empty body on success. The current token stays valid.
- `400 "Current password is incorrect."`
- `400 VALIDATION_ERROR`: `newPassword` must be 8–100 characters.

### Recommended client behaviour

- On **any `401`** (other than from the login form itself): clear the stored token and redirect to the login page.
- On **`403`**: stay on the page and show `body.message`.
- Hide admin-only UI for `EMPLOYEE` users (the backend enforces it anyway).

---

## 4. Permissions matrix

| Endpoint | ADMIN | EMPLOYEE |
|---|---|---|
| `POST /api/auth/login` | public | public |
| `GET /api/me`, `PUT /api/me/password` | ✅ | ✅ |
| `GET /api/rooms/**` | ✅ | ✅ |
| `POST/PUT/PATCH/DELETE /api/rooms/**` | ✅ | ❌ 403 |
| `/api/employees/**` (all methods, including GET) | ✅ | ❌ 403 |
| `/api/users/**` (all methods) | ✅ | ❌ 403 |
| `GET /api/reservations/**` | ✅ all | ✅ all (employees can see everyone's reservations) |
| `POST /api/reservations` | ✅ for any employee | ✅ always for themselves |
| `PUT /api/reservations/{id}` | ✅ any | ✅ own only, otherwise 403 |
| `DELETE /api/reservations/{id}` | ✅ any | ✅ own only, otherwise 403 |
| `PATCH /api/reservations/{id}/status` | ✅ | ❌ 403 |
| `PATCH /api/reservations/{id}/restore` | ✅ | ❌ 403 |

Because employees cannot call `/api/employees`, an employee-facing UI cannot list employees. Use `employeeName` / `employeeId` from reservation responses instead.

---

## 5. Response and error format

### Success

- `GET`, `POST`, `PUT`, `PATCH` return **`200`**, usually with a JSON body (POST returns `200`, not `201`).
- `DELETE`, `PATCH /api/users/{id}/password` and `PUT /api/me/password` return **`200` with an empty body**.
- List endpoints return a plain JSON array (no pagination), **sorted** as follows:
  - rooms by `name`
  - employees by `name`
  - users by `username`
  - reservations by `startTime`, ascending

### Errors

**Every error response has the same shape**, including 401 and 403:

```json
{ "error": "BAD_REQUEST", "message": "Room name already exists.", "timestamp": "2026-10-05T20:19:39.6981757" }
```

Validation errors add a `fields` object:

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Validation failed.",
  "fields": { "title": "Title is required.", "description": "Description must be at most 1000 characters." },
  "timestamp": "..."
}
```

All messages are in English and safe to show to users.

| Status | `error` | When |
|---|---|---|
| `400` | `BAD_REQUEST` | Business rule violated (messages listed per endpoint), malformed JSON (`"Malformed request body."`, also for invalid enum values like `role: "BOSS"`), missing query parameter (`"Required parameter 'end' is not present."`), wrongly typed parameter or path id (`"Invalid value for parameter 'id'."`) |
| `400` | `VALIDATION_ERROR` | Field validation failed; see `fields` |
| `401` | `UNAUTHORIZED` | Wrong credentials or disabled account (login), or missing/invalid/expired token or deleted/deactivated user (`"Authentication required."`) |
| `403` | `FORBIDDEN` | Role not allowed (`"You do not have permission to perform this action."`), employee modifying someone else's reservation (`"You can only modify your own reservations."`), or account not linked to an employee |
| `404` | `NOT_FOUND` | Entity id doesn't exist (`"Room not found."`), or unknown URL |
| `405` | `METHOD_NOT_ALLOWED` | Wrong HTTP method for a URL |
| `409` | `CONFLICT` | Rare database conflict, e.g. two requests creating the same unique value at the same moment (`"The request conflicts with existing data."`) |
| `500` | `INTERNAL_ERROR` | Unexpected server error (`"Unexpected error occurred."`) |

Error helper: show `body.message`, and if `body.fields` exists, show each field's message next to the matching form input.

---

## 6. Date and time format

- All timestamps are **`LocalDateTime` without a timezone**, formatted as ISO-8601: `"2026-10-06T10:00:00"`.
- Send dates in exactly that format: no `Z`, no offset. `new Date().toISOString()` produces `...Z` in UTC, which is **wrong**. Format the user's local date/time as `YYYY-MM-DDTHH:mm:ss`.
- The backend treats the value as server-local time and compares it with the server's "now" (e.g. for rejecting past bookings). Assume frontend and backend share a timezone.

---

## 7. Endpoints

### 7.1 Auth and current user

| Method | Path | Body | Returns |
|---|---|---|---|
| POST | `/api/auth/login` | `LoginRequest` | `LoginResponse` |
| GET | `/api/me` | — | `UserDTO` |
| PUT | `/api/me/password` | `ChangePasswordDTO` | empty |

### 7.2 Rooms

| Method | Path | Body | Returns | Who |
|---|---|---|---|---|
| GET | `/api/rooms` | — | `RoomDTO[]` (active and inactive) | all |
| GET | `/api/rooms/active` | — | `RoomDTO[]` (active only) | all |
| GET | `/api/rooms/{id}` | — | `RoomDTO` | all |
| GET | `/api/rooms/available?start=...&end=...` | — | `RoomDTO[]` | all |
| POST | `/api/rooms` | `SaveRoomDTO` | `RoomDTO` | admin |
| PUT | `/api/rooms/{id}` | `SaveRoomDTO` | `RoomDTO` | admin |
| DELETE | `/api/rooms/{id}` | — | empty (deactivates) | admin |
| PATCH | `/api/rooms/{id}/activate` | — | `RoomDTO` | admin |

**`GET /api/rooms/available`**
- Query params `start` and `end`, both required, ISO format: `/api/rooms/available?start=2026-10-06T10:00:00&end=2026-10-06T11:00:00`.
- Returns active rooms with **no** overlapping reservation in that window. Archived and `CANCELLED` reservations don't count as overlapping.
- Does **not** filter by capacity. Filter on the client using the attendee count.
- Errors: `400 "Start time must be before end time."`; `400` if a param is missing or malformed.

**Create / update rules**
- New rooms are `active: true`.
- `400 "Room name already exists."` — names are unique **case-insensitively** ("Room A" and "room a" clash). Keeping a room's own name on update is fine.
- `PUT` replaces all editable fields; send the full object. `PUT` cannot change `active`; use DELETE / activate.
- Field limits: `name` and `location` max 255 characters, `capacity` ≥ 1.

**Deactivate (`DELETE`)**
- `400 "Room cannot be deactivated because it has upcoming reservations."` — only blocked by non-archived `PLANNED`/`APPROVED` reservations that haven't ended yet. Past, cancelled and completed ones don't block.

**Activate (`PATCH /{id}/activate`)** — sets `active: true` and returns the room. Calling it on an already active room is harmless.

### 7.3 Employees (admin only)

| Method | Path | Body | Returns |
|---|---|---|---|
| GET | `/api/employees` | — | `EmployeeDTO[]` (active and inactive) |
| GET | `/api/employees/active` | — | `EmployeeDTO[]` |
| GET | `/api/employees/{id}` | — | `EmployeeDTO` |
| POST | `/api/employees` | `SaveEmployeeDTO` | `EmployeeDTO` |
| PUT | `/api/employees/{id}` | `SaveEmployeeDTO` | `EmployeeDTO` |
| DELETE | `/api/employees/{id}` | — | empty (deactivates) |
| PATCH | `/api/employees/{id}/activate` | — | `EmployeeDTO` |

- New employees are `active: true`.
- `400 "Email already exists."` on create, or on update when the email belongs to another employee.
- Field limits: all fields required, max 255 characters; `email` must be a valid email address.
- Deactivate: `400 "Employee cannot be deactivated because they have upcoming reservations."` — same rule as rooms.
- Deactivating an employee **immediately locks out their login account** (existing tokens get `401`; login returns `"Account is disabled."`). Activating them again restores access with the same credentials.
- Creating an employee does **not** create a login. Use `POST /api/users` afterwards.

### 7.4 Users / login accounts (admin only)

| Method | Path | Body | Returns |
|---|---|---|---|
| GET | `/api/users` | — | `UserDTO[]` |
| POST | `/api/users` | `CreateUserDTO` | `UserDTO` |
| PUT | `/api/users/{id}` | `UpdateUserDTO` | `UserDTO` |
| PATCH | `/api/users/{id}/password` | `ResetPasswordDTO` | empty |
| DELETE | `/api/users/{id}` | — | empty (hard delete) |

**Create rules** (`400` unless noted):
- `"Username already exists."`
- `"Employee accounts must be linked to an employee."` — `employeeId` is required when `role` is `"EMPLOYEE"`, optional for `"ADMIN"`.
- `"Employee is not active."`
- `"Employee already has a user account."`
- `404 "Employee not found."`
- Validation: username 3–50 characters, password 8–100 characters, role required.

**Update (`PUT`) — change role and/or linked employee:**
- Body `{ "role": "ADMIN" }` or `{ "role": "EMPLOYEE", "employeeId": 7 }`. Omitting `employeeId` **unlinks** the employee, which is only allowed for `ADMIN`.
- The same employee rules as create apply.
- `400 "You cannot change your own role."`
- The username cannot be changed.

**Reset password (`PATCH /{id}/password`):** body `{ "password": "newpass123" }` (8–100 characters). Sets the user's password without needing the old one. Use this for "forgot password" handled by an admin.

**Delete:** `400 "You cannot delete your own account."`; `404 "User not found."`.

Suggested admin UI: a "Users" page listing accounts, with:
- a "Create login" form: role select, plus an employee select populated from `GET /api/employees/active`, hiding employees that already appear in the users list
- per-row actions: "Change role / employee", "Reset password", "Delete" (delete and role change disabled for the logged-in admin's own row)

### 7.5 Reservations

| Method | Path | Body | Returns | Who |
|---|---|---|---|---|
| GET | `/api/reservations` | — | `ReservationDTO[]` (**including archived**) | all |
| GET | `/api/reservations/active` | — | `ReservationDTO[]` (non-archived; any status) | all |
| GET | `/api/reservations/{id}` | — | `ReservationDTO` (even if archived) | all |
| GET | `/api/reservations/room/{roomId}` | — | `ReservationDTO[]` non-archived for that room | all |
| GET | `/api/reservations/employee/{employeeId}` | — | `ReservationDTO[]` non-archived for that employee | all |
| POST | `/api/reservations` | `SaveReservationDTO` | `ReservationDTO` | all |
| PUT | `/api/reservations/{id}` | `SaveReservationDTO` | `ReservationDTO` | admin or owner |
| PATCH | `/api/reservations/{id}/status` | `UpdateReservationStatusDTO` | `ReservationDTO` | admin |
| DELETE | `/api/reservations/{id}` | — | empty (archives) | admin or owner |
| PATCH | `/api/reservations/{id}/restore` | — | `ReservationDTO` | admin |

`/room/{roomId}` and `/employee/{employeeId}` return `404` if the room or employee doesn't exist. All lists are sorted by `startTime`.

"My reservations" for the logged-in employee: `GET /api/reservations/employee/{employeeId}`, using `employeeId` from `GET /api/me`.

**Create (`POST`) rules:**
- The reservation is created with `status: "PLANNED"` and `archived: false`.
- **`employeeId` handling:**
  - Logged in as **EMPLOYEE**: `employeeId` is **ignored**. The reservation is always made for the logged-in user's own employee. It can be omitted.
  - Logged in as **ADMIN**: `employeeId` is **required** (`400 "Employee is required."`). The admin books on behalf of that employee.
- Errors, in the order they're checked:
  1. `400 VALIDATION_ERROR`: `title` blank or over 255 characters, `description` over 1000 characters, `startTime`/`endTime`/`attendeeCount`/`roomId` missing, `attendeeCount < 1`
  2. `400 "Start time must be before end time."`
  3. `400 "Reservation cannot start in the past."`
  4. `404 "Employee not found."` (admin only) / `403 "Your account is not linked to an employee."` (employee account without a linked employee)
  5. `400 "Employee is not active."`
  6. `404 "Room not found."`
  7. `400 "Room is not active."`
  8. `400 "Attendee count exceeds room capacity."`
  9. `400 "Room is already reserved in this time range."`
- **Overlap rule:** a conflict exists when an existing reservation in the same room is non-archived, not `CANCELLED`, and `existing.start < new.end && existing.end > new.start`. **Back-to-back bookings are allowed** (one ending 10:00, the next starting 10:00).

**Update (`PUT`) rules:**
- Same body and same validations as create. The overlap check excludes the reservation itself.
- `403 "You can only modify your own reservations."` when an employee edits someone else's reservation.
- `400 "Archived reservation cannot be modified."`
- `400 "Cancelled or completed reservation cannot be modified."`
- `400 "Reservation cannot start in the past."` — only checked when `startTime` is **changed**. A reservation that's already underway can still be edited (e.g. extending its end time) if its start time is sent unchanged.
- For employees, `employeeId` is ignored and the owner stays the same. Admins can reassign a reservation to another employee.
- `PUT` never changes `status`; use the PATCH endpoint for that.

**Status change (`PATCH /{id}/status`, admin only):**
- Body: `{ "status": "APPROVED" }`. Case-insensitive.
- `400 "Invalid reservation status."` for an unknown value.
- `400 "Status cannot be changed from PLANNED to COMPLETED."` (etc.) for a transition not in the table in section 2.
- `400 "Archived reservation status cannot be modified."`
- Suggested buttons: `PLANNED` → Approve / Reject (Cancel); `APPROVED` → Cancel / Mark completed; `CANCELLED` and `COMPLETED` → none.

**Archive (`DELETE`):**
- Sets `archived: true`, which also frees the slot. Owner or admin only; otherwise `403`.
- Archiving an already archived reservation succeeds silently.

**Restore (`PATCH /{id}/restore`, admin only):**
- Sets `archived: false` and returns the reservation.
- `400 "Reservation is not archived."`
- If the reservation is `PLANNED` or `APPROVED`, it must still be valid. Otherwise one of these is returned:
  - `400 "Room is not active."`
  - `400 "Employee is not active."`
  - `400 "Room is already reserved in this time range."` — someone booked the slot meanwhile
- Restoring a reservation whose start time has passed is allowed; restore doesn't apply the past-time rule.

---

## 8. TypeScript types

```ts
// ---------- Auth / current user ----------
export interface LoginRequest {
  username: string;
  password: string;
}

export type UserRole = "ADMIN" | "EMPLOYEE";

export interface LoginResponse {
  token: string;
  username: string;
  role: UserRole;
}

export interface ChangePasswordDTO {
  currentPassword: string;
  newPassword: string;   // 8-100 chars
}

// ---------- Rooms ----------
export interface RoomDTO {
  id: number;
  name: string;
  capacity: number;
  location: string;
  hasProjector: boolean;
  active: boolean;
}

export interface SaveRoomDTO {
  name: string;          // required, max 255, unique (case-insensitive)
  capacity: number;      // required, >= 1
  location: string;      // required, max 255
  hasProjector: boolean; // required
}

// ---------- Employees ----------
export interface EmployeeDTO {
  id: number;
  name: string;
  email: string;
  department: string;
  role: string;          // job title, NOT the permission role
  active: boolean;
}

export interface SaveEmployeeDTO {
  name: string;          // required, max 255
  email: string;         // required, valid email, max 255, unique
  department: string;    // required, max 255
  role: string;          // required, max 255, job title
}

// ---------- Users ----------
// Also returned by GET /api/me
export interface UserDTO {
  id: number;
  username: string;
  role: UserRole;
  employeeId: number | null;
  employeeName: string | null;
}

export interface CreateUserDTO {
  username: string;            // 3-50 chars, unique
  password: string;            // 8-100 chars
  role: UserRole;
  employeeId?: number | null;  // required when role === "EMPLOYEE"
}

export interface UpdateUserDTO {
  role: UserRole;
  employeeId?: number | null;  // required when role === "EMPLOYEE"; omitting it unlinks
}

export interface ResetPasswordDTO {
  password: string;            // 8-100 chars
}

// ---------- Reservations ----------
export type ReservationStatus = "PLANNED" | "APPROVED" | "CANCELLED" | "COMPLETED";

export const ALLOWED_TRANSITIONS: Record<ReservationStatus, ReservationStatus[]> = {
  PLANNED: ["APPROVED", "CANCELLED"],
  APPROVED: ["CANCELLED", "COMPLETED"],
  CANCELLED: [],
  COMPLETED: [],
};

export interface ReservationDTO {
  id: number;
  title: string;
  description: string | null;
  startTime: string;     // "YYYY-MM-DDTHH:mm:ss", no timezone
  endTime: string;
  attendeeCount: number;
  status: ReservationStatus;
  archived: boolean;
  employeeId: number;
  employeeName: string;
  roomId: number;
  roomName: string;
}

export interface SaveReservationDTO {
  title: string;               // required, max 255
  description?: string | null; // optional, max 1000
  startTime: string;           // required, "YYYY-MM-DDTHH:mm:ss", not in the past
  endTime: string;             // required, after startTime
  attendeeCount: number;       // required, >= 1, <= room capacity
  employeeId?: number | null;  // ADMIN: required. EMPLOYEE: ignored, omit it
  roomId: number;              // required
}

export interface UpdateReservationStatusDTO {
  status: ReservationStatus;
}

// ---------- Errors ----------
export type ApiErrorCode =
  | "BAD_REQUEST"
  | "VALIDATION_ERROR"
  | "UNAUTHORIZED"
  | "FORBIDDEN"
  | "NOT_FOUND"
  | "METHOD_NOT_ALLOWED"
  | "CONFLICT"
  | "INTERNAL_ERROR";

export interface ApiError {
  error: ApiErrorCode | string;
  message: string;
  timestamp: string;
  fields?: Record<string, string>; // only for VALIDATION_ERROR
}
```

---

## 9. Suggested screens per role

**EMPLOYEE**
- Login
- Rooms: browse active rooms and search availability by time range (+ client-side capacity filter)
- New reservation form: no employee picker, no past dates
- My reservations (`/reservations/employee/{employeeId from /api/me}`): edit and cancel (archive) own reservations, except cancelled or completed ones
- Calendar / overview of all active reservations (read-only for others' bookings)
- Profile: change password

**ADMIN**
- Everything above. The reservation form gets an **employee picker** (from `/employees/active`) and edit/archive buttons on every reservation.
- Approval queue: `PLANNED` reservations with Approve / Reject
- Archived reservations view (`GET /api/reservations`, filter `archived === true`) with a Restore button
- Rooms management: create, edit, deactivate, reactivate
- Employees management: create, edit, deactivate, reactivate
- Users management: create logins, change role or employee, reset password, delete

---

## 10. Remaining limitations

1. **No pagination.** All lists come back in full (sorted, see section 5).
2. **No real-time updates.** After create, update or delete, refetch the affected lists.
3. **Status changes are manual.** Nothing automatically marks past reservations `COMPLETED` or expires unapproved `PLANNED` ones.
4. **Availability search ignores capacity.** Filter on the client.
5. **Usernames can't be changed.**
6. **CORS only allows `http://localhost:3000`** (see section 1).
