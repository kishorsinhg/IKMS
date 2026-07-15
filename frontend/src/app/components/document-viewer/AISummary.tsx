import AutoAwesomeOutlinedIcon from "@mui/icons-material/AutoAwesomeOutlined";
import ErrorOutlineOutlinedIcon from "@mui/icons-material/ErrorOutlineOutlined";
import RemoveCircleOutlineOutlinedIcon from "@mui/icons-material/RemoveCircleOutlineOutlined";
import { Alert, Box, CircularProgress, Stack, Typography } from "@mui/material";
import { ConfidenceIndicator } from "./ConfidenceIndicator";
import type { AiSummaryState, ConfidenceLevel } from "./evidenceWorkspaceTypes";

export function AISummary({
  state,
  summary,
  confidence = "UNKNOWN",
  supportingNotes = [],
}: {
  state: AiSummaryState;
  summary?: string;
  confidence?: ConfidenceLevel;
  supportingNotes?: string[];
}) {
  if (state === "loading") {
    return (
      <Stack direction="row" spacing={1} alignItems="center" aria-live="polite">
        <CircularProgress size={18} />
        <Typography variant="body2" color="text.secondary">
          Loading AI summary
        </Typography>
      </Stack>
    );
  }

  if (state === "error") {
    return (
      <Alert severity="error" variant="outlined" icon={<ErrorOutlineOutlinedIcon fontSize="inherit" />}>
        AI summary is temporarily unavailable.
      </Alert>
    );
  }

  if (state === "unavailable" || !summary) {
    return (
      <Alert severity="info" variant="outlined" icon={<RemoveCircleOutlineOutlinedIcon fontSize="inherit" />}>
        No AI summary is available from the current API response.
      </Alert>
    );
  }

  return (
    <Stack spacing={1}>
      <Stack direction="row" spacing={0.75} alignItems="center" flexWrap="wrap" useFlexGap>
        <ConfidenceIndicator value={confidence} />
        <Stack direction="row" spacing={0.5} alignItems="center">
          <AutoAwesomeOutlinedIcon fontSize="small" color="action" />
          <Typography variant="caption" color="text.secondary">
            Evidence-constrained summary
          </Typography>
        </Stack>
      </Stack>
      <Typography variant="body2">{summary}</Typography>
      {supportingNotes.length > 0 ? (
        <Box component="ul" sx={{ pl: 2.25, my: 0, display: "grid", gap: 0.5 }}>
          {supportingNotes.map((note) => (
            <Typography key={note} component="li" variant="body2" color="text.secondary">
              {note}
            </Typography>
          ))}
        </Box>
      ) : null}
    </Stack>
  );
}
