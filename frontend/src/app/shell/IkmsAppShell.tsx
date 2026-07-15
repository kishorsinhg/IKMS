import MenuOutlinedIcon from "@mui/icons-material/MenuOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import FactCheckOutlinedIcon from "@mui/icons-material/FactCheckOutlined";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";
import ReceiptLongOutlinedIcon from "@mui/icons-material/ReceiptLongOutlined";
import VerifiedOutlinedIcon from "@mui/icons-material/VerifiedOutlined";
import PersonOutlinedIcon from "@mui/icons-material/PersonOutlined";
import NotificationsOutlinedIcon from "@mui/icons-material/NotificationsOutlined";
import HelpOutlineOutlinedIcon from "@mui/icons-material/HelpOutlineOutlined";
import LogoutOutlinedIcon from "@mui/icons-material/LogoutOutlined";
import AccountCircleOutlinedIcon from "@mui/icons-material/AccountCircleOutlined";
import KeyboardCommandKeyOutlinedIcon from "@mui/icons-material/KeyboardCommandKeyOutlined";
import {
  AppBar,
  Badge,
  Box,
  Divider,
  Drawer,
  IconButton,
  InputAdornment,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Stack,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
  useMediaQuery,
} from "@mui/material";
import { useTheme } from "@mui/material/styles";
import { useCallback, useMemo, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { Permission, logout } from "../../api/auth";
import { currentUserQueryKey, useCurrentUser } from "../auth/useCurrentUser";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { RightContextPanel } from "../components/RightContextPanel";
import { WorkspaceHeader } from "../components/WorkspaceHeader";
import { RestrictedContentState } from "../WorkspaceStates";
import type { ContextSection } from "../components/RightContextPanel";
import type { WorkspaceHeaderBreadcrumb } from "../components/WorkspaceHeader";
import ikmsLogoDrawer from "../assets/IKMSLogoV3.png";
import ikmsLogoCompact from "../assets/IKMSLogoV3Compact.png";

interface NavItem {
  to: string;
  label: string;
  permission: Permission;
  icon: React.ReactElement;
}

export interface ShellWorkspaceChrome {
  title?: string;
  subtitle?: string;
  breadcrumbs?: WorkspaceHeaderBreadcrumb[];
  primaryActions?: React.ReactNode;
  secondaryActions?: React.ReactNode;
  contextTitle?: string;
  contextSections?: ContextSection[];
  contextWidth?: number;
  globalSearchValue?: string;
  globalSearchPlaceholder?: string;
  onGlobalSearchChange?: (value: string) => void;
  onGlobalSearchSubmit?: () => void;
}

export interface IkmsShellOutletContext {
  setWorkspaceChrome: (chrome: ShellWorkspaceChrome) => void;
  clearWorkspaceChrome: () => void;
}

const shellNavItems: NavItem[] = [
  {
    to: "/search",
    label: "Search",
    permission: "SEARCH_CLIENT_KNOWLEDGE",
    icon: <SearchOutlinedIcon fontSize="small" />,
  },
  {
    to: "/clients",
    label: "Customer Access",
    permission: "CLIENT_VIEW",
    icon: <PersonOutlinedIcon fontSize="small" />,
  },
  {
    to: "/review-queue",
    label: "Review",
    permission: "REVIEW_QUEUE_ACCESS",
    icon: <FactCheckOutlinedIcon fontSize="small" />,
  },
  {
    to: "/knowledge-quality",
    label: "Knowledge Quality",
    permission: "MANAGE_CONFIGURATION",
    icon: <VerifiedOutlinedIcon fontSize="small" />,
  },
  {
    to: "/administration",
    label: "Administration",
    permission: "MANAGE_CONFIGURATION",
    icon: <SettingsOutlinedIcon fontSize="small" />,
  },
  {
    to: "/audit",
    label: "Audit",
    permission: "VIEW_AUDIT",
    icon: <ReceiptLongOutlinedIcon fontSize="small" />,
  },
];

const routeMeta = [
  {
    matches: (pathname: string) => pathname === "/search",
    title: "Search",
    subtitle: "Locate authorized customers and operational records without leaving the shared shell.",
    breadcrumbs: ["IKMS", "Search"],
  },
  {
    matches: (pathname: string) => pathname === "/clients",
    title: "Customer Access",
    subtitle: "Open client records and move into Customer360 without leaving the shared workspace shell.",
    breadcrumbs: ["IKMS", "Customer Access"],
  },
  {
    matches: (pathname: string) => pathname === "/clients/import",
    title: "Client Import",
    subtitle: "Validate CSV rows and import client records without leaving the shared application shell.",
    breadcrumbs: ["IKMS", "Customer Access", "Client Import"],
  },
  {
    matches: (pathname: string) => /^\/clients\/[^/]+$/.test(pathname),
    title: "Customer360",
    subtitle: "Client-linked documents, emails, notes, AI assistance, and activity remain in one workspace.",
    breadcrumbs: ["IKMS", "Customer360"],
  },
  {
    matches: (pathname: string) => pathname === "/review-queue",
    title: "Review",
    subtitle: "Resolve unlinked and low-confidence intake items in a permission-aware workspace.",
    breadcrumbs: ["IKMS", "Review"],
  },
  {
    matches: (pathname: string) => /^\/review-queue\/[^/]+$/.test(pathname),
    title: "Review Detail",
    subtitle: "Validate extracted metadata, review evidence context, and complete the next permitted action.",
    breadcrumbs: ["IKMS", "Review", "Review Detail"],
  },
  {
    matches: (pathname: string) => pathname === "/intake",
    title: "Intake",
    subtitle: "Manual upload and intake monitoring stay available inside the shared operational shell.",
    breadcrumbs: ["IKMS", "Intake"],
  },
  {
    matches: (pathname: string) => pathname === "/knowledge-quality",
    title: "Knowledge Quality",
    subtitle: "Evaluate customer knowledge quality, guide steward corrections, and trigger controlled revalidation.",
    breadcrumbs: ["IKMS", "Knowledge Quality"],
  },
  {
    matches: (pathname: string) => pathname === "/administration",
    title: "Administration",
    subtitle: "Manage configuration, intake, review, AI, and operational rules.",
    breadcrumbs: ["IKMS", "Administration"],
  },
  {
    matches: (pathname: string) => pathname === "/audit",
    title: "Audit",
    subtitle: "Search immutable operational history and export authorized records.",
    breadcrumbs: ["IKMS", "Audit"],
  },
] as const;

export function IkmsAppShell() {
  const location = useLocation();
  const navigate = useNavigate();
  const theme = useTheme();
  const queryClient = useQueryClient();
  const currentUserQuery = useCurrentUser();
  const isDesktopNav = useMediaQuery(theme.breakpoints.up(1200));
  const [mobileOpen, setMobileOpen] = useState(false);
  const [userMenuAnchor, setUserMenuAnchor] = useState<null | HTMLElement>(null);
  const [globalSearch, setGlobalSearch] = useState("");
  const [contextCollapsed, setContextCollapsed] = useState(false);
  const [workspaceChrome, setWorkspaceChrome] = useState<ShellWorkspaceChrome>({});
  const clearWorkspaceChrome = useCallback(() => setWorkspaceChrome({}), []);
  const defaultContextSections = useMemo(
    () => [
      {
        key: "ai-brief",
        title: "AI Brief",
        content: (
          <Typography variant="body2" color="text.secondary">
            Shared foundation only. Workspace-specific AI context sections will be introduced in later prompts.
          </Typography>
        ),
      },
      {
        key: "quick-actions",
        title: "Quick Actions",
        content: (
          <Stack spacing={0.75}>
            <Typography variant="body2" color="text.secondary">
              Use the primary navigation to switch workspaces without losing the authenticated shell.
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Direct route access is permission-guarded before business content is rendered.
            </Typography>
          </Stack>
        ),
      },
    ],
    [],
  );
  const contextSections = workspaceChrome.contextSections ?? defaultContextSections;
  const contextTitle = workspaceChrome.contextTitle ?? "Context";
  const contextWidth = workspaceChrome.contextWidth;
  const outletContext = useMemo<IkmsShellOutletContext>(
    () => ({
      setWorkspaceChrome,
      clearWorkspaceChrome,
    }),
    [clearWorkspaceChrome],
  );

  const signOut = useMutation({
    mutationFn: logout,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: currentUserQueryKey });
      navigate("/login", { replace: true });
    },
  });

  if (!currentUserQuery.data) {
    return (
      <main style={{ minHeight: "100vh", display: "grid", placeItems: "center", padding: 16 }}>
        <RestrictedContentState
          title="Workspace unavailable"
          message="The shared application shell requires an authenticated user session."
        />
      </main>
    );
  }

  const user = currentUserQuery.data;
  const visibleNavItems = shellNavItems.filter((item) => user.permissions.includes(item.permission));
  const activeNavItem =
    visibleNavItems.find((item) => location.pathname === item.to || location.pathname.startsWith(`${item.to}/`)) ??
    visibleNavItems[0];
  const activeRouteMeta = routeMeta.find((item) => item.matches(location.pathname));
  const headerTitle = workspaceChrome.title ?? activeRouteMeta?.title ?? activeNavItem?.label ?? "IKMS";
  const headerSubtitle = workspaceChrome.subtitle ?? activeRouteMeta?.subtitle;
  const headerBreadcrumbs =
    workspaceChrome.breadcrumbs ??
    (activeNavItem
      ? (activeRouteMeta?.breadcrumbs ?? ["IKMS", activeNavItem.label]).map((item) => ({ label: item }))
      : undefined);
  const sharedGlobalSearchValue = workspaceChrome.globalSearchValue ?? globalSearch;
  const sharedGlobalSearchPlaceholder =
    workspaceChrome.globalSearchPlaceholder ?? "Global search entry point";
  const isSearchWorkspace = location.pathname === "/search";

  function handleGlobalSearchChange(value: string) {
    if (workspaceChrome.onGlobalSearchChange) {
      workspaceChrome.onGlobalSearchChange(value);
      return;
    }
    setGlobalSearch(value);
  }

  function handleGlobalSearchSubmit() {
    if (workspaceChrome.onGlobalSearchSubmit) {
      workspaceChrome.onGlobalSearchSubmit();
      return;
    }

    const trimmed = sharedGlobalSearchValue.trim();
    navigate(trimmed ? `/search?q=${encodeURIComponent(trimmed)}` : "/search");
  }

  const drawerContent = (
    <Box sx={{ display: "grid", gridTemplateRows: "auto 1fr auto", height: "100%" }}>
      <Box sx={{ p: 2 }}>
        <Stack spacing={0.5}>
          <Box
            component="img"
            src={ikmsLogoDrawer}
            alt="IKMS logo"
            sx={{
              width: 172,
              height: 88,
              objectFit: "contain",
              objectPosition: "left center",
              display: "block",
            }}
          />
          <Typography variant="caption" color="rgba(255,255,255,0.72)">
            Enterprise Knowledge Workspace
          </Typography>
        </Stack>
      </Box>

      <List sx={{ px: 1 }}>
        {visibleNavItems.map((item) => (
          <ListItemButton
            key={item.to}
            component={NavLink}
            to={item.to}
            onClick={() => setMobileOpen(false)}
            selected={activeNavItem?.to === item.to}
            sx={{
              borderRadius: 1,
              mb: 0.5,
              color: "rgba(255,255,255,0.86)",
              "&.Mui-selected": {
                backgroundColor: "rgba(255,255,255,0.12)",
                color: "#FFFFFF",
              },
            }}
          >
            <ListItemIcon sx={{ color: "inherit", minWidth: 32 }}>{item.icon}</ListItemIcon>
            <ListItemText primary={item.label} primaryTypographyProps={{ variant: "body2", fontWeight: 600 }} />
          </ListItemButton>
        ))}
      </List>

      <Box sx={{ p: 2 }}>
        <Divider sx={{ mb: 1.5, borderColor: "rgba(255,255,255,0.12)" }} />
        <Stack spacing={0.5}>
          <Typography variant="body2" sx={{ color: "#FFFFFF", fontWeight: 600 }}>
            {user.displayName}
          </Typography>
          <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.72)" }}>
            {user.roles.join(", ")}
          </Typography>
        </Stack>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
      <AppBar
        position="sticky"
        color="inherit"
        elevation={0}
        sx={{
          borderBottom: (appTheme) => `1px solid ${appTheme.palette.divider}`,
          backgroundColor: "background.paper",
        }}
      >
        <Toolbar sx={{ minHeight: "48px !important", gap: 1 }}>
          {!isDesktopNav ? (
            <IconButton aria-label="Open navigation" onClick={() => setMobileOpen(true)}>
              <MenuOutlinedIcon fontSize="small" />
            </IconButton>
          ) : null}

          <Box
            component="img"
            src={ikmsLogoCompact}
            alt="IKMS logo"
            sx={{
              width: { xs: 88, sm: 96, md: 104 },
              height: { xs: 32, sm: 34, md: 36 },
              minWidth: { xs: 88, sm: 96, md: 104 },
              objectFit: "contain",
              objectPosition: "left center",
              display: "block",
            }}
          />

          {isSearchWorkspace ? (
            <Tooltip title="Focus Search workspace input">
              <IconButton
                aria-label="Focus Search workspace input"
                onClick={() => handleGlobalSearchSubmit()}
                sx={{ ml: "auto", mr: 1 }}
              >
                <KeyboardCommandKeyOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          ) : (
            <TextField
              value={sharedGlobalSearchValue}
              onChange={(event) => handleGlobalSearchChange(event.target.value)}
              onFocus={() => {
                if (location.pathname !== "/search") {
                  navigate("/search");
                }
              }}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  handleGlobalSearchSubmit();
                }
              }}
              placeholder={sharedGlobalSearchPlaceholder}
              size="small"
              sx={{ width: { xs: "100%", md: 320 }, ml: "auto", mr: 1 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchOutlinedIcon fontSize="small" />
                  </InputAdornment>
                ),
              }}
            />
          )}

          <Tooltip title="Notifications">
            <IconButton aria-label="Notifications">
              <Badge color="error" variant="dot" invisible>
                <NotificationsOutlinedIcon fontSize="small" />
              </Badge>
            </IconButton>
          </Tooltip>

          <Tooltip title="Help">
            <IconButton aria-label="Help">
              <HelpOutlineOutlinedIcon fontSize="small" />
            </IconButton>
          </Tooltip>

          <Tooltip title="User menu">
            <IconButton aria-label="User menu" onClick={(event) => setUserMenuAnchor(event.currentTarget)}>
              <AccountCircleOutlinedIcon fontSize="small" />
            </IconButton>
          </Tooltip>

          <Menu
            anchorEl={userMenuAnchor}
            open={Boolean(userMenuAnchor)}
            onClose={() => setUserMenuAnchor(null)}
          >
            <MenuItem disabled>{user.username}</MenuItem>
            <MenuItem
              onClick={() => {
                setUserMenuAnchor(null);
                signOut.mutate();
              }}
            >
              <ListItemIcon sx={{ minWidth: 28 }}>
                <LogoutOutlinedIcon fontSize="small" />
              </ListItemIcon>
              Sign out
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: {
            xs: "1fr",
            lg: "232px minmax(0, 1fr)",
          },
          minHeight: "calc(100vh - 49px)",
        }}
      >
        {isDesktopNav ? (
          <Drawer
            variant="permanent"
            open
            PaperProps={{
              sx: {
                width: 232,
                position: "static",
                bgcolor: "secondary.main",
                color: "secondary.contrastText",
                borderRight: "none",
              },
            }}
          >
            {drawerContent}
          </Drawer>
        ) : (
          <Drawer
            variant="temporary"
            open={mobileOpen}
            onClose={() => setMobileOpen(false)}
            ModalProps={{ keepMounted: true }}
            PaperProps={{
              sx: {
                width: 280,
                bgcolor: "secondary.main",
                color: "secondary.contrastText",
              },
            }}
          >
            {drawerContent}
          </Drawer>
        )}

        <Box sx={{ minWidth: 0, p: 2 }}>
          <Stack spacing={2}>
            {activeNavItem ? (
              <WorkspaceHeader
                breadcrumbs={headerBreadcrumbs}
                title={headerTitle}
                subtitle={headerSubtitle}
                primaryActions={workspaceChrome.primaryActions}
                secondaryActions={workspaceChrome.secondaryActions}
              />
            ) : null}

            <Box
              sx={{
                display: "grid",
                gridTemplateColumns: {
                  xs: "1fr",
                  xl: "minmax(0, 1fr) auto",
                },
                gap: 2,
                alignItems: "start",
              }}
            >
              <Box sx={{ minWidth: 0 }}>
                <Outlet
                  context={outletContext}
                />
              </Box>
              <RightContextPanel
                title={contextTitle}
                sections={contextSections}
                collapsed={contextCollapsed}
                onToggle={() => setContextCollapsed((value) => !value)}
                width={contextWidth}
              />
            </Box>
          </Stack>
        </Box>
      </Box>
    </Box>
  );
}
