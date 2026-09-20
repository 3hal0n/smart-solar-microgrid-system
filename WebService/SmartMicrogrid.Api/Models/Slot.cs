// ============================================================
// File: Slot.cs
// Purpose: MongoDB document model for a station's battery slot
//          (EnergyBookingSlots collection), per architecture.md §2.3.
// Author: Shalon
// ============================================================
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartMicrogrid.Api.Models;

public class Slot
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    [BsonElement("stationId")]
    [BsonRepresentation(BsonType.ObjectId)]
    public string StationId { get; set; } = string.Empty;

    [BsonElement("slotNumber")]
    public int SlotNumber { get; set; }

    [BsonElement("type")]
    [BsonRepresentation(BsonType.String)]
    public SlotType Type { get; set; }

    [BsonElement("capacityKWh")]
    public double CapacityKWh { get; set; }

    // Flipped to Reserved/Available by Dinil's reservation logic via SlotService.MarkReserved/MarkAvailable
    // (an internal call, not a public endpoint) per architecture.md §4.
    [BsonElement("status")]
    [BsonRepresentation(BsonType.String)]
    public SlotStatus Status { get; set; } = SlotStatus.Available;

    [BsonElement("updatedAt")]
    public DateTime UpdatedAt { get; set; }
}

public enum SlotType
{
    Charging,
    Discharging
}

public enum SlotStatus
{
    Available,
    Reserved,
    Maintenance
}
