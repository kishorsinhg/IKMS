import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, Navigate, Outlet, Route, Routes, useLocation } from "react-router-dom";
import { CurrentUser, Permission, getCurrentUser, login, logout } from "../api/auth";
import { ApiClientError } from "../api/client";
import { ClientProfilePage } from "../features/clients/ClientProfilePage";
import { ClientsWorkspacePage } from "../features/clients/ClientsWorkspacePage";
import { ClientImportPage } from "../features/clients/import/ClientImportPage";
import { IntakePage } from "../features/intake/IntakePage";
import { ReviewQueuePage } from "../features/intake/review/ReviewQueuePage";

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
          <Route path="/search" element={<PlaceholderPage title="Client search and AI Q&A" />} />
          <Route path="/administration" element={<PlaceholderPage title="Administration" />} />
          <Route path="/audit" element={<PlaceholderPage title="Audit and governance" />} />
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

  return (
    <div style={shellStyles.page}>
      <aside style={shellStyles.sidebar}>
        <div>
          <h1 style={shellStyles.productName}>IKMS</h1>
          <p style={shellStyles.caption}>Insurance knowledge workspace</p>
        </div>

        <nav style={shellStyles.nav}>
          {visibleNavItems.map((item) => (
            <Link key={item.to} to={item.to} style={shellStyles.navLink}>
              {item.label}
            </Link>
          ))}
        </nav>

        <div style={shellStyles.userCard}>
          <strong>{user.displayName}</strong>
          <span>{user.roles.join(", ")}</span>
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

function PlaceholderPage({ title }: { title: string }) {
  const user = useCurrentUser().data as CurrentUser;

  return (
    <section>
      <h2>{title}</h2>
      <p>Protected application shell is active for {user.displayName}.</p>
      <p>Permissions: {user.permissions.join(", ")}</p>
    </section>
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
    gridTemplateColumns: "280px 1fr",
    background: "linear-gradient(145deg, #f3efe6 0%, #fffdf8 55%, #ebe4d8 100%)",
    color: "#1f1c18",
  },
  sidebar: {
    display: "flex",
    flexDirection: "column",
    justifyContent: "space-between",
    padding: "2rem",
    borderRight: "1px solid rgba(31, 28, 24, 0.12)",
    background: "rgba(255, 252, 247, 0.82)",
    backdropFilter: "blur(10px)",
  },
  productName: {
    margin: 0,
    fontSize: "2rem",
    letterSpacing: "0.08em",
  },
  caption: {
    margin: "0.35rem 0 0",
    color: "#6d6253",
  },
  nav: {
    display: "grid",
    gap: "0.75rem",
    margin: "2rem 0",
  },
  navLink: {
    color: "#1f1c18",
    textDecoration: "none",
    fontWeight: 600,
  },
  userCard: {
    display: "grid",
    gap: "0.35rem",
    padding: "1rem",
    borderRadius: "1rem",
    background: "#f2e8d6",
  },
  content: {
    padding: "2.5rem",
  },
  loginPage: {
    minHeight: "100vh",
    display: "grid",
    placeItems: "center",
    background: "radial-gradient(circle at top, #f2e8d6 0%, #f8f3eb 45%, #efe7da 100%)",
  },
  loginCard: {
    width: "min(420px, 100%)",
    display: "grid",
    gap: "1rem",
    padding: "2rem",
    borderRadius: "1.25rem",
    background: "rgba(255, 252, 247, 0.94)",
    boxShadow: "0 24px 80px rgba(31, 28, 24, 0.08)",
  },
  loginTitle: {
    margin: 0,
    fontSize: "2rem",
  },
  field: {
    display: "grid",
    gap: "0.35rem",
  },
  primaryButton: {
    border: "none",
    borderRadius: "999px",
    padding: "0.85rem 1rem",
    background: "#1f1c18",
    color: "#fffaf3",
    fontWeight: 700,
    cursor: "pointer",
  },
  secondaryButton: {
    justifySelf: "start",
    border: "1px solid rgba(31, 28, 24, 0.15)",
    borderRadius: "999px",
    padding: "0.55rem 0.9rem",
    background: "transparent",
    color: "#1f1c18",
    cursor: "pointer",
  },
  errorText: {
    color: "#9f2d2d",
    fontWeight: 600,
  },
};
