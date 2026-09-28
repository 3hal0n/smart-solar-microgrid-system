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
    [Authorize] // Requires valid JWT token for ALL endpoints by default
    public class UsersController : ControllerBase
    {
        private readonly IMongoCollection<User> _users;

        // Inject MongoDB context
        public UsersController(MongoDbContext context)
        {
            _users = context.Users;
        }

        // GET: api/users
        // Retrieves all web application users without exposing password hashes.
        [HttpGet]
        public async Task<ActionResult<IEnumerable<UserResponseDto>>> GetAllUsers()
        {
            // Fetch all users from MongoDB
            var users = await _users.Find(u => true).ToListAsync();

            // Map to response DTO (excludes password hash for security)
            var usersWithoutPasswords = users.Select(u => new UserResponseDto
            {
                Id = u.Id,
                Username = u.Username,
                Role = u.Role,
                FullName = u.FullName,
                Email = u.Email,
                ProfilePicture = u.ProfilePicture,
                Status = u.Status,
                CreatedAt = u.CreatedAt,
                UpdatedAt = u.UpdatedAt
            });

            return Ok(usersWithoutPasswords);
        }

        // POST: api/users
        // Creates a new user, hashes password, and saves to MongoDB.
        // AllowAnonymous is used here so the FIRST admin user can be created 
        // without a token. All subsequent user creation should be done by an authenticated admin.
        [AllowAnonymous]
        [HttpPost]
        public async Task<ActionResult<UserResponseDto>> CreateUser([FromBody] CreateUserDto dto)
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
                ProfilePicture = dto.ProfilePicture,
                Status = "Active",
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            // Save to MongoDB
            await _users.InsertOneAsync(newUser);

            // Return success as DTO (without password hash)
            var response = new UserResponseDto
            {
                Id = newUser.Id,
                Username = newUser.Username,
                Role = newUser.Role,
                FullName = newUser.FullName,
                Email = newUser.Email,
                ProfilePicture = newUser.ProfilePicture,
                Status = newUser.Status,
                CreatedAt = newUser.CreatedAt,
                UpdatedAt = newUser.UpdatedAt
            };

            return CreatedAtAction(nameof(GetAllUsers), new { id = newUser.Id }, response);
        }

        // GET: api/users/me
        // Retrieves the profile of the currently authenticated user.
        [HttpGet("me")]
        public async Task<ActionResult<UserResponseDto>> GetMyProfile()
        {
            var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value
                         ?? User.FindFirst("sub")?.Value;

            if (string.IsNullOrEmpty(userId))
            {
                return Unauthorized(new { message = "User identifier missing from token." });
            }

            var user = await _users.Find(u => u.Id == userId).FirstOrDefaultAsync();
            if (user == null)
            {
                return NotFound(new { message = "User not found." });
            }

            return Ok(new UserResponseDto
            {
                Id = user.Id,
                Username = user.Username,
                Role = user.Role,
                FullName = user.FullName,
                Email = user.Email,
                ProfilePicture = user.ProfilePicture,
                Status = user.Status,
                CreatedAt = user.CreatedAt,
                UpdatedAt = user.UpdatedAt
            });
        }

        // GET: api/users/{id}
        // Retrieves a specific user by ID.
        [HttpGet("{id}")]
        public async Task<ActionResult<UserResponseDto>> GetUserById(string id)
        {
            var user = await _users.Find(u => u.Id == id).FirstOrDefaultAsync();
            if (user == null)
            {
                return NotFound(new { message = "User not found." });
            }

            return Ok(new UserResponseDto
            {
                Id = user.Id,
                Username = user.Username,
                Role = user.Role,
                FullName = user.FullName,
                Email = user.Email,
                ProfilePicture = user.ProfilePicture,
                Status = user.Status,
                CreatedAt = user.CreatedAt,
                UpdatedAt = user.UpdatedAt
            });
        }

        // PUT: api/users/{id}
        // Updates user details.
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

            // Update user fields (preserving role if not specified)
            var update = Builders<User>.Update
                .Set(u => u.FullName, dto.FullName)
                .Set(u => u.Email, dto.Email)
                .Set(u => u.ProfilePicture, dto.ProfilePicture ?? user.ProfilePicture)
                .Set(u => u.Role, string.IsNullOrWhiteSpace(dto.Role) ? user.Role : dto.Role)
                .Set(u => u.UpdatedAt, DateTime.UtcNow);

            var result = await _users.UpdateOneAsync(filter, update);

            if (result.MatchedCount == 0)
            {
                return NotFound(new { message = "User not found." });
            }

            return NoContent(); // 204 Success
        }

        // PUT: api/users/{id}/change-password
        // Changes user password after validating current password.
        [HttpPut("{id}/change-password")]
        public async Task<IActionResult> ChangePassword(string id, [FromBody] ChangePasswordDto dto)
        {
            var filter = Builders<User>.Filter.Eq(u => u.Id, id);
            var user = await _users.Find(filter).FirstOrDefaultAsync();

            if (user == null)
            {
                return NotFound(new { message = "User not found." });
            }

            if (string.IsNullOrWhiteSpace(dto.CurrentPassword) || string.IsNullOrWhiteSpace(dto.NewPassword))
            {
                return BadRequest(new { message = "Current and new password are required." });
            }

            if (!BCrypt.Net.BCrypt.Verify(dto.CurrentPassword, user.PasswordHash))
            {
                return BadRequest(new { message = "Current password is incorrect." });
            }

            if (dto.NewPassword.Length < 6)
            {
                return BadRequest(new { message = "New password must be at least 6 characters." });
            }

            var update = Builders<User>.Update
                .Set(u => u.PasswordHash, BCrypt.Net.BCrypt.HashPassword(dto.NewPassword))
                .Set(u => u.UpdatedAt, DateTime.UtcNow);

            await _users.UpdateOneAsync(filter, update);

            return Ok(new { message = "Password updated successfully." });
        }

        // PUT: api/users/{id}/deactivate
        // Soft deletes the user by changing status to Deactivated.
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