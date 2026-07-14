import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import ErrorOutlineOutlinedIcon from "@mui/icons-material/ErrorOutlineOutlined";
import InboxOutlinedIcon from "@mui/icons-material/InboxOutlined";
import SearchOffOutlinedIcon from "@mui/icons-material/SearchOffOutlined";
import { Button, CircularProgress, Paper, Stack, Typography } from "@mui/material";
import { ReactNode } from "react";

interface WorkspaceStateProps {
  icon?: ReactNode;
  title: string;
  message: string;
  action?: ReactNode;
  compact?: boolean;
}

export function LoadingState({
  title = "Loading workspace",
  message = "The current workspace is loading.",
}: Partial<WorkspaceStateProps>) {
  return (
    <WorkspaceState
      icon={<CircularProgress size={22} />}
      title={title}
      message={message}
    />
  );
}

export function EmptyState({
  title,
  message,
  action,
  compact,
}: Omit<WorkspaceStateProps, "icon">) {
  return (
    <WorkspaceState
      icon={<InboxOutlinedIcon color="action" />}
      title={title}
      message={message}
      action={action}
      compact={compact}
    />
  );
}

export function NoResultsState({
  title = "No matching results",
  message = "There are no results for the current search or filters.",
  action,
  compact,
}: Partial<WorkspaceStateProps>) {
  return (
    <WorkspaceState
      icon={<SearchOffOutlinedIcon color="action" />}
      title={title}
      message={message}
      action={action}
      compact={compact}
    />
  );
}

export function ErrorState({
  title,
  message,
  action,
}: Omit<WorkspaceStateProps, "icon" | "compact">) {
  return (
    <WorkspaceState
      icon={<ErrorOutlineOutlinedIcon color="error" />}
      title={title}
      message={message}
      action={action}
    />
  );
}

export function UnauthorizedState({
  title,
  message,
  action,
}: Omit<WorkspaceStateProps, "icon" | "compact">) {
  return (
    <WorkspaceState
      icon={<LockOutlinedIcon color="warning" />}
      title={title}
      message={message}
      action={action}
    />
  );
}

export function RestrictedContentState({
  title,
  message,
  action,
}: Omit<WorkspaceStateProps, "icon" | "compact">) {
  return (
    <WorkspaceState
      icon={<LockOutlinedIcon color="warning" />}
      title={title}
      message={message}
      action={action}
    />
  );
}

export function RetryAction({ label = "Retry", onClick }: { label?: string; onClick: () => void }) {
  return (
    <Button size="small" variant="outlined" onClick={onClick}>
      {label}
    </Button>
  );
}

function WorkspaceState({
  icon,
  title,
  message,
  action,
  compact = false,
}: WorkspaceStateProps) {
  return (
    <Paper
      sx={{
        p: compact ? 2 : 3,
        display: "grid",
        placeItems: "center",
        minHeight: compact ? 160 : 220,
        border: (theme) => `1px solid ${theme.palette.divider}`,
        textAlign: "center",
      }}
    >
      <Stack spacing={1.25} alignItems="center" maxWidth={480}>
        {icon}
        <Typography variant="h3">{title}</Typography>
        <Typography variant="body2" color="text.secondary">
          {message}
        </Typography>
        {action ? <Stack direction="row" spacing={1}>{action}</Stack> : null}
      </Stack>
    </Paper>
  );
}
