// ============================================================
// File: Station.cs
// Purpose: MongoDB document model for a microgrid hub (SolarStations
//          collection), per architecture.md §2.2.
// Author: Shalon
// ============================================================
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using MongoDB.Driver.GeoJsonObjectModel;

namespace SmartMicrogrid.Api.Models;

public class Station
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    [BsonElement("name")]
    public string Name { get; set; } = string.Empty;

    // Stored as GeoJSON so a 2dsphere index can support Migara's $geoNear "nearby stations" query.
    [BsonElement("location")]
    public GeoJsonPoint<GeoJson2DGeographicCoordinates> Location { get; set; } = null!;

    [BsonElement("capacityKWh")]
    public double CapacityKWh { get; set; }

    [BsonElement("totalBatterySlots")]
    public int TotalBatterySlots { get; set; }

    [BsonElement("operatingSchedule")]
    public OperatingSchedule OperatingSchedule { get; set; } = new();

    [BsonElement("status")]
    [BsonRepresentation(BsonType.String)]
    public StationStatus Status { get; set; } = StationStatus.Active;

    [BsonElement("createdByUserId")]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? CreatedByUserId { get; set; }

    [BsonElement("createdAt")]
    public DateTime CreatedAt { get; set; }

    [BsonElement("updatedAt")]
    public DateTime UpdatedAt { get; set; }
}

// Embedded opening/closing hours, per architecture.md §2.2.
public class OperatingSchedule
{
    [BsonElement("opensAt")]
    public string OpensAt { get; set; } = string.Empty;

    [BsonElement("closesAt")]
    public string ClosesAt { get; set; } = string.Empty;
}

public enum StationStatus
{
    Active,
    Inactive
}
