package com.toptrader.backend.user;

import com.toptrader.backend.friendship.RelationshipStatus;

public record UserSearchResult(
    Long id, String username, String avatarKey, RelationshipStatus relationshipStatus) {}
