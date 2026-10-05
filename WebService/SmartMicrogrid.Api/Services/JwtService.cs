// ============================================================
// File: JwtService.cs
// Purpose: Handles JWT token generation for authenticated users.
//          Adds 'nic' claim specifically for Prosumers.
// Author: Rukshan
// ============================================================

using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.IdentityModel.Tokens;

namespace SmartMicrogrid.Api.Services
{
    public class JwtService
    {
        private readonly IConfiguration _configuration;

        public JwtService(IConfiguration configuration)
        {
            _configuration = configuration;
        }

        // Generates a JWT token with Role and NIC claims
        public string GenerateToken(string userId, string role, string fullName, string? nic = null)
        {
            var jwtKey = _configuration["Jwt:Key"] ?? "YourSuperSecretKeyHereAtLeast32Chars!";
            var jwtIssuer = _configuration["Jwt:Issuer"] ?? "SmartMicrogridApi";
            var jwtAudience = _configuration["Jwt:Audience"] ?? "SmartMicrogridClients";

            var claims = new List<Claim>
            {
                new Claim(JwtRegisteredClaimNames.Sub, userId),
                new Claim("role", role),
                new Claim(ClaimTypes.Role, role),
                new Claim("fullName", fullName),
                new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString())
            };

            // Add NIC claim only for Prosumers (used for authorization checks)
            if (!string.IsNullOrEmpty(nic))
            {
                claims.Add(new Claim("nic", nic));
            }

            var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey));
            var creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

            var token = new JwtSecurityToken(
                issuer: jwtIssuer,
                audience: jwtAudience,
                claims: claims,
                expires: DateTime.UtcNow.AddDays(1), // Token valid for 1 day
                signingCredentials: creds
            );

            return new JwtSecurityTokenHandler().WriteToken(token);
        }
    }
}