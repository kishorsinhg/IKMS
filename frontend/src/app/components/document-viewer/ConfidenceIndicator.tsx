import CheckCircleOutlinedIcon from "@mui/icons-material/CheckCircleOutlined";
import HelpOutlineOutlinedIcon from "@mui/icons-material/HelpOutlineOutlined";
import PriorityHighOutlinedIcon from "@mui/icons-material/PriorityHighOutlined";
import ReportProblemOutlinedIcon from "@mui/icons-material/ReportProblemOutlined";
import { Chip, type ChipProps } from "@mui/material";
import type { ReactElement } from "react";
import type { ConfidenceLevel } from "./evidenceWorkspaceTypes";

export function ConfidenceIndicator({
  value,
  compact = false,
}: {
  value: ConfidenceLevel;
  compact?: boolean;
}) {
  const config = getConfidenceConfig(value);

  return (
    <Chip
      size="small"
      variant={compact ? "outlined" : "filled"}
      color={config.color}
      icon={config.icon}
      label={config.label}
      aria-label={`${config.label} confidence`}
    />
  );
}

function getConfidenceConfig(value: ConfidenceLevel): {
  label: string;
  color: ChipProps["color"];
  icon: ReactElement;
} {
  switch (value) {
    case "HIGH":
      return {
        label: "High",
        color: "success",
        icon: <CheckCircleOutlinedIcon fontSize="small" />,
      };
    case "MEDIUM":
      return {
        label: "Medium",
        color: "warning",
        icon: <PriorityHighOutlinedIcon fontSize="small" />,
      };
    case "LOW":
      return {
        label: "Low",
        color: "error",
        icon: <ReportProblemOutlinedIcon fontSize="small" />,
      };
    default:
      return {
        label: "Unknown",
        color: "default",
        icon: <HelpOutlineOutlinedIcon fontSize="small" />,
      };
  }
}
