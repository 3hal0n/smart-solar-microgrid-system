// ============================================================
// File: DashboardService.cs
// Purpose: Read-only queries over Dinil's Reservations collection —
//          a prosumer's dashboard summary — per architecture.md §3
//          (moved from Migara to Shalon 2026-09-21, see §3/§4/§7).
//          Used to also own GET /reservations search/filter, but that
//          duplicated Dinil's ReservationsController.List (added
//          during his own merged-in work) at the exact same route —
//          ASP.NET can't route GET /api/reservations to two
//          controllers, so this always 500'd once both existed
//          together, surfaced to the browser as a CORS error since
//          the exception happens during routing, before CORS headers
//          are attached. Removed 2026-09-26 rather than renamed: his
//          version does real JWT-based Prosumer NIC-scoping; mine was
//          an unauthenticated placeholder that would have been a
//          privacy hole if kept as a second, open route to the same
//          data.
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


    // ============================================================
    // Dinil addition to DashboardService — operator summary
    // aggregation. Reads the Reservations collection (architecture.md
    // 2.4, owner: Dinil) for confirmed/completed-today counts and
    // pending-by-station breakdown. Called by DashboardController's
    // GET /api/dashboard/operator/summary.
    // ============================================================

    // Returns today's confirmed/completed reservation counts plus a
    // per-station breakdown of pending (Confirmed but not yet scanned)
    // reservations. All times computed in UTC; the client renders them.
    // ============================================================
    // OPERATOR DASHBOARD SUMMARY
    // ============================================================
    public async Task<OperatorSummaryResponse> GetOperatorSummaryAsync()
    {
        var now = DateTime.UtcNow;

        var startOfTodayUtc = now.Date;
        var startOfTomorrowUtc = startOfTodayUtc.AddDays(1);

        var reservationFilter = Builders<Reservation>.Filter;

        // --------------------------------------------------------
        // Confirmed reservations created today
        // --------------------------------------------------------

        var confirmedTodayFilter =
            reservationFilter.Gte(
                r => r.CreatedAt,
                startOfTodayUtc
            )
            &
            reservationFilter.Lt(
                r => r.CreatedAt,
                startOfTomorrowUtc
            );

        var confirmedTodayCount =
            (int)await _context.Reservations
                .CountDocumentsAsync(confirmedTodayFilter);

        // --------------------------------------------------------
        // Completed reservations today
        // --------------------------------------------------------

        var completedTodayFilter =
            reservationFilter.Eq(
                r => r.Status,
                "Completed"
            )
            &
            reservationFilter.Gte(
                r => r.CompletedAt,
                startOfTodayUtc
            )
            &
            reservationFilter.Lt(
                r => r.CompletedAt,
                startOfTomorrowUtc
            );

        var completedTodayCount =
            (int)await _context.Reservations
                .CountDocumentsAsync(completedTodayFilter);

        // --------------------------------------------------------
        // Pending reservations by station
        // --------------------------------------------------------
        //
        // Pending = Confirmed reservations that haven't been
        // completed/scanned yet.
        //

        var pendingFilter =
            reservationFilter.Eq(
                r => r.Status,
                "Confirmed"
            );

        var pendingReservations =
            await _context.Reservations
                .Find(pendingFilter)
                .ToListAsync();

        var groupedByStation = pendingReservations
            .GroupBy(r => r.StationId)
            .Select(g => new
            {
                StationId = g.Key,
                Count = g.Count()
            })
            .ToList();

        // --------------------------------------------------------
        // Get station names
        // --------------------------------------------------------

        var stationIds = groupedByStation
            .Select(g => g.StationId)
            .Where(id => !string.IsNullOrWhiteSpace(id))
            .Distinct()
            .ToList();

        var stationLookup = new Dictionary<string, string>();

        if (stationIds.Count > 0)
        {
            // IMPORTANT:
            // Change SolarStations to the actual collection property
            // in MongoDbContext if your project uses another name.

            var stationFilter =
                Builders<Station>.Filter.In(
                    s => s.Id,
                    stationIds
                );

            var stations =
                await _context.SolarStations
                    .Find(stationFilter)
                    .ToListAsync();

            foreach (var station in stations)
            {
                if (!string.IsNullOrWhiteSpace(station.Id))
                {
                    stationLookup[station.Id] = station.Name;
                }
            }
        }

        // --------------------------------------------------------
        // Build response
        // --------------------------------------------------------

        var pendingByStation = groupedByStation
            .Select(g => new PendingByStation
            {
                StationId = g.StationId,
                StationName =
                    stationLookup.TryGetValue(
                        g.StationId,
                        out var stationName
                    )
                        ? stationName
                        : "",

                PendingCount = g.Count
            })
            .ToList();

        return new OperatorSummaryResponse
        {
            ConfirmedTodayCount = confirmedTodayCount,
            CompletedTodayCount = completedTodayCount,
            PendingByStation = pendingByStation
        };
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
