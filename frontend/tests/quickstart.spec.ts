import { expect, test } from "@playwright/test";

test.describe("IKMS quickstart smoke flow", () => {
  test("supports authenticated search-first navigation and protected route handling", async ({ page }) => {
    await page.goto("/");
    await expect(page).toHaveURL(/\/search$/);
    await expect(page.getByRole("heading", { name: "Search", exact: true })).toBeVisible();

    await page.goto("/clients");
    await expect(page).toHaveURL(/\/clients$/);
    await expect(page.getByRole("heading", { name: "Customer Access", exact: true })).toBeVisible();

    await page.goto("/clients/import");
    await expect(page).toHaveURL(/\/clients\/import$/);
    await expect(page.getByRole("heading", { name: "Client Import", exact: true })).toBeVisible();

    await page.goto("/intake");
    await expect(page).toHaveURL(/\/intake$/);
    await expect(page.getByRole("heading", { name: "Intake", exact: true })).toBeVisible();

    await page.goto("/search");
    await expect(page).toHaveURL(/\/search$/);
    await expect(page.getByRole("heading", { name: "Search", exact: true })).toBeVisible();

    await expect(page.getByRole("link", { name: "Administration" })).toHaveCount(0);
    await page.goto("/administration");
    await expect(page).toHaveURL(/\/administration$/);
    await expect(page.getByText("Workspace restricted")).toBeVisible();

    await page.goto("/audit");
    await expect(page).toHaveURL(/\/audit$/);
    await expect(page.getByRole("heading", { name: "Audit", exact: true })).toBeVisible();
  });
});
