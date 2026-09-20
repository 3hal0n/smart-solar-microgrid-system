// ============================================================
// File: Reservation.cs
// Purpose: MongoDB document model for a prosumer's energy-slot
//          booking (Reservations collection), per architecture.md
//          §2.4. Dinil owns this collection's write-side business
//          rules (create/update/cancel + QR issuance) and should be
//          consulted before this schema changes — this file exists
//          now only so Shalon's dashboard/reservations-search/nearby
//          endpoints (moved from Migara, see architecture.md §3/§4)
//          have a type-safe way to read it.
// Author: Shalon
// ============================================================
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartMicrogrid.Api.Models;

public class Reservation
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    [BsonElement("prosumerNic")]
    public string ProsumerNic { get; set; } = string.Empty;

    [BsonElement("stationId")]
    [BsonRepresentation(BsonType.ObjectId)]
    public string StationId { get; set; } = string.Empty;

    [BsonElement("slotId")]
    [BsonRepresentation(BsonType.ObjectId)]
    public string SlotId { get; set; } = string.Empty;

    [BsonElement("scheduledAt")]
    public DateTime ScheduledAt { get; set; }

    [BsonElement("status")]
    [BsonRepresentation(BsonType.String)]
    public ReservationStatus Status { get; set; }

    [BsonElement("qrToken")]
    public string QrToken { get; set; } = string.Empty;

    [BsonElement("qrTokenExpiresAt")]
    public DateTime QrTokenExpiresAt { get; set; }

    [BsonElement("createdAt")]
    public DateTime CreatedAt { get; set; }

    [BsonElement("updatedAt")]
    public DateTime UpdatedAt { get; set; }

    [BsonElement("completedAt")]
    public DateTime? CompletedAt { get; set; }

    [BsonElement("completedByUserId")]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? CompletedByUserId { get; set; }

    [BsonElement("cancelReason")]
    public string? CancelReason { get; set; }
}

public enum ReservationStatus
{
    Confirmed,
    Completed,
    Cancelled
}
