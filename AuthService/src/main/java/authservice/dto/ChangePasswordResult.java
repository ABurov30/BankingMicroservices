package authservice.dto;

public record ChangePasswordResult(String refreshToken, long refreshTokenDaysTtl) {}
