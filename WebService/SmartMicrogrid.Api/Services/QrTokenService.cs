// ============================================================
// File: QrTokenService.cs
// Purpose: HMAC-SHA256 signed QR tokens for reservation
//          verification. Issued by Dinil at reservation
//          creation, verified by Migara at operator scan.
//          Pair-built Day 2 - do not edit independently.
// Author: Dinil + Migara
// ============================================================

using System.Security.Cryptography;
using System.Text;

namespace SmartMicrogrid.Api.Services;

public class QrTokenService
{
    private readonly string _secret;

    public QrTokenService(IConfiguration config)
    {
        _secret = config["Qr:Secret"]
            ?? throw new InvalidOperationException("Qr:Secret missing from appsettings.");
    }

    // Issues a signed token binding reservationId to an expiry instant.
    public string Issue(string reservationId, DateTime expiryUtc)
    {
        var expiryUnix = new DateTimeOffset(expiryUtc).ToUnixTimeSeconds();
        var payload = $"{reservationId}|{expiryUnix}";
        var signature = Sign(payload);
        return Base64UrlEncode($"{payload}|{signature}");
    }

    // Verifies a token's signature and expiry; returns reservationId if valid.
    public string? Verify(string token)
    {
        try
        {
            var decoded = Base64UrlDecode(token);
            var parts = decoded.Split('|');
            if (parts.Length != 3) return null;

            var reservationId = parts[0];
            var expiryUnix = parts[1];
            var signature = parts[2];

            var expected = Sign($"{reservationId}|{expiryUnix}");
            if (!CryptographicOperations.FixedTimeEquals(
                    Encoding.UTF8.GetBytes(signature),
                    Encoding.UTF8.GetBytes(expected)))
                return null;

            var expiry = DateTimeOffset.FromUnixTimeSeconds(long.Parse(expiryUnix)).UtcDateTime;
            if (expiry <= DateTime.UtcNow) return null;

            return reservationId;
        }
        catch { return null; }
    }

    private string Sign(string payload)
    {
        using var hmac = new HMACSHA256(Encoding.UTF8.GetBytes(_secret));
        var hash = hmac.ComputeHash(Encoding.UTF8.GetBytes(payload));
        return Convert.ToBase64String(hash);
    }

    private static string Base64UrlEncode(string s)
        => Convert.ToBase64String(Encoding.UTF8.GetBytes(s))
            .Replace('+', '-').Replace('/', '_').TrimEnd('=');

    private static string Base64UrlDecode(string s)
    {
        s = s.Replace('-', '+').Replace('_', '/');
        switch (s.Length % 4) { case 2: s += "=="; break; case 3: s += "="; break; }
        return Encoding.UTF8.GetString(Convert.FromBase64String(s));
    }
}