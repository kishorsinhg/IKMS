import "@fontsource/inter";
import { createTheme } from "@mui/material/styles";
import type {} from "@mui/x-data-grid/themeAugmentation";

const baseTheme = createTheme();
const baseShadows = [...baseTheme.shadows] as typeof baseTheme.shadows;

baseShadows[1] = "0 1px 2px rgba(15, 23, 42, 0.06)";
baseShadows[2] = "0 2px 8px rgba(15, 23, 42, 0.08)";
baseShadows[3] = "0 4px 12px rgba(15, 23, 42, 0.10)";
baseShadows[4] = "0 6px 16px rgba(15, 23, 42, 0.10)";

export const ikmsTheme = createTheme({
  palette: {
    mode: "light",
    primary: {
      main: "#2563EB",
      dark: "#1D4ED8",
      contrastText: "#FFFFFF",
    },
    secondary: {
      main: "#20324A",
      dark: "#17263A",
      contrastText: "#FFFFFF",
    },
    background: {
      default: "#F5F6F8",
      paper: "#FFFFFF",
    },
    text: {
      primary: "#1F2937",
      secondary: "#5F6B7A",
      disabled: "#A0A8B3",
    },
    divider: "#D8DEE6",
    success: { main: "#2E7D32" },
    warning: { main: "#B45309" },
    error: { main: "#C62828" },
    info: { main: "#0369A1" },
  },
  typography: {
    fontFamily: '"Inter", system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    fontSize: 13,
    h1: {
      fontSize: "22px",
      lineHeight: 1.3,
      fontWeight: 600,
      letterSpacing: "-0.01em",
    },
    h2: {
      fontSize: "18px",
      lineHeight: 1.3,
      fontWeight: 600,
    },
    h3: {
      fontSize: "16px",
      lineHeight: 1.3,
      fontWeight: 600,
    },
    body1: {
      fontSize: "14px",
      lineHeight: 1.4,
    },
    body2: {
      fontSize: "13px",
      lineHeight: 1.4,
    },
    caption: {
      fontSize: "11px",
      lineHeight: 1.3,
    },
    subtitle2: {
      fontSize: "12px",
      lineHeight: 1.3,
      fontWeight: 600,
    },
    button: {
      fontSize: "13px",
      lineHeight: 1.3,
      fontWeight: 500,
      textTransform: "none",
    },
  },
  spacing: 4,
  shape: {
    borderRadius: 4,
  },
  shadows: baseShadows,
  zIndex: {
    appBar: 1200,
    drawer: 1100,
    modal: 1300,
    snackbar: 1400,
    tooltip: 1500,
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: (theme) => ({
        ":root": {
          colorScheme: "light",
          "--ikms-bg": theme.palette.background.default,
          "--ikms-bg-strong": "#EFF3F8",
          "--ikms-panel": theme.palette.background.paper,
          "--ikms-panel-muted": "#F8FAFC",
          "--ikms-sidebar": theme.palette.secondary.main,
          "--ikms-sidebar-alt": "#17263A",
          "--ikms-line": theme.palette.divider,
          "--ikms-line-strong": "#C3CBD5",
          "--ikms-text": theme.palette.text.primary,
          "--ikms-muted": theme.palette.text.secondary,
          "--ikms-muted-strong": "#374151",
          "--ikms-accent": theme.palette.primary.main,
          "--ikms-accent-strong": theme.palette.primary.dark,
          "--ikms-accent-soft": "#EFF6FF",
          "--ikms-success": theme.palette.success.main,
          "--ikms-danger": theme.palette.error.main,
          "--ikms-warning": theme.palette.warning.main,
        },
        "html, body, #root": {
          minHeight: "100%",
        },
        body: {
          margin: 0,
          backgroundColor: theme.palette.background.default,
          color: theme.palette.text.primary,
        },
        a: {
          color: theme.palette.primary.main,
        },
        code: {
          fontFamily: 'ui-monospace, SFMono-Regular, "SF Mono", Consolas, monospace',
        },
      }),
    },
    MuiButton: {
      defaultProps: {
        size: "small",
        disableElevation: true,
      },
      styleOverrides: {
        root: {
          minHeight: 32,
          borderRadius: 4,
          textTransform: "none",
          fontWeight: 600,
        },
        containedPrimary: {
          border: "1px solid transparent",
        },
        outlined: {
          borderColor: "#C3CBD5",
        },
      },
    },
    MuiIconButton: {
      defaultProps: {
        size: "small",
      },
      styleOverrides: {
        root: {
          borderRadius: 4,
          border: "1px solid transparent",
        },
      },
    },
    MuiTextField: {
      defaultProps: {
        size: "small",
        variant: "outlined",
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 4,
          fontSize: 13,
          backgroundColor: "#FFFFFF",
        },
        input: {
          paddingTop: 7,
          paddingBottom: 7,
        },
      },
    },
    MuiInputLabel: {
      styleOverrides: {
        root: {
          fontSize: 12,
          fontWeight: 500,
        },
      },
    },
    MuiFormHelperText: {
      styleOverrides: {
        root: {
          marginLeft: 0,
          marginRight: 0,
          fontSize: 11,
          lineHeight: 1.35,
        },
      },
    },
    MuiFormLabel: {
      styleOverrides: {
        root: {
          fontSize: 12,
          fontWeight: 600,
        },
      },
    },
    MuiSelect: {
      defaultProps: {
        size: "small",
      },
    },
    MuiAutocomplete: {
      defaultProps: {
        size: "small",
      },
    },
    MuiCheckbox: {
      defaultProps: {
        size: "small",
      },
    },
    MuiRadio: {
      defaultProps: {
        size: "small",
      },
    },
    MuiSwitch: {
      styleOverrides: {
        root: {
          padding: 6,
        },
      },
    },
    MuiToolbar: {
      styleOverrides: {
        root: {
          minHeight: "44px !important",
          paddingLeft: "8px !important",
          paddingRight: "8px !important",
        },
      },
    },
    MuiPaper: {
      defaultProps: {
        elevation: 0,
      },
      styleOverrides: {
        root: {
          backgroundImage: "none",
          boxShadow: "none",
        },
      },
    },
    MuiCard: {
      defaultProps: {
        elevation: 0,
      },
      styleOverrides: {
        root: {
          border: "1px solid #D8DEE6",
          borderRadius: 4,
          boxShadow: "none",
        },
      },
    },
    MuiTabs: {
      styleOverrides: {
        root: {
          minHeight: 38,
        },
        indicator: {
          height: 2,
        },
      },
    },
    MuiTab: {
      styleOverrides: {
        root: {
          minHeight: 38,
          paddingLeft: 12,
          paddingRight: 12,
          fontSize: 13,
          textTransform: "none",
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          height: 22,
          borderRadius: 3,
          fontSize: 11,
        },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: 6,
          border: "1px solid #D8DEE6",
        },
      },
    },
    MuiDialogTitle: {
      styleOverrides: {
        root: {
          padding: "12px 16px",
          borderBottom: "1px solid #D8DEE6",
          fontSize: 14,
          fontWeight: 600,
        },
      },
    },
    MuiDialogContent: {
      styleOverrides: {
        root: {
          padding: "16px",
        },
        dividers: {
          padding: "16px",
          borderTop: "none",
          borderBottom: "1px solid #D8DEE6",
        },
      },
    },
    MuiDialogActions: {
      styleOverrides: {
        root: {
          padding: "10px 16px",
          borderTop: "1px solid #D8DEE6",
          gap: 8,
        },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          borderRadius: 0,
          borderColor: "#D8DEE6",
          boxShadow: "none",
        },
      },
    },
    MuiTooltip: {
      defaultProps: {
        arrow: true,
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: 4,
          paddingTop: 4,
          paddingBottom: 4,
          alignItems: "center",
        },
        message: {
          paddingTop: 6,
          paddingBottom: 6,
        },
        action: {
          alignItems: "center",
        },
      },
    },
    MuiSnackbar: {
      defaultProps: {
        anchorOrigin: {
          vertical: "top",
          horizontal: "right",
        },
      },
    },
    MuiMenuItem: {
      styleOverrides: {
        root: {
          minHeight: 32,
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottom: "1px solid #D8DEE6",
          paddingTop: 10,
          paddingBottom: 10,
        },
        head: {
          fontSize: 11,
          fontWeight: 600,
          letterSpacing: "0.04em",
          textTransform: "uppercase",
          color: "#5F6B7A",
        },
      },
    },
    MuiDataGrid: {
      styleOverrides: {
        root: {
          borderColor: "#D8DEE6",
          borderRadius: 4,
          backgroundColor: "#FFFFFF",
        },
        columnHeaders: {
          backgroundColor: "#F8FAFC",
          borderBottom: "1px solid #D8DEE6",
        },
        row: {
          "&:hover": {
            backgroundColor: "#F8FAFC",
          },
        },
        cell: {
          borderTop: "none",
          alignItems: "center",
        },
        footerContainer: {
          borderTop: "1px solid #D8DEE6",
          minHeight: 44,
        },
      },
    },
  },
});

declare module "@mui/material/styles" {
  interface TypographyVariants {
    workspaceTitle: React.CSSProperties;
  }

  interface TypographyVariantsOptions {
    workspaceTitle?: React.CSSProperties;
  }
}

declare module "@mui/material/Typography" {
  interface TypographyPropsVariantOverrides {
    workspaceTitle: true;
  }
}
