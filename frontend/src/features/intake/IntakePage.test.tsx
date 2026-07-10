import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { IntakePage } from "./IntakePage";

describe("IntakePage", () => {
  it("uploads a file and shows the duplicate outcome", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });
    const user = userEvent.setup();
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify([
        { id: "client-1", clientId: "TMP-1", clientIdTemporary: true, clientType: "INDIVIDUAL", status: "ACTIVE", displayName: "Alex Broker" },
      ]), { status: 200, headers: { "content-type": "application/json" } }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        documentId: null,
        versionId: null,
        outcome: "DUPLICATE",
        reviewStatus: "PENDING_REVIEW",
        duplicateOfDocumentId: "doc-123",
      }), { status: 200, headers: { "content-type": "application/json" } }));
    vi.stubGlobal("fetch", fetchMock);

    render(
      <QueryClientProvider client={queryClient}>
        <IntakePage />
      </QueryClientProvider>,
    );

    const fileInput = document.querySelector("input[type='file']") as HTMLInputElement;
    await user.upload(fileInput, new File(["pdf"], "renewal.pdf", { type: "application/pdf" }));
    await user.click(screen.getByRole("button", { name: "Upload document" }));

    await waitFor(() => expect(screen.getByText("Duplicate blocked")).toBeInTheDocument());
    expect(screen.getByText("Duplicate of: doc-123")).toBeInTheDocument();
  });
});
