// ============================================================
// File: ReservationDtos.cs
// Purpose: Request/response shapes for the reservation
//          endpoints, keeping API contract decoupled from the
//          MongoDB model and matching architecture.md §3.
// Author: Dinil
// ============================================================

namespace SmartMicrogrid.Api.Models;

// ---------- Requests ----------
public class CreateReservationRequest
{
    public string StationId { get; set; } = null!;
    public string SlotId { get; set; } = null!;
    public DateTime ScheduledAt { get; set; }
}

public class UpdateReservationRequest
{
    public string? SlotId { get; set; }
    public DateTime? ScheduledAt { get; set; }
}

public class CancelReservationRequest
{
    public string? Reason { get; set; }
}

public class VerifyQrRequest
{
    public string QrToken { get; set; } = null!;
}

// ---------- Responses ----------
public class CreateReservationResponse
{
    public string Id { get; set; } = null!;
    public string Status { get; set; } = null!;
    public string QrToken { get; set; } = null!;
    public DateTime QrTokenExpiresAt { get; set; }
}

public class ReservationResponse
{
    public string Id { get; set; } = null!;
    public string ProsumerNic { get; set; } = null!;
    public string StationId { get; set; } = null!;
    public string SlotId { get; set; } = null!;
    public DateTime ScheduledAt { get; set; }
    public string Status { get; set; } = null!;
    public string? QrToken { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime? CompletedAt { get; set; }
    public string? CancelReason { get; set; }
}

public class VerifyQrResponse
{
    public string ReservationId { get; set; } = null!;
    public string ProsumerName { get; set; } = null!;
    public string StationName { get; set; } = null!;
    public int SlotNumber { get; set; }
    public string Status { get; set; } = null!;
}