import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Outlet, Route, Routes } from "react-router-dom";
import { NotificationProvider } from "../../app/providers/NotificationProvider";
import { IkmsThemeProvider } from "../../app/theme/IkmsThemeProvider";
import { SearchLandingPage } from "./SearchLandingPage";

describe("SearchLandingPage", () => {
  it("renders the desktop grid, keeps workspace search primary, and opens customer results", async () => {
    mockViewport(1440);
    const setWorkspaceChrome = vi.fn();
    const user = userEvent.setup();

    stubSearchFetch();

    renderSearchPage({
      initialEntry: "/search?q=alex",
      setWorkspaceChrome,
    });

    expect(await screen.findByPlaceholderText("Search by customer, policy reference, claim reference, email, metadata, or note")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Search" })).toBeInTheDocument();
    expect(screen.getByRole("grid")).toBeInTheDocument();
    expect(screen.getByText("Search results")).toBeInTheDocument();

    await waitFor(() =>
      expect(setWorkspaceChrome).toHaveBeenLastCalledWith(
        expect.objectContaining({
          contextTitle: "Search Context",
        }),
      ),
    );

    const alexRows = await screen.findAllByText("Alex Broker");
    await user.dblClick(alexRows[0]!);

    await waitFor(() => expect(screen.getByText("Customer profile")).toBeInTheDocument());
  });

  it("switches to a narrow-screen result list and opens detail in a drawer", async () => {
    mockViewport(390);
    const setWorkspaceChrome = vi.fn();
    const user = userEvent.setup();

    stubSearchFetch();

    renderSearchPage({
      initialEntry: "/search?q=alex",
      setWorkspaceChrome,
    });

    const alexResults = await screen.findAllByText("Alex Broker");
    expect(alexResults.length).toBeGreaterThan(0);
    expect(screen.queryByRole("grid")).not.toBeInTheDocument();

    await user.click(alexResults[0]!);

    expect(await screen.findByText("Selected result")).toBeInTheDocument();
    expect(screen.getByText("Matched evidence")).toBeInTheDocument();

    await waitFor(() =>
      expect(setWorkspaceChrome).toHaveBeenLastCalledWith(
        expect.objectContaining({
          contextTitle: "Selected Result",
        }),
      ),
    );
  });
});

function renderSearchPage({
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
                <Route path="/search" element={<SearchLandingPage />} />
                <Route path="/clients/:clientId" element={<div>Customer profile</div>} />
              </Route>
            </Routes>
          </MemoryRouter>
        </NotificationProvider>
      </IkmsThemeProvider>
    </QueryClientProvider>,
  );
}

function stubSearchFetch() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    if (url.endsWith("/api/auth/me")) {
      return new Response(JSON.stringify({
        id: "user-1",
        username: "processor",
        displayName: "Processor",
        email: "processor@example.com",
        status: "ACTIVE",
        roles: ["PROCESSOR"],
        permissions: ["CLIENT_VIEW", "SEARCH_CLIENT_KNOWLEDGE", "REVIEW_QUEUE_ACCESS"],
      }), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (url.includes("/api/clients?query=alex")) {
      return new Response(JSON.stringify([
        { id: "client-1", clientId: "TMP-1", clientIdTemporary: true, clientType: "INDIVIDUAL", status: "ACTIVE", displayName: "Alex Broker" },
      ]), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (url.endsWith("/api/clients")) {
      return new Response(JSON.stringify([
        { id: "client-1", clientId: "TMP-1", clientIdTemporary: true, clientType: "INDIVIDUAL", status: "ACTIVE", displayName: "Alex Broker" },
        { id: "client-2", clientId: "C-200", clientIdTemporary: false, clientType: "BUSINESS", status: "ACTIVE", displayName: "Beacon Holdings" },
      ]), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (url.includes("/api/review-queue")) {
      return new Response(JSON.stringify([
        { id: "review-1", status: "OPEN" },
      ]), { status: 200, headers: { "content-type": "application/json" } });
    }
    return new Response(JSON.stringify([]), { status: 200, headers: { "content-type": "application/json" } });
  });
  vi.stubGlobal("fetch", fetchMock);
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
