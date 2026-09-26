// ============================================================
// File: ProsumerController.cs
// Purpose: Handles Prosumer self-service actions (Register, Edit, Request Deactivation).
//          Used by the Native Android Mobile Application.
// Author: Rukshan
// ============================================================

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Services;

namespace SmartMicrogrid.Api.Controllers
{
    [ApiController]
    [Route("api/prosumers")]
    public class ProsumerController : ControllerBase
    {
        private readonly ProsumerService _prosumerService;

        public ProsumerController(ProsumerService prosumerService)
        {
            _prosumerService = prosumerService;
        }

        // POST: api/prosumers/register
        // AllowAnonymous so the mobile app can register without a token
        [AllowAnonymous]
        [HttpPost("register")]
        public async Task<IActionResult> Register([FromBody] ProsumerRegistrationDto dto)
        {
            var success = await _prosumerService.RegisterAsync(dto);
            if (!success) return Conflict(new { message = "A prosumer with this NIC already exists." });
            
            return CreatedAtAction(nameof(GetProfile), new { nic = dto.Nic }, new { message = "Registration successful. Pending Backoffice activation." });
        }

        // GET: api/prosumers/{nic}
        [Authorize]
        [HttpGet("{nic}")]
        public async Task<ActionResult<ProsumerResponseDto>> GetProfile(string nic)
        {
            // Security: Ensure the user can only fetch their own profile (or let Backoffice fetch any)
            var userNic = User.FindFirstValue("nic");
            var role = User.FindFirstValue(ClaimTypes.Role);

            if (role != "Backoffice" && userNic != nic)
            {
                return Forbid(); // 403 Forbidden
            }

            var prosumer = await _prosumerService.GetByNicAsync(nic);
            if (prosumer == null) return NotFound(new { message = "Prosumer not found." });

            return Ok(prosumer);
        }

        // PUT: api/prosumers/{nic}
        [Authorize]
        [HttpPut("{nic}")]
        public async Task<IActionResult> UpdateProfile(string nic, [FromBody] UpdateProsumerDto dto)
        {
            var userNic = User.FindFirstValue("nic");
            if (userNic != nic) return Forbid();

            var success = await _prosumerService.UpdateAsync(nic, dto);
            if (!success) return NotFound(new { message = "Prosumer not found." });

            return NoContent();
        }

        // PUT: api/prosumers/{nic}/request-deactivation
        [Authorize]
        [HttpPut("{nic}/request-deactivation")]
        public async Task<IActionResult> RequestDeactivation(string nic)
        {
            var userNic = User.FindFirstValue("nic");
            if (userNic != nic) return Forbid();

            var success = await _prosumerService.RequestDeactivationAsync(nic);
            if (!success) return NotFound(new { message = "Prosumer not found." });

            return NoContent();
        }
    }
}