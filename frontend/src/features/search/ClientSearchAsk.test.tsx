import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { ClientProfilePage } from "../clients/ClientProfilePage";

describe("ClientSearchAsk", () => {
  it("searches and asks within one client profile", async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });
    const user = userEvent.setup();
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.endsWith("/api/auth/me")) {
        return new Response(JSON.stringify({
          id: "user-1",
          username: "processor",
          displayName: "Processor",
          email: "processor@example.com",
          status: "ACTIVE",
          roles: ["PROCESSOR"],
          permissions: ["CLIENT_VIEW", "SEARCH_CLIENT_KNOWLEDGE", "ASK_CLIENT_AI", "VIEW_REDACTED_DOCUMENTS"],
        }), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/clients/client-1")) {
        return new Response(JSON.stringify({
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
        }), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/clients/client-1/notes")) {
        return new Response(JSON.stringify([]), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/clients/client-1/documents")) {
        return new Response(JSON.stringify([]), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/clients/client-1/emails")) {
        return new Response(JSON.stringify([]), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.includes("/api/clients/client-1/search?query=renewal")) {
        return new Response(JSON.stringify([
          {
            sourceType: "DOCUMENT",
            sourceId: "doc-1",
            title: "Policy Schedule",
            excerpt: "renewal due next month",
            citation: "Document: Policy Schedule",
            occurredAt: "2026-07-10T10:00:00Z",
          },
        ]), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/clients/client-1/ask") && init?.method === "POST") {
        return new Response(JSON.stringify({
          interactionId: "interaction-1",
          status: "Answered",
          answer: "Policy Schedule: renewal due next month",
          citations: [
            {
              sourceType: "DOCUMENT",
              sourceId: "doc-1",
              title: "Policy Schedule",
              excerpt: "renewal due next month",
            },
          ],
          createdAt: "2026-07-10T10:00:00Z",
        }), { status: 200, headers: { "content-type": "application/json" } });
      }
      if (url.endsWith("/api/ai-interactions/interaction-1/feedback")) {
        return new Response("", { status: 200, headers: { "content-type": "text/plain" } });
      }
      return new Response(JSON.stringify([]), { status: 200, headers: { "content-type": "application/json" } });
    });
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

    await waitFor(() => expect(screen.getByRole("heading", { name: "AI Q&A" })).toBeInTheDocument());
    await user.type(screen.getByPlaceholderText("Search documents, emails, and notes"), "renewal");
    await waitFor(() => expect(screen.getByText("Policy Schedule")).toBeInTheDocument());

    await user.type(screen.getByPlaceholderText("Ask an evidence-based question about this client"), "What is due next month?");
    await user.click(screen.getByRole("button", { name: "Ask client AI" }));

    await waitFor(() => expect(screen.getByText("Answered")).toBeInTheDocument());
    expect(screen.getByText("Policy Schedule: renewal due next month")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Helpful" })).toBeInTheDocument();
  });
});
