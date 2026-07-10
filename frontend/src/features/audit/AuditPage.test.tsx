import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuditPage } from "./AuditPage";

describe("AuditPage", () => {
  it("renders audit events and exports csv", async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    const user = userEvent.setup();
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith("/api/auth/me")) {
        return new Response(JSON.stringify({
          id: "u1",
          username: "admin",
          displayName: "Admin User",
          email: "admin@ikms.local",
          status: "ACTIVE",
          roles: ["ADMINISTRATOR"],
          permissions: ["VIEW_AUDIT", "EXPORT_AUDIT"],
        }), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.includes("/api/audit/export")) {
        return new Response("occurredAt,actorUsername,category,action\n2026-07-10T10:00:00Z,admin,AUTHENTICATION,LOGIN_SUCCESS\n", { status: 200, headers: { "content-type": "text/csv" } });
      }
      if (url.includes("/api/audit")) {
        return new Response(JSON.stringify([{
          id: "a1",
          occurredAt: "2026-07-10T10:00:00Z",
          retainedUntil: "2033-07-09T10:00:00Z",
          actorUserId: "u1",
          actorUsername: "admin",
          clientId: "c1",
          category: "AUTHENTICATION",
          action: "LOGIN_SUCCESS",
          outcome: "SUCCESS",
          targetType: "Session",
          targetId: "current",
          piiAccess: false,
          details: { ipAddress: "127.0.0.1" },
        }]), { status: 200, headers: { "content-type": "application/json" } });
      }
      return new Response("not found", { status: 404 });
    });
    vi.stubGlobal("fetch", fetchMock);

    render(
      <QueryClientProvider client={queryClient}>
        <AuditPage />
      </QueryClientProvider>,
    );

    await waitFor(() => expect(screen.getByRole("heading", { name: "Audit and governance" })).toBeInTheDocument());
    await waitFor(() => expect(screen.getByText("LOGIN_SUCCESS")).toBeInTheDocument());
    await waitFor(() => expect(screen.getByRole("button", { name: "Export CSV" })).toBeEnabled());
    expect(screen.getByText(/Details: ipAddress=127.0.0.1/)).toBeInTheDocument();

    await user.type(screen.getByPlaceholderText("Actor username"), "admin");
    await user.click(screen.getByRole("button", { name: "Search audit" }));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/audit?actor=admin",
      expect.objectContaining({ credentials: "include", method: "GET" }),
    ));

    await user.click(screen.getByRole("button", { name: "Export CSV" }));
    await waitFor(() => expect(screen.getByText(/occurredAt,actorUsername,category,action/)).toBeInTheDocument());
  });
});
