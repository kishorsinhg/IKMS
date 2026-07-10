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
          { id: "item-1", itemType: "DOCUMENT", itemId: "doc-1", reason: "UNLINKED", status: "OPEN", assignedTo: null },
        ]), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/review-queue/item-1")) {
        return new Response(JSON.stringify({ id: "item-1", itemType: "DOCUMENT", itemId: "doc-1", reason: "UNLINKED", status: "OPEN", assignedTo: null }), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/clients")) {
        return new Response(JSON.stringify([
          { id: "client-1", clientId: "TMP-1", clientIdTemporary: true, clientType: "INDIVIDUAL", status: "ACTIVE", displayName: "Alex Broker" },
        ]), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/review-queue/item-1/link-client") && init?.method === "POST") {
        return new Response(JSON.stringify({ id: "item-1", itemType: "DOCUMENT", itemId: "doc-1", reason: "UNLINKED", status: "IN_PROGRESS", assignedTo: null }), { status: 200, headers: { "content-type": "application/json" } });
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

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/review-queue/item-1/link-client",
      expect.objectContaining({ method: "POST" }),
    ));
    expect(screen.getByRole("heading", { name: "Review queue" })).toBeInTheDocument();
  });
});
