// ============================================================
// File: ProsumerDtos.cs
// Purpose: Data Transfer Objects for Prosumer management operations.
// Author: Rukshan
// ============================================================

namespace SmartMicrogrid.Api.Models
{
    // DTO for Mobile App Registration or Backoffice Manual Creation
    public class ProsumerRegistrationDto
    {
        public string Nic { get; set; } = null!;
        public string FullName { get; set; } = null!;
        public string Email { get; set; } = null!;
        public string? Phone { get; set; }
        public string? Address { get; set; }
        public string Password { get; set; } = null!;
    }

    // DTO for Backoffice/Admin or Self Updates
    public class UpdateProsumerDto
    {
        public string? FullName { get; set; }
        public string? Email { get; set; }
        public string? Phone { get; set; }
        public string? Address { get; set; }
        public string? ProfilePicture { get; set; }
    }

    // DTO for API Responses (Hides password hash for security)
    public class ProsumerResponseDto
    {
        public string Id { get; set; } = null!;
        public string Nic { get; set; } = null!;
        public string FullName { get; set; } = null!;
        public string Email { get; set; } = null!;
        public string? Phone { get; set; }
        public string? Address { get; set; }
        public string? ProfilePicture { get; set; }
        public string Status { get; set; } = null!;
        public DateTime? DeactivationRequestedAt { get; set; }
        public DateTime CreatedAt { get; set; }
        public DateTime UpdatedAt { get; set; }
    }
}