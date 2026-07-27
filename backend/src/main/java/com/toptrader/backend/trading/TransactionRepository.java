package com.toptrader.backend.trading;

import com.toptrader.backend.user.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
  List<Transaction> findByUserOrderByExecutedAtDesc(User user);
}
