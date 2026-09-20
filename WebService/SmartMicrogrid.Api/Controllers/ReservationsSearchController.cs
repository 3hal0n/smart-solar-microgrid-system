// ============================================================
// File: ReservationsSearchController.cs
// Purpose: Read-only reservations search/filter endpoint, per
//          architecture.md §3 (moved from Migara to Shalon
//          2026-09-21, see §3/§4/§7). Named distinctly from Dinil's
//          ReservationsController.cs (which owns the write side —
//          create/update/cancel + GET /reservations/{id}) so the two
//          files never collide. Thin controller: filtering logic
//          lives in DashboardService per the FAT service pattern.
// Author: Shalon
// ============================================================
using Microsoft.AspNetCore.Mvc;
using SmartMicrogrid.Api.Services;

namespace SmartMicrogrid.Api.Controllers;

[ApiController]
[Route("api/reservations")]
public class ReservationsSearchController : ControllerBase
{
    private readonly DashboardService _dashboardService;

    // Injects the service that owns all reservation search/filter logic.
    public ReservationsSearchController(DashboardService dashboardService)
    {
        _dashboardService = dashboardService;
    }

    // GET /api/reservations?nic=&stationId=&status=&from=&to= - search/filter, read-only.
    // TODO(Rukshan): once JwtService/auth scheme is wired up, replace the two hardcoded
    // arguments below with the real values read from the caller's JWT claims
    // (User.FindFirst("role")/("nic")) instead of trusting the client. Until then this endpoint
    // cannot actually enforce "Prosumer restricted to own nic" — it is effectively unrestricted,
    // same as every other endpoint in this repo still waiting on Rukshan's auth wiring.
    // DashboardService.SearchAsync's callerRole/callerNic parameters exist specifically so that
    // swap is the only thing that needs to change here.
    [HttpGet]
    public async Task<IActionResult> Search(
        [FromQuery] string? nic,
        [FromQuery] string? stationId,
        [FromQuery] string? status,
        [FromQuery] DateTime? from,
        [FromQuery] DateTime? to)
    {
        try
        {
            var reservations = await _dashboardService.SearchAsync(
                nic, stationId, status, from, to, callerRole: "GridOperator", callerNic: null);
            return Ok(reservations);
        }
        catch (ValidationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }
}
