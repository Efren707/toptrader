package com.toptrader.backend.friendship;

import java.time.LocalDateTime;

public record OutgoingFriendRequest(Long id, FriendSummary addressee, LocalDateTime createdAt) {}
