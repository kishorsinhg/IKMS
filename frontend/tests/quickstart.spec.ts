import { expect, test } from "@playwright/test";

test.describe("IKMS quickstart smoke flow", () => {
  test("supports login, client navigation, intake, admin, and audit shell flows", async ({ page }) => {
    await page.goto("/login");

    await expect(page.getByRole("heading", { name: "IKMS Sign In" })).toBeVisible();
    await page.getByLabel("Username").fill("admin");
    await page.getByLabel("Password").fill("ChangeMe123!");
    await page.getByRole("button", { name: "Sign in" }).click();

    await expect(page).toHaveURL(/\/clients$/);
    await expect(page.getByRole("heading", { name: "Clients workspace" })).toBeVisible();

    await page.getByRole("link", { name: "Open CSV import" }).click();
    await expect(page).toHaveURL(/\/clients\/import$/);
    await expect(page.getByRole("heading", { name: "Client CSV import" })).toBeVisible();

    await page.getByRole("link", { name: "Intake" }).click();
    await expect(page).toHaveURL(/\/intake$/);
    await expect(page.getByRole("heading", { name: "Intake operations" })).toBeVisible();

    await page.getByRole("link", { name: "Search" }).click();
    await expect(page).toHaveURL(/\/search$/);
    await expect(page.getByRole("heading", { name: "Client search and AI Q&A" })).toBeVisible();

    await page.getByRole("link", { name: "Administration" }).click();
    await expect(page).toHaveURL(/\/administration$/);
    await expect(page.getByRole("heading", { name: "Administration" })).toBeVisible();

    await page.getByRole("link", { name: "Audit" }).click();
    await expect(page).toHaveURL(/\/audit$/);
    await expect(page.getByRole("heading", { name: "Audit and governance" })).toBeVisible();
  });
});
