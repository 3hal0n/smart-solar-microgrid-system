// ============================================================
// File: ExceptionHandlingMiddleware.cs
// Purpose: Global exception handler. Maps ServiceException to
//          its declared HTTP status + JSON error body so clients
//          receive the exact status/code/message the service
//          threw — never a generic 500 for a business rejection.
//          Anything that isn't a known service exception is a
//          genuine 500 and gets logged with the full stack.
//          Must be registered BEFORE UseAuthentication /
//          UseAuthorization / MapControllers in Program.cs.
// Author: Dinil (shared infra)
// ============================================================

using System.Text.Json;
using SmartMicrogrid.Api.Services;

namespace SmartMicrogrid.Api.Middleware;

public class ExceptionHandlingMiddleware
{
    private readonly RequestDelegate _next;
    private readonly ILogger<ExceptionHandlingMiddleware> _logger;

    // Captures the next middleware and the logger for unhandled-exception diagnostics.
    public ExceptionHandlingMiddleware(RequestDelegate next, ILogger<ExceptionHandlingMiddleware> logger)
    {
        _next = next;
        _logger = logger;
    }

    // Wraps the downstream pipeline; every request passes through here.
    public async Task InvokeAsync(HttpContext context)
    {
        try
        {
            await _next(context);
        }
        catch (ServiceException ex)
        {
            // The service declared its own HTTP status — honor it exactly.
            // 4xx business rejections are logged at Information level so they
            // don't pollute the error log.
            _logger.LogInformation("ServiceException {Code}: {Message}", ex.Code, ex.Message);
            await WriteErrorAsync(context, ex.StatusCode, ex.Code, ex.Message);
        }
        catch (NotFoundException ex)
        {
            await WriteErrorAsync(context, StatusCodes.Status404NotFound, "NOT_FOUND", ex.Message);
        }
        catch (ValidationException ex)
        {
            await WriteErrorAsync(context, StatusCodes.Status400BadRequest, "VALIDATION_ERROR", ex.Message);
        }
        catch (ConflictException ex)
        {
            await WriteErrorAsync(context, StatusCodes.Status409Conflict, "CONFLICT", ex.Message);
        }
        catch (Exception ex)
        {
            // Genuine unhandled error — log with stack trace, return a generic body
            // so internal details never leak to clients.
            _logger.LogError(ex, "Unhandled exception");
            await WriteErrorAsync(context, StatusCodes.Status500InternalServerError,
                "INTERNAL_ERROR", "An unexpected error occurred.");
        }
    }

    // Serializes { code, message } as JSON with the given HTTP status.
    private static async Task WriteErrorAsync(HttpContext context, int statusCode, string code, string message)
    {
        if (context.Response.HasStarted)
        {
            // Response already partially written — can't change status. Nothing to do.
            return;
        }
        context.Response.StatusCode = statusCode;
        context.Response.ContentType = "application/json";
        var payload = JsonSerializer.Serialize(new { code, message });
        await context.Response.WriteAsync(payload);
    }
}