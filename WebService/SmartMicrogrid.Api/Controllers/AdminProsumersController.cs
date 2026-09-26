// ============================================================
// File: AdminProsumersController.cs
// Purpose: Handles Backoffice administration of Prosumer profiles.
//          Enforces that ONLY Backoffice can create, deactivate, 
//          activate, and reactivate accounts.
// Author: Rukshan
// ============================================================

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Services;

namespace SmartMicrogrid.Api.Controllers
{
    [ApiController]
    [Route("api/admin/prosumers")]
    [Authorize(Roles = "Backoffice")] // STRICT: Only Backoffice can access these endpoints
    public class AdminProsumersController : ControllerBase
    {
        private readonly ProsumerService _prosumerService;

        public AdminProsumersController(ProsumerService prosumerService)
        {
            _prosumerService = prosumerService;
        }

        // GET: api/admin/prosumers/pending
        // Returns only prosumers awaiting Backoffice activation (for the web app pending view)
        [HttpGet("pending")]
        public async Task<ActionResult<List<ProsumerResponseDto>>> GetPendingProsumers()
        {
            var allProsumers = await _prosumerService.GetAllProsumersAsync();
            var pending = allProsumers.Where(p => p.Status == "PendingActivation").ToList();
            return Ok(pending);
        }
        
        // GET: api/admin/prosumers
        [HttpGet]
        public async Task<ActionResult<List<ProsumerResponseDto>>> GetAllProsumers()
        {
            var prosumers = await _prosumerService.GetAllProsumersAsync();
            return Ok(prosumers);
        }

        // GET: api/admin/prosumers/{nic}
        [HttpGet("{nic}")]
        public async Task<ActionResult<ProsumerResponseDto>> GetProsumerByNic(string nic)
        {
            var prosumer = await _prosumerService.GetByNicAsync(nic);
            if (prosumer == null) return NotFound(new { message = "Prosumer not found." });
            
            return Ok(prosumer);
        }

        // POST: api/admin/prosumers
        // Allows Backoffice to manually create a prosumer profile
        [HttpPost]
        public async Task<IActionResult> CreateProsumer([FromBody] ProsumerRegistrationDto dto)
        {
            var success = await _prosumerService.CreateManuallyAsync(dto);
            if (!success) return Conflict(new { message = "A prosumer with this NIC already exists." });
            
            return CreatedAtAction(nameof(GetProsumerByNic), new { nic = dto.Nic }, new { message = "Prosumer created successfully." });
        }

        // PUT: api/admin/prosumers/{nic}
        [HttpPut("{nic}")]
        public async Task<IActionResult> UpdateProsumer(string nic, [FromBody] UpdateProsumerDto dto)
        {
            var success = await _prosumerService.UpdateAsync(nic, dto);
            if (!success) return NotFound(new { message = "Prosumer not found or no changes made." });
            
            return NoContent(); // 204 Success
        }

        // PUT: api/admin/prosumers/{nic}/deactivate
        [HttpPut("{nic}/deactivate")]
        public async Task<IActionResult> DeactivateProsumer(string nic)
        {
            var success = await _prosumerService.DeactivateAsync(nic);
            if (!success) return NotFound(new { message = "Prosumer not found." });
            
            return NoContent(); // 204 Success
        }

        // PUT: api/admin/prosumers/{nic}/activate
        // Activates a prosumer who is currently in "PendingActivation" status
        [HttpPut("{nic}/activate")]
        public async Task<IActionResult> ActivateProsumer(string nic)
        {
            var prosumer = await _prosumerService.GetByNicAsync(nic);
            if (prosumer == null) return NotFound(new { message = "Prosumer not found." });

            if (prosumer.Status != "PendingActivation")
            {
                return BadRequest(new { message = "Only pending accounts can be activated." });
            }

            var success = await _prosumerService.ActivateAsync(nic);
            if (!success) return StatusCode(500, new { message = "Failed to activate prosumer." });
            
            return NoContent(); // 204 Success
        }

        // PUT: api/admin/prosumers/{nic}/reactivate
        // CRITICAL RULE: Deactivated accounts can ONLY be reactivated by a Backoffice officer.
        [HttpPut("{nic}/reactivate")]
        public async Task<IActionResult> ReactivateProsumer(string nic)
        {
            var prosumer = await _prosumerService.GetByNicAsync(nic);
            if (prosumer == null) return NotFound(new { message = "Prosumer not found." });

            if (prosumer.Status != "Deactivated")
            {
                return BadRequest(new { message = "Only deactivated accounts can be reactivated." });
            }

            var success = await _prosumerService.ReactivateAsync(nic);
            if (!success) return StatusCode(500, new { message = "Failed to reactivate prosumer." });
            
            return NoContent(); // 204 Success
        }
        
    }
}