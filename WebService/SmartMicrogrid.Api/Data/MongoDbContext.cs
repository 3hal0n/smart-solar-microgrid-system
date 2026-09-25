// ============================================================
// File: MongoDbContext.cs
// Purpose: Shared MongoDB connection context exposing the database
//          handle and typed collection accessors for the
//          SmartMicrogridDB database. Built once, shared across all
//          controllers/services regardless of which collection they
//          own, per architecture.md §8.
// Author: Shalon
// ============================================================
using MongoDB.Driver;
using SmartMicrogrid.Api.Models;

namespace SmartMicrogrid.Api.Data;

public class MongoDbContext
{
    private readonly IMongoDatabase _database;

    // Opens the MongoDB connection using the connection string and database name from configuration.
    public MongoDbContext(IConfiguration configuration)
    {
        var connectionString = configuration.GetConnectionString("MongoDb")
            ?? throw new InvalidOperationException("Missing 'ConnectionStrings:MongoDb' configuration value.");
        var databaseName = configuration["MongoDb:DatabaseName"] ?? "SmartMicrogridDB";

        var client = new MongoClient(connectionString);
        _database = client.GetDatabase(databaseName);
    }

    // Exposes the raw database handle for collections that don't have a typed accessor yet.
    public IMongoDatabase Database => _database;

    // Collection for system users (Backoffice/GridOperator)
        public IMongoCollection<User> Users => _database.GetCollection<User>("Users");


    // Typed accessor for the SolarStations collection (owner: Shalon).
    public IMongoCollection<Station> SolarStations => _database.GetCollection<Station>("SolarStations");

    // Typed accessor for the EnergyBookingSlots collection (owner: Shalon).
    public IMongoCollection<Slot> EnergyBookingSlots => _database.GetCollection<Slot>("EnergyBookingSlots");

        // ADDED BY MIGARA: Required for ReservationService to access the Reservations collection.
    public IMongoCollection<Reservation> Reservations => _database.GetCollection<Reservation>("Reservations");
}
