import { FormEvent, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Navigate,
  Outlet,
  Route,
  Routes,
  useLocation,
} from "react-router-dom";
import {
  Alert,
  Box,
  Button,
  Checkbox,
  FormControlLabel,
  Link,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { Permission, login } from "../api/auth";
import { ApiClientError } from "../api/client";
import { PermissionGuard } from "./auth/PermissionGuard";
import { currentUserQueryKey, useCurrentUser } from "./auth/useCurrentUser";
import { ErrorState, LoadingState, RestrictedContentState } from "./WorkspaceStates";
import { AdminConfigurationPage } from "../features/admin/AdminConfigurationPage";
import { AuditPage } from "../features/audit/AuditPage";
import { ClientProfilePage } from "../features/clients/ClientProfilePage";
import { ClientsWorkspacePage } from "../features/clients/ClientsWorkspacePage";
import { ClientImportPage } from "../features/clients/import/ClientImportPage";
import { IntakePage } from "../features/intake/IntakePage";
import { ReviewDetailPage } from "../features/intake/review/ReviewDetailPage";
import { ReviewQueuePage } from "../features/intake/review/ReviewQueuePage";
import { SearchLandingPage } from "../features/search/SearchLandingPage";
import { IkmsAppShell } from "./shell/IkmsAppShell";

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<IkmsAppShell />}>
          <Route path="/" element={<Navigate to="/search" replace />} />
          <Route
            path="/clients"
            element={
              <RoutePermissionGuard permission="CLIENT_VIEW">
                <ClientsWorkspacePage />
              </RoutePermissionGuard>
            }
          />
          <Route
            path="/clients/import"
            element={
              <RoutePermissionGuard permission="CLIENT_VIEW">
                <ClientImportPage />
              </RoutePermissionGuard>
            }
          />
          <Route
            path="/clients/:clientId"
            element={
              <RoutePermissionGuard permission="CLIENT_VIEW">
                <ClientProfilePage />
              </RoutePermissionGuard>
            }
          />
          <Route
            path="/review-queue"
            element={
              <RoutePermissionGuard permission="REVIEW_QUEUE_ACCESS">
                <ReviewQueuePage />
              </RoutePermissionGuard>
            }
          />
          <Route
            path="/review-queue/:reviewId"
            element={
              <RoutePermissionGuard permission="REVIEW_QUEUE_ACCESS">
                <ReviewDetailPage />
              </RoutePermissionGuard>
            }
          />
          <Route
            path="/intake"
            element={
              <RoutePermissionGuard permission="INTAKE_ACCESS">
                <IntakePage />
              </RoutePermissionGuard>
            }
          />
          <Route
            path="/search"
            element={
              <RoutePermissionGuard permission="SEARCH_CLIENT_KNOWLEDGE">
                <SearchLandingPage />
              </RoutePermissionGuard>
            }
          />
          <Route
            path="/administration"
            element={
              <RoutePermissionGuard permission="MANAGE_CONFIGURATION">
                <AdminConfigurationPage />
              </RoutePermissionGuard>
            }
          />
          <Route
            path="/audit"
            element={
              <RoutePermissionGuard permission="VIEW_AUDIT">
                <AuditPage />
              </RoutePermissionGuard>
            }
          />
        </Route>
      </Route>
    </Routes>
  );
}

function ProtectedRoute() {
  const location = useLocation();
  const currentUserQuery = useCurrentUser();

  if (currentUserQuery.isLoading) {
    return (
      <main style={{ minHeight: "100vh", display: "grid", placeItems: "center", padding: 16 }}>
        <LoadingState
          title="Loading IKMS security context"
          message="Checking the active session and workspace permissions."
        />
      </main>
    );
  }

  if (currentUserQuery.isError && isUnauthorized(currentUserQuery.error)) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: `${location.pathname}${location.search}${location.hash}` }}
      />
    );
  }

  if (currentUserQuery.isError) {
    return (
      <main style={{ minHeight: "100vh", display: "grid", placeItems: "center", padding: 16 }}>
        <ErrorState
          title="Unable to load the current user session"
          message="Confirm the backend is running on http://localhost:8080 and CORS allows http://localhost:5173."
        />
      </main>
    );
  }

  return <Outlet />;
}

function RoutePermissionGuard({
  permission,
  children,
}: {
  permission: Permission;
  children: React.ReactNode;
}) {
  return (
    <PermissionGuard
      allOf={[permission]}
      fallback={
        <RestrictedContentState
          title="Workspace restricted"
          message="This route is protected by role-based permissions. The current account cannot open it directly."
        />
      }
    >
      {children}
    </PermissionGuard>
  );
}

function LoginPage() {
  const queryClient = useQueryClient();
  const location = useLocation();
  const [username, setUsername] = useState("processor");
  const [password, setPassword] = useState("ChangeMe123!");
  const [rememberMe, setRememberMe] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const currentUserQuery = useCurrentUser();

  const signIn = useMutation({
    mutationFn: login,
    onSuccess: async () => {
      setErrorMessage(null);
      await queryClient.invalidateQueries({ queryKey: currentUserQueryKey });
    },
    onError: (error: ApiClientError) => {
      setErrorMessage(isUnauthorized(error) ? "Invalid username or password." : "Login failed.");
    },
  });

  if (currentUserQuery.data) {
    const redirectTo =
      (location.state as { from?: string } | null)?.from ?? "/search";
    return <Navigate to={redirectTo} replace />;
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    signIn.mutate({ username, password });
  }

  return (
    <Box
      component="main"
      sx={{
        minHeight: "100vh",
        display: "grid",
        placeItems: "center",
        px: 2,
        py: 3,
        bgcolor: "background.default",
      }}
    >
      <Paper
        component="form"
        onSubmit={handleSubmit}
        sx={{
          width: "min(420px, 100%)",
          p: 4,
          display: "grid",
          gap: 2,
          border: (theme) => `1px solid ${theme.palette.divider}`,
        }}
      >
        <Stack spacing={0.75}>
          <Typography variant="subtitle2" color="text.secondary">
            Insurance Knowledge Management System
          </Typography>
          <Typography variant="h1">Sign in</Typography>
          <Typography variant="body2" color="text.secondary">
            Local username and password authentication for the current single-tenant IKMS deployment.
          </Typography>
        </Stack>

        {errorMessage ? <Alert severity="error">{errorMessage}</Alert> : null}

        <TextField
          label="Username"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
        />
        <TextField
          label="Password"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />

        <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={1}>
          <FormControlLabel
            control={
              <Checkbox
                checked={rememberMe}
                onChange={(event) => setRememberMe(event.target.checked)}
              />
            }
            label="Remember Me"
          />
          <Link href="#" underline="hover">
            Forgot Password
          </Link>
        </Stack>

        <Button type="submit" variant="contained" disabled={signIn.isPending}>
          {signIn.isPending ? "Signing in..." : "Sign in"}
        </Button>
      </Paper>
    </Box>
  );
}

function isUnauthorized(error: unknown) {
  return error instanceof ApiClientError && error.status === 401;
}
