// ============================================================
// File: ProsumerAuthController.cs
// Purpose: Handles Prosumer authentication (Login) for the Mobile App.
//          Calls ProsumerService to enforce business rules (Status checks).
// Author: Rukshan
// ============================================================

using Microsoft.AspNetCore.Mvc;
using SmartMicrogrid.Api.Models;
using SmartMicrogrid.Api.Services;

namespace SmartMicrogrid.Api.Controllers
{
    [ApiController]
    [Route("api/auth/prosumer")]
    public class ProsumerAuthController : ControllerBase
    {
        private readonly ProsumerService _prosumerService;
        private readonly JwtService _jwtService;

        public ProsumerAuthController(ProsumerService prosumerService, JwtService jwtService)
        {
            _prosumerService = prosumerService;
            _jwtService = jwtService;
        }

        // POST: api/auth/prosumer/login
        [HttpPost("login")]
        public async Task<IActionResult> Login([FromBody] LoginProsumerDto dto)
        {
            // 1. Call Service to handle business logic (Status checks, Password verify)
            var result = await _prosumerService.AuthenticateAsync(dto.Nic, dto.Password);

            // 2. Handle Failure Cases
            if (!result.Success)
            {
                // Return 403 Forbidden for Pending/Deactivated, 401 Unauthorized for wrong credentials
                if (result.Message.Contains("pending") || result.Message.Contains("deactivated"))
                {
                    return StatusCode(403, new { message = result.Message });
                }
                return Unauthorized(new { message = result.Message });
            }

            // 3. Generate JWT Token on Success
            var prosumer = result.Prosumer!;
            var token = _jwtService.GenerateToken(prosumer.Id, prosumer.Role, prosumer.FullName, prosumer.Nic);

            // 4. Return Success Response
            return Ok(new
            {
                token = token,
                role = prosumer.Role,
                fullName = prosumer.FullName,
                nic = prosumer.Nic,
                status = prosumer.Status
            });
        }
    }

    // DTO for Prosumer Login Request
    public class LoginProsumerDto
    {
        public string Nic { get; set; } = null!;
        public string Password { get; set; } = null!;
    }
}