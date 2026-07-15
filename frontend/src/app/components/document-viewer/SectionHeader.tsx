import ExpandMoreOutlinedIcon from "@mui/icons-material/ExpandMoreOutlined";
import { AccordionSummary, Chip, Stack, Typography } from "@mui/material";

export function SectionHeader({
  title,
  summary,
  countLabel,
}: {
  title: string;
  summary?: string;
  countLabel?: string;
}) {
  return (
    <AccordionSummary
      expandIcon={<ExpandMoreOutlinedIcon fontSize="small" />}
      aria-controls={`${title}-content`}
      id={`${title}-header`}
      sx={{
        minHeight: 46,
        "& .MuiAccordionSummary-content": {
          my: 0.875,
        },
      }}
    >
      <Stack direction="row" spacing={1} justifyContent="space-between" alignItems="center" sx={{ width: "100%" }}>
        <Stack spacing={0.25} sx={{ minWidth: 0 }}>
          <Typography variant="subtitle2">{title}</Typography>
          {summary ? (
            <Typography variant="caption" color="text.secondary" noWrap>
              {summary}
            </Typography>
          ) : null}
        </Stack>
        {countLabel ? <Chip size="small" label={countLabel} variant="outlined" /> : null}
      </Stack>
    </AccordionSummary>
  );
}
