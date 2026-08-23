package com.toptrader.backend.friendship;

import jakarta.validation.constraints.NotNull;

public record FriendshipRequest(@NotNull Long addresseeId) {}
