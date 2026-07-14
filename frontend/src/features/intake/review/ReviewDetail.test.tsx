import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Outlet, Route, Routes } from "react-router-dom";
import { NotificationProvider } from "../../../app/providers/NotificationProvider";
import { IkmsThemeProvider } from "../../../app/theme/IkmsThemeProvider";
import { ReviewDetailPage } from "./ReviewDetailPage";

describe("ReviewDetailPage", () => {
  it("renders the review detail workspace with metadata and context", async () => {
    mockViewport(1440);
    const setWorkspaceChrome = vi.fn();
    stubReviewDetailFetch();

    renderReviewDetailPage({
      initialEntry: "/review-queue/review-2?status=OPEN&page=0",
      setWorkspaceChrome,
    });

    expect(await screen.findByText("ACORD 125 Property Schedule")).toBeInTheDocument();
    expect(screen.getByText("Document Viewer")).toBeInTheDocument();
    expect(screen.getByText("Extracted Metadata")).toBeInTheDocument();

    await waitFor(() =>
      expect(setWorkspaceChrome).toHaveBeenLastCalledWith(
        expect.objectContaining({
          contextTitle: "Evidence Assistant",
        }),
      ),
    );
  });

  it("supports metadata save and approve actions", async () => {
    mockViewport(1440);
    const user = userEvent.setup();
    const fetchMock = stubReviewDetailFetch();

    renderReviewDetailPage({
      initialEntry: "/review-queue/review-2",
      setWorkspaceChrome: vi.fn(),
    });

    expect(await screen.findByDisplayValue("ACORD 125 Property Schedule")).toBeInTheDocument();

    await user.click(screen.getAllByRole("button", { name: "Edit metadata" })[0]!);
    await user.clear(screen.getByLabelText("Title"));
    await user.type(screen.getByLabelText("Title"), "Updated schedule");
    await user.clear(screen.getByLabelText("Carrier"));
    await user.type(screen.getByLabelText("Carrier"), "Updated Carrier");
    await user.click(screen.getByRole("button", { name: "Save changes" }));

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "http://localhost:8080/api/review-queue/review-2/metadata",
        expect.objectContaining({ method: "PATCH" }),
      ),
    );

    await user.keyboard("{Control>}{Enter}{/Control}");
    const approveDialog = await screen.findByRole("dialog", { name: "Approve review item" });
    await user.click(within(approveDialog).getByRole("button", { name: "Approve" }));

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "http://localhost:8080/api/review-queue/review-2/approve",
        expect.objectContaining({ method: "POST" }),
      ),
    );
  });

  it("opens the context drawer on mobile", async () => {
    mockViewport(390);
    const user = userEvent.setup();
    stubReviewDetailFetch();

    renderReviewDetailPage({
      initialEntry: "/review-queue/review-2",
      setWorkspaceChrome: vi.fn(),
    });

    expect(await screen.findByText("ACORD 125 Property Schedule")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "More actions" }));
    await user.click(screen.getByRole("menuitem", { name: "Open context" }));

    expect(await screen.findByText("Evidence Assistant")).toBeInTheDocument();
    expect(screen.getByText("Workflow Status")).toBeInTheDocument();
  });
});

function renderReviewDetailPage({
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
                <Route path="/review-queue/:reviewId" element={<ReviewDetailPage />} />
              </Route>
            </Routes>
          </MemoryRouter>
        </NotificationProvider>
      </IkmsThemeProvider>
    </QueryClientProvider>,
  );
}

function stubReviewDetailFetch() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);

    if (url.endsWith("/api/auth/me")) {
      return jsonResponse({
        id: "user-supervisor",
        username: "supervisor",
        displayName: "Sloane Reyes",
        email: "sloane.reyes@harborcrestbrokers.com",
        status: "ACTIVE",
        roles: ["SUPERVISOR"],
        permissions: [
          "CLIENT_VIEW",
          "REVIEW_QUEUE_ACCESS",
          "INTAKE_ACCESS",
          "SEARCH_CLIENT_KNOWLEDGE",
          "ASK_CLIENT_AI",
          "VIEW_REDACTED_DOCUMENTS",
          "VIEW_ORIGINAL_DOCUMENTS",
          "VIEW_PII",
          "VIEW_AUDIT",
        ],
      });
    }
    if (url.endsWith("/api/review-queue/review-2")) {
      return jsonResponse({
        id: "review-2",
        itemType: "DOCUMENT",
        itemId: "doc-silverridge-accord",
        reason: "LOW_EXTRACTION_CONFIDENCE",
        status: "OPEN",
        assignedTo: "processor",
        title: "ACORD 125 Property Schedule",
        clientId: "client-silverridge",
        documentTypeId: "doc-type-accord",
        metadataValues: {
          carrier: "Mountain West Indemnity",
          policyNumber: "TMP-PROP-90814",
        },
      });
    }
    if (url.endsWith("/api/admin/document-types")) {
      return jsonResponse([
        { id: "doc-type-accord", name: "ACORD Form", description: null, active: true, createdAt: "2026-07-10T10:00:00Z" },
      ]);
    }
    if (url.endsWith("/api/admin/metadata-fields")) {
      return jsonResponse([
        { id: "field-1", fieldKey: "carrier", label: "Carrier", pii: false, active: true, createdAt: "2026-07-10T10:00:00Z" },
        { id: "field-2", fieldKey: "policyNumber", label: "Policy Number", pii: false, active: true, createdAt: "2026-07-10T10:00:00Z" },
      ]);
    }
    if (url.endsWith("/api/clients")) {
      return jsonResponse([
        { id: "client-silverridge", clientId: "C-200", clientIdTemporary: false, clientType: "BUSINESS", status: "ACTIVE", displayName: "Silver Ridge Hospitality" },
      ]);
    }
    if (url.endsWith("/api/clients/client-silverridge")) {
      return jsonResponse({
        id: "client-silverridge",
        clientId: "C-200",
        clientIdTemporary: false,
        clientType: "BUSINESS",
        status: "ACTIVE",
        displayName: "Silver Ridge Hospitality",
        legalName: "Silver Ridge Hospitality LLC",
        primaryEmail: "ops@silverridge.example",
        primaryPhone: "555-0100",
        contactPerson: "Jules Hart",
        createdAt: "2026-07-01T10:00:00Z",
        updatedAt: "2026-07-13T10:00:00Z",
      });
    }
    if (url.endsWith("/api/clients/client-silverridge/documents")) {
      return jsonResponse([
        {
          id: "doc-silverridge-accord",
          clientId: "client-silverridge",
          title: "ACORD 125 Property Schedule",
          source: "EMAIL_ATTACHMENT",
          processingStatus: "COMPLETE",
          reviewStatus: "OPEN",
          redactionStatus: "AVAILABLE",
          containsPii: true,
          currentVersionId: "ver-silverridge-accord",
          parentEmailId: "email-silverridge-submission",
          createdAt: "2026-07-13T05:41:00Z",
        },
      ]);
    }
    if (url.endsWith("/api/clients/client-silverridge/emails")) {
      return jsonResponse([
        {
          id: "email-silverridge-submission",
          clientId: "client-silverridge",
          subject: "Silver Ridge submission",
          sender: "submissions@example.com",
          recipients: "ops@harborcrestbrokers.com",
          processingStatus: "COMPLETE",
          reviewStatus: "OPEN",
          receivedAt: "2026-07-13T05:40:00Z",
        },
      ]);
    }
    if (url.endsWith("/api/review-queue/review-2/metadata") && init?.method === "PATCH") {
      return jsonResponse({
        id: "review-2",
        itemType: "DOCUMENT",
        itemId: "doc-silverridge-accord",
        reason: "LOW_EXTRACTION_CONFIDENCE",
        status: "OPEN",
        assignedTo: "processor",
        title: "Updated schedule",
        clientId: "client-silverridge",
        documentTypeId: "doc-type-accord",
        metadataValues: {
          carrier: "Updated Carrier",
          policyNumber: "TMP-PROP-90814",
        },
      });
    }
    if (url.endsWith("/api/review-queue/review-2/approve") && init?.method === "POST") {
      return jsonResponse({
        id: "review-2",
        itemType: "DOCUMENT",
        itemId: "doc-silverridge-accord",
        reason: "LOW_EXTRACTION_CONFIDENCE",
        status: "RESOLVED",
        assignedTo: "processor",
        title: "Updated schedule",
        clientId: "client-silverridge",
        documentTypeId: "doc-type-accord",
        metadataValues: {
          carrier: "Updated Carrier",
          policyNumber: "TMP-PROP-90814",
        },
      });
    }
    if (url.endsWith("/api/review-queue/review-2/link-client") && init?.method === "POST") {
      return jsonResponse({});
    }
    if (url.endsWith("/api/review-queue/review-2/reject") && init?.method === "POST") {
      return jsonResponse({});
    }

    return jsonResponse([]);
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
