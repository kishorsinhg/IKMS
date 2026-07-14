import MoreHorizOutlinedIcon from "@mui/icons-material/MoreHorizOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import TuneOutlinedIcon from "@mui/icons-material/TuneOutlined";
import ViewColumnOutlinedIcon from "@mui/icons-material/ViewColumnOutlined";
import DownloadOutlinedIcon from "@mui/icons-material/DownloadOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import { ReactNode, Ref, useState } from "react";
import {
  Box,
  Chip,
  IconButton,
  InputAdornment,
  Menu,
  MenuItem,
  Stack,
  TextField,
  Tooltip,
} from "@mui/material";
import type { TextFieldProps } from "@mui/material";

export interface ToolbarAction {
  key: string;
  label: string;
  onClick?: () => void;
}

export interface ActiveToolbarFilter {
  key: string;
  label: string;
  onDelete?: () => void;
}

export interface WorkspaceToolbarProps {
  searchPlaceholder?: string;
  searchValue?: string;
  onSearchChange?: (value: string) => void;
  searchInputRef?: Ref<HTMLInputElement>;
  searchAriaLabel?: string;
  onSearchKeyDown?: TextFieldProps["onKeyDown"];
  filters?: ReactNode;
  activeFilters?: ActiveToolbarFilter[];
  bulkActions?: ReactNode;
  primaryAction?: ReactNode;
  onRefresh?: () => void;
  onExport?: () => void;
  onColumns?: () => void;
  secondaryActions?: ToolbarAction[];
}

export function WorkspaceToolbar({
  searchPlaceholder,
  searchValue,
  onSearchChange,
  searchInputRef,
  searchAriaLabel,
  onSearchKeyDown,
  filters,
  activeFilters,
  bulkActions,
  primaryAction,
  onRefresh,
  onExport,
  onColumns,
  secondaryActions,
}: WorkspaceToolbarProps) {
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null);

  return (
    <Box
      sx={{
        display: "grid",
        gap: 0.75,
        px: 1.5,
        py: 1,
        border: (theme) => `1px solid ${theme.palette.divider}`,
        borderRadius: 1,
        backgroundColor: "background.paper",
      }}
    >
      <Stack
        direction={{ xs: "column", lg: "row" }}
        spacing={1}
        justifyContent="space-between"
        alignItems={{ xs: "stretch", lg: "center" }}
        sx={{ minWidth: 0 }}
      >
        <Stack
          direction={{ xs: "column", md: "row" }}
          spacing={1}
          flexWrap="wrap"
          alignItems={{ md: "center" }}
          sx={{ minWidth: 0, flex: 1 }}
        >
          {onSearchChange ? (
            <TextField
              size="small"
              inputRef={searchInputRef}
              aria-label={searchAriaLabel ?? searchPlaceholder ?? "Search"}
              value={searchValue ?? ""}
              onChange={(event) => onSearchChange(event.target.value)}
              onKeyDown={onSearchKeyDown}
              placeholder={searchPlaceholder ?? "Search"}
              sx={{ minWidth: { xs: "100%", md: 320 }, flex: 1.2 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchOutlinedIcon fontSize="small" />
                  </InputAdornment>
                ),
              }}
            />
          ) : null}
          {filters}
          {bulkActions}
        </Stack>

        <Stack
          direction="row"
          spacing={0.5}
          alignItems="center"
          justifyContent={{ xs: "space-between", lg: "flex-end" }}
          sx={{ flexShrink: 0, minWidth: 0 }}
        >
          {primaryAction ? (
            <Box sx={{ display: "flex", alignItems: "center", mr: { xs: "auto", lg: 0 } }}>
              {primaryAction}
            </Box>
          ) : null}
          {onRefresh ? (
            <Tooltip title="Refresh">
              <IconButton aria-label="Refresh" onClick={onRefresh}>
                <RefreshOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          ) : null}
          {onExport ? (
            <Tooltip title="Export">
              <IconButton aria-label="Export" onClick={onExport}>
                <DownloadOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          ) : null}
          {onColumns ? (
            <Tooltip title="Column settings">
              <IconButton aria-label="Column settings" onClick={onColumns}>
                <ViewColumnOutlinedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          ) : null}
          {secondaryActions && secondaryActions.length > 0 ? (
            <>
              <Tooltip title="More actions">
                <IconButton aria-label="More actions" onClick={(event) => setMenuAnchor(event.currentTarget)}>
                  <MoreHorizOutlinedIcon fontSize="small" />
                </IconButton>
              </Tooltip>
              <Menu
                anchorEl={menuAnchor}
                open={Boolean(menuAnchor)}
                onClose={() => setMenuAnchor(null)}
              >
                {secondaryActions.map((item) => (
                  <MenuItem
                    key={item.key}
                    onClick={() => {
                      setMenuAnchor(null);
                      item.onClick?.();
                    }}
                  >
                    {item.label}
                  </MenuItem>
                ))}
              </Menu>
            </>
          ) : null}
        </Stack>
      </Stack>

      {activeFilters && activeFilters.length > 0 ? (
        <Stack direction="row" spacing={0.75} useFlexGap flexWrap="wrap" alignItems="center" sx={{ pt: 0.25 }}>
          <TuneOutlinedIcon fontSize="small" color="action" />
          {activeFilters.map((filter) => (
            <Chip
              key={filter.key}
              label={filter.label}
              size="small"
              onDelete={filter.onDelete}
            />
          ))}
        </Stack>
      ) : null}
    </Box>
  );
}
