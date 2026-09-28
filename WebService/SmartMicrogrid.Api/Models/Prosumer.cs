// ============================================================
// File: Prosumer.cs
// Purpose: Defines the MongoDB document structure for Prosumers.
//          NIC is used as the primary business key for lookups.
// Author: Rukshan
// ============================================================

using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartMicrogrid.Api.Models
{
    public class Prosumer
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string Id { get; set; } = null!;

        [BsonElement("role")]
        public string Role { get; set; } = "Prosumer";

        [BsonElement("nic")]
        public string Nic { get; set; } = null!; // Primary Business Key

        [BsonElement("fullName")]
        public string FullName { get; set; } = null!;

        [BsonElement("email")]
        public string Email { get; set; } = null!;

        [BsonElement("phone")]
        public string? Phone { get; set; }

        [BsonElement("address")]
        public string? Address { get; set; }

        [BsonElement("profilePicture")]
        public string? ProfilePicture { get; set; }

        [BsonElement("passwordHash")]
        public string PasswordHash { get; set; } = null!;

        [BsonElement("status")]
        public string Status { get; set; } = "PendingActivation"; // Active, PendingActivation, Deactivated

        [BsonElement("deactivationRequestedAt")]
        public DateTime? DeactivationRequestedAt { get; set; }

        [BsonElement("createdAt")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        [BsonElement("updatedAt")]
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    }
}