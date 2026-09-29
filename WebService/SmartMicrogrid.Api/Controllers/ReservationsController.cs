// ============================================================
// File: ReservationsController.cs
// Purpose: Reservation lifecycle endpoints - create, update,
//          cancel, fetch, list, and QR verification. All
//          business rules delegated to ReservationService
//          (FAT service pattern). No validation in clients.
//          Operators/Backoffice can create reservations on
//          behalf of any Prosumer (phone-in / walk-in booking).
// Author: Dinil
// ============================================================

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Services;

namespace SmartMicrogrid.Api.Controllers;

[ApiController]
[Route("api/reservations")]
//[Authorize]
public class ReservationsController : ControllerBase
{
    private readonly ReservationService _service;

    public ReservationsController(ReservationService service)
    {
        _service = service;
    }

    // Creates a reservation.
    // - Prosumer: creates for themselves (NIC from JWT).
    // - GridOperator / Backoffice: creates on behalf of a Prosumer (NIC from body).
    [HttpPost]
    //[Authorize(Roles = "Prosumer,GridOperator,Backoffice")]
    public async Task<IActionResult> Create([FromBody] CreateReservationRequest req)
    {
        var nic = ResolveProsumerNic(req.ProsumerNic);
        var result = await _service.CreateAsync(nic, req);
        return Ok(result);
    }

    // Updates a reservation (12-hour rule, 7-day window re-check).
    [HttpPut("{id}")]
    [Authorize(Roles = "Prosumer,GridOperator,Backoffice")]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateReservationRequest req)
    {
        var role = User.FindFirst("role")?.Value;
        var callerNic = User.FindFirst("nic")?.Value;

        if (role == "Prosumer")
        {
            // Prosumer can only update their own reservation.
            if (string.IsNullOrEmpty(callerNic))
                throw new ServiceException(401, "MISSING_NIC", "JWT missing nic claim.");
            await _service.UpdateAsync(id, callerNic, req);
        }
        else
        {
            // Operator/Backoffice updates without NIC ownership check.
            await _service.UpdateAsAdminAsync(id, req);
        }

        return NoContent();
    }

    // Cancels a reservation (12-hour rule, slot returned to Available).
    [HttpPut("{id}/cancel")]
    [Authorize(Roles = "Prosumer,GridOperator,Backoffice")]
    public async Task<IActionResult> Cancel(string id, [FromBody] CancelReservationRequest req)
    {
        var role = User.FindFirst("role")?.Value;
        var callerNic = User.FindFirst("nic")?.Value;

        if (role == "Prosumer")
        {
            if (string.IsNullOrEmpty(callerNic))
                throw new ServiceException(401, "MISSING_NIC", "JWT missing nic claim.");
            await _service.CancelAsync(id, callerNic, req.Reason);
        }
        else
        {
            // Operator/Backoffice cancels on behalf - no NIC ownership check.
            await _service.CancelAsAdminAsync(id, req.Reason);
        }

        return NoContent();
    }

    // Fetches one reservation - owning Prosumer or GridOperator/Backoffice.
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

    // ---------- Helpers ----------

    // Resolves the NIC of the Prosumer the reservation is for.
    // Prosumer: NIC must come from JWT.
    // Operator/Backoffice: NIC must come from the request body.
    private string ResolveProsumerNic(string? bodyNic)
    {
        var role = User.FindFirst("role")?.Value;

        if (role == "Prosumer")
        {
            return User.FindFirst("nic")?.Value
                ?? throw new ServiceException(401, "MISSING_NIC", "JWT missing nic claim.");
        }

        // Operator / Backoffice path
        if (string.IsNullOrWhiteSpace(bodyNic))
            throw new ServiceException(400, "MISSING_NIC", "Prosumer NIC is required.");

        return bodyNic.Trim();
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