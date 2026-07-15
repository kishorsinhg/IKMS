import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IkmsThemeProvider } from "../../theme/IkmsThemeProvider";
import { AssistantPanel } from "./AssistantPanel";

describe("AssistantPanel", () => {
  it("renders empty and suggested-question states", async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();

    render(
      <IkmsThemeProvider>
        <AssistantPanel
          title="Enterprise AI Assistant"
          conversationState="empty"
          messages={[]}
          suggestedQuestions={[
            { key: "explain", label: "Explain this document", onSelect },
          ]}
          emptyTitle="Conversation unavailable"
          emptyMessage="No assistant endpoint is configured."
        />
      </IkmsThemeProvider>,
    );

    expect(screen.getByRole("heading", { name: "Enterprise AI Assistant" })).toBeInTheDocument();
    expect(screen.getByText("Conversation unavailable")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Explain this document" }));
    expect(onSelect).toHaveBeenCalledTimes(1);
  });
});
