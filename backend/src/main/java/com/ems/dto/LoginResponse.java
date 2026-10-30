package com.ems.dto;

import java.time.Instant;

/** Send the token back as "Authorization: Bearer &lt;token&gt;" on every request. */
public record LoginResponse(String token, String tokenType, Instant expiresAt, CurrentUserResponse user) {
}
