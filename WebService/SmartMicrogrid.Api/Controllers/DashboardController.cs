// ============================================================
// File: DashboardController.cs
// Purpose: HTTP endpoint for a prosumer's dashboard summary, per
//          architecture.md §3 (moved from Migara to Shalon
//          2026-09-21, see §3/§4/§7). Thin controller: aggregation
//          logic lives in DashboardService per the FAT service
//          pattern; this file only maps requests/exceptions to HTTP.
// Author: Shalon
// ============================================================
using Microsoft.AspNetCore.Mvc;
using SmartMicrogrid.Api.Services;

namespace SmartMicrogrid.Api.Controllers;

[ApiController]
[Route("api/dashboard")]
public class DashboardController : ControllerBase
{
    private readonly DashboardService _dashboardService;

    // Injects the service that owns all dashboard aggregation logic.
    public DashboardController(DashboardService dashboardService)
    {
        _dashboardService = dashboardService;
    }

    // GET /api/dashboard/prosumer/{nic}/summary - active/pending/approved-future counts + recent history.
    // TODO(Rukshan): restore [Authorize] (Self or Backoffice per architecture.md §3) once
    // JwtService/auth scheme is wired up.
    [HttpGet("prosumer/{nic}/summary")]
    public async Task<IActionResult> GetProsumerSummary(string nic)
    {
        try
        {
            var summary = await _dashboardService.GetProsumerSummaryAsync(nic);
            return Ok(summary);
        }
        catch (ValidationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }
}
