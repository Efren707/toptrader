package com.toptrader.backend.trading;

import com.toptrader.backend.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
  Optional<Holding> findByUserAndTicker(User user, String ticker);
}
