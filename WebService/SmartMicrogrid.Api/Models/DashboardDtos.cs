// ============================================================
// File: DashboardDtos.cs
// Purpose: Response shapes for GET /dashboard/prosumer/{nic}/summary,
//          per architecture.md §3 (moved from Migara to Shalon
//          2026-09-21 - see §3/§4/§7). Used to also hold
//          ReservationSearchResultResponse for a GET /reservations
//          this file's own service implemented - removed 2026-09-26
//          once that turned out to duplicate Dinil's already-built,
//          already-authenticated ReservationsController.List at the
//          identical route (see DashboardService.cs's header).
// Author: Shalon
// ============================================================
namespace SmartMicrogrid.Api.Models;

// Compact shape for a reservation inside a dashboard's recent-history list.
public class ReservationHistoryItemResponse
{
    public string Id { get; set; } = string.Empty;
    public string StationId { get; set; } = string.Empty;
    public string SlotId { get; set; } = string.Empty;
    public DateTime ScheduledAt { get; set; }
    public string Status { get; set; } = string.Empty;
}

// Response body for GET /dashboard/prosumer/{nic}/summary. See DashboardService.GetProsumerSummaryAsync
// for what activeCount/pendingCount/approvedFutureCount mean - architecture.md §3 only named these
// fields, nobody had defined their exact semantics before this implementation.
public class ProsumerDashboardSummaryResponse
{
    public int ActiveCount { get; set; }
    public int PendingCount { get; set; }
    public int ApprovedFutureCount { get; set; }
    public List<ReservationHistoryItemResponse> RecentHistory { get; set; } = new();
}
