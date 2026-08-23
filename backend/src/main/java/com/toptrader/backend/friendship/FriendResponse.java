package com.toptrader.backend.friendship;

import java.time.LocalDateTime;

public record FriendResponse(
    Long id, String username, String avatarKey, LocalDateTime friendsSince) {}
