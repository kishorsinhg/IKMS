package com.ikms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;

class AuthControllerTest {

  private AuthenticationManager authenticationManager;
  private AuthenticationGovernanceService authenticationGovernanceService;
  private AuthController controller;

  @BeforeEach
  void setUp() {
    authenticationManager = mock(AuthenticationManager.class);
    authenticationGovernanceService = mock(AuthenticationGovernanceService.class);
    when(authenticationGovernanceService.sessionTimeoutSeconds()).thenReturn(1800);
    controller = new AuthController(authenticationManager, authenticationGovernanceService);
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
    assertThat(request.getSession(false).getMaxInactiveInterval()).isEqualTo(1800);
    assertThat(request.getSession(false).getAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNotNull();
    verify(authenticationGovernanceService).recordSuccessfulLogin(principal.id(), "processor");
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

  @Test
  void loginShouldRecordFailedAttemptsAndReturnUnauthorized() {
    when(authenticationManager.authenticate(any(Authentication.class)))
        .thenThrow(new BadCredentialsException("bad credentials"));

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
        ResponseStatusException.class,
        () -> controller.login(new AuthController.LoginRequest("processor", "wrong"), request, response));

    assertThat(exception.getStatusCode().value()).isEqualTo(401);
    verify(authenticationGovernanceService).recordFailedLogin(any(), any());
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
