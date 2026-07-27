import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Button } from "./button";

describe("Button", () => {
  it("prevents duplicate actions while loading", async () => {
    const action = vi.fn();
    render(
      <Button loading onClick={action}>
        Save
      </Button>
    );
    const button = screen.getByRole("button");
    expect(button).toBeDisabled();
    await userEvent.click(button);
    expect(action).not.toHaveBeenCalled();
  });
});
