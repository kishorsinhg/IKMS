import { CssBaseline, ThemeProvider } from "@mui/material";
import { ReactNode } from "react";
import { ikmsTheme } from "./ikmsTheme";

export function IkmsThemeProvider({ children }: { children: ReactNode }) {
  return (
    <ThemeProvider theme={ikmsTheme}>
      <CssBaseline />
      {children}
    </ThemeProvider>
  );
}
