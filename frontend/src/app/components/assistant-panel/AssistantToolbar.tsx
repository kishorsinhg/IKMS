import AutorenewOutlinedIcon from "@mui/icons-material/AutorenewOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import { Button, IconButton, Stack, Typography } from "@mui/material";
import type { ReactNode } from "react";

export function AssistantToolbar({
  title,
  subtitle,
  actions,
  onRetry,
  onClose,
}: {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
  onRetry?: () => void;
  onClose?: () => void;
}) {
  return (
    <Stack spacing={1} sx={{ px: 1.5, py: 1.25, borderBottom: (theme) => `1px solid ${theme.palette.divider}` }}>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1}>
        <Stack spacing={0.25} sx={{ minWidth: 0 }}>
          <Typography variant="subtitle2" component="h2">
            {title}
          </Typography>
          {subtitle ? (
            <Typography variant="body2" color="text.secondary">
              {subtitle}
            </Typography>
          ) : null}
        </Stack>
        <Stack direction="row" spacing={0.5} alignItems="center">
          {onRetry ? (
            <Button size="small" variant="text" color="inherit" startIcon={<AutorenewOutlinedIcon fontSize="small" />} onClick={onRetry}>
              Retry
            </Button>
          ) : null}
          {actions}
          {onClose ? (
            <IconButton size="small" aria-label="Close assistant panel" onClick={onClose}>
              <CloseOutlinedIcon fontSize="small" />
            </IconButton>
          ) : null}
        </Stack>
      </Stack>
    </Stack>
  );
}
