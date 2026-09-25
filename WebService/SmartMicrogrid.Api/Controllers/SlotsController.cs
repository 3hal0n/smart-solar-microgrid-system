// ============================================================
// File: SlotsController.cs
// Purpose: HTTP endpoints for battery slot administration under a
//          station — create and partial update. Thin controller:
//          business rules live in SlotService per the FAT service
//          pattern; this file only maps requests/exceptions to HTTP.
// Author: Shalon
// ============================================================
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Services;

namespace SmartMicrogrid.Api.Controllers;

[ApiController]
[Route("api")]
public class SlotsController : ControllerBase
{
    private readonly SlotService _slotService;

    public SlotsController(SlotService slotService)
    {
        _slotService = slotService;
    }

    // POST /api/stations/{stationId}/slots - creates a new slot under an existing station.
    // Backoffice only, per architecture.md §3's endpoint table.
    [Authorize(Roles = "Backoffice")]
    [HttpPost("stations/{stationId}/slots")]
    public async Task<IActionResult> Create(string stationId, [FromBody] CreateSlotRequest request)
    {
        try
        {
            var id = await _slotService.CreateAsync(stationId, request);
            return Created($"/api/slots/{id}", new CreatedIdResponse { Id = id });
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

    // PUT /api/slots/{id} - partial update of slot fields. Backoffice only, per architecture.md
    // §3's endpoint table.
    [Authorize(Roles = "Backoffice")]
    [HttpPut("slots/{id}")]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateSlotRequest request)
    {
        try
        {
            await _slotService.UpdateAsync(id, request);
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
}
