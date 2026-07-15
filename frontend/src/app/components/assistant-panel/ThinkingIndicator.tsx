import SmartToyOutlinedIcon from "@mui/icons-material/SmartToyOutlined";
import { CircularProgress, Stack, Typography } from "@mui/material";

export function ThinkingIndicator({
  label = "Assistant is preparing a response",
}: {
  label?: string;
}) {
  return (
    <Stack direction="row" spacing={1} alignItems="center" aria-live="polite">
      <CircularProgress size={16} />
      <SmartToyOutlinedIcon fontSize="small" color="action" />
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
    </Stack>
  );
}
