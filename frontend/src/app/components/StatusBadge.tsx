import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import InfoOutlinedIcon from "@mui/icons-material/InfoOutlined";
import CheckCircleOutlineOutlinedIcon from "@mui/icons-material/CheckCircleOutlineOutlined";
import WarningAmberOutlinedIcon from "@mui/icons-material/WarningAmberOutlined";
import ErrorOutlineOutlinedIcon from "@mui/icons-material/ErrorOutlineOutlined";
import RemoveOutlinedIcon from "@mui/icons-material/RemoveOutlined";
import { Chip, ChipProps } from "@mui/material";

export type StatusTone = "success" | "warning" | "error" | "info" | "neutral" | "restricted";

const toneConfig: Record<
  StatusTone,
  {
    chipColor: ChipProps["color"];
    icon: React.ReactElement;
  }
> = {
  success: { chipColor: "success", icon: <CheckCircleOutlineOutlinedIcon fontSize="inherit" /> },
  warning: { chipColor: "warning", icon: <WarningAmberOutlinedIcon fontSize="inherit" /> },
  error: { chipColor: "error", icon: <ErrorOutlineOutlinedIcon fontSize="inherit" /> },
  info: { chipColor: "info", icon: <InfoOutlinedIcon fontSize="inherit" /> },
  neutral: { chipColor: "default", icon: <RemoveOutlinedIcon fontSize="inherit" /> },
  restricted: { chipColor: "default", icon: <LockOutlinedIcon fontSize="inherit" /> },
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
      variant={variant ?? (tone === "neutral" || tone === "restricted" ? "outlined" : "filled")}
      icon={config.icon}
      label={label}
    />
  );
}
