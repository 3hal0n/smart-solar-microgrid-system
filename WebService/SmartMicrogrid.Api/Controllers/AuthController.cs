// ============================================================
// File: AuthController.cs
// Purpose: Handles user authentication, password verification, and JWT token generation.
// Author: Migara
// ============================================================

using Microsoft.AspNetCore.Mvc;
using Microsoft.IdentityModel.Tokens;
using MongoDB.Driver;
using SmartMicrogrid.Api.Data;
using SmartMicrogrid.Api.Models;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;

namespace SmartMicrogrid.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly IMongoCollection<User> _users;
        private readonly IConfiguration _config;

        // Inject MongoDB context and configuration
        public AuthController(MongoDbContext context, IConfiguration config)
        {
            _users = context.Users;
            _config = config;
        }

        // POST: api/auth/login
        // Authenticates user and returns JWT token
        [HttpPost("login")]
        public async Task<ActionResult<LoginResponseDto>> Login([FromBody] LoginDto loginDto)
        {
            // Find user by username
            var user = await _users.Find(u => u.Username == loginDto.Username).FirstOrDefaultAsync();

            // Validate user exists, is active, and password matches
            if (user == null || user.Status != "Active" || !BCrypt.Net.BCrypt.Verify(loginDto.Password, user.PasswordHash))
            {
                return Unauthorized(new { message = "Invalid username, password, or account is deactivated." });
            }

            // Generate JWT token
            var token = GenerateJwtToken(user);

            // Return token and user info
            return Ok(new LoginResponseDto
            {
                Token = token,
                Role = user.Role,
                FullName = user.FullName,
                UserId = user.Id
            });
        }

        // Generates a JWT token for the authenticated user
        private string GenerateJwtToken(User user)
        {
            var securityKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_config["Jwt:Key"]));
            var credentials = new SigningCredentials(securityKey, SecurityAlgorithms.HmacSha256);

            var claims = new[]
            {
                new Claim(JwtRegisteredClaimNames.Sub, user.Id),
                new Claim("role", user.Role),
                new Claim("fullName", user.FullName),
                new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString())
            };

            var token = new JwtSecurityToken(
                issuer: _config["Jwt:Issuer"],
                audience: _config["Jwt:Audience"],
                claims: claims,
                expires: DateTime.Now.AddHours(2),
                signingCredentials: credentials
            );

            return new JwtSecurityTokenHandler().WriteToken(token);
        }
    }
}