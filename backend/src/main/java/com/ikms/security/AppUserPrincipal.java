package com.ikms.security;

import com.ikms.security.domain.AppUser;
import com.ikms.security.domain.Permission;
import com.ikms.security.domain.UserRole;
import com.ikms.security.domain.UserStatus;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AppUserPrincipal(
    UUID id,
    String username,
    String password,
    String displayName,
    String email,
    UserStatus status,
    Set<UserRole> roles,
    Set<Permission> permissions,
    Collection<? extends GrantedAuthority> authorities) implements UserDetails {

  public static AppUserPrincipal from(AppUser user, Set<Permission> permissions) {
    Set<GrantedAuthority> authorities = new java.util.LinkedHashSet<>();
    user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name())));
    permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.name())));

    return new AppUserPrincipal(
        user.getId(),
        user.getUsername(),
        user.getPasswordHash(),
        user.getDisplayName(),
        user.getEmail(),
        user.getStatus(),
        Set.copyOf(user.getRoles()),
        permissions,
        Set.copyOf(authorities));
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return status != UserStatus.LOCKED;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return status == UserStatus.ACTIVE;
  }
}
