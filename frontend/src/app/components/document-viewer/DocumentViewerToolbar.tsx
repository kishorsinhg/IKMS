import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import ChevronLeftOutlinedIcon from "@mui/icons-material/ChevronLeftOutlined";
import ChevronRightOutlinedIcon from "@mui/icons-material/ChevronRightOutlined";
import DownloadOutlinedIcon from "@mui/icons-material/DownloadOutlined";
import FindInPageOutlinedIcon from "@mui/icons-material/FindInPageOutlined";
import FitScreenOutlinedIcon from "@mui/icons-material/FitScreenOutlined";
import HeightOutlinedIcon from "@mui/icons-material/HeightOutlined";
import MoreHorizOutlinedIcon from "@mui/icons-material/MoreHorizOutlined";
import OpenInNewOutlinedIcon from "@mui/icons-material/OpenInNewOutlined";
import RotateRightOutlinedIcon from "@mui/icons-material/RotateRightOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import ZoomInOutlinedIcon from "@mui/icons-material/ZoomInOutlined";
import ZoomOutOutlinedIcon from "@mui/icons-material/ZoomOutOutlined";
import {
  Box,
  Button,
  IconButton,
  InputAdornment,
  Menu,
  MenuItem,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import { useState } from "react";

export interface DocumentViewerToolbarAction {
  key: string;
  label: string;
  onClick: () => void;
}

export interface DocumentViewerToolbarProps {
  title: string;
  subtitle?: string;
  canGoBack?: boolean;
  onBack?: () => void;
  zoomPercent: number;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onFitWidth: () => void;
  onFitPage: () => void;
  onRotate: () => void;
  pageIndex: number;
  pageCount: number;
  onPreviousPage: () => void;
  onNextPage: () => void;
  searchValue: string;
  onSearchChange: (value: string) => void;
  searchPlaceholder?: string;
  onDownload?: () => void;
  onOpenOriginal?: () => void;
  originalActionLabel?: string;
  onToggleEvidence?: () => void;
  evidenceToggleLabel?: string;
  overflowActions?: DocumentViewerToolbarAction[];
}

export function DocumentViewerToolbar({
  title,
  subtitle,
  canGoBack = false,
  onBack,
  zoomPercent,
  onZoomIn,
  onZoomOut,
  onFitWidth,
  onFitPage,
  onRotate,
  pageIndex,
  pageCount,
  onPreviousPage,
  onNextPage,
  searchValue,
  onSearchChange,
  searchPlaceholder = "Search within document",
  onDownload,
  onOpenOriginal,
  originalActionLabel = "Open original",
  onToggleEvidence,
  evidenceToggleLabel = "Evidence",
  overflowActions = [],
}: DocumentViewerToolbarProps) {
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);

  return (
    <Box
      sx={{
        border: (theme) => `1px solid ${theme.palette.divider}`,
        borderRadius: 1,
        backgroundColor: "background.paper",
        px: 1.25,
        py: 1,
      }}
    >
      <Stack spacing={1}>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1}>
          <Stack spacing={0.25} sx={{ minWidth: 0 }}>
            <Typography variant="subtitle2" noWrap>
              {title}
            </Typography>
            {subtitle ? (
              <Typography variant="body2" color="text.secondary" noWrap>
                {subtitle}
              </Typography>
            ) : null}
          </Stack>
          {canGoBack && onBack ? (
            <Button
              size="small"
              variant="text"
              color="inherit"
              startIcon={<ArrowBackOutlinedIcon fontSize="small" />}
              onClick={onBack}
            >
              Back
            </Button>
          ) : null}
        </Stack>

        <Stack
          direction={{ xs: "column", xl: "row" }}
          spacing={1}
          justifyContent="space-between"
          alignItems={{ xs: "stretch", xl: "center" }}
        >
          <Stack direction="row" spacing={0.25} flexWrap="wrap" useFlexGap alignItems="center">
            <Tooltip title="Zoom out">
              <span>
                <IconButton size="small" aria-label="Zoom out" onClick={onZoomOut}>
                  <ZoomOutOutlinedIcon fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>
            <Tooltip title="Zoom in">
              <span>
                <IconButton size="small" aria-label="Zoom in" onClick={onZoomIn}>
                  <ZoomInOutlinedIcon fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>
            <Button size="small" variant="text" color="inherit" onClick={onFitWidth} startIcon={<HeightOutlinedIcon fontSize="small" />}>
              Fit width
            </Button>
            <Button size="small" variant="text" color="inherit" onClick={onFitPage} startIcon={<FitScreenOutlinedIcon fontSize="small" />}>
              Fit page
            </Button>
            <Tooltip title="Rotate">
              <span>
                <IconButton size="small" aria-label="Rotate document" onClick={onRotate}>
                  <RotateRightOutlinedIcon fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>
            <Stack direction="row" spacing={0.25} alignItems="center" sx={{ ml: 0.5 }}>
              <IconButton size="small" aria-label="Previous page" onClick={onPreviousPage} disabled={pageCount <= 1 || pageIndex <= 0}>
                <ChevronLeftOutlinedIcon fontSize="small" />
              </IconButton>
              <Typography variant="caption" color="text.secondary" sx={{ minWidth: 74, textAlign: "center" }}>
                Page {Math.min(pageIndex + 1, Math.max(pageCount, 1))} of {Math.max(pageCount, 1)}
              </Typography>
              <IconButton size="small" aria-label="Next page" onClick={onNextPage} disabled={pageCount <= 1 || pageIndex >= pageCount - 1}>
                <ChevronRightOutlinedIcon fontSize="small" />
              </IconButton>
            </Stack>
            <Typography variant="caption" color="text.secondary" sx={{ ml: 0.5 }}>
              {zoomPercent}%
            </Typography>
          </Stack>

          <Stack direction={{ xs: "column", md: "row" }} spacing={1} alignItems={{ xs: "stretch", md: "center" }}>
            <TextField
              size="small"
              value={searchValue}
              onChange={(event) => onSearchChange(event.target.value)}
              placeholder={searchPlaceholder}
              inputProps={{ "aria-label": searchPlaceholder }}
              sx={{ minWidth: { xs: "100%", md: 260 } }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchOutlinedIcon fontSize="small" />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <FindInPageOutlinedIcon fontSize="small" color="action" />
                  </InputAdornment>
                ),
              }}
            />
            <Stack direction="row" spacing={0.25} justifyContent={{ xs: "space-between", md: "flex-end" }}>
              {onDownload ? (
                <Tooltip title="Download">
                  <span>
                    <IconButton size="small" aria-label="Download document" onClick={onDownload}>
                      <DownloadOutlinedIcon fontSize="small" />
                    </IconButton>
                  </span>
                </Tooltip>
              ) : null}
              {onOpenOriginal ? (
                <Button size="small" variant="outlined" startIcon={<OpenInNewOutlinedIcon fontSize="small" />} onClick={onOpenOriginal}>
                  {originalActionLabel}
                </Button>
              ) : null}
              {onToggleEvidence ? (
                <Button size="small" variant="text" color="inherit" onClick={onToggleEvidence}>
                  {evidenceToggleLabel}
                </Button>
              ) : null}
              {overflowActions.length > 0 ? (
                <>
                  <IconButton size="small" aria-label="More viewer actions" onClick={(event) => setMenuAnchor(event.currentTarget)}>
                    <MoreHorizOutlinedIcon fontSize="small" />
                  </IconButton>
                  <Menu anchorEl={menuAnchor} open={Boolean(menuAnchor)} onClose={() => setMenuAnchor(null)}>
                    {overflowActions.map((action) => (
                      <MenuItem
                        key={action.key}
                        onClick={() => {
                          setMenuAnchor(null);
                          action.onClick();
                        }}
                      >
                        {action.label}
                      </MenuItem>
                    ))}
                  </Menu>
                </>
              ) : null}
            </Stack>
          </Stack>
        </Stack>
      </Stack>
    </Box>
  );
}
