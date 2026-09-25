// ============================================================
// File: Reservation.cs
// Purpose: MongoDB model for the Reservations collection — a
//          prosumer's energy slot booking, including the QR token
//          issued at creation and its lifecycle status.
//          Write-side owner: Dinil (create/update/cancel + QR
//          issuance/verification — the 7-day window and 12-hour
//          notice rules live in his ReservationService). Shalon
//          reads this collection for the station-deactivation check,
//          the reservations-overview panel, and the
//          dashboard/search/nearby endpoints (architecture.md §3/§4)
//          — her services were updated during the 2026-09-26 merge to
//          compare Status as the plain string below (matching
//          Dinil's already-written create/update logic) rather than
//          the ReservationStatus enum her own draft of this file
//          used before the two versions were reconciled.
// Author: Dinil
// ============================================================
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartMicrogrid.Api.Models;

public class Reservation
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string Id { get; set; } = ObjectId.GenerateNewId().ToString();

    [BsonElement("prosumerNic")]
    public string ProsumerNic { get; set; } = null!;

    [BsonElement("stationId")]
    [BsonRepresentation(BsonType.ObjectId)]
    public string StationId { get; set; } = null!;

    [BsonElement("slotId")]
    [BsonRepresentation(BsonType.ObjectId)]
    public string SlotId { get; set; } = null!;

    [BsonElement("scheduledAt")]
    public DateTime ScheduledAt { get; set; }

    [BsonElement("status")]
    public string Status { get; set; } = "Confirmed"; // Confirmed | Completed | Cancelled

    [BsonElement("qrToken")]
    public string QrToken { get; set; } = null!;

    [BsonElement("qrTokenExpiresAt")]
    public DateTime QrTokenExpiresAt { get; set; }

    [BsonElement("createdAt")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [BsonElement("updatedAt")]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    [BsonElement("completedAt")]
    public DateTime? CompletedAt { get; set; }

    [BsonElement("completedByUserId")]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? CompletedByUserId { get; set; }

    [BsonElement("cancelReason")]
    public string? CancelReason { get; set; }
}
