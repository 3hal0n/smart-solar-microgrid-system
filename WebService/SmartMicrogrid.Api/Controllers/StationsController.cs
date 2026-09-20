// ============================================================
// File: StationsController.cs
// Purpose: HTTP endpoints for microgrid hub (station) management —
//          list/search, detail-with-slots, create, and partial
//          update. Thin controller: all business rules and
//          validation live in StationService per the FAT service
//          pattern; this file only maps requests/exceptions to HTTP.
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
    // once JwtService/auth scheme is wired up (see CLAUDE.md cross-module dependencies).
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
}
