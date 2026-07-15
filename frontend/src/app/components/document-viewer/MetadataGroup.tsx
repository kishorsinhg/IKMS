import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import TaskAltOutlinedIcon from "@mui/icons-material/TaskAltOutlined";
import WarningAmberOutlinedIcon from "@mui/icons-material/WarningAmberOutlined";
import {
  Chip,
  Stack,
  Typography,
} from "@mui/material";
import { ConfidenceIndicator } from "./ConfidenceIndicator";
import type { MetadataFieldDescriptor, MetadataFieldState } from "./evidenceWorkspaceTypes";

export function MetadataGroup({
  title,
  fields,
}: {
  title: string;
  fields: MetadataFieldDescriptor[];
}) {
  return (
    <Stack spacing={1}>
      <Typography variant="body2" fontWeight={600}>
        {title}
      </Typography>
      <Stack spacing={0.9}>
        {fields.map((field) => (
          <Stack
            key={field.key}
            spacing={0.5}
            sx={{
              p: 1,
              border: (theme) => `1px solid ${theme.palette.divider}`,
              borderRadius: 1,
              backgroundColor: "background.default",
            }}
          >
            <Stack direction="row" spacing={0.75} justifyContent="space-between" alignItems="flex-start">
              <Stack spacing={0.25} sx={{ minWidth: 0 }}>
                <Typography variant="caption" color="text.secondary">
                  {field.label}
                </Typography>
                <Typography variant="body2">
                  {field.value?.trim() ? field.value : "No value available"}
                </Typography>
              </Stack>
              <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap justifyContent="flex-end">
                <Chip
                  size="small"
                  variant="outlined"
                  color={stateChipColor(field.state)}
                  icon={stateChipIcon(field.state)}
                  label={stateChipLabel(field.state)}
                />
                {field.confidence ? <ConfidenceIndicator value={field.confidence} compact /> : null}
              </Stack>
            </Stack>
            {field.helperText ? (
              <Typography variant="caption" color="text.secondary">
                {field.helperText}
              </Typography>
            ) : null}
          </Stack>
        ))}
      </Stack>
    </Stack>
  );
}

function stateChipLabel(value: MetadataFieldState) {
  switch (value) {
    case "VERIFIED":
      return "Verified";
    case "NEEDS_REVIEW":
      return "Needs review";
    case "MISSING":
      return "Missing";
    case "READ_ONLY":
      return "Read only";
  }
}

function stateChipColor(value: MetadataFieldState): "success" | "warning" | "default" {
  switch (value) {
    case "VERIFIED":
      return "success";
    case "NEEDS_REVIEW":
      return "warning";
    case "MISSING":
      return "warning";
    default:
      return "default";
  }
}

function stateChipIcon(value: MetadataFieldState) {
  switch (value) {
    case "VERIFIED":
      return <TaskAltOutlinedIcon fontSize="small" />;
    case "NEEDS_REVIEW":
    case "MISSING":
      return <WarningAmberOutlinedIcon fontSize="small" />;
    case "READ_ONLY":
      return <LockOutlinedIcon fontSize="small" />;
  }
}
