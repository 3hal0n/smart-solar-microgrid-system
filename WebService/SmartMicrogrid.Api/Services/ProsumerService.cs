// ============================================================
// File: ProsumerService.cs
// Purpose: Handles all business logic for Prosumer management.
//          Enforces rules like reactivation only by Backoffice.
// Author: Rukshan
// ============================================================

using MongoDB.Driver;
using SmartMicrogrid.Api.Data;
using SmartMicrogrid.Api.Models;

namespace SmartMicrogrid.Api.Services
{
    public class ProsumerService
    {
        private readonly IMongoCollection<Prosumer> _prosumers;

        public ProsumerService(MongoDbContext context)
        {
            // Explicitly targeting the unified "Users" collection as per architecture.md
            _prosumers = context.Database.GetCollection<Prosumer>("Users");
        }

        // Get all prosumers (for Backoffice dashboard)
        public async Task<List<ProsumerResponseDto>> GetAllProsumersAsync()
        {
            var filter = Builders<Prosumer>.Filter.Eq(p => p.Role, "Prosumer");
            var prosumers = await _prosumers.Find(filter).ToListAsync();
            
            return prosumers.Select(p => new ProsumerResponseDto
            {
                Id = p.Id,
                Nic = p.Nic,
                FullName = p.FullName,
                Email = p.Email,
                Phone = p.Phone,
                Address = p.Address,
                ProfilePicture = p.ProfilePicture,
                Status = p.Status,
                DeactivationRequestedAt = p.DeactivationRequestedAt,
                CreatedAt = p.CreatedAt,
                UpdatedAt = p.UpdatedAt
            }).ToList();
        }
        // Returns all prosumers, optionally filtered by status.
        // Uses a projection so MongoDB only returns the fields the DTO needs -
        // this prevents deserialization 500s on documents missing optional
        // fields (e.g. legacy test data with no CreatedAt, no Phone, etc.).
        public async Task<List<ProsumerResponseDto>> ListAsync(string? status)
        {
            var builder = Builders<Prosumer>.Filter;
            var filter = builder.Eq(p => p.Role, "Prosumer");

            if (!string.IsNullOrWhiteSpace(status))
            {
                filter &= builder.Eq(p => p.Status, status);
            }

            return await _prosumers
                .Find(filter)
                .SortByDescending(p => p.CreatedAt)
                .Project<ProsumerResponseDto>(Builders<Prosumer>.Projection
                    .Expression(p => new ProsumerResponseDto
                    {
                        Id = p.Id,
                        Nic = p.Nic,
                        FullName = p.FullName,
                        Email = p.Email,
                        Phone = p.Phone,
                        Address = p.Address,
                        Status = p.Status,
                        DeactivationRequestedAt = p.DeactivationRequestedAt,
                        CreatedAt = p.CreatedAt,
                        UpdatedAt = p.UpdatedAt
                    }))
                .ToListAsync();
        }


        // Get a single prosumer by NIC
        public async Task<ProsumerResponseDto?> GetByNicAsync(string nic)
        {
            var filter = Builders<Prosumer>.Filter.And(
                Builders<Prosumer>.Filter.Eq(p => p.Role, "Prosumer"),
                Builders<Prosumer>.Filter.Eq(p => p.Nic, nic)
            );
            var prosumer = await _prosumers.Find(filter).FirstOrDefaultAsync();

            if (prosumer == null) return null;

            return new ProsumerResponseDto
            {
                Id = prosumer.Id,
                Nic = prosumer.Nic,
                FullName = prosumer.FullName,
                Email = prosumer.Email,
                Phone = prosumer.Phone,
                Address = prosumer.Address,
                ProfilePicture = prosumer.ProfilePicture,
                Status = prosumer.Status,
                DeactivationRequestedAt = prosumer.DeactivationRequestedAt,
                CreatedAt = prosumer.CreatedAt,
                UpdatedAt = prosumer.UpdatedAt
            };
        }

        // Create prosumer manually (Backoffice requirement)
        public async Task<bool> CreateManuallyAsync(ProsumerRegistrationDto dto)
        {
            var exists = await _prosumers.Find(p => p.Nic == dto.Nic).AnyAsync();
            if (exists) return false;

            var newProsumer = new Prosumer
            {
                Nic = dto.Nic,
                FullName = dto.FullName,
                Email = dto.Email,
                Phone = dto.Phone,
                Address = dto.Address,
                PasswordHash = BCrypt.Net.BCrypt.HashPassword(dto.Password),
                Status = "Active", // Backoffice creation defaults to Active
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            await _prosumers.InsertOneAsync(newProsumer);
            return true;
        }

        // Register a new prosumer (Called by Mobile App, defaults to PendingActivation)
        public async Task<bool> RegisterAsync(ProsumerRegistrationDto dto)
        {
            var exists = await _prosumers.Find(p => p.Nic == dto.Nic).AnyAsync();
            if (exists) return false;

            var newProsumer = new Prosumer
            {
                Nic = dto.Nic,
                FullName = dto.FullName,
                Email = dto.Email,
                Phone = dto.Phone,
                Address = dto.Address,
                PasswordHash = BCrypt.Net.BCrypt.HashPassword(dto.Password),
                Status = "PendingActivation", 
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            await _prosumers.InsertOneAsync(newProsumer);
            return true;
        }

        // Update prosumer details
        public async Task<bool> UpdateAsync(string nic, UpdateProsumerDto dto)
        {
            var filter = Builders<Prosumer>.Filter.Eq(p => p.Nic, nic);
            var update = Builders<Prosumer>.Update
                .Set(p => p.FullName, dto.FullName)
                .Set(p => p.Email, dto.Email)
                .Set(p => p.Phone, dto.Phone)
                .Set(p => p.Address, dto.Address)
                .Set(p => p.ProfilePicture, dto.ProfilePicture)
                .Set(p => p.UpdatedAt, DateTime.UtcNow);

            var result = await _prosumers.UpdateOneAsync(filter, update);
            return result.ModifiedCount > 0;
        }

        // Deactivate prosumer (Can be forced by Backoffice)
        public async Task<bool> DeactivateAsync(string nic)
        {
            var filter = Builders<Prosumer>.Filter.Eq(p => p.Nic, nic);
            var update = Builders<Prosumer>.Update
                .Set(p => p.Status, "Deactivated")
                .Set(p => p.UpdatedAt, DateTime.UtcNow);

            var result = await _prosumers.UpdateOneAsync(filter, update);
            return result.ModifiedCount > 0;
        }

        // Activate prosumer (PendingActivation -> Active)
        public async Task<bool> ActivateAsync(string nic)
        {
            var filter = Builders<Prosumer>.Filter.Eq(p => p.Nic, nic);
            var update = Builders<Prosumer>.Update
                .Set(p => p.Status, "Active")
                .Set(p => p.UpdatedAt, DateTime.UtcNow);

            var result = await _prosumers.UpdateOneAsync(filter, update);
            return result.ModifiedCount > 0;
        }

        // Reactivate prosumer (Deactivated -> Active) - STRICTLY Backoffice only
        public async Task<bool> ReactivateAsync(string nic)
        {
            var filter = Builders<Prosumer>.Filter.Eq(p => p.Nic, nic);
            var update = Builders<Prosumer>.Update
                .Set(p => p.Status, "Active")
                .Set(p => p.DeactivationRequestedAt, null)
                .Set(p => p.UpdatedAt, DateTime.UtcNow);

            var result = await _prosumers.UpdateOneAsync(filter, update);
            return result.ModifiedCount > 0;
        }

        // Prosumer requests deactivation (Mobile app self-service)
        public async Task<bool> RequestDeactivationAsync(string nic)
        {
            var filter = Builders<Prosumer>.Filter.Eq(p => p.Nic, nic);
            var update = Builders<Prosumer>.Update
                .Set(p => p.Status, "Deactivated")
                .Set(p => p.DeactivationRequestedAt, DateTime.UtcNow)
                .Set(p => p.UpdatedAt, DateTime.UtcNow);

            var result = await _prosumers.UpdateOneAsync(filter, update);
            return result.ModifiedCount > 0;
        }

        // Authenticate prosumer and validate business rules (Status checks)
        public async Task<(bool Success, string Message, Prosumer? Prosumer)> AuthenticateAsync(string nic, string password)
        {
            // 1. Find prosumer by NIC and Role
            var filter = Builders<Prosumer>.Filter.And(
                Builders<Prosumer>.Filter.Eq(p => p.Nic, nic),
                Builders<Prosumer>.Filter.Eq(p => p.Role, "Prosumer")
            );
            var prosumer = await _prosumers.Find(filter).FirstOrDefaultAsync();

            // 2. Check if exists
            if (prosumer == null)
                return (false, "Invalid NIC or password.", null);

            // 3. Business Rule: Reject Pending Activation
            if (prosumer.Status == "PendingActivation")
                return (false, "Account is pending activation by Backoffice.", null);

            // 4. Business Rule: Reject Deactivated accounts
            if (prosumer.Status == "Deactivated")
                return (false, "Account has been deactivated. Contact Backoffice.", null);

            // 5. Verify Password
            if (!BCrypt.Net.BCrypt.Verify(password, prosumer.PasswordHash))
                return (false, "Invalid NIC or password.", null);

            // 6. Success
            return (true, "Success", prosumer);
        }
    }
}