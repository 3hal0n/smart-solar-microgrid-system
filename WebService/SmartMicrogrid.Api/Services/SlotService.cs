// ============================================================
// File: SlotService.cs
// Purpose: Business rules for EnergyBookingSlots (battery slot
//          administration) — create and partial update, per
//          architecture.md §2.3 and §3 "Owned by Shalon". All
//          validation and Mongo access for slots lives here;
//          SlotsController stays thin.
// Author: Shalon
// ============================================================
using MongoDB.Bson;
using MongoDB.Driver;
using SmartMicrogrid.Api.Data;
using SmartMicrogrid.Api.Models;

namespace SmartMicrogrid.Api.Services;

public class SlotService
{
    private readonly MongoDbContext _context;

    public SlotService(MongoDbContext context)
    {
        _context = context;
    }

    // Creates a new slot under an existing station, validating capacity and slot-number uniqueness.
    public async Task<string> CreateAsync(string stationId, CreateSlotRequest request)
    {
        if (!ObjectId.TryParse(stationId, out _))
        {
            throw new NotFoundException($"Station '{stationId}' not found.");
        }

        var stationExists = await _context.SolarStations.Find(s => s.Id == stationId).AnyAsync();
        if (!stationExists)
        {
            throw new NotFoundException($"Station '{stationId}' not found.");
        }

        ValidateSlotNumber(request.SlotNumber);
        ValidateCapacity(request.CapacityKWh);
        await EnsureSlotNumberIsUniqueAsync(stationId, request.SlotNumber, excludeSlotId: null);

        var slot = new Slot
        {
            StationId = stationId,
            SlotNumber = request.SlotNumber,
            Type = request.Type,
            CapacityKWh = request.CapacityKWh,
            Status = SlotStatus.Available,
            UpdatedAt = DateTime.UtcNow
        };

        await _context.EnergyBookingSlots.InsertOneAsync(slot);
        return slot.Id!;
    }

    // Applies only the fields present on the request to an existing slot, re-validating anything that's present.
    public async Task UpdateAsync(string id, UpdateSlotRequest request)
    {
        if (!ObjectId.TryParse(id, out _))
        {
            throw new NotFoundException($"Slot '{id}' not found.");
        }

        var existing = await _context.EnergyBookingSlots.Find(s => s.Id == id).FirstOrDefaultAsync();
        if (existing is null)
        {
            throw new NotFoundException($"Slot '{id}' not found.");
        }

        var updateBuilder = Builders<Slot>.Update;
        var updates = new List<UpdateDefinition<Slot>>();

        if (request.SlotNumber.HasValue)
        {
            ValidateSlotNumber(request.SlotNumber.Value);
            await EnsureSlotNumberIsUniqueAsync(existing.StationId, request.SlotNumber.Value, excludeSlotId: id);
            updates.Add(updateBuilder.Set(s => s.SlotNumber, request.SlotNumber.Value));
        }

        if (request.Type.HasValue)
        {
            updates.Add(updateBuilder.Set(s => s.Type, request.Type.Value));
        }

        if (request.CapacityKWh.HasValue)
        {
            ValidateCapacity(request.CapacityKWh.Value);
            updates.Add(updateBuilder.Set(s => s.CapacityKWh, request.CapacityKWh.Value));
        }

        if (request.Status.HasValue)
        {
            updates.Add(updateBuilder.Set(s => s.Status, request.Status.Value));
        }

        if (updates.Count == 0)
        {
            throw new ValidationException("No fields provided to update.");
        }

        updates.Add(updateBuilder.Set(s => s.UpdatedAt, DateTime.UtcNow));

        await _context.EnergyBookingSlots.UpdateOneAsync(s => s.Id == id, updateBuilder.Combine(updates));
    }

    // Ensures no other slot at the same station already uses this slot number.
    private async Task EnsureSlotNumberIsUniqueAsync(string stationId, int slotNumber, string? excludeSlotId)
    {
        var filterBuilder = Builders<Slot>.Filter;
        var filter = filterBuilder.Eq(s => s.StationId, stationId) & filterBuilder.Eq(s => s.SlotNumber, slotNumber);
        if (excludeSlotId is not null)
        {
            filter &= filterBuilder.Ne(s => s.Id, excludeSlotId);
        }

        var duplicateExists = await _context.EnergyBookingSlots.Find(filter).AnyAsync();
        if (duplicateExists)
        {
            throw new ValidationException($"Slot number {slotNumber} already exists for this station.");
        }
    }

    // Ensures capacity is a positive number.
    private static void ValidateCapacity(double capacityKWh)
    {
        if (capacityKWh <= 0)
        {
            throw new ValidationException("capacityKWh must be greater than 0.");
        }
    }

    // Ensures the slot number is a positive integer.
    private static void ValidateSlotNumber(int slotNumber)
    {
        if (slotNumber <= 0)
        {
            throw new ValidationException("slotNumber must be greater than 0.");
        }
    }
    
        // ============================================================
    // ADDED BY MIGARA: Helper methods required by ReservationService
    // These methods allow the ReservationService to query and update slot status.
    // TODO: @Shalon - Please review and integrate with your business logic if needed.
    // ============================================================

    // Retrieves a slot by its ID from the database.
    // Returns null if the slot is not found.
    public async Task<Slot?> GetByIdAsync(string slotId)
    {
        if (!ObjectId.TryParse(slotId, out _))
        {
            throw new NotFoundException($"Invalid slot ID format: '{slotId}'.");
        }

        var filter = Builders<Slot>.Filter.Eq(s => s.Id, slotId);
        return await _context.EnergyBookingSlots.Find(filter).FirstOrDefaultAsync();
    }

    // Marks a slot as Reserved when a reservation is created.
    // This prevents other prosumers from booking the same slot.
    public async Task MarkReserved(string slotId)
    {
        if (!ObjectId.TryParse(slotId, out _))
        {
            throw new NotFoundException($"Invalid slot ID format: '{slotId}'.");
        }

        var filter = Builders<Slot>.Filter.Eq(s => s.Id, slotId);
        var update = Builders<Slot>.Update
            .Set(s => s.Status, SlotStatus.Reserved)
            .Set(s => s.UpdatedAt, DateTime.UtcNow);

        var result = await _context.EnergyBookingSlots.UpdateOneAsync(filter, update);

        if (result.MatchedCount == 0)
        {
            throw new NotFoundException($"Slot '{slotId}' not found.");
        }
    }

    // Marks a slot as Available when a reservation is cancelled or completed.
    // This allows other prosumers to book the slot again.
    public async Task MarkAvailable(string slotId)
    {
        if (!ObjectId.TryParse(slotId, out _))
        {
            throw new NotFoundException($"Invalid slot ID format: '{slotId}'.");
        }

        var filter = Builders<Slot>.Filter.Eq(s => s.Id, slotId);
        var update = Builders<Slot>.Update
            .Set(s => s.Status, SlotStatus.Available)
            .Set(s => s.UpdatedAt, DateTime.UtcNow);

        var result = await _context.EnergyBookingSlots.UpdateOneAsync(filter, update);

        if (result.MatchedCount == 0)
        {
            throw new NotFoundException($"Slot '{slotId}' not found.");
        }
    }
}
