import { render, screen } from "@testing-library/react";
import { ReviewQueuePage } from "./ReviewQueuePage";

describe("ReviewQueuePage", () => {
  it("renders the review queue workflow baseline", () => {
    render(<ReviewQueuePage />);

    expect(screen.getByRole("heading", { name: "Review queue" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Filters" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Open review item" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Resolve actions" })).toBeInTheDocument();
  });
});
