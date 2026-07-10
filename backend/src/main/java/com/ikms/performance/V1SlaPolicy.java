package com.ikms.performance;

import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class V1SlaPolicy {

  public static final Duration CLIENT_PROFILE_OPEN_LIMIT = Duration.ofSeconds(10);
  public static final double SUCCESS_RATE_TARGET = 0.95d;

  public boolean meetsClientProfileOpenSla(Duration duration) {
    return duration.compareTo(CLIENT_PROFILE_OPEN_LIMIT) <= 0;
  }

  public boolean meetsRoutingSuccessRate(int routedWithinSla, int totalItems) {
    if (totalItems <= 0) {
      return false;
    }
    return (double) routedWithinSla / totalItems >= SUCCESS_RATE_TARGET;
  }
}
