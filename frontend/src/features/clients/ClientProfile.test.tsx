import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Outlet, Route, Routes } from "react-router-dom";
import { NotificationProvider } from "../../app/providers/NotificationProvider";
import { IkmsThemeProvider } from "../../app/theme/IkmsThemeProvider";
import { ClientProfilePage } from "./ClientProfilePage";

describe("ClientProfilePage", () => {
  it("renders Customer360 sections, preserves tab behavior, and supports note editing", async () => {
    mockViewport(1440);
    const setWorkspaceChrome = vi.fn();
    const user = userEvent.setup();
    stubClientProfileFetch();

    renderClientProfile(setWorkspaceChrome);

    expect(await screen.findByText("Customer Summary")).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Documents" })).toBeInTheDocument();
    expect(screen.getAllByText("Policy Schedule").length).toBeGreaterThan(0);

    await user.click(screen.getByRole("tab", { name: "Relationships" }));
    expect((await screen.findAllByText("Policy Schedule")).length).toBeGreaterThan(0);

    await user.click(screen.getByRole("tab", { name: "Timeline" }));
    expect((await screen.findAllByText("Manual upload document recorded in customer knowledge.")).length).toBeGreaterThan(0);

    await user.click(screen.getByRole("tab", { name: "Documents" }));
    await user.click(screen.getByLabelText("Preview Policy Schedule"));
    expect(await screen.findByText("Evidence Workspace")).toBeInTheDocument();
    expect(await screen.findByText("Business Reference Fields")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Back" }));

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
      events: [
        {
          eventId: "timeline-1",
          customerId: "client-1",
          eventType: "DOCUMENT_CREATED",
          sourceType: "DOCUMENT",
          sourceId: "doc-1",
          sourceVersionId: "ver-1",
          title: "Policy Schedule",
          summary: "Manual upload document recorded in customer knowledge.",
          occurredAt: "2026-07-10T10:00:00Z",
          recordedAt: "2026-07-10T10:00:00Z",
          actor: "System",
          documentType: "MANUAL_UPLOAD",
          businessReferenceFields: [
            { key: "policy_number", label: "Policy Number", value: "POL-12345" },
          ],
          status: "APPROVED",
          evidenceReferences: [],
          availableActions: ["OPEN_SOURCE"],
          permissionState: "AVAILABLE",
          correlationId: "doc-1",
        },
      ],
      nextCursor: null,
      hasMore: false,
      appliedFilters: {
        query: null,
        from: null,
        to: null,
        sourceType: null,
        eventType: null,
        documentType: null,
        reviewStatus: null,
        policyNumber: null,
        claimNumber: null,
        insurer: null,
        actor: null,
        sortDirection: "DESC",
        limit: 50,
      },
    }), { status: 200, headers: { "content-type": "application/json" } }))
    .mockResolvedValueOnce(new Response(JSON.stringify({
      customerId: "client-1",
      sourceType: "CUSTOMER",
      sourceId: "client-1",
      links: [
        {
          relationshipId: "rel-1",
          customerId: "client-1",
          sourceType: "EMAIL",
          sourceId: "email-1",
          sourceTitle: "Renewal reminder",
          relatedSourceType: "DOCUMENT",
          relatedSourceId: "doc-1",
          relatedTitle: "Policy Schedule",
          relationshipType: "EMAIL_ATTACHMENT",
          score: 0.99,
          explanation: "Email includes this document as related knowledge.",
          supportingFields: { policy_number: "POL-12345" },
          evidenceReferences: [],
          derivationType: "EMAIL_ATTACHMENT",
          createdAt: "2026-07-10T10:00:00Z",
          inferred: false,
        },
      ],
      restrictedContentNotice: null,
    }), { status: 200, headers: { "content-type": "application/json" } }))
    .mockResolvedValueOnce(new Response(JSON.stringify({
      customerId: "client-1",
      sourceType: "DOCUMENT",
      sourceId: "doc-1",
      links: [
        {
          relationshipId: "rel-doc-1",
          customerId: "client-1",
          sourceType: "DOCUMENT",
          sourceId: "doc-1",
          sourceTitle: "Policy Schedule",
          relatedSourceType: "EMAIL",
          relatedSourceId: "email-1",
          relatedTitle: "Renewal reminder",
          relationshipType: "EMAIL_ATTACHMENT",
          score: 0.99,
          explanation: "Email includes this document as related knowledge.",
          supportingFields: { policy_number: "POL-12345" },
          evidenceReferences: [],
          derivationType: "EMAIL_ATTACHMENT",
          createdAt: "2026-07-10T10:00:00Z",
          inferred: false,
        },
      ],
      restrictedContentNotice: null,
    }), { status: 200, headers: { "content-type": "application/json" } }))
    .mockResolvedValueOnce(new Response(JSON.stringify([
      {
        id: "ver-1",
        documentId: "doc-1",
        versionNumber: 1,
        fileName: "policy-schedule.pdf",
        mimeType: "application/pdf",
        redactionStatus: "AVAILABLE",
        current: true,
        fileHash: "abc123",
        createdAt: "2026-07-10T10:00:00Z",
        createdBy: null,
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
