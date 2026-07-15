import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import { App } from "../../app/App";
import { NotificationProvider } from "../../app/providers/NotificationProvider";
import { IkmsThemeProvider } from "../../app/theme/IkmsThemeProvider";

describe("KnowledgeQualityPage", () => {
  it("renders the stewardship workspace and selected customer issues", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input);

        if (url.endsWith("/api/auth/me")) {
          return new Response(
            JSON.stringify({
              id: "user-1",
              username: "admin",
              displayName: "Admin",
              email: "admin@example.com",
              status: "ACTIVE",
              roles: ["ADMINISTRATOR"],
              permissions: ["MANAGE_CONFIGURATION"],
            }),
            { status: 200, headers: { "content-type": "application/json" } },
          );
        }

        if (url.includes("/api/knowledge-quality/customers")) {
          return new Response(
            JSON.stringify({
              customers: [
                {
                  clientId: "client-1",
                  customerName: "Acme Transport",
                  customerExternalId: "C-100",
                  overallScore: 0.78,
                  readinessState: "NEEDS_ATTENTION",
                  issueCount: 2,
                  openIssueCount: 2,
                  evaluatedAt: "2026-07-15T12:00:00Z",
                  dimensions: [
                    {
                      key: "completeness",
                      label: "Metadata completeness",
                      score: 0.8,
                      summary: "Most required metadata is available.",
                    },
                  ],
                  recommendationHighlights: ["Review broker reference normalization."],
                },
              ],
            }),
            { status: 200, headers: { "content-type": "application/json" } },
          );
        }

        if (url.includes("/api/knowledge-quality/customer/client-1")) {
          return new Response(
            JSON.stringify({
              summary: {
                clientId: "client-1",
                customerName: "Acme Transport",
                customerExternalId: "C-100",
                overallScore: 0.78,
                readinessState: "NEEDS_ATTENTION",
                issueCount: 2,
                openIssueCount: 2,
                evaluatedAt: "2026-07-15T12:00:00Z",
                dimensions: [
                  {
                    key: "business-references",
                    label: "Business Reference quality",
                    score: 0.72,
                    summary: "One conflicting policy reference remains open.",
                  },
                ],
                recommendationHighlights: ["Correct the customer-facing policy reference metadata."],
              },
              issues: [
                {
                  id: "issue-1",
                  clientId: "client-1",
                  sourceType: "DOCUMENT",
                  sourceId: "document-1",
                  category: "BUSINESS_REFERENCE",
                  issueType: "CONFLICTING_POLICY_NUMBER",
                  severity: "HIGH",
                  status: "OPEN",
                  title: "Conflicting policy reference",
                  detail: "Two document versions contain different Policy Number values.",
                  recommendationType: "BUSINESS_REFERENCE_CORRECTION",
                  recommendationDetail: "Confirm the authoritative policy reference from the approved document version.",
                  businessReferenceKey: "policyNumber",
                  scoreImpact: 0.15,
                  createdAt: "2026-07-15T11:30:00Z",
                  updatedAt: "2026-07-15T11:45:00Z",
                },
              ],
            }),
            { status: 200, headers: { "content-type": "application/json" } },
          );
        }

        return new Response(JSON.stringify([]), {
          status: 200,
          headers: { "content-type": "application/json" },
        });
      }),
    );

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
            <MemoryRouter initialEntries={["/knowledge-quality"]}>
              <App />
            </MemoryRouter>
          </NotificationProvider>
        </IkmsThemeProvider>
      </QueryClientProvider>,
    );

    expect(await screen.findByRole("heading", { name: "Knowledge Quality" })).toBeInTheDocument();
    expect(await screen.findByText("Acme Transport")).toBeInTheDocument();
    expect(await screen.findByText("Conflicting policy reference")).toBeInTheDocument();
    expect(
      await screen.findByText("Two document versions contain different Policy Number values."),
    ).toBeInTheDocument();
  });
});
