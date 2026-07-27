import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Input } from "./input";

describe("Input", () => {
  it("associates its label, hint, and validation state accessibly", () => {
    const { rerender } = render(
      <Input id="email" label="Email address" hint="Any valid email works." />
    );
    expect(screen.getByRole("textbox", { name: "Email address" })).toHaveAccessibleDescription(
      "Any valid email works."
    );

    rerender(<Input id="email" label="Email address" error="Enter a valid email address." />);
    expect(screen.getByRole("textbox", { name: "Email address" })).toHaveAttribute(
      "aria-invalid",
      "true"
    );
    expect(screen.getByRole("alert")).toHaveTextContent("Enter a valid email address.");
  });
});
