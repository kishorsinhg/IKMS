import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AdminConfigurationPage } from "./AdminConfigurationPage";

describe("AdminConfigurationPage", () => {
  it("renders admin configuration areas and saves changes", async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    const user = userEvent.setup();
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.endsWith("/api/admin/users")) {
        return new Response(JSON.stringify([{ id: "u1", username: "admin", displayName: "Admin User", email: "admin@ikms.local", status: "ACTIVE", roles: ["ADMINISTRATOR"] }]), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/admin/document-types") && (!init || init.method === "GET")) {
        return new Response(JSON.stringify([{ id: "d1", name: "Policy Schedule", description: "Policy docs", active: true, createdAt: "2026-07-10T10:00:00Z" }]), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/admin/metadata-fields")) {
        return new Response(JSON.stringify([{ id: "m1", fieldKey: "carrier", label: "Carrier", pii: true, active: true, createdAt: "2026-07-10T10:00:00Z" }]), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/admin/intake/shared-folders")) {
        return new Response(JSON.stringify([{ id: "s1", path: "/network/share/incoming", active: true, createdAt: "2026-07-10T10:00:00Z" }]), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/admin/intake/mailboxes")) {
        return new Response(JSON.stringify([{ id: "mb1", name: "Claims", host: "imap.example.com", username: "claims", active: true, createdAt: "2026-07-10T10:00:00Z" }]), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/admin/review-settings") && (!init || init.method === "GET")) {
        return new Response(JSON.stringify({ id: "r1", mode: "confidence", lowConfidenceThreshold: 0.75, updatedAt: "2026-07-10T10:00:00Z" }), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/admin/ai-settings") && (!init || init.method === "GET")) {
        return new Response(JSON.stringify({ id: "a1", providerName: "mistral", modelName: "mistral-small", ocrProvider: "tesseract", active: true, updatedAt: "2026-07-10T10:00:00Z" }), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/admin/review-settings") && init?.method === "PATCH") {
        return new Response(JSON.stringify({ id: "r1", mode: "manual", lowConfidenceThreshold: 0.82, updatedAt: "2026-07-10T10:00:00Z" }), { status: 200, headers: { "content-type": "application/json" } });
      }
      return new Response(JSON.stringify({}), { status: 200, headers: { "content-type": "application/json" } });
    });
    vi.stubGlobal("fetch", fetchMock);

    render(
      <QueryClientProvider client={queryClient}>
        <AdminConfigurationPage />
      </QueryClientProvider>,
    );

    await waitFor(() => expect(screen.getByRole("heading", { name: "Administration" })).toBeInTheDocument());
    expect(screen.getByText("Users and roles")).toBeInTheDocument();
    expect(screen.getByText("Document types")).toBeInTheDocument();
    expect(screen.getByText("Metadata fields")).toBeInTheDocument();
    expect(screen.getByText("Shared folder paths")).toBeInTheDocument();
    expect(screen.getByText("IMAP mailboxes")).toBeInTheDocument();
    expect(screen.getByText("Review mode")).toBeInTheDocument();
    expect(screen.getByText("AI/OCR provider settings")).toBeInTheDocument();

    await user.clear(screen.getByPlaceholderText("confidence"));
    await user.type(screen.getByPlaceholderText("confidence"), "manual");
    await user.clear(screen.getByPlaceholderText("0.75"));
    await user.type(screen.getByPlaceholderText("0.75"), "0.82");
    await user.click(screen.getByRole("button", { name: "Save review settings" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/admin/review-settings",
      expect.objectContaining({ method: "PATCH" }),
    ));
  });
});
