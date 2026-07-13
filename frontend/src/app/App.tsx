import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { NavLink, Navigate, Outlet, Route, Routes, useLocation } from "react-router-dom";
import { Permission, getCurrentUser, login, logout } from "../api/auth";
import { ApiClientError } from "../api/client";
import { AdminConfigurationPage } from "../features/admin/AdminConfigurationPage";
import { AuditPage } from "../features/audit/AuditPage";
import { ClientProfilePage } from "../features/clients/ClientProfilePage";
import { ClientsWorkspacePage } from "../features/clients/ClientsWorkspacePage";
import { ClientImportPage } from "../features/clients/import/ClientImportPage";
import { IntakePage } from "../features/intake/IntakePage";
import { ReviewQueuePage } from "../features/intake/review/ReviewQueuePage";
import { SearchLandingPage } from "../features/search/SearchLandingPage";

const currentUserQueryKey = ["auth", "me"];

const navItems: Array<{
  to: string;
  label: string;
  permission: Permission;
}> = [
  { to: "/clients", label: "Clients", permission: "CLIENT_VIEW" },
  { to: "/review-queue", label: "Review Queue", permission: "REVIEW_QUEUE_ACCESS" },
  { to: "/intake", label: "Intake", permission: "INTAKE_ACCESS" },
  { to: "/search", label: "Search", permission: "SEARCH_CLIENT_KNOWLEDGE" },
  { to: "/administration", label: "Administration", permission: "MANAGE_CONFIGURATION" },
  { to: "/audit", label: "Audit", permission: "VIEW_AUDIT" },
];

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route path="/" element={<Navigate to="/clients" replace />} />
          <Route path="/clients" element={<ClientsWorkspacePage />} />
          <Route path="/clients/import" element={<ClientImportPage />} />
          <Route path="/clients/:clientId" element={<ClientProfilePage />} />
          <Route path="/review-queue" element={<ReviewQueuePage />} />
          <Route path="/intake" element={<IntakePage />} />
          <Route path="/search" element={<SearchLandingPage />} />
          <Route path="/administration" element={<AdminConfigurationPage />} />
          <Route path="/audit" element={<AuditPage />} />
        </Route>
      </Route>
    </Routes>
  );
}

function ProtectedRoute() {
  const location = useLocation();
  const currentUserQuery = useCurrentUser();

  if (currentUserQuery.isLoading) {
    return <div>Loading IKMS security context...</div>;
  }

  if (currentUserQuery.isError && isUnauthorized(currentUserQuery.error)) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (currentUserQuery.isError) {
    return (
      <div>
        Unable to load the current user session. Confirm the backend is running on
        {" "}
        <code>http://localhost:8080</code>
        {" "}
        and CORS allows
        {" "}
        <code>http://localhost:5173</code>.
      </div>
    );
  }

  return <Outlet />;
}

function AppShell() {
  const location = useLocation();
  const queryClient = useQueryClient();
  const currentUserQuery = useCurrentUser();
  const signOut = useMutation({
    mutationFn: logout,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: currentUserQueryKey });
    },
  });

  const user = currentUserQuery.data!;
  const visibleNavItems = navItems.filter((item) => user.permissions.includes(item.permission));
  const currentWorkspace = visibleNavItems.find((item) => location.pathname.startsWith(item.to))?.label ?? "Workspace";

  return (
    <div style={shellStyles.page}>
      <aside style={shellStyles.sidebar}>
        <div style={shellStyles.sidebarTop}>
          <div style={shellStyles.brandBlock}>
            <span style={shellStyles.brandKicker}>Enterprise operations</span>
            <h1 style={shellStyles.productName}>IKMS</h1>
            <p style={shellStyles.caption}>Insurance Knowledge Management System</p>
          </div>

          <div style={shellStyles.workspaceTag}>
            <strong style={shellStyles.workspaceLabel}>Current workspace</strong>
            <span>{currentWorkspace}</span>
          </div>
        </div>

        <nav style={shellStyles.nav}>
          {visibleNavItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              style={({ isActive }) => (isActive ? shellStyles.navLinkActive : shellStyles.navLink)}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div style={shellStyles.userCard}>
          <div style={shellStyles.userMeta}>
            <strong>{user.displayName}</strong>
            <span>{user.roles.join(", ")}</span>
          </div>
          <button
            type="button"
            onClick={() => signOut.mutate()}
            style={shellStyles.secondaryButton}
          >
            Sign out
          </button>
        </div>
      </aside>

      <main style={shellStyles.content}>
        <div style={shellStyles.topBar}>
          <div>
            <p style={shellStyles.topBarLabel}>Operations workspace</p>
            <h2 style={shellStyles.topBarTitle}>{currentWorkspace}</h2>
          </div>
          <div style={shellStyles.topBarMeta}>
            <span style={shellStyles.metaChip}>{user.username}</span>
            <span style={shellStyles.metaChip}>{user.permissions.length} permissions</span>
          </div>
        </div>
        <Outlet />
      </main>
    </div>
  );
}

function LoginPage() {
  const queryClient = useQueryClient();
  const location = useLocation();
  const [username, setUsername] = useState("processor");
  const [password, setPassword] = useState("ChangeMe123!");
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
    const redirectTo = (location.state as { from?: string } | null)?.from ?? "/clients";
    return <Navigate to={redirectTo} replace />;
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    signIn.mutate({ username, password });
  }

  return (
    <main style={shellStyles.loginPage}>
      <form onSubmit={handleSubmit} style={shellStyles.loginCard}>
        <div>
          <h1 style={shellStyles.loginTitle}>IKMS Sign In</h1>
          <p style={shellStyles.caption}>Session-based local authentication for foundation testing.</p>
        </div>

        <label style={shellStyles.field}>
          <span>Username</span>
          <input value={username} onChange={(event) => setUsername(event.target.value)} />
        </label>

        <label style={shellStyles.field}>
          <span>Password</span>
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </label>

        {errorMessage ? <div style={shellStyles.errorText}>{errorMessage}</div> : null}

        <button type="submit" style={shellStyles.primaryButton} disabled={signIn.isPending}>
          {signIn.isPending ? "Signing in..." : "Sign in"}
        </button>
      </form>
    </main>
  );
}

function useCurrentUser() {
  return useQuery({
    queryKey: currentUserQueryKey,
    queryFn: getCurrentUser,
    retry: false,
  });
}

function isUnauthorized(error: unknown) {
  return error instanceof ApiClientError && error.status === 401;
}

const shellStyles: Record<string, React.CSSProperties> = {
  page: {
    minHeight: "100vh",
    display: "grid",
    gridTemplateColumns: "300px 1fr",
    background: "transparent",
    color: "var(--text)",
  },
  sidebar: {
    display: "flex",
    flexDirection: "column",
    justifyContent: "space-between",
    padding: "1.75rem 1.35rem",
    borderRight: "1px solid rgba(255, 255, 255, 0.08)",
    background: "linear-gradient(180deg, var(--sidebar) 0%, var(--sidebar-alt) 100%)",
    color: "#dce7f6",
  },
  sidebarTop: {
    display: "grid",
    gap: "1.5rem",
  },
  brandBlock: {
    display: "grid",
    gap: "0.35rem",
  },
  brandKicker: {
    color: "#8ea6c5",
    fontSize: "0.75rem",
    letterSpacing: "0.14em",
    textTransform: "uppercase",
  },
  workspaceTag: {
    display: "grid",
    gap: "0.25rem",
    padding: "0.9rem 1rem",
    borderRadius: "1rem",
    background: "rgba(255,255,255,0.05)",
    border: "1px solid rgba(214, 224, 236, 0.1)",
  },
  workspaceLabel: {
    color: "#9fb4cf",
    fontSize: "0.78rem",
  },
  productName: {
    margin: 0,
    fontSize: "2.1rem",
    letterSpacing: "-0.04em",
    color: "#f8fbff",
  },
  caption: {
    margin: "0.35rem 0 0",
    color: "#9fb4cf",
  },
  nav: {
    display: "grid",
    gap: "0.45rem",
    margin: "2rem 0",
  },
  navLink: {
    color: "#b9cae0",
    textDecoration: "none",
    fontWeight: 600,
    padding: "0.9rem 1rem",
    borderRadius: "0.95rem",
    border: "1px solid transparent",
  },
  navLinkActive: {
    color: "#f8fbff",
    textDecoration: "none",
    fontWeight: 700,
    padding: "0.9rem 1rem",
    borderRadius: "0.95rem",
    background: "linear-gradient(180deg, rgba(11, 99, 206, 0.34) 0%, rgba(11, 99, 206, 0.18) 100%)",
    border: "1px solid rgba(123, 173, 240, 0.3)",
  },
  userCard: {
    display: "grid",
    gap: "0.85rem",
    padding: "1rem 1.05rem",
    borderRadius: "1rem",
    background: "rgba(255,255,255,0.07)",
    border: "1px solid rgba(214, 224, 236, 0.1)",
  },
  userMeta: {
    display: "grid",
    gap: "0.2rem",
  },
  content: {
    display: "grid",
    alignContent: "start",
    gap: "1.35rem",
    padding: "1.5rem 1.75rem 2rem",
  },
  topBar: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    gap: "1rem",
    padding: "1.1rem 1.25rem",
    borderRadius: "1.25rem",
    background: "rgba(255,255,255,0.78)",
    border: "1px solid var(--line)",
    boxShadow: "0 10px 30px rgba(15, 23, 40, 0.05)",
  },
  topBarLabel: {
    margin: 0,
    fontSize: "0.78rem",
    letterSpacing: "0.08em",
    textTransform: "uppercase",
    color: "var(--muted)",
  },
  topBarTitle: {
    margin: "0.2rem 0 0",
    fontSize: "1.4rem",
    letterSpacing: "-0.03em",
  },
  topBarMeta: {
    display: "flex",
    gap: "0.65rem",
    flexWrap: "wrap",
  },
  metaChip: {
    padding: "0.45rem 0.75rem",
    borderRadius: "999px",
    background: "var(--accent-soft)",
    color: "var(--accent-strong)",
    fontWeight: 700,
    fontSize: "0.82rem",
  },
  loginPage: {
    minHeight: "100vh",
    display: "grid",
    placeItems: "center",
    padding: "1.5rem",
    background:
      "radial-gradient(circle at top left, rgba(11,99,206,0.16) 0%, transparent 26%), radial-gradient(circle at bottom right, rgba(15,27,45,0.15) 0%, transparent 30%), linear-gradient(180deg, #f8fbfe 0%, #e9f0f7 100%)",
  },
  loginCard: {
    width: "min(420px, 100%)",
    display: "grid",
    gap: "1.1rem",
    padding: "2rem",
    borderRadius: "1.4rem",
    background: "rgba(255, 255, 255, 0.92)",
    border: "1px solid var(--line)",
    boxShadow: "0 28px 64px rgba(15, 23, 40, 0.12)",
  },
  loginTitle: {
    margin: 0,
    fontSize: "2.1rem",
    letterSpacing: "-0.04em",
  },
  field: {
    display: "grid",
    gap: "0.35rem",
  },
  primaryButton: {
    border: "none",
    borderRadius: "0.9rem",
    padding: "0.9rem 1rem",
    background: "linear-gradient(180deg, var(--accent) 0%, var(--accent-strong) 100%)",
    color: "#f8fbff",
    fontWeight: 700,
    cursor: "pointer",
    boxShadow: "0 10px 18px rgba(11, 99, 206, 0.2)",
  },
  secondaryButton: {
    justifySelf: "start",
    border: "1px solid rgba(214, 224, 236, 0.16)",
    borderRadius: "0.8rem",
    padding: "0.65rem 0.9rem",
    background: "rgba(255,255,255,0.06)",
    color: "#f8fbff",
    cursor: "pointer",
  },
  errorText: {
    color: "var(--danger)",
    fontWeight: 600,
  },
};
