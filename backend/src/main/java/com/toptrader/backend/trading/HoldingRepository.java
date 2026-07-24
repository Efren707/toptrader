package com.toptrader.backend.trading;

import com.toptrader.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    Optional<Holding> findByUserAndTicker(User user, String ticker);
}
