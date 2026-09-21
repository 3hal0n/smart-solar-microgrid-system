// ============================================================
// File: SlotDtos.cs
// Purpose: Request/response shapes for the Slots endpoints, per
//          architecture.md §3 "Owned by Shalon" table.
// Author: Shalon
// ============================================================
namespace SmartMicrogrid.Api.Models;

// Request body for POST /stations/{id}/slots.
public class CreateSlotRequest
{
    public int SlotNumber { get; set; }
    public SlotType Type { get; set; }
    public double CapacityKWh { get; set; }
}

// Request body for PUT /slots/{id} - every field optional (partial update).
public class UpdateSlotRequest
{
    public int? SlotNumber { get; set; }
    public SlotType? Type { get; set; }
    public double? CapacityKWh { get; set; }
    public SlotStatus? Status { get; set; }
}

// Response shape for a slot, used standalone and nested inside StationDetailResponse.
public class SlotResponse
{
    public string Id { get; set; } = string.Empty;
    public string StationId { get; set; } = string.Empty;
    public int SlotNumber { get; set; }
    public string Type { get; set; } = string.Empty;
    public double CapacityKWh { get; set; }
    public string Status { get; set; } = string.Empty;
    public DateTime UpdatedAt { get; set; }
}
