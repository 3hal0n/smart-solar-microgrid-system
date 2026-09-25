// ============================================================
// File: DashboardDtos.cs
// Purpose: Request/response shapes for GET /reservations and
//          GET /dashboard/prosumer/{nic}/summary, per architecture.md
//          §3 (moved from Migara to Shalon 2026-09-21 — see §3/§4/§7).
// Author: Shalon
// ============================================================
namespace SmartMicrogrid.Api.Models;

// Response row for GET /reservations — the full reservation shape architecture.md §3 calls
// "[{ ...reservation }]", matching every field in §2.4's Reservations schema. Named
// ReservationSearchResultResponse (not the more obvious ReservationResponse) since that name was
// already taken by Dinil's ReservationDtos.cs for his own, differently-shaped single-reservation
// response — this is purely a C# type name; it has no effect on this endpoint's JSON.
public class ReservationSearchResultResponse
{
    public string Id { get; set; } = string.Empty;
    public string ProsumerNic { get; set; } = string.Empty;
    public string StationId { get; set; } = string.Empty;
    public string SlotId { get; set; } = string.Empty;
    public DateTime ScheduledAt { get; set; }
    public string Status { get; set; } = string.Empty;
    public string QrToken { get; set; } = string.Empty;
    public DateTime QrTokenExpiresAt { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public DateTime? CompletedAt { get; set; }
    public string? CompletedByUserId { get; set; }
    public string? CancelReason { get; set; }
}

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
// for what activeCount/pendingCount/approvedFutureCount mean — architecture.md §3 only named these
// fields, nobody had defined their exact semantics before this implementation.
public class ProsumerDashboardSummaryResponse
{
    public int ActiveCount { get; set; }
    public int PendingCount { get; set; }
    public int ApprovedFutureCount { get; set; }
    public List<ReservationHistoryItemResponse> RecentHistory { get; set; } = new();
}
