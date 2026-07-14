import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Outlet, Route, Routes } from "react-router-dom";
import { NotificationProvider } from "../../app/providers/NotificationProvider";
import { IkmsThemeProvider } from "../../app/theme/IkmsThemeProvider";
import { AuditPage } from "./AuditPage";

describe("AuditPage", () => {
  it("renders the audit grid, applies filters, and exports csv", async () => {
    mockViewport(1440);
    const user = userEvent.setup();
    const setWorkspaceChrome = vi.fn();
    const fetchMock = stubAuditFetch();

    renderAuditPage({ initialEntry: "/audit", setWorkspaceChrome });

    expect(await screen.findByRole("grid")).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: /Open audit event Login Success/i })).toBeInTheDocument();

    await user.type(screen.getByLabelText("Actor"), "admin");
    await user.click(screen.getByRole("button", { name: "Search" }));

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "http://localhost:8080/api/audit?actor=admin",
        expect.objectContaining({ credentials: "include", method: "GET" }),
      ),
    );

    await user.click(screen.getByRole("button", { name: "More actions" }));
    await user.click(screen.getByRole("menuitem", { name: "Export CSV" }));

    const exportDialog = await screen.findByRole("dialog", { name: "Audit CSV preview" });
    expect(within(exportDialog).getByText(/occurredAt,actor,category,action/)).toBeInTheDocument();

    await waitFor(() =>
      expect(setWorkspaceChrome).toHaveBeenLastCalledWith(
        expect.objectContaining({
          contextTitle: "Selected Audit Event",
        }),
      ),
    );
  });

  it("switches to the mobile list pattern and opens selected event detail", async () => {
    mockViewport(390);
    const user = userEvent.setup();
    stubAuditFetch();

    renderAuditPage({ initialEntry: "/audit", setWorkspaceChrome: vi.fn() });

    const eventMatches = await screen.findAllByText("Login Success");
    expect(eventMatches.length).toBeGreaterThan(0);
    expect(screen.queryByRole("grid")).not.toBeInTheDocument();

    await user.click(eventMatches[0]);

    expect(await screen.findByText("Selected audit event")).toBeInTheDocument();
    expect(screen.getByText("Audit Details")).toBeInTheDocument();
  });
});

function renderAuditPage({
  initialEntry,
  setWorkspaceChrome,
}: {
  initialEntry: string;
  setWorkspaceChrome: ReturnType<typeof vi.fn>;
}) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <IkmsThemeProvider>
        <NotificationProvider>
          <MemoryRouter initialEntries={[initialEntry]}>
            <Routes>
              <Route
                element={
                  <Outlet
                    context={{
                      setWorkspaceChrome,
                      clearWorkspaceChrome: vi.fn(),
                    }}
                  />
                }
              >
                <Route path="/audit" element={<AuditPage />} />
              </Route>
            </Routes>
          </MemoryRouter>
        </NotificationProvider>
      </IkmsThemeProvider>
    </QueryClientProvider>,
  );
}

function stubAuditFetch() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);

    if (url.endsWith("/api/auth/me")) {
      return jsonResponse({
        id: "u1",
        username: "admin",
        displayName: "Admin User",
        email: "admin@ikms.local",
        status: "ACTIVE",
        roles: ["ADMINISTRATOR"],
        permissions: ["VIEW_AUDIT", "EXPORT_AUDIT", "CLIENT_VIEW"],
      });
    }
    if (url.includes("/api/audit/export")) {
      return new Response(
        "occurredAt,actor,category,action\n2026-07-10T10:00:00Z,admin,AUTHENTICATION,LOGIN_SUCCESS\n",
        { status: 200, headers: { "content-type": "text/csv" } },
      );
    }
    if (url.includes("/api/audit")) {
      return jsonResponse([
        {
          id: "a1",
          occurredAt: "2026-07-10T10:00:00Z",
          retainedUntil: "2033-07-09T10:00:00Z",
          actorUserId: "u1",
          actorUsername: "admin",
          clientId: "client-1",
          category: "AUTHENTICATION",
          action: "LOGIN_SUCCESS",
          outcome: "SUCCESS",
          targetType: "SESSION",
          targetId: "current",
          piiAccess: false,
          details: { ipAddress: "127.0.0.1" },
        },
        {
          id: "a2",
          occurredAt: "2026-07-11T09:00:00Z",
          retainedUntil: "2033-07-10T09:00:00Z",
          actorUserId: "u2",
          actorUsername: "processor",
          clientId: "client-2",
          category: "NOTE",
          action: "CREATE_NOTE",
          outcome: "SUCCESS",
          targetType: "CLIENT",
          targetId: "client-2",
          piiAccess: false,
          details: { policyNumber: "PN-42", correlationId: "corr-1" },
        },
      ]);
    }
    return new Response("not found", { status: 404 });
  });

  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function jsonResponse(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json" },
  });
}

function mockViewport(width: number) {
  Object.defineProperty(window, "matchMedia", {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
      matches: matchesMediaQuery(query, width),
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  });
}

function matchesMediaQuery(query: string, width: number) {
  const min = query.match(/min-width:\s*([0-9.]+)px/);
  const max = query.match(/max-width:\s*([0-9.]+)px/);
  const minMatches = min ? width >= Number(min[1]) : true;
  const maxMatches = max ? width <= Number(max[1]) : true;
  return minMatches && maxMatches;
}
