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
    // Deliberately left open (2026-09-26): Migara's JWT auth now exists for web Backoffice/
    // GridOperator accounts, but there is still no Prosumer login/JWT anywhere (mobile or web) —
    // the mobile Dashboard screen that calls this reads a fixture NIC with no session at all.
    // Restoring [Authorize] here would 401 every mobile call with nothing to fix it yet. Revisit
    // once a Prosumer auth flow exists; this isn't a forgotten TODO, it's blocked on that.
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

    // GET /api/dashboard/operator/summary - today's confirmed/completed counts
    // plus pending-by-station breakdown. GridOperator only.
    [HttpGet("operator/summary")]
    [Microsoft.AspNetCore.Authorization.Authorize(
        Roles = "GridOperator,Backoffice")]
    public async Task<IActionResult> GetOperatorSummary()
    {
        try
        {
            var summary = await _dashboardService.GetOperatorSummaryAsync();
            return Ok(summary);
        }
        catch (ValidationException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }
}
