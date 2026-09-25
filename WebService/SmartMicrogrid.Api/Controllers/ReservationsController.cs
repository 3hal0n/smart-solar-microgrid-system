// ============================================================
// File: ReservationsController.cs
// Purpose: Reservation lifecycle endpoints — create, update,
//          cancel, fetch, list, and QR verification. All
//          business rules delegated to ReservationService
//          (FAT service pattern). No validation in clients.
// Author: Dinil
// ============================================================

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Services;

namespace SmartMicrogrid.Api.Controllers;

[ApiController]
[Route("api/reservations")]
[Authorize]
public class ReservationsController : ControllerBase
{
    private readonly ReservationService _service;

    public ReservationsController(ReservationService service)
    {
        _service = service;
    }

    // Creates a reservation under the caller's prosumer identity.
    [HttpPost]
    [Authorize(Roles = "Prosumer")]
    public async Task<IActionResult> Create([FromBody] CreateReservationRequest req)
    {
        var nic = User.FindFirst("nic")?.Value
            ?? throw new ServiceException(401, "MISSING_NIC", "JWT missing nic claim.");
        var result = await _service.CreateAsync(nic, req);
        return Ok(result);
    }

    // Updates a reservation (12-hour rule, 7-day window re-check).
    [HttpPut("{id}")]
    [Authorize(Roles = "Prosumer")]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateReservationRequest req)
    {
        var nic = User.FindFirst("nic")?.Value!;
        await _service.UpdateAsync(id, nic, req);
        return NoContent();
    }

    // Cancels a reservation (12-hour rule, slot returned to Available).
    [HttpPut("{id}/cancel")]
    [Authorize(Roles = "Prosumer")]
    public async Task<IActionResult> Cancel(string id, [FromBody] CancelReservationRequest req)
    {
        var nic = User.FindFirst("nic")?.Value!;
        await _service.CancelAsync(id, nic, req.Reason);
        return NoContent();
    }

    // Fetches one reservation — owning Prosumer or GridOperator/Backoffice.
    [HttpGet("{id}")]
    [Authorize(Roles = "Prosumer,GridOperator,Backoffice")]
    public async Task<IActionResult> Get(string id)
    {
        var role = User.FindFirst("role")?.Value;
        var nic = User.FindFirst("nic")?.Value;

        var reservation = (role == "Prosumer")
            ? await _service.GetOwnedAsync(id, nic!)
            : await _service.GetByIdAsync(id);

        return Ok(MapToResponse(reservation));
    }

    // Lists reservations with filters. Prosumers are scoped to their own NIC.
    [HttpGet]
    [Authorize(Roles = "Prosumer,GridOperator,Backoffice")]
    public async Task<IActionResult> List(
        [FromQuery] string? nic,
        [FromQuery] string? stationId,
        [FromQuery] string? status,
        [FromQuery] DateTime? from,
        [FromQuery] DateTime? to)
    {
        var role = User.FindFirst("role")?.Value;
        var jwtNic = User.FindFirst("nic")?.Value;

        // Prosumers can only ever see their own reservations.
        var effectiveNic = (role == "Prosumer") ? jwtNic : nic;

        var list = await _service.ListAsync(effectiveNic, stationId, status, from, to);
        return Ok(list.Select(MapToResponse));
    }

    // Grid Operator verifies a scanned QR token, flipping the reservation to Completed.
    [HttpPost("verify-qr")]
    [Authorize(Roles = "GridOperator")]
    public async Task<IActionResult> VerifyQr([FromBody] VerifyQrRequest req)
    {
        var operatorUserId = User.FindFirst("sub")?.Value
            ?? throw new ServiceException(401, "MISSING_SUB", "JWT missing sub claim.");
        var result = await _service.VerifyQrAsync(req.QrToken, operatorUserId);
        return Ok(result);
    }

    // Maps a Reservation model to its API response shape.
    private static ReservationResponse MapToResponse(Reservation r) => new()
    {
        Id = r.Id,
        ProsumerNic = r.ProsumerNic,
        StationId = r.StationId,
        SlotId = r.SlotId,
        ScheduledAt = r.ScheduledAt,
        Status = r.Status,
        QrToken = r.QrToken,
        CreatedAt = r.CreatedAt,
        CompletedAt = r.CompletedAt,
        CancelReason = r.CancelReason
    };
}