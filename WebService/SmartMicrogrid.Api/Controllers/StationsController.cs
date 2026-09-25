// ============================================================
// File: StationsController.cs
// Purpose: HTTP endpoints for microgrid hub (station) management —
//          list/search, detail-with-slots, create, partial update,
//          and the nearby-stations query (moved from Migara
//          2026-09-21, see architecture.md §3/§4). Thin controller:
//          all business rules and validation live in StationService
//          per the FAT service pattern; this file only maps
//          requests/exceptions to HTTP.
// Author: Shalon
// ============================================================
using Microsoft.AspNetCore.Mvc;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Services;

namespace SmartMicrogrid.Api.Controllers;

[ApiController]
[Route("api/stations")]
public class StationsController : ControllerBase
{
    private readonly StationService _stationService;

    public StationsController(StationService stationService)
    {
        _stationService = stationService;
    }

    // GET /api/stations?search=&status= - lists stations for any authenticated role.
    [HttpGet]
    public async Task<IActionResult> GetAll([FromQuery] string? search, [FromQuery] string? status)
    {
        try
        {
            var stations = await _stationService.GetAllAsync(search, status);
            return Ok(stations);
        }
        catch (ValidationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    // GET /api/stations/nearby?lat=&lng=&radiusKm= - active stations within radiusKm, closest
    // first. Moved from Migara to Shalon 2026-09-21 — see architecture.md §3/§4. Registered before
    // "{id}" in this file only for readability; ASP.NET Core's routing always matches the literal
    // "nearby" segment ahead of the "{id}" parameter regardless of declaration order.
    [HttpGet("nearby")]
    public async Task<IActionResult> GetNearby([FromQuery] double lat, [FromQuery] double lng, [FromQuery] double radiusKm)
    {
        try
        {
            var stations = await _stationService.GetNearbyAsync(lat, lng, radiusKm);
            return Ok(stations);
        }
        catch (ValidationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    // GET /api/stations/{id} - full station detail plus its slots.
    [HttpGet("{id}")]
    public async Task<IActionResult> GetById(string id)
    {
        var station = await _stationService.GetByIdAsync(id);
        if (station is null)
        {
            return NotFound(new { message = $"Station '{id}' not found." });
        }

        return Ok(station);
    }

    // POST /api/stations - creates a new station.
    // TODO(Rukshan): restore [Authorize(Roles = "Backoffice")] and read the caller's id from the JWT
    // once JwtService/auth scheme is wired up 
    [HttpPost]
    public async Task<IActionResult> Create([FromBody] CreateStationRequest request)
    {
        try
        {
            var id = await _stationService.CreateAsync(request, createdByUserId: null);
            return CreatedAtAction(nameof(GetById), new { id }, new CreatedIdResponse { Id = id });
        }
        catch (ValidationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    // PUT /api/stations/{id} - partial update of station fields.
    // TODO(Rukshan): restore [Authorize(Roles = "Backoffice")] once JwtService/auth scheme is wired up.
    [HttpPut("{id}")]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateStationRequest request)
    {
        try
        {
            await _stationService.UpdateAsync(id, request);
            return NoContent();
        }
        catch (NotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
        catch (ValidationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }

    // PUT /api/stations/{id}/deactivate - blocks with 409 if the station has active slots/reservations.
    // TODO(Rukshan): restore [Authorize(Roles = "Backoffice")] once JwtService/auth scheme is wired up.
    [HttpPut("{id}/deactivate")]
    public async Task<IActionResult> Deactivate(string id)
    {
        try
        {
            await _stationService.DeactivateAsync(id);
            return NoContent();
        }
        catch (NotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
        catch (ConflictException ex)
        {
            return Conflict(new { message = ex.Message });
        }
    }

    // PUT /api/stations/{id}/activate - flips an Inactive station back to Active. No conflict check
    // needed (unlike deactivate): reactivating never blocks on anything.
    // TODO(Rukshan): restore [Authorize(Roles = "Backoffice")] once JwtService/auth scheme is wired up.
    [HttpPut("{id}/activate")]
    public async Task<IActionResult> Activate(string id)
    {
        try
        {
            await _stationService.ActivateAsync(id);
            return NoContent();
        }
        catch (NotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
    }

    // GET /api/stations/{id}/reservations-overview - read-only "who's booked what" panel.
    // Currently always returns [] — see StationService.GetReservationsOverviewAsync's TODO(Dinil).
    [HttpGet("{id}/reservations-overview")]
    public async Task<IActionResult> GetReservationsOverview(string id)
    {
        try
        {
            var overview = await _stationService.GetReservationsOverviewAsync(id);
            return Ok(overview);
        }
        catch (NotFoundException ex)
        {
            return NotFound(new { message = ex.Message });
        }
    }
}
