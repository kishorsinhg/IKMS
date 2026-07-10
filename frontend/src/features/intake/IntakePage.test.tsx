import { render, screen } from "@testing-library/react";
import { IntakePage } from "./IntakePage";

describe("IntakePage", () => {
  it("renders the intake controls baseline", () => {
    render(<IntakePage />);

    expect(screen.getByRole("heading", { name: "Intake operations" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Manual upload" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Duplicate result" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Automated intake status" })).toBeInTheDocument();
  });
});
