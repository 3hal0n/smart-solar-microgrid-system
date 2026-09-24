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
        public string Id { get; set; }

        [BsonElement("role")]
        public string Role { get; set; } // "Backoffice" or "GridOperator"

        [BsonElement("username")]
        public string Username { get; set; }

        [BsonElement("passwordHash")]
        public string PasswordHash { get; set; }

        [BsonElement("fullName")]
        public string FullName { get; set; }

        [BsonElement("email")]
        public string Email { get; set; }

        [BsonElement("status")]
        public string Status { get; set; } // "Active" or "Deactivated"

        [BsonElement("createdAt")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        [BsonElement("updatedAt")]
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    }

    // DTO for creating a user (hides password hash from requests)
    public class CreateUserDto
    {
        public string Username { get; set; }
        public string Password { get; set; }
        public string Role { get; set; }
        public string FullName { get; set; }
        public string Email { get; set; }
    }

    // DTO for updating a user
    public class UpdateUserDto
    {
        public string FullName { get; set; }
        public string Email { get; set; }
        public string Role { get; set; }
    }

    // DTO for login request
    public class LoginDto
    {
        public string Username { get; set; }
        public string Password { get; set; }
    }

    // DTO for login response
    public class LoginResponseDto
    {
        public string Token { get; set; }
        public string Role { get; set; }
        public string FullName { get; set; }
        public string UserId { get; set; }
    }
}