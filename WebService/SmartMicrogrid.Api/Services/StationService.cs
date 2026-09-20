// ============================================================
// File: StationService.cs
// Purpose: Business rules for SolarStations (microgrid hub)
//          administration — list/search, detail-with-slots, create,
//          partial update, and the nearby-stations $geoNear query
//          (moved from Migara 2026-09-21, see architecture.md §3/§4)
//          — per architecture.md §2.2 and §3 "Owned by Shalon". All
//          validation and Mongo access lives here; StationsController
//          stays thin.
// Author: Shalon
// ============================================================
using System.Text.RegularExpressions;
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using MongoDB.Driver;
using MongoDB.Driver.GeoJsonObjectModel;
using SmartMicrogrid.Api.Data;
using SmartMicrogrid.Api.Models;

namespace SmartMicrogrid.Api.Services;

public class StationService
{
    private readonly MongoDbContext _context;

    public StationService(MongoDbContext context)
    {
        _context = context;
    }

    // Ensures the 2dsphere index on SolarStations.location exists; safe to call on every startup
    // since MongoDB no-ops index creation when an equivalent index already exists.
    public async Task EnsureIndexesAsync()
    {
        var indexKeys = Builders<Station>.IndexKeys.Geo2DSphere(s => s.Location);
        var indexModel = new CreateIndexModel<Station>(indexKeys, new CreateIndexOptions { Name = "location_2dsphere" });
        await _context.SolarStations.Indexes.CreateOneAsync(indexModel);
    }

    // Builds an optional name-search + status filter, then returns matching stations as summaries.
    public async Task<List<StationSummaryResponse>> GetAllAsync(string? search, string? status)
    {
        var filterBuilder = Builders<Station>.Filter;
        var filter = filterBuilder.Empty;

        if (!string.IsNullOrWhiteSpace(search))
        {
            filter &= filterBuilder.Regex(s => s.Name, new BsonRegularExpression(Regex.Escape(search), "i"));
        }

        if (!string.IsNullOrWhiteSpace(status))
        {
            if (!Enum.TryParse<StationStatus>(status, ignoreCase: true, out var parsedStatus))
            {
                throw new ValidationException($"Invalid status filter '{status}'. Expected 'Active' or 'Inactive'.");
            }
            filter &= filterBuilder.Eq(s => s.Status, parsedStatus);
        }

        var stations = await _context.SolarStations.Find(filter).SortBy(s => s.Name).ToListAsync();
        return stations.Select(MapToSummary).ToList();
    }

    // Loads a station by id and attaches its slots for the full detail view; returns null if not found.
    public async Task<StationDetailResponse?> GetByIdAsync(string id)
    {
        if (!ObjectId.TryParse(id, out _))
        {
            return null;
        }

        var station = await _context.SolarStations.Find(s => s.Id == id).FirstOrDefaultAsync();
        if (station is null)
        {
            return null;
        }

        var slots = await _context.EnergyBookingSlots.Find(sl => sl.StationId == id).SortBy(sl => sl.SlotNumber).ToListAsync();

        return new StationDetailResponse
        {
            Id = station.Id!,
            Name = station.Name,
            Location = MapLocation(station.Location),
            CapacityKWh = station.CapacityKWh,
            TotalBatterySlots = station.TotalBatterySlots,
            OperatingSchedule = new OperatingScheduleDto
            {
                OpensAt = station.OperatingSchedule.OpensAt,
                ClosesAt = station.OperatingSchedule.ClosesAt
            },
            Status = station.Status.ToString(),
            CreatedByUserId = station.CreatedByUserId,
            CreatedAt = station.CreatedAt,
            UpdatedAt = station.UpdatedAt,
            Slots = slots.Select(MapSlot).ToList()
        };
    }

    // Validates the incoming station fields, then inserts a new SolarStations document.
    public async Task<string> CreateAsync(CreateStationRequest request, string? createdByUserId)
    {
        ValidateName(request.Name);
        ValidateCoordinates(request.Lat, request.Lng);
        ValidateCapacity(request.CapacityKWh);
        ValidateTotalBatterySlots(request.TotalBatterySlots);
        ValidateOperatingSchedule(request.OperatingSchedule);

        var now = DateTime.UtcNow;
        var station = new Station
        {
            Name = request.Name.Trim(),
            Location = new GeoJsonPoint<GeoJson2DGeographicCoordinates>(
                new GeoJson2DGeographicCoordinates(request.Lng, request.Lat)),
            CapacityKWh = request.CapacityKWh,
            TotalBatterySlots = request.TotalBatterySlots,
            OperatingSchedule = new OperatingSchedule
            {
                OpensAt = request.OperatingSchedule.OpensAt,
                ClosesAt = request.OperatingSchedule.ClosesAt
            },
            Status = StationStatus.Active,
            CreatedByUserId = createdByUserId,
            CreatedAt = now,
            UpdatedAt = now
        };

        await _context.SolarStations.InsertOneAsync(station);
        return station.Id!;
    }

    // Applies only the fields present on the request to an existing station, validating each one that's present.
    public async Task UpdateAsync(string id, UpdateStationRequest request)
    {
        if (!ObjectId.TryParse(id, out _))
        {
            throw new NotFoundException($"Station '{id}' not found.");
        }

        var updateBuilder = Builders<Station>.Update;
        var updates = new List<UpdateDefinition<Station>>();

        if (request.Name is not null)
        {
            ValidateName(request.Name);
            updates.Add(updateBuilder.Set(s => s.Name, request.Name.Trim()));
        }

        if (request.Lat.HasValue || request.Lng.HasValue)
        {
            if (!request.Lat.HasValue || !request.Lng.HasValue)
            {
                throw new ValidationException("Both 'lat' and 'lng' must be provided together to update location.");
            }
            ValidateCoordinates(request.Lat.Value, request.Lng.Value);
            updates.Add(updateBuilder.Set(
                s => s.Location,
                new GeoJsonPoint<GeoJson2DGeographicCoordinates>(
                    new GeoJson2DGeographicCoordinates(request.Lng.Value, request.Lat.Value))));
        }

        if (request.CapacityKWh.HasValue)
        {
            ValidateCapacity(request.CapacityKWh.Value);
            updates.Add(updateBuilder.Set(s => s.CapacityKWh, request.CapacityKWh.Value));
        }

        if (request.TotalBatterySlots.HasValue)
        {
            ValidateTotalBatterySlots(request.TotalBatterySlots.Value);
            updates.Add(updateBuilder.Set(s => s.TotalBatterySlots, request.TotalBatterySlots.Value));
        }

        if (request.OperatingSchedule is not null)
        {
            ValidateOperatingSchedule(request.OperatingSchedule);
            updates.Add(updateBuilder.Set(s => s.OperatingSchedule, new OperatingSchedule
            {
                OpensAt = request.OperatingSchedule.OpensAt,
                ClosesAt = request.OperatingSchedule.ClosesAt
            }));
        }

        if (updates.Count == 0)
        {
            throw new ValidationException("No fields provided to update.");
        }

        updates.Add(updateBuilder.Set(s => s.UpdatedAt, DateTime.UtcNow));

        var result = await _context.SolarStations.UpdateOneAsync(s => s.Id == id, updateBuilder.Combine(updates));
        if (result.MatchedCount == 0)
        {
            throw new NotFoundException($"Station '{id}' not found.");
        }
    }

    // Flips a station to Inactive, but only once it's confirmed there are no active slots or
    // reservations tying it up (architecture.md §3: PUT /stations/{id}/deactivate, 409 if any Slot
    // is Reserved or any Reservation is Confirmed at this station).
    public async Task DeactivateAsync(string id)
    {
        if (!ObjectId.TryParse(id, out _))
        {
            throw new NotFoundException($"Station '{id}' not found.");
        }

        var station = await _context.SolarStations.Find(s => s.Id == id).FirstOrDefaultAsync();
        if (station is null)
        {
            throw new NotFoundException($"Station '{id}' not found.");
        }

        var hasReservedSlots = await HasReservedSlotsAsync(id);
        var hasConfirmedReservations = await HasConfirmedReservationsAsync(id);

        if (hasReservedSlots || hasConfirmedReservations)
        {
            var blockers = new List<string>();
            if (hasReservedSlots)
            {
                blockers.Add("one or more of its slots are currently Reserved");
            }
            if (hasConfirmedReservations)
            {
                blockers.Add("one or more of its reservations are currently Confirmed");
            }
            throw new ConflictException($"Cannot deactivate station '{id}': {string.Join(", and ", blockers)}.");
        }

        var update = Builders<Station>.Update
            .Set(s => s.Status, StationStatus.Inactive)
            .Set(s => s.UpdatedAt, DateTime.UtcNow);
        await _context.SolarStations.UpdateOneAsync(s => s.Id == id, update);
    }

    // Returns the read-only "who's booked what" overview for a station, per architecture.md §3
    // GET /stations/{id}/reservations-overview. Reads Dinil's Reservations collection (write-side
    // owner: Dinil; Shalon reads it — see §4).
    public async Task<List<ReservationOverviewResponse>> GetReservationsOverviewAsync(string stationId)
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

        var reservations = await _context.Reservations
            .Find(r => r.StationId == stationId)
            .SortByDescending(r => r.ScheduledAt)
            .ToListAsync();

        return reservations.Select(r => new ReservationOverviewResponse
        {
            SlotId = r.SlotId,
            Status = r.Status.ToString(),
            ProsumerNic = r.ProsumerNic,
            ScheduledAt = r.ScheduledAt
        }).ToList();
    }

    // Finds active stations within radiusKm of (lat, lng), closest first, via a $geoNear
    // aggregation against the 2dsphere index on SolarStations.location. Moved from Migara to
    // Shalon 2026-09-21 — see architecture.md §3/§4.
    public async Task<List<NearbyStationResponse>> GetNearbyAsync(double lat, double lng, double radiusKm)
    {
        ValidateCoordinates(lat, lng);
        if (radiusKm <= 0)
        {
            throw new ValidationException("radiusKm must be greater than 0.");
        }

        var geoNearStage = new BsonDocument("$geoNear", new BsonDocument
        {
            { "near", new BsonDocument { { "type", "Point" }, { "coordinates", new BsonArray { lng, lat } } } },
            { "distanceField", "distanceMeters" },
            { "maxDistance", radiusKm * 1000 },
            { "spherical", true },
            { "query", new BsonDocument("status", StationStatus.Active.ToString()) }
        });
        var projectStage = new BsonDocument("$project", new BsonDocument
        {
            { "_id", 1 },
            { "name", 1 },
            { "location", 1 },
            { "distanceMeters", 1 }
        });

        var pipeline = PipelineDefinition<Station, NearbyAggregationResult>.Create(
            new[] { geoNearStage, projectStage });
        var results = await _context.SolarStations.Aggregate(pipeline).ToListAsync();

        return results.Select(r => new NearbyStationResponse
        {
            Id = r.Id,
            Name = r.Name,
            Location = MapLocation(r.Location),
            DistanceKm = Math.Round(r.DistanceMeters / 1000, 3)
        }).ToList();
    }

    // Checks EnergyBookingSlots (Shalon's own collection) for any Reserved slot at this station.
    private async Task<bool> HasReservedSlotsAsync(string stationId)
    {
        return await _context.EnergyBookingSlots
            .Find(sl => sl.StationId == stationId && sl.Status == SlotStatus.Reserved)
            .AnyAsync();
    }

    // Checks Dinil's Reservations collection (write-side owner: Dinil; Shalon reads it — see §4)
    // for any Confirmed reservation at this station.
    private async Task<bool> HasConfirmedReservationsAsync(string stationId)
    {
        return await _context.Reservations
            .Find(r => r.StationId == stationId && r.Status == ReservationStatus.Confirmed)
            .AnyAsync();
    }

    // Maps a stored Station document to the GET /stations list-row shape.
    private static StationSummaryResponse MapToSummary(Station station) => new()
    {
        Id = station.Id!,
        Name = station.Name,
        Location = MapLocation(station.Location),
        CapacityKWh = station.CapacityKWh,
        TotalBatterySlots = station.TotalBatterySlots,
        Status = station.Status.ToString()
    };

    // Maps a stored Slot document to its client-facing response shape.
    private static SlotResponse MapSlot(Slot slot) => new()
    {
        Id = slot.Id!,
        StationId = slot.StationId,
        SlotNumber = slot.SlotNumber,
        Type = slot.Type.ToString(),
        CapacityKWh = slot.CapacityKWh,
        Status = slot.Status.ToString(),
        UpdatedAt = slot.UpdatedAt
    };

    // Converts the stored GeoJSON point back into the flat {type, coordinates} shape clients receive.
    private static LocationResponse MapLocation(GeoJsonPoint<GeoJson2DGeographicCoordinates> location) => new()
    {
        Type = "Point",
        Coordinates = new[] { location.Coordinates.Longitude, location.Coordinates.Latitude }
    };

    // Ensures the station name is present and not just whitespace.
    private static void ValidateName(string name)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            throw new ValidationException("Station name is required.");
        }
    }

    // Ensures latitude/longitude are within valid GPS bounds (numeric-ness is already enforced by the double type).
    private static void ValidateCoordinates(double lat, double lng)
    {
        if (lat < -90 || lat > 90)
        {
            throw new ValidationException("Latitude must be a number between -90 and 90.");
        }
        if (lng < -180 || lng > 180)
        {
            throw new ValidationException("Longitude must be a number between -180 and 180.");
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

    // Ensures the declared slot count is a positive number.
    private static void ValidateTotalBatterySlots(int totalBatterySlots)
    {
        if (totalBatterySlots <= 0)
        {
            throw new ValidationException("totalBatterySlots must be greater than 0.");
        }
    }

    // Ensures both operating-hours fields are present.
    private static void ValidateOperatingSchedule(OperatingScheduleDto schedule)
    {
        if (string.IsNullOrWhiteSpace(schedule.OpensAt) || string.IsNullOrWhiteSpace(schedule.ClosesAt))
        {
            throw new ValidationException("operatingSchedule.opensAt and closesAt are required.");
        }
    }

    // Shape the $geoNear + $project pipeline in GetNearbyAsync projects into — kept to exactly
    // these fields so deserialization doesn't collide with Station's own class map (which has no
    // "distanceMeters" field and would otherwise reject it as an unmapped element).
    private class NearbyAggregationResult
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string Id { get; set; } = string.Empty;

        [BsonElement("name")]
        public string Name { get; set; } = string.Empty;

        [BsonElement("location")]
        public GeoJsonPoint<GeoJson2DGeographicCoordinates> Location { get; set; } = null!;

        [BsonElement("distanceMeters")]
        public double DistanceMeters { get; set; }
    }
}
