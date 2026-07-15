import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IkmsThemeProvider } from "../../theme/IkmsThemeProvider";
import { EnterpriseDocumentViewer } from "./EnterpriseDocumentViewer";

describe("EnterpriseDocumentViewer", () => {
  it("renders desktop evidence and page controls for a ready document", async () => {
    mockViewport(1440);

    render(
      <IkmsThemeProvider>
        <EnterpriseDocumentViewer
          embedded
          document={{
            id: "doc-1",
            title: "Broker Submission",
            subtitle: "Silver Ridge Hospitality",
            fileKind: "pdf",
            previewUrl: "/api/documents/doc-1/preview",
            downloadUrl: "/api/documents/doc-1/download",
            originalUrl: "/api/documents/doc-1/preview",
            pages: [
              { id: "page-1", label: "Page 1", pageNumber: 1 },
              { id: "page-2", label: "Page 2", pageNumber: 2 },
            ],
          }}
          evidenceSections={[
            {
              key: "evidence",
              title: "Evidence",
              content: "Carrier: Mountain West Indemnity",
            },
          ]}
        />
      </IkmsThemeProvider>,
    );

    expect(screen.getByText("Evidence Workspace")).toBeInTheDocument();
    expect(screen.getByText("Carrier: Mountain West Indemnity")).toBeInTheDocument();
    expect(screen.getByText("Page 1 of 2")).toBeInTheDocument();
  });

  it("opens the mobile evidence sheet from the toolbar", async () => {
    mockViewport(390);
    const user = userEvent.setup();

    render(
      <IkmsThemeProvider>
        <EnterpriseDocumentViewer
          embedded
          document={{
            id: "doc-2",
            title: "Renewal Email",
            fileKind: "email",
            previewUrl: null,
            pages: [{ id: "message-1", label: "Message 1", pageNumber: 1 }],
          }}
          evidenceSections={[
            {
              key: "metadata",
              title: "Metadata",
              content: "Sender: submissions@example.com",
            },
          ]}
        />
      </IkmsThemeProvider>,
    );

    await user.click(screen.getByRole("button", { name: "Evidence" }));
    expect(await screen.findByText("Sender: submissions@example.com")).toBeInTheDocument();
  });
});

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
