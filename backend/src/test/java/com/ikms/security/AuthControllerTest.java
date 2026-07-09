package com.ikms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ikms.security.domain.Permission;
import com.ikms.security.domain.UserRole;
import com.ikms.security.domain.UserStatus;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

class AuthControllerTest {

  private AuthenticationManager authenticationManager;
  private AuthController controller;

  @BeforeEach
  void setUp() {
    authenticationManager = mock(AuthenticationManager.class);
    controller = new AuthController(authenticationManager);
  }

  @Test
  void loginShouldStoreSecurityContextInSession() {
    AppUserPrincipal principal = principal();
    Authentication authenticatedToken = UsernamePasswordAuthenticationToken.authenticated(
        principal,
        principal.getPassword(),
        principal.getAuthorities());

    when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticatedToken);

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    AuthController.CurrentUserResponse currentUser = controller.login(
        new AuthController.LoginRequest("processor", "ChangeMe123!"),
        request,
        response);

    assertThat(currentUser.username()).isEqualTo("processor");
    assertThat(currentUser.permissions()).contains(Permission.CLIENT_VIEW);
    assertThat(request.getSession(false)).isNotNull();
    assertThat(request.getSession(false).getAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNotNull();
  }

  @Test
  void meShouldReturnAuthenticatedPrincipalSummary() {
    AppUserPrincipal principal = principal();
    Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
        principal,
        principal.getPassword(),
        principal.getAuthorities());

    AuthController.CurrentUserResponse currentUser = controller.me(authentication);

    assertThat(currentUser.roles()).containsExactly(UserRole.PROCESSOR);
    assertThat(currentUser.permissions()).contains(Permission.ASK_CLIENT_AI);
  }

  @Test
  void logoutShouldInvalidateExistingSession() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.getSession(true);
    MockHttpServletResponse response = new MockHttpServletResponse();

    controller.logout(request, response);

    assertThat(response.getStatus()).isEqualTo(MockHttpServletResponse.SC_NO_CONTENT);
    assertThat(request.getSession(false)).isNull();
  }

  private AppUserPrincipal principal() {
    return new AppUserPrincipal(
        UUID.randomUUID(),
        "processor",
        "encoded-password",
        "Processor User",
        "processor@ikms.local",
        UserStatus.ACTIVE,
        Set.of(UserRole.PROCESSOR),
        Set.of(Permission.CLIENT_VIEW, Permission.ASK_CLIENT_AI),
        Set.of());
  }
}
