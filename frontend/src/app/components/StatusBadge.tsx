import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import InfoOutlinedIcon from "@mui/icons-material/InfoOutlined";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import WarningAmberOutlinedIcon from "@mui/icons-material/WarningAmberOutlined";
import ErrorOutlineOutlinedIcon from "@mui/icons-material/ErrorOutlineOutlined";
import RemoveOutlinedIcon from "@mui/icons-material/RemoveOutlined";
import { Chip, ChipProps } from "@mui/material";

export type StatusTone = "success" | "warning" | "error" | "info" | "neutral" | "restricted" | "pending";

const toneConfig: Record<
  StatusTone,
  {
    chipColor: ChipProps["color"];
    icon: React.ReactElement;
    variant?: ChipProps["variant"];
    sx?: ChipProps["sx"];
  }
> = {
  success: {
    chipColor: "success",
    icon: <CheckCircleOutlineOutlinedIcon fontSize="inherit" />,
    sx: { fontWeight: 600 },
  },
  warning: {
    chipColor: "warning",
    icon: <WarningAmberOutlinedIcon fontSize="inherit" />,
    sx: { fontWeight: 600 },
  },
  error: {
    chipColor: "error",
    icon: <ErrorOutlineOutlinedIcon fontSize="inherit" />,
    sx: { fontWeight: 600 },
  },
  info: {
    chipColor: "info",
    icon: <InfoOutlinedIcon fontSize="inherit" />,
    sx: { fontWeight: 600 },
  },
  neutral: {
    chipColor: "default",
    icon: <RemoveOutlinedIcon fontSize="inherit" />,
    variant: "outlined",
    sx: { fontWeight: 500, color: "text.secondary" },
  },
  restricted: {
    chipColor: "default",
    icon: <LockOutlinedIcon fontSize="inherit" />,
    variant: "outlined",
    sx: { fontWeight: 600, color: "text.primary", borderColor: "divider", backgroundColor: "action.hover" },
  },
  pending: {
    chipColor: "warning",
    icon: <InfoOutlinedIcon fontSize="inherit" />,
    variant: "outlined",
    sx: { fontWeight: 600 },
  },
};

export interface StatusBadgeProps {
  label: string;
  tone?: StatusTone;
  size?: ChipProps["size"];
  variant?: ChipProps["variant"];
}

export function StatusBadge({ label, tone = "neutral", size = "small", variant }: StatusBadgeProps) {
  const config = toneConfig[tone];

  return (
    <Chip
      size={size}
      color={config.chipColor}
      variant={variant ?? config.variant ?? "filled"}
      icon={config.icon}
      label={label}
      sx={config.sx}
    />
  );
}
