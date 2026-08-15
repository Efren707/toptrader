package com.toptrader.backend.friendship;

public record FriendshipResponse(
    Long id, Long requesterId, Long addresseeId, Friendship.Status status) {}
