// ============================================================
// File: User.cs
// Purpose: Defines the MongoDB document structure for system users 
//          (Backoffice and Grid Operators).
// Author: Migara
// ============================================================

using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartMicrogrid.Api.Models
{
    // Represents a system user (Backoffice or Grid Operator)
    public class User
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string Id { get; set; } = null!;

        [BsonElement("role")]
        public string Role { get; set; } = null!;

        [BsonElement("username")]
        public string Username { get; set; } = null!;

        [BsonElement("passwordHash")]
        public string PasswordHash { get; set; } = null!;

        [BsonElement("fullName")]
        public string FullName { get; set; } = null!;

        [BsonElement("email")]
        public string Email { get; set; } = null!;

        [BsonElement("status")]
        public string Status { get; set; } = null!;

        [BsonElement("createdAt")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        [BsonElement("updatedAt")]
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    }

    // DTO for creating a user
    public class CreateUserDto
    {
        public string Username { get; set; } = null!;
        public string Password { get; set; } = null!;
        public string Role { get; set; } = null!;
        public string FullName { get; set; } = null!;
        public string Email { get; set; } = null!;
    }

    // DTO for updating a user
    public class UpdateUserDto
    {
        public string FullName { get; set; } = null!;
        public string Email { get; set; } = null!;
        public string Role { get; set; } = null!;
    }

    // DTO for login request
    public class LoginDto
    {
        public string Username { get; set; } = null!;
        public string Password { get; set; } = null!;
    }

    // DTO for login response
    public class LoginResponseDto
    {
        public string Token { get; set; } = null!;
        public string Role { get; set; } = null!;
        public string FullName { get; set; } = null!;
        public string UserId { get; set; } = null!;
    }

    
}