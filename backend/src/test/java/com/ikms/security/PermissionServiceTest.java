package com.ikms.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ikms.security.domain.Permission;
import com.ikms.security.domain.UserRole;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PermissionServiceTest {

  private final PermissionService permissionService = new PermissionService();

  @Test
  void supervisorShouldReceivePiiAndOriginalDocumentPermissions() {
    var permissions = permissionService.permissionsForRoles(Set.of(UserRole.SUPERVISOR));

    assertThat(permissions).contains(Permission.VIEW_PII, Permission.VIEW_ORIGINAL_DOCUMENTS);
    assertThat(permissions).doesNotContain(Permission.MANAGE_CONFIGURATION);
  }

  @Test
  void administratorShouldNotImplicitlyReceiveClientDataPermissions() {
    var permissions = permissionService.permissionsForRoles(Set.of(UserRole.ADMINISTRATOR));

    assertThat(permissions).contains(Permission.MANAGE_CONFIGURATION, Permission.MANAGE_USERS);
    assertThat(permissions).doesNotContain(Permission.CLIENT_VIEW, Permission.VIEW_PII);
  }
}
