// ============================================================
// File: Program.cs
// Purpose: Application entry point. Configures services, middleware,
//          JWT authentication, and CORS for the Smart Microgrid API.
//          ExceptionHandlingMiddleware registered first so every
//          ServiceException maps to its declared HTTP status.
// Author: Shalon (updated by Dinil — exception middleware + claim fix)
// ============================================================

using System.Text;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using SmartMicrogrid.Api.Data;
using SmartMicrogrid.Api.Middleware;
using SmartMicrogrid.Api.Services;

// Loads .env into process environment variables before config reads them.
if (File.Exists(".env"))
{
    DotNetEnv.Env.Load();
}

// ⚠️ CRITICAL: ASP.NET's JWT handler by default renames the standard "sub" claim
// to "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier",
// so User.FindFirst("sub") returns null. Clearing the inbound map preserves
// the original claim names from the token. Must run BEFORE the app is built.
System.IdentityModel.Tokens.Jwt.JwtSecurityTokenHandler.DefaultInboundClaimTypeMap.Clear();

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddEndpointsApiExplorer();

builder.Services.AddSwaggerGen(options =>
{
    options.AddSecurityDefinition("Bearer", new Microsoft.OpenApi.Models.OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = Microsoft.OpenApi.Models.SecuritySchemeType.Http,
        Scheme = "Bearer",
        BearerFormat = "JWT",
        In = Microsoft.OpenApi.Models.ParameterLocation.Header,
        Description = "Enter 'Bearer' [space] and then your JWT token. Example: Bearer eyJhbGci..."
    });

    options.AddSecurityRequirement(new Microsoft.OpenApi.Models.OpenApiSecurityRequirement
    {
        {
            new Microsoft.OpenApi.Models.OpenApiSecurityScheme
            {
                Reference = new Microsoft.OpenApi.Models.OpenApiReference
                {
                    Type = Microsoft.OpenApi.Models.ReferenceType.SecurityScheme,
                    Id = "Bearer"
                }
            },
            Array.Empty<string>()
        }
    });
});

builder.Services.AddControllers()
    .AddJsonOptions(options => options.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter()));

// Shared MongoDB connection and Shalon's/Dinil's services.
builder.Services.AddSingleton<MongoDbContext>();
builder.Services.AddScoped<StationService>();
builder.Services.AddScoped<SlotService>();
builder.Services.AddScoped<DashboardService>();

// Dinil's services
builder.Services.AddSingleton<QrTokenService>();
builder.Services.AddScoped<ReservationService>();

// Rukshan's services
builder.Services.AddScoped<ProsumerService>();
builder.Services.AddScoped<JwtService>();

// ============================================================
// JWT Authentication Configuration
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

builder.Services.AddAuthorization();

var corsOrigins = builder.Configuration.GetSection("Cors:AllowedOrigins").Get<string[]>() ?? Array.Empty<string>();
builder.Services.AddCors(options =>
{
    options.AddPolicy("WebApp", policy =>
    {
        policy.WithOrigins(corsOrigins).AllowAnyHeader().AllowAnyMethod();
    });
});

var app = builder.Build();

// ⚠️ ORDER: Exception handler FIRST, so anything downstream (auth, controllers)
// that throws a ServiceException gets its declared HTTP status, not a blanket 500.
app.UseMiddleware<ExceptionHandlingMiddleware>();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

// ⚠️ ORDER: CORS before Auth, Auth before Controllers
app.UseCors("WebApp");
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();

// Ensures the 2dsphere index on SolarStations.location exists before serving traffic.
using (var startupScope = app.Services.CreateScope())
{
    var stationService = startupScope.ServiceProvider.GetRequiredService<StationService>();
    await stationService.EnsureIndexesAsync();
}

var summaries = new[]
{
    "Freezing", "Bracing", "Chilly", "Cool", "Mild", "Warm", "Balmy", "Hot", "Sweltering", "Scorching"
};

app.MapGet("/weatherforecast", () =>
{
    var forecast = Enumerable.Range(1, 5).Select(index =>
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