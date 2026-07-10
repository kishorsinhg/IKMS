package com.ikms.security;

import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import com.ikms.config.AppSettingService;
import com.ikms.security.domain.AppUser;
import com.ikms.security.domain.AppUserRepository;
import com.ikms.security.domain.UserStatus;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthenticationGovernanceService {

  private static final String FAILED_LOGIN_MAX_ATTEMPTS = "security.failed-login.max-attempts";
  private static final String SESSION_TIMEOUT_MINUTES = "security.session.timeout-minutes";

  private final AppUserRepository appUserRepository;
  private final AppSettingService appSettingService;
  private final AuditService auditService;

  public AuthenticationGovernanceService(
      AppUserRepository appUserRepository,
      AppSettingService appSettingService,
      AuditService auditService) {
    this.appUserRepository = appUserRepository;
    this.appSettingService = appSettingService;
    this.auditService = auditService;
  }

  public int sessionTimeoutSeconds() {
    return intSetting(SESSION_TIMEOUT_MINUTES, 30) * 60;
  }

  public void recordSuccessfulLogin(UUID actorUserId, String username) {
    appUserRepository.findByUsernameIgnoreCase(username).ifPresent(user -> {
      user.setFailedLoginCount(0);
      user.setLastLoginAt(Instant.now());
      appUserRepository.save(user);
    });

    auditService.write(new AuditEvent(
        Instant.now(),
        "AUTHENTICATION",
        "LOGIN",
        AuditOutcome.SUCCESS,
        actorUserId,
        null,
        "User",
        username,
        false,
        Map.of("username", username)));
  }

  public void recordFailedLogin(String username, AuthenticationException exception) {
    Optional<AppUser> userResult = appUserRepository.findByUsernameIgnoreCase(username);
    UUID actorUserId = null;
    String failureReason = "BAD_CREDENTIALS";

    if (exception instanceof LockedException) {
      failureReason = "LOCKED";
    } else if (!(exception instanceof BadCredentialsException)) {
      failureReason = exception.getClass().getSimpleName().toUpperCase();
    }

    if (userResult.isPresent()) {
      AppUser user = userResult.get();
      actorUserId = user.getId();

      if (exception instanceof BadCredentialsException && user.getStatus() == UserStatus.ACTIVE) {
        int updatedFailedCount = user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(updatedFailedCount);
        if (updatedFailedCount >= intSetting(FAILED_LOGIN_MAX_ATTEMPTS, 5)) {
          user.setStatus(UserStatus.LOCKED);
          failureReason = "LOCKED_AFTER_FAILED_ATTEMPTS";
        }
        appUserRepository.save(user);
      }
    }

    auditService.write(new AuditEvent(
        Instant.now(),
        "AUTHENTICATION",
        "LOGIN",
        AuditOutcome.FAILURE,
        actorUserId,
        null,
        "User",
        username,
        false,
        Map.of("reason", failureReason, "username", username)));
  }

  private int intSetting(String key, int fallback) {
    return appSettingService.get(key)
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .map(Integer::parseInt)
        .orElse(fallback);
  }
}
