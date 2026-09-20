using System.Text.Json.Serialization;
using SmartMicrogrid.Api.Data;
using SmartMicrogrid.Api.Services;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
// Learn more about configuring OpenAPI at https://aka.ms/aspnet/openapi
builder.Services.AddOpenApi();

// Controllers, with enums serialized as strings so the JSON contract matches architecture.md
// (e.g. Station/Slot "status" as "Active"/"Available" rather than raw integers).
builder.Services.AddControllers()
    .AddJsonOptions(options => options.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter()));

// Shared MongoDB connection and Shalon's Stations/Slots services.
builder.Services.AddSingleton<MongoDbContext>();
builder.Services.AddScoped<StationService>();
builder.Services.AddScoped<SlotService>();

var app = builder.Build();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}

app.UseHttpsRedirection();
app.MapControllers();

// Ensures the 2dsphere index on SolarStations.location exists before the API serves traffic.
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
