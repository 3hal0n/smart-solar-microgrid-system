// ============================================================
// File: StationDtos.cs
// Purpose: Request/response shapes for the Stations endpoints, per
//          architecture.md §3 "Owned by Shalon" table. Kept separate
//          from the Station document model so the wire format (flat
//          lat/lng, partial-update fields) can differ from the
//          MongoDB storage shape (nested GeoJSON location).
// Author: Shalon
// ============================================================
namespace SmartMicrogrid.Api.Models;

// Request body for POST /stations.
public class CreateStationRequest
{
    public string Name { get; set; } = string.Empty;
    public double Lat { get; set; }
    public double Lng { get; set; }
    public double CapacityKWh { get; set; }
    public int TotalBatterySlots { get; set; }
    public OperatingScheduleDto OperatingSchedule { get; set; } = new();
}

// Request body for PUT /stations/{id} - every field optional (partial update).
public class UpdateStationRequest
{
    public string? Name { get; set; }
    public double? Lat { get; set; }
    public double? Lng { get; set; }
    public double? CapacityKWh { get; set; }
    public int? TotalBatterySlots { get; set; }
    public OperatingScheduleDto? OperatingSchedule { get; set; }
}

// Nested operating-hours shape shared by requests and responses.
public class OperatingScheduleDto
{
    public string OpensAt { get; set; } = string.Empty;
    public string ClosesAt { get; set; } = string.Empty;
}

// GeoJSON-shaped location as returned to clients, matching the stored format.
public class LocationResponse
{
    public string Type { get; set; } = "Point";
    public double[] Coordinates { get; set; } = Array.Empty<double>();
}

// Response row for GET /stations.
public class StationSummaryResponse
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public LocationResponse Location { get; set; } = new();
    public double CapacityKWh { get; set; }
    public string Status { get; set; } = string.Empty;
}

// Response body for GET /stations/{id} - full station plus its slots.
public class StationDetailResponse
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public LocationResponse Location { get; set; } = new();
    public double CapacityKWh { get; set; }
    public int TotalBatterySlots { get; set; }
    public OperatingScheduleDto OperatingSchedule { get; set; } = new();
    public string Status { get; set; } = string.Empty;
    public string? CreatedByUserId { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public List<SlotResponse> Slots { get; set; } = new();
}

// Response body for POST /stations and POST /stations/{id}/slots.
public class CreatedIdResponse
{
    public string Id { get; set; } = string.Empty;
}
