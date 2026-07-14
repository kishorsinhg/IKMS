import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Outlet, Route, Routes } from "react-router-dom";
import { NotificationProvider } from "../../app/providers/NotificationProvider";
import { IkmsThemeProvider } from "../../app/theme/IkmsThemeProvider";
import { ClientsWorkspacePage } from "./ClientsWorkspacePage";

describe("ClientsWorkspacePage", () => {
  it("renders the desktop grid, applies filters, and opens Customer360", async () => {
    mockViewport(1440);
    const setWorkspaceChrome = vi.fn();
    const user = userEvent.setup();
    stubClientsFetch();

    renderClientsWorkspace({
      initialEntry: "/clients?q=beacon",
      setWorkspaceChrome,
    });

    expect(await screen.findByPlaceholderText("Search by customer name or client ID")).toBeInTheDocument();
    expect(screen.getByRole("grid")).toBeInTheDocument();
    expect((await screen.findAllByText("Beacon Holdings")).length).toBeGreaterThan(0);

    await user.click((await screen.findAllByText("Beacon Holdings"))[0]!);

    await waitFor(() =>
      expect(setWorkspaceChrome).toHaveBeenLastCalledWith(
        expect.objectContaining({
          contextTitle: "Selected Customer",
        }),
      ),
    );

    await user.dblClick((await screen.findAllByText("Beacon Holdings"))[0]!);
    await waitFor(() => expect(screen.getByText("Customer360 destination")).toBeInTheDocument());
  });

  it("switches to the mobile list pattern and opens selected customer detail", async () => {
    mockViewport(390);
    const setWorkspaceChrome = vi.fn();
    const user = userEvent.setup();
    stubClientsFetch();

    renderClientsWorkspace({
      initialEntry: "/clients",
      setWorkspaceChrome,
    });

    expect((await screen.findAllByText("Alex Broker")).length).toBeGreaterThan(0);
    expect(screen.queryByRole("grid")).not.toBeInTheDocument();

    await user.click((await screen.findAllByText("Alex Broker"))[0]!);

    expect(await screen.findByText("Selected customer")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Open Customer360" })).toBeInTheDocument();
  });
});

function renderClientsWorkspace({
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

  render(
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
                <Route path="/clients" element={<ClientsWorkspacePage />} />
                <Route path="/clients/:clientId" element={<div>Customer360 destination</div>} />
              </Route>
            </Routes>
          </MemoryRouter>
        </NotificationProvider>
      </IkmsThemeProvider>
    </QueryClientProvider>,
  );
}

function stubClientsFetch() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.includes("/api/clients?query=beacon")) {
      return new Response(JSON.stringify([
        { id: "client-2", clientId: "C-200", clientIdTemporary: false, clientType: "BUSINESS", status: "ACTIVE", displayName: "Beacon Holdings" },
      ]), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (url.endsWith("/api/clients") && (!init?.method || init.method === "GET")) {
      return new Response(JSON.stringify([
        { id: "client-1", clientId: "TMP-1", clientIdTemporary: true, clientType: "INDIVIDUAL", status: "ACTIVE", displayName: "Alex Broker" },
        { id: "client-2", clientId: "C-200", clientIdTemporary: false, clientType: "BUSINESS", status: "ACTIVE", displayName: "Beacon Holdings" },
      ]), { status: 200, headers: { "content-type": "application/json" } });
    }
    if (url.endsWith("/api/clients") && init?.method === "POST") {
      return new Response(JSON.stringify({
        id: "client-3",
        clientId: "TMP-2",
        clientIdTemporary: true,
        clientType: "BUSINESS",
        status: "ACTIVE",
        displayName: "New Customer",
      }), { status: 200, headers: { "content-type": "application/json" } });
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
