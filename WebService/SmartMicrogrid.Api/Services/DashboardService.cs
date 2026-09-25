// ============================================================
// File: DashboardService.cs
// Purpose: Read-only queries over Dinil's Reservations collection —
//          a prosumer's dashboard summary and the reservations
//          search/filter list — per architecture.md §3 (moved from
//          Migara to Shalon 2026-09-21, see §3/§4/§7). All filtering
//          and the Prosumer-restricted-to-own-nic rule live here,
//          never in a client, per the FAT service pattern.
// Author: Shalon
// ============================================================
using MongoDB.Driver;
using SmartMicrogrid.Api.Data;
using SmartMicrogrid.Api.Models;

namespace SmartMicrogrid.Api.Services;

public class DashboardService
{
    private const int RecentHistoryLimit = 10;

    private readonly MongoDbContext _context;

    // Injects the shared Mongo context this service reads Reservations from.
    public DashboardService(MongoDbContext context)
    {
        _context = context;
    }

    // Builds a prosumer's dashboard summary: how many reservations are active, how many of those
    // are past their scheduled time and awaiting operator check-in, how many are still upcoming,
    // and their most recent bookings.
    //
    // Field semantics (architecture.md §3 only ever named these fields — nobody had implemented
    // this endpoint before, so these definitions are set here for the first time):
    //   - activeCount: every reservation with status == Confirmed (booked, not yet completed or
    //     cancelled).
    //   - pendingCount: Confirmed AND scheduledAt <= now — i.e. its slot time has arrived/passed
    //     but a Grid Operator hasn't scanned/completed it yet.
    //   - approvedFutureCount: Confirmed AND scheduledAt > now — upcoming approved bookings, per
    //     the explicit "count of approved (Confirmed) future reservations" requirement.
    //   pendingCount + approvedFutureCount always equals activeCount, since they're a time-based
    //   partition of the same Confirmed set.
    public async Task<ProsumerDashboardSummaryResponse> GetProsumerSummaryAsync(string nic)
    {
        ValidateNic(nic);

        var now = DateTime.UtcNow;
        var filterBuilder = Builders<Reservation>.Filter;
        var byNic = filterBuilder.Eq(r => r.ProsumerNic, nic);
        var confirmed = byNic & filterBuilder.Eq(r => r.Status, "Confirmed");

        var activeCount = (int)await _context.Reservations.CountDocumentsAsync(confirmed);
        var pendingCount = (int)await _context.Reservations
            .CountDocumentsAsync(confirmed & filterBuilder.Lte(r => r.ScheduledAt, now));
        var approvedFutureCount = (int)await _context.Reservations
            .CountDocumentsAsync(confirmed & filterBuilder.Gt(r => r.ScheduledAt, now));

        var recentHistory = await _context.Reservations
            .Find(byNic)
            .SortByDescending(r => r.ScheduledAt)
            .Limit(RecentHistoryLimit)
            .ToListAsync();

        return new ProsumerDashboardSummaryResponse
        {
            ActiveCount = activeCount,
            PendingCount = pendingCount,
            ApprovedFutureCount = approvedFutureCount,
            RecentHistory = recentHistory.Select(MapToHistoryItem).ToList()
        };
    }

    // Searches/filters reservations by any combination of nic, stationId, status, and a
    // scheduledAt date range. When callerRole is "Prosumer", the nic filter is forced to
    // callerNic regardless of what was requested in nic — see the TODO(Rukshan) in
    // ReservationsSearchController for why callerRole/callerNic are hardcoded placeholders today
    // rather than real JWT claims.
    public async Task<List<ReservationResponse>> SearchAsync(
        string? nic,
        string? stationId,
        string? status,
        DateTime? from,
        DateTime? to,
        string callerRole,
        string? callerNic)
    {
        var effectiveNic = nic;
        if (string.Equals(callerRole, "Prosumer", StringComparison.OrdinalIgnoreCase))
        {
            effectiveNic = callerNic;
        }

        var filterBuilder = Builders<Reservation>.Filter;
        var filter = filterBuilder.Empty;

        if (!string.IsNullOrWhiteSpace(effectiveNic))
        {
            filter &= filterBuilder.Eq(r => r.ProsumerNic, effectiveNic);
        }

        if (!string.IsNullOrWhiteSpace(stationId))
        {
            filter &= filterBuilder.Eq(r => r.StationId, stationId);
        }

        if (!string.IsNullOrWhiteSpace(status))
        {
            filter &= filterBuilder.Eq(r => r.Status, NormalizeStatus(status));
        }

        if (from.HasValue)
        {
            filter &= filterBuilder.Gte(r => r.ScheduledAt, from.Value);
        }

        if (to.HasValue)
        {
            filter &= filterBuilder.Lte(r => r.ScheduledAt, to.Value);
        }

        var reservations = await _context.Reservations
            .Find(filter)
            .SortByDescending(r => r.ScheduledAt)
            .ToListAsync();

        return reservations.Select(MapToResponse).ToList();
    }

    // Ensures a NIC was actually supplied — this endpoint reads another person's reservations by
    // NIC, so an empty value would otherwise silently match nothing rather than fail loudly.
    private static void ValidateNic(string nic)
    {
        if (string.IsNullOrWhiteSpace(nic))
        {
            throw new ValidationException("nic is required.");
        }
    }

    // Reservation.Status is a plain string (Dinil's model — see Reservation.cs), not a validated
    // enum, so a status query param is matched case-insensitively against the exact three values
    // his ReservationService ever writes and normalized to that casing before filtering, rather
    // than trusting the caller's casing to already match what's stored.
    private static readonly string[] ValidStatuses = { "Confirmed", "Completed", "Cancelled" };

    private static string NormalizeStatus(string status)
    {
        var match = ValidStatuses.FirstOrDefault(s => string.Equals(s, status, StringComparison.OrdinalIgnoreCase));
        if (match is null)
        {
            throw new ValidationException($"Invalid status filter '{status}'. Expected 'Confirmed', 'Completed', or 'Cancelled'.");
        }
        return match;
    }

    // Maps a stored Reservation to its full client-facing response shape.
    private static ReservationResponse MapToResponse(Reservation reservation) => new()
    {
        Id = reservation.Id!,
        ProsumerNic = reservation.ProsumerNic,
        StationId = reservation.StationId,
        SlotId = reservation.SlotId,
        ScheduledAt = reservation.ScheduledAt,
        Status = reservation.Status,
        QrToken = reservation.QrToken,
        QrTokenExpiresAt = reservation.QrTokenExpiresAt,
        CreatedAt = reservation.CreatedAt,
        UpdatedAt = reservation.UpdatedAt,
        CompletedAt = reservation.CompletedAt,
        CompletedByUserId = reservation.CompletedByUserId,
        CancelReason = reservation.CancelReason
    };

    // Maps a stored Reservation to the compact shape used in a dashboard's recent-history list.
    private static ReservationHistoryItemResponse MapToHistoryItem(Reservation reservation) => new()
    {
        Id = reservation.Id!,
        StationId = reservation.StationId,
        SlotId = reservation.SlotId,
        ScheduledAt = reservation.ScheduledAt,
        Status = reservation.Status
    };
}
