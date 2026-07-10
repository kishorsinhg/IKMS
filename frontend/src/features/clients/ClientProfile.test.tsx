import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { ClientProfilePage } from "./ClientProfilePage";

describe("ClientProfilePage", () => {
  it("renders the required client profile sections", () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <ClientProfilePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText("The client workspace is the central broker view for linked documents, emails, notes, AI Q&A, and activity.")).toBeInTheDocument();
    expect(screen.getAllByRole("heading", { name: "Client Profile" })).toHaveLength(2);
    expect(screen.getByRole("heading", { name: "Documents" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Emails" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Notes" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "AI Q&A" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Audit/Activity" })).toBeInTheDocument();
  });
});
