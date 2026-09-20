// ============================================================
// File: ServiceExceptions.cs
// Purpose: Lightweight exception types services throw to signal
//          business-rule failures. Controllers catch these and map
//          them to the correct HTTP status code — the decision of
//          *whether* something is allowed still lives entirely in
//          the service, per the FAT service pattern; the controller
//          only translates the outcome.
// Author: Shalon
// ============================================================
namespace SmartMicrogrid.Api.Services;

// Thrown when a requested station/slot doesn't exist.
public class NotFoundException : Exception
{
    public NotFoundException(string message) : base(message)
    {
    }
}

// Thrown when a request fails a business validation rule (bad GPS range, capacity <= 0, etc.).
public class ValidationException : Exception
{
    public ValidationException(string message) : base(message)
    {
    }
}
