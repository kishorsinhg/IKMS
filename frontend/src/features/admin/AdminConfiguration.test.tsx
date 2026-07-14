import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Outlet, Route, Routes } from "react-router-dom";
import { NotificationProvider } from "../../app/providers/NotificationProvider";
import { IkmsThemeProvider } from "../../app/theme/IkmsThemeProvider";
import { AdminConfigurationPage } from "./AdminConfigurationPage";

describe("AdminConfigurationPage", () => {
  it("renders the administration grid, switches modules, and updates supported settings", async () => {
    mockViewport(1440);
    const user = userEvent.setup();
    const setWorkspaceChrome = vi.fn();
    const fetchMock = stubAdminFetch();

    renderAdminPage({ setWorkspaceChrome });

    expect(await screen.findByRole("grid")).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: /Open configuration Admin User/i })).toBeInTheDocument();

    await user.click(screen.getByRole("combobox", { name: "Configuration Type" }));
    await user.click(screen.getByRole("option", { name: "Permission Groups" }));

    expect(await screen.findByRole("button", { name: /Open configuration Review Policy/i })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Open configuration Review Policy/i }));
    expect(await screen.findByRole("button", { name: "Save review settings" })).toBeInTheDocument();

    await user.clear(screen.getByLabelText("Review Mode"));
    await user.type(screen.getByLabelText("Review Mode"), "manual");
    await user.clear(screen.getByLabelText("Low Confidence Threshold"));
    await user.type(screen.getByLabelText("Low Confidence Threshold"), "0.82");
    await user.click(screen.getByRole("button", { name: "Save review settings" }));

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "http://localhost:8080/api/admin/review-settings",
        expect.objectContaining({
          method: "PATCH",
          body: JSON.stringify({ mode: "manual", lowConfidenceThreshold: 0.82 }),
        }),
      ),
    );

    await user.click(screen.getByRole("combobox", { name: "Configuration Type" }));
    await user.click(screen.getByRole("option", { name: "AI Configuration" }));

    await user.click(await screen.findByRole("button", { name: /Open configuration mistral/i }));

    await user.clear(screen.getByLabelText("AI Provider"));
    await user.type(screen.getByLabelText("AI Provider"), "openai");
    await user.clear(screen.getByLabelText("Chat / Classification Model"));
    await user.type(screen.getByLabelText("Chat / Classification Model"), "gpt-5-mini");
    await user.clear(screen.getByLabelText("Embedding Model"));
    await user.type(screen.getByLabelText("Embedding Model"), "text-embedding-3-large");
    await user.clear(screen.getByLabelText("API Base URL"));
    await user.type(screen.getByLabelText("API Base URL"), "https://api.openai.com/v1");
    await user.clear(screen.getByLabelText("API Key"));
    await user.type(screen.getByLabelText("API Key"), "new-secret");
    await user.click(screen.getByRole("button", { name: "Validate AI settings" }));

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "http://localhost:8080/api/admin/ai-settings/validate",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({
            providerName: "openai",
            modelName: "gpt-5-mini",
            embeddingModelName: "text-embedding-3-large",
            apiBaseUrl: "https://api.openai.com/v1",
            apiKey: "new-secret",
            ocrProvider: "tesseract",
            active: true,
          }),
        }),
      ),
    );

    await user.click(screen.getByRole("button", { name: "Save AI settings" }));

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "http://localhost:8080/api/admin/ai-settings",
        expect.objectContaining({
          method: "PATCH",
          body: JSON.stringify({
            providerName: "openai",
            modelName: "gpt-5-mini",
            embeddingModelName: "text-embedding-3-large",
            apiBaseUrl: "https://api.openai.com/v1",
            apiKey: "new-secret",
            ocrProvider: "tesseract",
            active: true,
          }),
        }),
      ),
    );

    await waitFor(() =>
      expect(setWorkspaceChrome).toHaveBeenLastCalledWith(
        expect.objectContaining({
          title: "Administration",
        }),
      ),
    );
  });

  it("uses the mobile list pattern and opens the editor drawer from a selected item", async () => {
    mockViewport(390);
    const user = userEvent.setup();
    stubAdminFetch();

    renderAdminPage({ setWorkspaceChrome: vi.fn() });

    expect(await screen.findByText("Admin User")).toBeInTheDocument();
    expect(screen.queryByRole("grid")).not.toBeInTheDocument();

    const item = await screen.findByText("Admin User");
    await user.click(item);

    expect(await screen.findByText(/Assigned roles: ADMINISTRATOR/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Close" })).toBeInTheDocument();
  });
});

function renderAdminPage({ setWorkspaceChrome }: { setWorkspaceChrome: ReturnType<typeof vi.fn> }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <IkmsThemeProvider>
        <NotificationProvider>
          <MemoryRouter initialEntries={["/administration"]}>
            <Routes>
              <Route
                element={(
                  <Outlet
                    context={{
                      setWorkspaceChrome,
                      clearWorkspaceChrome: vi.fn(),
                    }}
                  />
                )}
              >
                <Route path="/administration" element={<AdminConfigurationPage />} />
              </Route>
            </Routes>
          </MemoryRouter>
        </NotificationProvider>
      </IkmsThemeProvider>
    </QueryClientProvider>,
  );
}

function stubAdminFetch() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith("/api/admin/users")) {
      return jsonResponse([{ id: "u1", username: "admin", displayName: "Admin User", email: "admin@ikms.local", status: "ACTIVE", roles: ["ADMINISTRATOR"] }]);
    }
    if (url.endsWith("/api/admin/document-types") && (!init || init.method === "GET")) {
      return jsonResponse([{ id: "d1", name: "Policy Schedule", description: "Policy docs", active: true, createdAt: "2026-07-10T10:00:00Z" }]);
    }
    if (url.endsWith("/api/admin/metadata-fields") && (!init || init.method === "GET")) {
      return jsonResponse([{ id: "m1", fieldKey: "carrier", label: "Carrier", pii: true, active: true, createdAt: "2026-07-10T10:00:00Z" }]);
    }
    if (url.endsWith("/api/admin/intake/shared-folders") && (!init || init.method === "GET")) {
      return jsonResponse([{ id: "s1", path: "/network/share/incoming", active: true, createdAt: "2026-07-10T10:00:00Z" }]);
    }
    if (url.endsWith("/api/admin/intake/mailboxes") && (!init || init.method === "GET")) {
      return jsonResponse([{ id: "mb1", name: "Claims", host: "imap.example.com", username: "claims", active: true, createdAt: "2026-07-10T10:00:00Z" }]);
    }
    if (url.endsWith("/api/admin/review-settings") && (!init || init.method === "GET")) {
      return jsonResponse({ id: "r1", mode: "confidence", lowConfidenceThreshold: 0.75, updatedAt: "2026-07-10T10:00:00Z" });
    }
    if (url.endsWith("/api/admin/ai-settings") && (!init || init.method === "GET")) {
      return jsonResponse({
        id: "a1",
        providerName: "mistral",
        modelName: "mistral-small",
        embeddingModelName: "mistral-embed",
        apiBaseUrl: "https://llm.internal/v1",
        apiKeyConfigured: true,
        ocrProvider: "tesseract",
        active: true,
        updatedAt: "2026-07-10T10:00:00Z",
      });
    }
    if (url.endsWith("/api/admin/review-settings") && init?.method === "PATCH") {
      return jsonResponse({ id: "r1", mode: "manual", lowConfidenceThreshold: 0.82, updatedAt: "2026-07-10T10:00:00Z" });
    }
    if (url.endsWith("/api/admin/ai-settings") && init?.method === "PATCH") {
      return jsonResponse({
        id: "a1",
        providerName: "openai",
        modelName: "gpt-5-mini",
        embeddingModelName: "text-embedding-3-large",
        apiBaseUrl: "https://api.openai.com/v1",
        apiKeyConfigured: true,
        ocrProvider: "tesseract",
        active: true,
        updatedAt: "2026-07-10T10:00:00Z",
      });
    }
    if (url.endsWith("/api/admin/ai-settings/validate") && init?.method === "POST") {
      return jsonResponse({
        valid: true,
        chatModelReachable: true,
        embeddingModelReachable: true,
        ocrProviderSupported: true,
        status: "READY",
        message: "Provider configuration validated.",
        checkedAt: "2026-07-10T10:05:00Z",
      });
    }
    return jsonResponse({});
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
