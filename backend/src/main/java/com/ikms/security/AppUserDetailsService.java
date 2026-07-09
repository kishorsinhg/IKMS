package com.ikms.security;

import com.ikms.security.domain.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

  private final AppUserRepository repository;
  private final PermissionService permissionService;

  public AppUserDetailsService(AppUserRepository repository, PermissionService permissionService) {
    this.repository = repository;
    this.permissionService = permissionService;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var user = repository.findByUsernameIgnoreCase(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    return AppUserPrincipal.from(user, permissionService.permissionsForRoles(user.getRoles()));
  }
}
