// ============================================================
// File: UsersController.cs
// Purpose: Handles CRUD operations for system users (Backoffice/GridOperator).
//          Enforces role-based access and interacts with MongoDB.
// Author: Migara
// ============================================================

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MongoDB.Driver;
using SmartMicrogrid.Api.Data;
using SmartMicrogrid.Api.Models;

namespace SmartMicrogrid.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize] // Requires valid JWT token for all endpoints
    public class UsersController : ControllerBase
    {
        private readonly IMongoCollection<User> _users;

        // Inject MongoDB context
        public UsersController(MongoDbContext context)
        {
            _users = context.Users;
        }

        // GET: api/users
        // Retrieves all web application users
        [HttpGet]
        public async Task<ActionResult<IEnumerable<User>>> GetAllUsers()
        {
            // Fetch all users from MongoDB
            var users = await _users.Find(u => true).ToListAsync();

            // Remove password hashes from response
            var usersWithoutPasswords = users.Select(u =>
            {
                u.PasswordHash = null;
                return u;
            });

            return Ok(usersWithoutPasswords);
        }

        // POST: api/users
        // Creates a new user, hashes password, and saves to MongoDB
        [HttpPost]
        public async Task<ActionResult<User>> CreateUser([FromBody] CreateUserDto dto)
        {
            // Validate if username already exists
            var existingUser = await _users.Find(u => u.Username == dto.Username).FirstOrDefaultAsync();
            if (existingUser != null)
            {
                return Conflict(new { message = "Username already exists." });
            }

            // Validate email uniqueness
            var existingEmail = await _users.Find(u => u.Email == dto.Email).FirstOrDefaultAsync();
            if (existingEmail != null)
            {
                return Conflict(new { message = "Email already exists." });
            }

            // Hash the password using BCrypt
            string passwordHash = BCrypt.Net.BCrypt.HashPassword(dto.Password);

            // Create the user object
            var newUser = new User
            {
                Username = dto.Username,
                PasswordHash = passwordHash,
                Role = dto.Role,
                FullName = dto.FullName,
                Email = dto.Email,
                Status = "Active",
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            // Save to MongoDB
            await _users.InsertOneAsync(newUser);

            // Return success (without the password hash)
            newUser.PasswordHash = string.Empty;
            return CreatedAtAction(nameof(GetAllUsers), new { id = newUser.Id }, newUser);
        }

        // PUT: api/users/{id}
        // Updates user details
        [HttpPut("{id}")]
        public async Task<IActionResult> UpdateUser(string id, [FromBody] UpdateUserDto dto)
        {
            // Find the user
            var filter = Builders<User>.Filter.Eq(u => u.Id, id);
            var user = await _users.Find(filter).FirstOrDefaultAsync();

            if (user == null)
            {
                return NotFound(new { message = "User not found." });
            }

            // Update user fields
            var update = Builders<User>.Update
                .Set(u => u.FullName, dto.FullName)
                .Set(u => u.Email, dto.Email)
                .Set(u => u.Role, dto.Role)
                .Set(u => u.UpdatedAt, DateTime.UtcNow);

            var result = await _users.UpdateOneAsync(filter, update);

            if (result.MatchedCount == 0)
            {
                return NotFound(new { message = "User not found." });
            }

            return NoContent(); // 204 Success
        }

        // PUT: api/users/{id}/deactivate
        // Soft deletes the user by changing status to Deactivated
        [HttpPut("{id}/deactivate")]
        public async Task<IActionResult> DeactivateUser(string id)
        {
            // Find the user
            var filter = Builders<User>.Filter.Eq(u => u.Id, id);
            var user = await _users.Find(filter).FirstOrDefaultAsync();

            if (user == null)
            {
                return NotFound(new { message = "User not found." });
            }

            // Update status to Deactivated
            var update = Builders<User>.Update
                .Set(u => u.Status, "Deactivated")
                .Set(u => u.UpdatedAt, DateTime.UtcNow);

            var result = await _users.UpdateOneAsync(filter, update);

            if (result.MatchedCount == 0)
            {
                return NotFound(new { message = "User not found." });
            }

            return NoContent(); // 204 Success
        }
    }
}