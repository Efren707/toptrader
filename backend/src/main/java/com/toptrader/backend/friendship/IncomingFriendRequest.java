package com.toptrader.backend.friendship;

import java.time.LocalDateTime;

public record IncomingFriendRequest(Long id, FriendSummary requester, LocalDateTime createdAt) {}
