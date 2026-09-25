// ============================================================
// File: ReservationService.cs
// Purpose: Business logic for the reservation lifecycle —
//          7-day booking window, 12-hour notice rule, slot
//          coordination through SlotService, QR issuance,
//          and operator QR verification (flips Confirmed ->
//          Completed). All validation lives here, never in
//          controllers or clients.
// Author: Dinil
// ============================================================

using MongoDB.Driver;
using SmartMicrogrid.Api.Data;
using SmartMicrogrid.Api.Models;

namespace SmartMicrogrid.Api.Services;

public class ReservationService
{
    private const int BOOKING_WINDOW_DAYS = 7;
    private const int NOTICE_HOURS = 12;

    private readonly MongoDbContext _db;
    private readonly SlotService _slotService;
    private readonly StationService _stationService;
    private readonly QrTokenService _qrService;

    public ReservationService(
        MongoDbContext db,
        SlotService slotService,
        StationService stationService,
        QrTokenService qrService)
    {
        _db = db;
        _slotService = slotService;
        _stationService = stationService;
        _qrService = qrService;
    }

    // Creates a reservation under the given prosumer's NIC, enforcing the 7-day
    // window and slot availability; issues the QR token immediately at creation.
    public async Task<CreateReservationResponse> CreateAsync(string prosumerNic, CreateReservationRequest req)
    {
        var now = DateTime.UtcNow;
        var scheduledUtc = DateTime.SpecifyKind(req.ScheduledAt, DateTimeKind.Utc);

        // 7-day window (must be in future, within BOOKING_WINDOW_DAYS days).
        if (scheduledUtc <= now)
            throw new ServiceException(400, "SCHEDULED_IN_PAST", "Scheduled time must be in the future.");
        if (scheduledUtc > now.AddDays(BOOKING_WINDOW_DAYS))
            throw new ServiceException(400, "OUTSIDE_7_DAY_WINDOW",
                $"Reservations must be within {BOOKING_WINDOW_DAYS} days.");

        // Slot must exist and be Available.
        var slot = await _slotService.GetByIdAsync(req.SlotId)
            ?? throw new ServiceException(404, "SLOT_NOT_FOUND", "Slot does not exist.");
         if (slot.Status != SlotStatus.Available)
            throw new ServiceException(409, "SLOT_NOT_AVAILABLE", "Slot is not available.");

        // Station must exist and be Active.
        var station = await _stationService.GetByIdAsync(req.StationId)
            ?? throw new ServiceException(404, "STATION_NOT_FOUND", "Station does not exist.");
        if (station.Status != "Active")
            throw new ServiceException(409, "STATION_INACTIVE", "Station is not active.");

        // Issue QR immediately — no separate approval step (see architecture.md §2.4).
        var reservationId = MongoDB.Bson.ObjectId.GenerateNewId().ToString();
        var qrExpiry = scheduledUtc.AddHours(24);
        var qrToken = _qrService.Issue(reservationId, qrExpiry);

        var reservation = new Reservation
        {
            Id = reservationId,
            ProsumerNic = prosumerNic,
            StationId = req.StationId,
            SlotId = req.SlotId,
            ScheduledAt = scheduledUtc,
            Status = "Confirmed",
            QrToken = qrToken,
            QrTokenExpiresAt = qrExpiry,
            CreatedAt = now,
            UpdatedAt = now
        };

        await _db.Reservations.InsertOneAsync(reservation);
        await _slotService.MarkReserved(req.SlotId);

        return new CreateReservationResponse
        {
            Id = reservation.Id,
            Status = reservation.Status,
            QrToken = reservation.QrToken,
            QrTokenExpiresAt = reservation.QrTokenExpiresAt
        };
    }

    // Updates a reservation, enforcing 12-hour notice against the CURRENT stored
    // time and re-validating the 7-day window against any new time.
    public async Task UpdateAsync(string id, string prosumerNic, UpdateReservationRequest req)
    {
        var reservation = await GetOwnedAsync(id, prosumerNic);
        var now = DateTime.UtcNow;

        if (reservation.Status != "Confirmed")
            throw new ServiceException(409, "RESERVATION_NOT_EDITABLE",
                "Only Confirmed reservations can be edited.");

        // 12-hour rule against current stored time.
        if (reservation.ScheduledAt - now < TimeSpan.FromHours(NOTICE_HOURS))
            throw new ServiceException(409, "TOO_LATE_TO_MODIFY",
                $"Changes require at least {NOTICE_HOURS} hours' notice.");

        // Re-validate 7-day window if rescheduling.
        if (req.ScheduledAt.HasValue)
        {
            var newScheduled = DateTime.SpecifyKind(req.ScheduledAt.Value, DateTimeKind.Utc);
            if (newScheduled <= now)
                throw new ServiceException(400, "SCHEDULED_IN_PAST", "New time must be in the future.");
            if (newScheduled > now.AddDays(BOOKING_WINDOW_DAYS))
                throw new ServiceException(400, "OUTSIDE_7_DAY_WINDOW",
                    $"New time must be within {BOOKING_WINDOW_DAYS} days.");
            reservation.ScheduledAt = newScheduled;
        }

        // If slot is changing, validate new slot and coordinate state.
        if (!string.IsNullOrEmpty(req.SlotId) && req.SlotId != reservation.SlotId)
        {
            var newSlot = await _slotService.GetByIdAsync(req.SlotId)
                ?? throw new ServiceException(404, "SLOT_NOT_FOUND", "New slot does not exist.");
            if (newSlot.Status != SlotStatus.Available)
                throw new ServiceException(409, "SLOT_NOT_AVAILABLE", "New slot is not available.");

            await _slotService.MarkAvailable(reservation.SlotId);
            await _slotService.MarkReserved(req.SlotId);
            reservation.SlotId = req.SlotId;
        }

        reservation.UpdatedAt = now;
        await _db.Reservations.ReplaceOneAsync(r => r.Id == id, reservation);
    }

    // Cancels a reservation, enforcing 12-hour notice and freeing the slot.
    public async Task CancelAsync(string id, string prosumerNic, string? reason)
    {
        var reservation = await GetOwnedAsync(id, prosumerNic);
        var now = DateTime.UtcNow;

        if (reservation.Status != "Confirmed")
            throw new ServiceException(409, "RESERVATION_NOT_CANCELLABLE",
                "Only Confirmed reservations can be cancelled.");

        if (reservation.ScheduledAt - now < TimeSpan.FromHours(NOTICE_HOURS))
            throw new ServiceException(409, "TOO_LATE_TO_CANCEL",
                $"Cancellations require at least {NOTICE_HOURS} hours' notice.");

        reservation.Status = "Cancelled";
        reservation.CancelReason = reason;
        reservation.UpdatedAt = now;

        await _db.Reservations.ReplaceOneAsync(r => r.Id == id, reservation);
        await _slotService.MarkAvailable(reservation.SlotId);
    }

    // Verifies a QR token and flips a Confirmed reservation to Completed.
    // Called by Grid Operators via POST /reservations/verify-qr.
    public async Task<VerifyQrResponse> VerifyQrAsync(string qrToken, string operatorUserId)
    {
        // 1. Verify HMAC signature + token expiry.
        var reservationId = _qrService.Verify(qrToken)
            ?? throw new ServiceException(400, "INVALID_QR", "QR is invalid or expired.");

        // 2. Load reservation and ensure it is still Confirmed (not already used).
        var reservation = await _db.Reservations.Find(r => r.Id == reservationId).FirstOrDefaultAsync()
            ?? throw new ServiceException(404, "RESERVATION_NOT_FOUND", "Reservation not found.");
        if (reservation.Status != "Confirmed")
            throw new ServiceException(409, "RESERVATION_NOT_CONFIRMED",
                $"Reservation is {reservation.Status}, cannot complete.");

        // 3. Load station + slot for the operator-visible confirmation payload.
        var station = await _stationService.GetByIdAsync(reservation.StationId)
            ?? throw new ServiceException(404, "STATION_NOT_FOUND", "Station not found.");
        var slot = await _slotService.GetByIdAsync(reservation.SlotId)
            ?? throw new ServiceException(404, "SLOT_NOT_FOUND", "Slot not found.");

        // 4. Flip status and record the operator who completed it.
        var now = DateTime.UtcNow;
        var update = Builders<Reservation>.Update
            .Set(r => r.Status, "Completed")
            .Set(r => r.CompletedAt, now)
            .Set(r => r.CompletedByUserId, operatorUserId)
            .Set(r => r.UpdatedAt, now);
        await _db.Reservations.UpdateOneAsync(r => r.Id == reservationId, update);

        // 5. Free the slot again now that the energy transfer is complete.
        await _slotService.MarkAvailable(reservation.SlotId);

        return new VerifyQrResponse
        {
            ReservationId = reservation.Id,
            ProsumerName = reservation.ProsumerNic,
            StationName = station.Name,
            SlotNumber = slot.SlotNumber,
            Status = "Completed"
        };
    }

    // Loads a reservation and asserts the caller owns it.
    public async Task<Reservation> GetOwnedAsync(string id, string prosumerNic)
    {
        var reservation = await _db.Reservations.Find(r => r.Id == id).FirstOrDefaultAsync()
            ?? throw new ServiceException(404, "RESERVATION_NOT_FOUND", "Reservation not found.");
        if (reservation.ProsumerNic != prosumerNic)
            throw new ServiceException(403, "FORBIDDEN", "Not your reservation.");
        return reservation;
    }

    // Loads a reservation by ID with no ownership check (operator/backoffice).
    public async Task<Reservation> GetByIdAsync(string id)
    {
        return await _db.Reservations.Find(r => r.Id == id).FirstOrDefaultAsync()
            ?? throw new ServiceException(404, "RESERVATION_NOT_FOUND", "Reservation not found.");
    }

    // Lists reservations with optional filters. GridOperator/Backoffice see all;
    // Prosumers get restricted to their own NIC via a JWT-scoped query param.
    public async Task<List<Reservation>> ListAsync(
        string? prosumerNic, string? stationId, string? status, DateTime? from, DateTime? to)
    {
        var builder = Builders<Reservation>.Filter;
        var filter = builder.Empty;
        if (!string.IsNullOrEmpty(prosumerNic)) filter &= builder.Eq(r => r.ProsumerNic, prosumerNic);
        if (!string.IsNullOrEmpty(stationId)) filter &= builder.Eq(r => r.StationId, stationId);
        if (!string.IsNullOrEmpty(status)) filter &= builder.Eq(r => r.Status, status);
        if (from.HasValue) filter &= builder.Gte(r => r.ScheduledAt, from.Value);
        if (to.HasValue) filter &= builder.Lte(r => r.ScheduledAt, to.Value);

        return await _db.Reservations.Find(filter).SortBy(r => r.ScheduledAt).ToListAsync();
    }
}