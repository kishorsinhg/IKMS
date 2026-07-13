import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReviewQueuePage } from "./ReviewQueuePage";

describe("ReviewQueuePage", () => {
  it("loads and resolves review queue items", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });
    const user = userEvent.setup();
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.endsWith("/api/review-queue?status=OPEN")) {
        return new Response(JSON.stringify([
          { id: "item-1", itemType: "DOCUMENT", itemId: "doc-1", reason: "UNLINKED", status: "OPEN", assignedTo: null, title: "Inbound renewal", clientId: null, documentTypeId: null, metadataValues: {} },
        ]), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/review-queue/item-1")) {
        return new Response(JSON.stringify({ id: "item-1", itemType: "DOCUMENT", itemId: "doc-1", reason: "UNLINKED", status: "OPEN", assignedTo: null, title: "Inbound renewal", clientId: null, documentTypeId: null, metadataValues: { carrier: "Carrier A" } }), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/clients")) {
        return new Response(JSON.stringify([
          { id: "client-1", clientId: "TMP-1", clientIdTemporary: true, clientType: "INDIVIDUAL", status: "ACTIVE", displayName: "Alex Broker" },
        ]), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/admin/document-types")) {
        return new Response(JSON.stringify([
          { id: "doc-type-1", name: "Policy", description: null, active: true, createdAt: "2026-07-10T10:00:00Z" },
        ]), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/admin/metadata-fields")) {
        return new Response(JSON.stringify([
          { id: "field-1", fieldKey: "carrier", label: "Carrier", pii: false, active: true, createdAt: "2026-07-10T10:00:00Z" },
          { id: "field-2", fieldKey: "policyNumber", label: "Policy Number", pii: true, active: true, createdAt: "2026-07-10T10:00:00Z" },
        ]), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/review-queue/item-1/link-client") && init?.method === "POST") {
        return new Response(JSON.stringify({ id: "item-1", itemType: "DOCUMENT", itemId: "doc-1", reason: "UNLINKED", status: "IN_PROGRESS", assignedTo: null, title: "Inbound renewal", clientId: "client-1", documentTypeId: null, metadataValues: { carrier: "Carrier A" } }), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/review-queue/item-1/metadata") && init?.method === "PATCH") {
        return new Response(JSON.stringify({ id: "item-1", itemType: "DOCUMENT", itemId: "doc-1", reason: "UNLINKED", status: "IN_PROGRESS", assignedTo: null, title: "Inbound renewal", clientId: "client-1", documentTypeId: "doc-type-1", metadataValues: { carrier: "Carrier A", policyNumber: "PN-42" } }), { status: 200, headers: { "content-type": "application/json" } });
      }
      return new Response(JSON.stringify([]), { status: 200, headers: { "content-type": "application/json" } });
    });
    vi.stubGlobal("fetch", fetchMock);

    render(
      <QueryClientProvider client={queryClient}>
        <ReviewQueuePage />
      </QueryClientProvider>,
    );

    await waitFor(() => expect(screen.getByRole("button", { name: "DOCUMENT · UNLINKED · OPEN" })).toBeInTheDocument());
    await user.selectOptions(screen.getByRole("combobox", { name: /link to client/i }), "client-1");
    await user.click(screen.getByRole("button", { name: "Link client" }));
    await user.selectOptions(screen.getByRole("combobox", { name: /document type/i }), "doc-type-1");
    await user.clear(screen.getByRole("textbox", { name: /policy number metadata/i }));
    await user.type(screen.getByRole("textbox", { name: /policy number metadata/i }), "PN-42");
    await user.click(screen.getByRole("button", { name: "Save metadata" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/review-queue/item-1/link-client",
      expect.objectContaining({ method: "POST" }),
    ));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/review-queue/item-1/metadata",
      expect.objectContaining({ method: "PATCH" }),
    ));
    expect(screen.getByRole("heading", { name: "Review queue" })).toBeInTheDocument();
  });
});
