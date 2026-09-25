// ============================================================
// File: Program.cs
// Purpose: Application entry point. Configures services, middleware, 
//          JWT authentication, and CORS for the Smart Microgrid API.
//  
// ============================================================

using System.Text;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using SmartMicrogrid.Api.Data;
using SmartMicrogrid.Api.Services;

// Loads .env into process environment variables (e.g. ConnectionStrings__MongoDb, Jwt__Key) before the
// config builder reads them, so a real Atlas connection string or JWT secret never has to live in appsettings.json
// (which is committed to git). Optional — teammates without a .env fall back to appsettings.json's defaults.
if (File.Exists(".env"))
{
    DotNetEnv.Env.Load();
}

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
// Learn more about configuring OpenAPI at https://aka.ms/aspnet/openapi
builder.Services.AddOpenApi();

// Controllers, with enums serialized as strings so the JSON contract matches architecture.md
// (e.g. Station/Slot "status" as "Active"/"Available" rather than raw integers).
builder.Services.AddControllers()
    .AddJsonOptions(options => options.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter()));

// Shared MongoDB connection and Shalon's/Dinil's services.
builder.Services.AddSingleton<MongoDbContext>();
builder.Services.AddScoped<StationService>();
builder.Services.AddScoped<SlotService>();
builder.Services.AddSingleton<QrTokenService>();
builder.Services.AddScoped<ReservationService>();

// ============================================================
// ADDED: JWT Authentication Configuration (For User Management)
// ============================================================
var jwtKey = builder.Configuration["Jwt:Key"] ?? "YourSuperSecretKeyHereAtLeast32Chars!";
var jwtIssuer = builder.Configuration["Jwt:Issuer"] ?? "SmartMicrogridApi";
var jwtAudience = builder.Configuration["Jwt:Audience"] ?? "SmartMicrogridClients";

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidateAudience = true,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            ValidIssuer = jwtIssuer,
            ValidAudience = jwtAudience,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey))
        };
    });

// Enable Authorization policies (required for [Authorize] attributes)
builder.Services.AddAuthorization();

// CORS for the browser-based WebApp — without this, every fetch/axios call from the React dev
// server is blocked by the browser. Allowed origins come from config (Cors:AllowedOrigins).
var corsOrigins = builder.Configuration.GetSection("Cors:AllowedOrigins").Get<string[]>() ?? Array.Empty<string>();
builder.Services.AddCors(options =>
{
    options.AddPolicy("WebApp", policy =>
    {
        policy.WithOrigins(corsOrigins).AllowAnyHeader().AllowAnyMethod();
    });
});

var app = builder.Build();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}

app.UseHttpsRedirection();

// ⚠️ ORDER MATTERS: CORS must come before Auth, and Auth must come before Controllers
app.UseCors("WebApp");

// ADDED: Authentication and Authorization Middleware
app.UseAuthentication(); 
app.UseAuthorization();  

app.MapControllers();

// Ensures the 2dsphere index on SolarStations.location exists before the API serves traffic.
using (var startupScope = app.Services.CreateScope())
{
    var stationService = startupScope.ServiceProvider.GetRequiredService<StationService>();
    await stationService.EnsureIndexesAsync();
}

// (Kept exactly as your team had it to avoid breaking anything)
var summaries = new[]
{
    "Freezing", "Bracing", "Chilly", "Cool", "Mild", "Warm", "Balmy", "Hot", "Sweltering", "Scorching"
};

app.MapGet("/weatherforecast", () =>
{
    var forecast =  Enumerable.Range(1, 5).Select(index =>
        new WeatherForecast
        (
            DateOnly.FromDateTime(DateTime.Now.AddDays(index)),
            Random.Shared.Next(-20, 55),
            summaries[Random.Shared.Next(summaries.Length)]
        ))
        .ToArray();
    return forecast;
})
.WithName("GetWeatherForecast");

app.Run();

record WeatherForecast(DateOnly Date, int TemperatureC, string? Summary)
{
    public int TemperatureF => 32 + (int)(TemperatureC / 0.5556);
}