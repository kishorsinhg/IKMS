import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { ClientProfilePage } from "./ClientProfilePage";

describe("PiiVisibility", () => {
  it("shows masked client data and redacted actions for processor", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({
        id: "user-1",
        username: "processor",
        displayName: "Processor",
        email: "processor@example.com",
        status: "ACTIVE",
        roles: ["PROCESSOR"],
        permissions: ["CLIENT_VIEW", "SEARCH_CLIENT_KNOWLEDGE", "ASK_CLIENT_AI", "VIEW_REDACTED_DOCUMENTS"],
      }), { status: 200, headers: { "content-type": "application/json" } }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        id: "client-1",
        clientId: "TMP-1",
        clientIdTemporary: true,
        clientType: "INDIVIDUAL",
        status: "ACTIVE",
        displayName: "Alex Broker",
        legalName: null,
        primaryEmail: "a***@example.com",
        primaryPhone: "***-***-0100",
        contactPerson: "A***",
        createdAt: "2026-07-10T10:00:00Z",
        updatedAt: "2026-07-10T10:00:00Z",
      }), { status: 200, headers: { "content-type": "application/json" } }))
      .mockResolvedValueOnce(new Response(JSON.stringify([]), { status: 200, headers: { "content-type": "application/json" } }))
      .mockResolvedValueOnce(new Response(JSON.stringify([
        {
          id: "doc-1",
          clientId: "client-1",
          title: "Policy Schedule",
          source: "MANUAL_UPLOAD",
          processingStatus: "CLASSIFIED",
          reviewStatus: "APPROVED",
          redactionStatus: "AVAILABLE",
          containsPii: true,
          currentVersionId: "ver-1",
          parentEmailId: null,
          createdAt: "2026-07-10T10:00:00Z",
        },
      ]), { status: 200, headers: { "content-type": "application/json" } }))
      .mockResolvedValueOnce(new Response(JSON.stringify([
        {
          id: "email-1",
          clientId: "client-1",
          subject: "Renewal reminder",
          sender: "c***@example.com",
          recipients: "a***@example.com",
          processingStatus: "LINKED",
          reviewStatus: "APPROVED",
          receivedAt: "2026-07-10T10:00:00Z",
        },
      ]), { status: 200, headers: { "content-type": "application/json" } }));
    vi.stubGlobal("fetch", fetchMock);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/clients/client-1"]}>
          <Routes>
            <Route path="/clients/:clientId" element={<ClientProfilePage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => expect(screen.getByText("Policy Schedule")).toBeInTheDocument());
    expect(screen.getByText(/a\*\*\*@example\.com/)).toBeInTheDocument();
    expect(screen.getByText("Preview redacted")).toBeInTheDocument();
    expect(screen.queryByText("Preview original")).not.toBeInTheDocument();
  });

  it("shows original access actions for supervisor", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({
        id: "user-2",
        username: "supervisor",
        displayName: "Supervisor",
        email: "supervisor@example.com",
        status: "ACTIVE",
        roles: ["SUPERVISOR"],
        permissions: ["CLIENT_VIEW", "SEARCH_CLIENT_KNOWLEDGE", "ASK_CLIENT_AI", "VIEW_REDACTED_DOCUMENTS", "VIEW_ORIGINAL_DOCUMENTS", "VIEW_PII", "VIEW_AUDIT"],
      }), { status: 200, headers: { "content-type": "application/json" } }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        id: "client-1",
        clientId: "TMP-1",
        clientIdTemporary: true,
        clientType: "INDIVIDUAL",
        status: "ACTIVE",
        displayName: "Alex Broker",
        legalName: null,
        primaryEmail: "alex@example.com",
        primaryPhone: "+1-555-0100",
        contactPerson: "Alex Broker",
        createdAt: "2026-07-10T10:00:00Z",
        updatedAt: "2026-07-10T10:00:00Z",
      }), { status: 200, headers: { "content-type": "application/json" } }))
      .mockResolvedValueOnce(new Response(JSON.stringify([]), { status: 200, headers: { "content-type": "application/json" } }))
      .mockResolvedValueOnce(new Response(JSON.stringify([
        {
          id: "doc-1",
          clientId: "client-1",
          title: "Policy Schedule",
          source: "MANUAL_UPLOAD",
          processingStatus: "CLASSIFIED",
          reviewStatus: "APPROVED",
          redactionStatus: "AVAILABLE",
          containsPii: true,
          currentVersionId: "ver-1",
          parentEmailId: null,
          createdAt: "2026-07-10T10:00:00Z",
        },
      ]), { status: 200, headers: { "content-type": "application/json" } }))
      .mockResolvedValueOnce(new Response(JSON.stringify([
        {
          id: "email-1",
          clientId: "client-1",
          subject: "Renewal reminder",
          sender: "carrier@example.com",
          recipients: "alex@example.com",
          processingStatus: "LINKED",
          reviewStatus: "APPROVED",
          receivedAt: "2026-07-10T10:00:00Z",
        },
      ]), { status: 200, headers: { "content-type": "application/json" } }));
    vi.stubGlobal("fetch", fetchMock);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/clients/client-1"]}>
          <Routes>
            <Route path="/clients/:clientId" element={<ClientProfilePage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => expect(screen.getByText("Policy Schedule")).toBeInTheDocument());
    expect(screen.getByText(/INDIVIDUAL .* alex@example\.com/)).toBeInTheDocument();
    expect(screen.getByText("Preview original")).toBeInTheDocument();
    expect(screen.getByText("Download original")).toBeInTheDocument();
  });
});
