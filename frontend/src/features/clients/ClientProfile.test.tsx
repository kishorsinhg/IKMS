import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { ClientProfilePage } from "./ClientProfilePage";

describe("ClientProfilePage", () => {
  it("renders the required client profile sections", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({
        id: "client-1",
        clientId: "TMP-1",
        clientIdTemporary: true,
        clientType: "INDIVIDUAL",
        status: "ACTIVE",
        displayName: "Alex Broker",
        legalName: null,
        primaryEmail: "alex@example.com",
        primaryPhone: null,
        contactPerson: null,
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
    expect(screen.getByText("Renewal reminder")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Notes" })).toBeInTheDocument();
  });
});
