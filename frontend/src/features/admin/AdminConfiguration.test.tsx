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

    await user.click(screen.getByRole("combobox", { name: "Configuration Type" }));
    await user.click(screen.getByRole("option", { name: "AI Governance" }));
    await user.click(await screen.findByRole("button", { name: /Open configuration Approved Model Registry/i }));
    await user.clear(screen.getByLabelText("Prompt Policy Version"));
    await user.type(screen.getByLabelText("Prompt Policy Version"), "prompt-policy-v2");
    await user.click(screen.getByRole("button", { name: "Save AI governance" }));

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "http://localhost:8080/api/governance/ai",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({
            approvedModels: ["mistral:mistral-small", "openai:gpt-5-mini"],
            promptPolicyVersion: "prompt-policy-v2",
            responsePolicyVersion: "response-policy-v1",
            citationRequired: true,
            groundingValidationRequired: true,
          }),
        }),
      ),
    );

    await user.click(screen.getByRole("combobox", { name: "Configuration Type" }));
    await user.click(screen.getByRole("option", { name: "Queues" }));
    await user.click(await screen.findByRole("button", { name: /Open configuration Reindex Queue/i }));
    await user.click(screen.getByRole("button", { name: "Pause queue" }));

    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        "http://localhost:8080/api/operations/queues/REINDEX/pause",
        expect.objectContaining({ method: "POST" }),
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
    if (url.endsWith("/api/governance/classification") && (!init || init.method === "GET")) {
      return jsonResponse({
        levels: ["PUBLIC", "INTERNAL", "CONFIDENTIAL", "RESTRICTED", "HIGHLY_RESTRICTED"],
        defaultClassification: "INTERNAL",
        aiRestrictionThreshold: "RESTRICTED",
        exportRestrictionThreshold: "CONFIDENTIAL",
        updatedAt: "2026-07-10T10:00:00Z",
      });
    }
    if (url.endsWith("/api/governance/retention") && (!init || init.method === "GET")) {
      return jsonResponse({
        policies: [{ contentType: "CUSTOMER_DOCUMENT", retentionDays: 2555, reviewAfterDays: 1825, archivalAfterDays: 2190, disposalAfterDays: 2555 }],
        updatedAt: "2026-07-10T10:00:00Z",
      });
    }
    if (url.endsWith("/api/governance/legal-holds") && (!init || init.method === "GET")) {
      return jsonResponse([
        {
          id: "lh1",
          targetType: "DOCUMENT",
          targetId: "doc-1",
          clientId: "c1",
          legalHold: true,
          holdType: "LITIGATION",
          retentionPolicyKey: "legal-hold",
          reviewAt: "2026-08-10T10:00:00Z",
          archivalEligibleAt: null,
          disposalEligibleAt: null,
          executedAt: "2026-07-10T10:00:00Z",
          reason: "Open litigation",
        },
      ]);
    }
    if (url.endsWith("/api/governance/ai") && (!init || init.method === "GET")) {
      return jsonResponse({
        approvedModels: ["mistral:mistral-small", "openai:gpt-5-mini"],
        promptPolicyVersion: "prompt-policy-v1",
        responsePolicyVersion: "response-policy-v1",
        citationRequired: true,
        groundingValidationRequired: true,
        updatedAt: "2026-07-10T10:00:00Z",
      });
    }
    if (url.endsWith("/api/governance/security") && (!init || init.method === "GET")) {
      return jsonResponse({
        encryptionAtRest: "AES-256",
        encryptionInTransit: "TLS 1.2+",
        keyManagement: "Vendor-neutral KMS abstraction",
        secretManagement: "Environment-backed secret store",
        exportApprovalRequired: true,
        watermarkByDefault: true,
        updatedAt: "2026-07-10T10:00:00Z",
      });
    }
    if (url.endsWith("/api/governance/reports/compliance") && (!init || init.method === "GET")) {
      return jsonResponse({
        activeLegalHolds: 1,
        retentionExceptions: 0,
        sensitiveDocuments: 4,
        restrictedDocuments: 2,
        piiAuditEvents: 3,
        exportEvents: 1,
        aiInteractions: 5,
        stewardshipSignals: ["Customer remains the primary business context."],
      });
    }
    if (url.endsWith("/api/operations/jobs") && (!init || init.method === "GET")) {
      return jsonResponse([
        {
          jobId: "job-1",
          jobType: "FULL_PROJECTION_REBUILD",
          submittedBy: "admin-user",
          submittedAt: "2026-07-15T10:00:00Z",
          startedAt: "2026-07-15T10:01:00Z",
          completedAt: null,
          duration: null,
          status: "RUNNING",
          progress: 42,
          errorSummary: null,
          retryCount: 0,
          queueKey: "REINDEX",
          targetType: "PROJECTION",
          targetId: "ALL",
          priority: 100,
          cancelRequested: false,
          details: { scope: "full" },
        },
        {
          jobId: "job-2",
          jobType: "OCR_RETRY",
          submittedBy: "admin-user",
          submittedAt: "2026-07-15T09:00:00Z",
          startedAt: null,
          completedAt: "2026-07-15T09:05:00Z",
          duration: 300000,
          status: "FAILED",
          progress: 100,
          errorSummary: "OCR timeout",
          retryCount: 1,
          queueKey: "OCR",
          targetType: "DOCUMENT",
          targetId: "doc-44",
          priority: 90,
          cancelRequested: false,
          details: { scope: "document" },
        },
      ]);
    }
    if (url.endsWith("/api/operations/queues") && (!init || init.method === "GET")) {
      return jsonResponse([
        {
          queueKey: "REINDEX",
          queueName: "Reindex Queue",
          status: "RUNNING",
          paused: false,
          depth: 2,
          runningItems: 1,
          failedItems: 0,
          updatedAt: "2026-07-15T10:02:00Z",
          explanation: "Controls reindex and projection rebuild jobs.",
        },
      ]);
    }
    if (url.endsWith("/api/operations/queues/REINDEX/pause") && init?.method === "POST") {
      return jsonResponse({
        queueKey: "REINDEX",
        queueName: "Reindex Queue",
        status: "PAUSED",
        paused: true,
        depth: 2,
        runningItems: 0,
        failedItems: 0,
        updatedAt: "2026-07-15T10:03:00Z",
        explanation: "Controls reindex and projection rebuild jobs.",
      });
    }
    if (url.endsWith("/api/operations/schedulers") && (!init || init.method === "GET")) {
      return jsonResponse([
        {
          schedulerKey: "nightly-reindex",
          displayName: "Nightly Reindex",
          description: "Refresh retrieval projections.",
          enabled: true,
          nextExecution: "2026-07-16T00:00:00Z",
          lastExecution: "2026-07-15T00:00:00Z",
          lastStatus: "COMPLETED",
          history: [],
        },
      ]);
    }
    if (url.endsWith("/api/operations/cache") && (!init || init.method === "GET")) {
      return jsonResponse([
        {
          cacheKey: "retrieval-cache",
          displayName: "Retrieval cache",
          entryCount: 0,
          lastAction: "READY",
          lastActionAt: "2026-07-15T10:00:00Z",
        },
      ]);
    }
    if (url.endsWith("/api/operations/health") && (!init || init.method === "GET")) {
      return jsonResponse({
        overallStatus: "WARNING",
        components: [
          {
            component: "Database",
            status: "HEALTHY",
            explanation: "Spring Data repositories are available.",
          },
        ],
      });
    }
    if (url.endsWith("/api/operations/diagnostics") && (!init || init.method === "GET")) {
      return jsonResponse({
        systemInformation: {
          javaVersion: "21",
          retrievalImplementation: "PostgreSQL + pgvector",
        },
        activeWorkers: {
          runningOperationsJobs: 1,
          runningDocumentJobs: 0,
        },
        queueDepth: {
          "Reindex Queue": 2,
        },
        failedJobs: 1,
        bottlenecks: ["Operational job failures require operator review."],
        configurationValidation: [],
        dependencyValidation: ["No OpenSearch dependency is introduced."],
        recentFailures: [],
        metrics: [],
      });
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
    if (url.endsWith("/api/governance/ai") && init?.method === "POST") {
      return jsonResponse({
        approvedModels: ["mistral:mistral-small", "openai:gpt-5-mini"],
        promptPolicyVersion: "prompt-policy-v2",
        responsePolicyVersion: "response-policy-v1",
        citationRequired: true,
        groundingValidationRequired: true,
        updatedAt: "2026-07-10T10:10:00Z",
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
