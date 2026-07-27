import { describe, expect, it } from "vitest";
import { validateRegistration } from "./registration";

describe("registration validation", () => {
  it.each(["person@gmail.com", "engineer@yahoo.com", "responder@company.dev"])(
    "accepts personal and work email %s",
    (email) => {
      expect(
        validateRegistration({
          displayName: "Responder",
          email,
          password: "correct-horse-battery",
        })
      ).toEqual({});
    }
  );

  it("returns actionable field errors", () => {
    expect(validateRegistration({ displayName: " ", email: "invalid", password: "short" })).toEqual(
      {
        displayName: "Enter your name.",
        email: "Enter a valid email address.",
        password: "Use at least 12 characters.",
      }
    );
  });
});
