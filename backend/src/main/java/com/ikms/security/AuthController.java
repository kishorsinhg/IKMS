package com.ikms.security;

import com.ikms.security.domain.Permission;
import com.ikms.security.domain.UserRole;
import com.ikms.security.domain.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthenticationManager authenticationManager;

  public AuthController(AuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  @PostMapping("/login")
  public CurrentUserResponse login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    Authentication authentication = authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    new HttpSessionSecurityContextRepository().saveContext(context, httpRequest, httpResponse);

    return CurrentUserResponse.from((AppUserPrincipal) authentication.getPrincipal());
  }

  @PostMapping("/logout")
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    SecurityContextHolder.clearContext();
    if (request.getSession(false) != null) {
      request.getSession(false).invalidate();
    }
    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }

  @GetMapping("/me")
  public CurrentUserResponse me(Authentication authentication) {
    return CurrentUserResponse.from((AppUserPrincipal) authentication.getPrincipal());
  }

  public record LoginRequest(
      @NotBlank(message = "Username is required.") String username,
      @NotBlank(message = "Password is required.") String password) {
  }

  public record CurrentUserResponse(
      UUID id,
      String username,
      String displayName,
      String email,
      UserStatus status,
      Set<UserRole> roles,
      Set<Permission> permissions) {

    static CurrentUserResponse from(AppUserPrincipal principal) {
      return new CurrentUserResponse(
          principal.id(),
          principal.username(),
          principal.displayName(),
          principal.email(),
          principal.status(),
          principal.roles(),
          principal.permissions());
    }
  }
}
