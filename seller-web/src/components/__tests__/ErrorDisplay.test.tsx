"use client";

import { render, screen } from "@testing-library/react";
import { ErrorDisplay } from "@/components/ErrorDisplay";

describe("ErrorDisplay", () => {
  it("renders a string error message", () => {
    render(<ErrorDisplay error="Something went wrong" />);

    expect(screen.getByText("Something went wrong")).toBeInTheDocument();
  });
});
