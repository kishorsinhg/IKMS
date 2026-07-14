import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Outlet, Route, Routes } from "react-router-dom";
import { NotificationProvider } from "../../../app/providers/NotificationProvider";
import { IkmsThemeProvider } from "../../../app/theme/IkmsThemeProvider";
import { ReviewQueuePage } from "./ReviewQueuePage";

describe("ReviewQueuePage", () => {
  it("renders the review grid and updates the shared context for a selected item", async () => {
    mockViewport(1440);
    const setWorkspaceChrome = vi.fn();
    stubReviewFetch();

    renderReviewPage({
      initialEntry: "/review-queue",
      setWorkspaceChrome,
    });

    expect(await screen.findByText("Inbound renewal")).toBeInTheDocument();
    expect(screen.getByRole("grid")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole("button", { name: "Open review item" })).toBeInTheDocument());

    await waitFor(() =>
      expect(setWorkspaceChrome).toHaveBeenLastCalledWith(
        expect.objectContaining({
          contextTitle: "Selected Review Item",
        }),
      ),
    );
  });

  it("supports link, metadata update, and approve actions", async () => {
    mockViewport(1440);
    const user = userEvent.setup();
    const fetchMock = stubReviewFetch();

    renderReviewPage({
      initialEntry: "/review-queue",
      setWorkspaceChrome: vi.fn(),
    });

    expect(await screen.findByText("Inbound renewal")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole("button", { name: "More actions" })).toBeInTheDocument());

    await user.click(screen.getByRole("button", { name: "More actions" }));
    await user.click(screen.getByRole("menuitem", { name: "Link customer" }));
    await user.click(screen.getByLabelText("Customer"));
    await user.click(await screen.findByRole("option", { name: "Alex Broker" }));
    await user.click(screen.getByRole("button", { name: "Link customer" }));
    await waitFor(() => expect(screen.queryByRole("dialog", { name: "Link review item to customer" })).not.toBeInTheDocument());

    await user.click(screen.getByRole("button", { name: "More actions" }));
    await user.click(screen.getByRole("menuitem", { name: "Edit metadata" }));
    await user.clear(screen.getByLabelText("Title"));
    await user.type(screen.getByLabelText("Title"), "Updated inbound renewal");
    await user.clear(screen.getByLabelText("Carrier"));
    await user.type(screen.getByLabelText("Carrier"), "Carrier A");
    await user.clear(screen.getByLabelText("Policy Number"));
    await user.type(screen.getByLabelText("Policy Number"), "PN-42");
    await user.click(screen.getByRole("button", { name: "Save metadata" }));
    await waitFor(() => expect(screen.queryByRole("dialog", { name: "Edit review metadata" })).not.toBeInTheDocument());

    await user.click(screen.getAllByRole("button", { name: "Approve" })[0]!);
    const approveDialog = await screen.findByRole("dialog", { name: "Approve review item" });
    await user.click(within(approveDialog).getByRole("button", { name: "Approve" }));
    await waitFor(() => expect(screen.queryByRole("dialog", { name: "Approve review item" })).not.toBeInTheDocument());

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "http://localhost:8080/api/review-queue/item-1/link-client",
        expect.objectContaining({ method: "POST" }),
      ),
    );
    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "http://localhost:8080/api/review-queue/item-1/metadata",
        expect.objectContaining({ method: "PATCH" }),
      ),
    );
    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "http://localhost:8080/api/review-queue/item-1/approve",
        expect.objectContaining({ method: "POST" }),
      ),
    );
  });

  it("switches to the mobile review list and opens the selected-item drawer", async () => {
    mockViewport(390);
    const user = userEvent.setup();
    stubReviewFetch();

    renderReviewPage({
      initialEntry: "/review-queue",
      setWorkspaceChrome: vi.fn(),
    });

    expect(await screen.findByText("Inbound renewal")).toBeInTheDocument();
    expect(screen.queryByRole("grid")).not.toBeInTheDocument();

    await user.click(screen.getByText("Inbound renewal"));

    expect(await screen.findByText("Selected review item")).toBeInTheDocument();
    expect(screen.getByText("Customer context")).toBeInTheDocument();
  });
});

function renderReviewPage({
  initialEntry,
  setWorkspaceChrome,
}: {
  initialEntry: string;
  setWorkspaceChrome: ReturnType<typeof vi.fn>;
}) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
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
                <Route path="/review-queue" element={<ReviewQueuePage />} />
              </Route>
            </Routes>
          </MemoryRouter>
        </NotificationProvider>
      </IkmsThemeProvider>
    </QueryClientProvider>,
  );
}

function stubReviewFetch() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith("/api/review-queue?status=OPEN")) {
      return jsonResponse([
        {
          id: "item-1",
          itemType: "DOCUMENT",
          itemId: "doc-1",
          reason: "UNLINKED",
          status: "OPEN",
          assignedTo: null,
          title: "Inbound renewal",
          clientId: null,
          documentTypeId: null,
          metadataValues: { carrier: "", policyNumber: "" },
        },
        {
          id: "item-2",
          itemType: "EMAIL",
          itemId: "email-2",
          reason: "LOW_CLIENT_CONFIDENCE",
          status: "OPEN",
          assignedTo: "processor",
          title: "Silver Ridge new submission",
          clientId: null,
          documentTypeId: null,
          metadataValues: {},
        },
        {
          id: "item-3",
          itemType: "DOCUMENT_VERSION",
          itemId: "version-3",
          reason: "DUPLICATE_UNCERTAINTY",
          status: "IN_PROGRESS",
          assignedTo: "supervisor",
          title: "Marine cargo renewal",
          clientId: "client-2",
          documentTypeId: "doc-type-1",
          metadataValues: { carrier: "Beacon" },
        },
      ]);
    }
    if (url.endsWith("/api/review-queue/item-1")) {
      return jsonResponse({
        id: "item-1",
        itemType: "DOCUMENT",
        itemId: "doc-1",
        reason: "UNLINKED",
        status: "OPEN",
        assignedTo: null,
        title: "Inbound renewal",
        clientId: null,
        documentTypeId: null,
        metadataValues: { carrier: "", policyNumber: "" },
      });
    }
    if (url.endsWith("/api/clients")) {
      return jsonResponse([
        { id: "client-1", clientId: "TMP-1", clientIdTemporary: true, clientType: "INDIVIDUAL", status: "ACTIVE", displayName: "Alex Broker" },
        { id: "client-2", clientId: "C-200", clientIdTemporary: false, clientType: "BUSINESS", status: "ACTIVE", displayName: "Beacon Holdings" },
      ]);
    }
    if (url.endsWith("/api/admin/document-types")) {
      return jsonResponse([
        { id: "doc-type-1", name: "Policy", description: null, active: true, createdAt: "2026-07-10T10:00:00Z" },
      ]);
    }
    if (url.endsWith("/api/admin/metadata-fields")) {
      return jsonResponse([
        { id: "field-1", fieldKey: "carrier", label: "Carrier", pii: false, active: true, createdAt: "2026-07-10T10:00:00Z" },
        { id: "field-2", fieldKey: "policyNumber", label: "Policy Number", pii: true, active: true, createdAt: "2026-07-10T10:00:00Z" },
      ]);
    }
    if (url.endsWith("/api/review-queue/item-1/link-client") && init?.method === "POST") {
      return jsonResponse({
        id: "item-1",
        itemType: "DOCUMENT",
        itemId: "doc-1",
        reason: "UNLINKED",
        status: "IN_PROGRESS",
        assignedTo: null,
        title: "Inbound renewal",
        clientId: "client-1",
        documentTypeId: null,
        metadataValues: { carrier: "", policyNumber: "" },
      });
    }
    if (url.endsWith("/api/review-queue/item-1/metadata") && init?.method === "PATCH") {
      return jsonResponse({
        id: "item-1",
        itemType: "DOCUMENT",
        itemId: "doc-1",
        reason: "UNLINKED",
        status: "OPEN",
        assignedTo: null,
        title: "Updated inbound renewal",
        clientId: "client-1",
        documentTypeId: "doc-type-1",
        metadataValues: { carrier: "Carrier A", policyNumber: "PN-42" },
      });
    }
    if (url.endsWith("/api/review-queue/item-1/approve") && init?.method === "POST") {
      return jsonResponse({ success: true });
    }
    if (url.endsWith("/api/review-queue/item-1/reject") && init?.method === "POST") {
      return jsonResponse({ success: true });
    }
    return jsonResponse([]);
  });

  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function jsonResponse(body: unknown) {
  return new Response(JSON.stringify(body), {
    status: 200,
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
