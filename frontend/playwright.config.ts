import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./tests",
  timeout: 30_000,
  use: {
    baseURL: process.env.IKMS_UI_BASE_URL ?? "http://localhost:5173",
    trace: "on-first-retry",
  },
  webServer: [
    {
      command: "npm run dev -- --host 127.0.0.1 --port 5173",
      url: "http://127.0.0.1:5173",
      reuseExistingServer: !process.env.CI,
      cwd: ".",
    },
  ],
});
