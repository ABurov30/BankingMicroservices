package apigateway.ratelimit;

public record RateLimitResult(boolean allowed, long retryAfterSeconds) {}
