package com.ikms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ikms.audit.AuditService;
import com.ikms.config.AppSettingService;
import com.ikms.security.domain.AppUser;
import com.ikms.security.domain.AppUserRepository;
import com.ikms.security.domain.UserRole;
import com.ikms.security.domain.UserStatus;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;

class AuthenticationGovernanceTest {

  private AppUserRepository appUserRepository;
  private AppSettingService appSettingService;
  private AuditService auditService;
  private AuthenticationGovernanceService service;

  @BeforeEach
  void setUp() {
    appUserRepository = mock(AppUserRepository.class);
    appSettingService = mock(AppSettingService.class);
    auditService = mock(AuditService.class);
    service = new AuthenticationGovernanceService(appUserRepository, appSettingService, auditService);
  }

  @Test
  void failedLoginsShouldIncrementCountAndLockUserAtThreshold() {
    AppUser user = activeUser();
    user.setFailedLoginCount(2);
    when(appUserRepository.findByUsernameIgnoreCase("processor")).thenReturn(Optional.of(user));
    when(appSettingService.get("security.failed-login.max-attempts")).thenReturn(Optional.of("3"));

    service.recordFailedLogin("processor", new BadCredentialsException("bad credentials"));

    assertThat(user.getFailedLoginCount()).isEqualTo(3);
    assertThat(user.getStatus()).isEqualTo(UserStatus.LOCKED);
    verify(appUserRepository).save(user);
    verify(auditService).write(any());
  }

  @Test
  void successfulLoginShouldResetFailedCountAndSetLastLogin() {
    AppUser user = activeUser();
    user.setFailedLoginCount(4);
    when(appUserRepository.findByUsernameIgnoreCase("processor")).thenReturn(Optional.of(user));

    service.recordSuccessfulLogin(user.getId(), "processor");

    assertThat(user.getFailedLoginCount()).isZero();
    assertThat(user.getLastLoginAt()).isNotNull();
    verify(appUserRepository).save(user);
    verify(auditService).write(any());
  }

  @Test
  void sessionTimeoutShouldUseConfiguredMinutes() {
    when(appSettingService.get("security.session.timeout-minutes")).thenReturn(Optional.of("45"));

    assertThat(service.sessionTimeoutSeconds()).isEqualTo(2700);
  }

  @Test
  void lockedFailureShouldAuditWithoutChangingCounts() {
    AppUser user = activeUser();
    when(appUserRepository.findByUsernameIgnoreCase("processor")).thenReturn(Optional.of(user));

    service.recordFailedLogin("processor", new LockedException("locked"));

    assertThat(user.getFailedLoginCount()).isZero();
    verify(auditService).write(any());
  }

  private AppUser activeUser() {
    AppUser user = new AppUser();
    user.setId(UUID.randomUUID());
    user.setUsername("processor");
    user.setPasswordHash("encoded");
    user.setDisplayName("Processor");
    user.setStatus(UserStatus.ACTIVE);
    user.setRoles(Set.of(UserRole.PROCESSOR));
    return user;
  }
}
