import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { Button } from "@mui/material";
import { ReactElement, useState } from "react";
import { IkmsThemeProvider } from "./theme/IkmsThemeProvider";
import { IkmsAppShell } from "./shell/IkmsAppShell";
import { WorkspaceHeader } from "./components/WorkspaceHeader";
import { WorkspaceToolbar } from "./components/WorkspaceToolbar";
import { RightContextPanel } from "./components/RightContextPanel";
import { EntityGrid } from "./components/EntityGrid";
import { NotificationProvider } from "./providers/NotificationProvider";
import { useNotification } from "./providers/useNotification";
import { AppErrorBoundary } from "./error/AppErrorBoundary";
import { PermissionGuard } from "./auth/PermissionGuard";

function renderWithProviders(ui: React.ReactNode) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <IkmsThemeProvider>
        <NotificationProvider>{ui}</NotificationProvider>
      </IkmsThemeProvider>
    </QueryClientProvider>,
  );
}

describe("Frontend foundation", () => {
  it("theme provider renders MUI components with the shared theme", () => {
    renderWithProviders(<Button variant="contained">Theme action</Button>);

    expect(screen.getByRole("button", { name: "Theme action" })).toBeInTheDocument();
  });

  it("application shell renders and navigation respects permissions", async () => {
    const user = userEvent.setup();
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
            permissions: ["SEARCH_CLIENT_KNOWLEDGE", "CLIENT_VIEW"],
          }),
          { status: 200, headers: { "content-type": "application/json" } },
        ),
      ),
    );

    renderWithProviders(
      <MemoryRouter initialEntries={["/search"]}>
        <Routes>
          <Route element={<IkmsAppShell />}>
            <Route path="/search" element={<div>Search page</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => expect(screen.getByRole("heading", { name: "Search" })).toBeInTheDocument());
    await user.click(screen.getByLabelText("Open navigation"));
    expect(await screen.findByRole("link", { name: "Search" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Customer Access" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Administration" })).not.toBeInTheDocument();
  });

  it("workspace header renders breadcrumb and actions", () => {
    renderWithProviders(
      <WorkspaceHeader
        breadcrumbs={[{ label: "IKMS" }, { label: "Search" }]}
        title="Search"
        subtitle="Authenticated landing page"
        primaryActions={<Button>Export</Button>}
      />,
    );

    expect(screen.getByRole("heading", { name: "Search" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Export" })).toBeInTheDocument();
  });

  it("toolbar renders search, filters, and secondary actions", async () => {
    const user = userEvent.setup();
    const onColumns = vi.fn();

    renderWithProviders(
      <WorkspaceToolbar
        searchValue="alex"
        onSearchChange={() => {}}
        searchPlaceholder="Search clients"
        filters={<Button>Filters</Button>}
        activeFilters={[{ key: "status", label: "Status: Open" }]}
        onColumns={onColumns}
        secondaryActions={[{ key: "archive", label: "Archive" }]}
      />,
    );

    expect(screen.getByPlaceholderText("Search clients")).toBeInTheDocument();
    expect(screen.getByText("Status: Open")).toBeInTheDocument();
    await user.click(screen.getByLabelText("Column settings"));
    expect(onColumns).toHaveBeenCalled();
  });

  it("right context panel collapses and expands", async () => {
    const user = userEvent.setup();

    function Example() {
      const [collapsed, setCollapsed] = useState(false);

      return (
        <RightContextPanel
          title="Context"
          sections={[{ key: "one", title: "AI Brief", content: <div>Section content</div> }]}
          collapsed={collapsed}
          onToggle={() => setCollapsed((value) => !value)}
        />
      );
    }

    renderWithProviders(<Example />);

    expect(screen.getByText("AI Brief")).toBeInTheDocument();
    await user.click(screen.getByLabelText("Collapse context panel"));
    await user.click(screen.getByLabelText("Expand context panel"));
    expect(screen.getByText("Section content")).toBeInTheDocument();
  });

  it("entity grid renders populated and empty states", async () => {
    renderWithProviders(
      <EntityGrid
        rows={[{ id: "1", name: "Alex Broker" }]}
        columns={[{ field: "name", headerName: "Name", flex: 1 }]}
      />,
    );

    expect(await screen.findByRole("grid")).toBeInTheDocument();
    expect(screen.getByText("Alex Broker")).toBeInTheDocument();

    renderWithProviders(
      <EntityGrid
        rows={[]}
        columns={[{ field: "name", headerName: "Name", flex: 1 }]}
        emptyTitle="No rows"
        emptyMessage="Nothing to show"
      />,
    );

    expect(await screen.findByText("No rows")).toBeInTheDocument();
  });

  it("permission guard hides restricted content", async () => {
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

    renderWithProviders(
      <PermissionGuard allOf={["MANAGE_CONFIGURATION"]}>
        <div>Restricted admin content</div>
      </PermissionGuard>,
    );

    await waitFor(() => expect(screen.getByText("Content restricted")).toBeInTheDocument());
    expect(screen.queryByText("Restricted admin content")).not.toBeInTheDocument();
  });

  it("notification pattern renders snackbar alerts", async () => {
    const user = userEvent.setup();

    function Example() {
      const { notify } = useNotification();
      return <Button onClick={() => notify({ message: "Saved", severity: "success" })}>Notify</Button>;
    }

    renderWithProviders(<Example />);
    await user.click(screen.getByRole("button", { name: "Notify" }));
    expect(await screen.findByText("Saved")).toBeInTheDocument();
  });

  it("error boundary displays a safe fallback", () => {
    function Broken(): ReactElement {
      throw new Error("boom");
    }

    render(
      <IkmsThemeProvider>
        <AppErrorBoundary>
          <Broken />
        </AppErrorBoundary>
      </IkmsThemeProvider>,
    );

    expect(screen.getByText("Something went wrong")).toBeInTheDocument();
  });
});
