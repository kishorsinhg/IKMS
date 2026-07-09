package com.ikms.security;

import com.ikms.config.AppSettingService;
import com.ikms.security.domain.AppUser;
import com.ikms.security.domain.AppUserRepository;
import com.ikms.security.domain.UserRole;
import com.ikms.security.domain.UserStatus;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SecurityBootstrapInitializer implements ApplicationRunner {

  private final AppUserRepository appUserRepository;
  private final AppSettingService appSettingService;
  private final PasswordEncoder passwordEncoder;
  private final boolean enabled;
  private final String defaultPassword;

  public SecurityBootstrapInitializer(
      AppUserRepository appUserRepository,
      AppSettingService appSettingService,
      PasswordEncoder passwordEncoder,
      @Value("${ikms.security.bootstrap.enabled:true}") boolean enabled,
      @Value("${ikms.security.bootstrap.default-password:ChangeMe123!}") String defaultPassword) {
    this.appUserRepository = appUserRepository;
    this.appSettingService = appSettingService;
    this.passwordEncoder = passwordEncoder;
    this.enabled = enabled;
    this.defaultPassword = defaultPassword;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!enabled) {
      return;
    }

    if (appUserRepository.count() == 0) {
      createUser("indexer", "IKMS Indexer", "indexer@ikms.local", UserRole.INDEXER);
      createUser("processor", "IKMS Processor", "processor@ikms.local", UserRole.PROCESSOR);
      createUser("supervisor", "IKMS Supervisor", "supervisor@ikms.local", UserRole.SUPERVISOR);
      createUser("admin", "IKMS Administrator", "admin@ikms.local", UserRole.ADMINISTRATOR);
    }

    if (appSettingService.getAll().isEmpty()) {
      appSettingService.put("security.session.timeout-minutes", "30", "Default local session timeout.");
      appSettingService.put("review.mode", "confidence", "Default review mode for local development.");
    }
  }

  private void createUser(String username, String displayName, String email, UserRole role) {
    AppUser user = new AppUser();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(defaultPassword));
    user.setDisplayName(displayName);
    user.setEmail(email);
    user.setStatus(UserStatus.ACTIVE);
    user.setRoles(Set.of(role));
    appUserRepository.save(user);
  }
}
