import PlaceOutlinedIcon from "@mui/icons-material/PlaceOutlined";
import { Button, Stack, Typography } from "@mui/material";
import type { AssistantEvidenceReference } from "./assistantTypes";

export function EvidenceReference({ reference }: { reference: AssistantEvidenceReference }) {
  return (
    <Button
      size="small"
      variant="text"
      color="inherit"
      onClick={reference.onClick}
      disabled={reference.disabled ?? !reference.onClick}
      aria-label={reference.label}
      sx={{ justifyContent: "flex-start", textAlign: "left", px: 0 }}
    >
      <Stack direction="row" spacing={0.75} alignItems="center">
        <PlaceOutlinedIcon fontSize="small" color="action" />
        <Stack spacing={0} alignItems="flex-start">
          <Typography variant="body2">{reference.label}</Typography>
          {reference.detail ? (
            <Typography variant="caption" color="text.secondary">
              {reference.detail}
            </Typography>
          ) : null}
        </Stack>
      </Stack>
    </Button>
  );
}
