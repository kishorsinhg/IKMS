package com.ikms.security;

import com.ikms.security.domain.Permission;
import com.ikms.security.domain.UserRole;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

  private final EnumMap<UserRole, Set<Permission>> permissionsByRole = new EnumMap<>(UserRole.class);

  public PermissionService() {
    permissionsByRole.put(UserRole.INDEXER, EnumSet.of(
        Permission.REVIEW_QUEUE_ACCESS,
        Permission.INTAKE_ACCESS));

    permissionsByRole.put(UserRole.PROCESSOR, EnumSet.of(
        Permission.CLIENT_VIEW,
        Permission.SEARCH_CLIENT_KNOWLEDGE,
        Permission.ASK_CLIENT_AI,
        Permission.VIEW_REDACTED_DOCUMENTS));

    permissionsByRole.put(UserRole.SUPERVISOR, EnumSet.of(
        Permission.CLIENT_VIEW,
        Permission.SEARCH_CLIENT_KNOWLEDGE,
        Permission.ASK_CLIENT_AI,
        Permission.VIEW_REDACTED_DOCUMENTS,
        Permission.VIEW_ORIGINAL_DOCUMENTS,
        Permission.VIEW_PII,
        Permission.VIEW_AUDIT));

    permissionsByRole.put(UserRole.ADMINISTRATOR, EnumSet.of(
        Permission.MANAGE_CONFIGURATION,
        Permission.MANAGE_USERS,
        Permission.VIEW_AUDIT,
        Permission.EXPORT_AUDIT));
  }

  public Set<Permission> permissionsForRoles(Collection<UserRole> roles) {
    Set<Permission> permissions = new LinkedHashSet<>();
    roles.forEach(role -> permissions.addAll(permissionsByRole.getOrDefault(role, Set.of())));
    return Set.copyOf(permissions);
  }

  public boolean hasPermission(Authentication authentication, Permission permission) {
    if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
      return false;
    }
    return principal.permissions().contains(permission);
  }
}
