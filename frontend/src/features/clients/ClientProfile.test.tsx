import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Outlet, Route, Routes } from "react-router-dom";
import { NotificationProvider } from "../../app/providers/NotificationProvider";
import { IkmsThemeProvider } from "../../app/theme/IkmsThemeProvider";
import { ClientProfilePage } from "./ClientProfilePage";

describe("ClientProfilePage", () => {
  it("renders Customer360 sections, preserves tab behavior, and supports note editing", async () => {
    const setWorkspaceChrome = vi.fn();
    const user = userEvent.setup();
    stubClientProfileFetch();

    renderClientProfile(setWorkspaceChrome);

    expect(await screen.findByText("Customer Summary")).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Documents" })).toBeInTheDocument();
    expect(screen.getByText("Policy Schedule")).toBeInTheDocument();

    await user.click(screen.getByRole("tab", { name: "Notes" }));
    expect(await screen.findByText("Initial broker note")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Edit note" }));
    const textarea = await screen.findByPlaceholderText("Add a customer note");
    await user.clear(textarea);
    await user.type(textarea, "Updated broker note");
    await user.click(screen.getByRole("button", { name: "Save note" }));

    await waitFor(() => expect(screen.getByText("Updated broker note")).toBeInTheDocument());
    await waitFor(() =>
      expect(setWorkspaceChrome).toHaveBeenLastCalledWith(
        expect.objectContaining({
          title: "Alex Broker",
        }),
      ),
    );
  });
});

function renderClientProfile(setWorkspaceChrome: ReturnType<typeof vi.fn>) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  render(
    <QueryClientProvider client={queryClient}>
      <IkmsThemeProvider>
        <NotificationProvider>
          <MemoryRouter initialEntries={["/clients/client-1"]}>
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
                <Route path="/clients/:clientId" element={<ClientProfilePage />} />
              </Route>
            </Routes>
          </MemoryRouter>
        </NotificationProvider>
      </IkmsThemeProvider>
    </QueryClientProvider>,
  );
}

function stubClientProfileFetch() {
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
      primaryEmail: "alex@example.com",
      primaryPhone: null,
      contactPerson: null,
      createdAt: "2026-07-10T10:00:00Z",
      updatedAt: "2026-07-10T10:00:00Z",
    }), { status: 200, headers: { "content-type": "application/json" } }))
    .mockResolvedValueOnce(new Response(JSON.stringify([
      {
        id: "note-1",
        clientId: "client-1",
        noteText: "Initial broker note",
        status: "ACTIVE",
        createdAt: "2026-07-10T10:00:00Z",
        updatedAt: "2026-07-10T10:00:00Z",
      },
    ]), { status: 200, headers: { "content-type": "application/json" } }))
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
    ]), { status: 200, headers: { "content-type": "application/json" } }))
    .mockResolvedValueOnce(new Response(JSON.stringify({
      id: "note-1",
      clientId: "client-1",
      noteText: "Updated broker note",
      status: "ACTIVE",
      createdAt: "2026-07-10T10:00:00Z",
      updatedAt: "2026-07-10T11:00:00Z",
    }), { status: 200, headers: { "content-type": "application/json" } }))
    .mockResolvedValueOnce(new Response(JSON.stringify([
      {
        id: "note-1",
        clientId: "client-1",
        noteText: "Updated broker note",
        status: "ACTIVE",
        createdAt: "2026-07-10T10:00:00Z",
        updatedAt: "2026-07-10T11:00:00Z",
      },
    ]), { status: 200, headers: { "content-type": "application/json" } }));

  vi.stubGlobal("fetch", fetchMock);
}
