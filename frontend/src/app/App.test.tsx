import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import { App } from "./App";
import { IkmsThemeProvider } from "./theme/IkmsThemeProvider";
import { NotificationProvider } from "./providers/NotificationProvider";

describe("App", () => {
  it("renders login screen when unauthenticated", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 401,
        headers: {
          get: () => "application/json",
        },
        json: async () => ({ message: "Unauthorized" }),
        text: async () => "Unauthorized",
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
            <MemoryRouter>
              <App />
            </MemoryRouter>
          </NotificationProvider>
        </IkmsThemeProvider>
      </QueryClientProvider>,
    );

    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
  });

  it("keeps search as the authenticated default route", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input);
        if (url.endsWith("/api/auth/me")) {
          return new Response(
            JSON.stringify({
              id: "user-1",
              username: "processor",
              displayName: "Processor",
              email: "processor@example.com",
              status: "ACTIVE",
              roles: ["PROCESSOR"],
              permissions: ["SEARCH_CLIENT_KNOWLEDGE", "CLIENT_VIEW"],
            }),
            { status: 200, headers: { "content-type": "application/json" } },
          );
        }

        if (url.endsWith("/api/clients")) {
          return new Response(
            JSON.stringify([
              {
                id: "client-1",
                clientId: "C-100",
                clientIdTemporary: false,
                clientType: "INDIVIDUAL",
                status: "ACTIVE",
                displayName: "Alex Broker",
              },
            ]),
            { status: 200, headers: { "content-type": "application/json" } },
          );
        }

        if (url.includes("/api/review-queue")) {
          return new Response(JSON.stringify([]), {
            status: 200,
            headers: { "content-type": "application/json" },
          });
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
            <MemoryRouter initialEntries={["/"]}>
              <App />
            </MemoryRouter>
          </NotificationProvider>
        </IkmsThemeProvider>
      </QueryClientProvider>,
    );

    expect(await screen.findByRole("heading", { name: "Search" })).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: "Focus Search workspace input" })).toBeInTheDocument();
    expect(
      await screen.findByPlaceholderText("Search by customer, policy reference, claim reference, email, metadata, or note"),
    ).toBeInTheDocument();
  });

  it("handles direct protected-route access safely", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            id: "user-1",
            username: "processor",
            displayName: "Processor",
            email: "processor@example.com",
            status: "ACTIVE",
            roles: ["PROCESSOR"],
            permissions: ["SEARCH_CLIENT_KNOWLEDGE"],
          }),
          { status: 200, headers: { "content-type": "application/json" } },
        ),
      ),
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
            <MemoryRouter initialEntries={["/administration"]}>
              <App />
            </MemoryRouter>
          </NotificationProvider>
        </IkmsThemeProvider>
      </QueryClientProvider>,
    );

    expect(await screen.findByText("Workspace restricted")).toBeInTheDocument();
  });
});
