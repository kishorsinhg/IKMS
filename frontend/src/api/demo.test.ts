import { describe, expect, it } from "vitest";
import { getDemoClientWorkspace, searchDemoWorkspace } from "./demo";

describe("demo dataset", () => {
  it("provides a realistic broker workspace for Harborview", async () => {
    const workspace = await getDemoClientWorkspace("client-harborview");

    expect(workspace.policyReferences.length).toBeGreaterThan(0);
    expect(workspace.claimReferences.length).toBeGreaterThan(0);
    expect(workspace.aiSummaries.length).toBeGreaterThan(0);
    expect(workspace.recentActivity.length).toBeGreaterThan(0);
    expect(workspace.reviewQueue.length).toBeGreaterThan(0);
    expect(workspace.auditEvents.length).toBeGreaterThan(0);
    expect(workspace.policyReferences[0]?.carrier).toBe("Trident Specialty Marine");
  });

  it("returns grouped search results across customer knowledge types", async () => {
    const carrierResults = await searchDemoWorkspace("Trident");
    const customerResults = await searchDemoWorkspace("Harborview");
    const groups = new Set([...carrierResults, ...customerResults].map((item) => item.group));

    expect(groups.has("Customers")).toBe(true);
    expect(groups.has("Knowledge")).toBe(true);
    expect(groups.has("Policy References")).toBe(true);
    expect(groups.has("Claim References")).toBe(true);
  });
});
