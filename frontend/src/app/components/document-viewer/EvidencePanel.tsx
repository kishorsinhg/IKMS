import { Accordion, AccordionDetails, Paper, Stack, Typography } from "@mui/material";
import { SectionHeader } from "./SectionHeader";
import type { EvidenceWorkspaceSection } from "./evidenceWorkspaceTypes";

export function EvidencePanel({
  title,
  subtitle,
  sections,
  mobile = false,
}: {
  title: string;
  subtitle?: string;
  sections: EvidenceWorkspaceSection[];
  mobile?: boolean;
}) {
  return (
    <Paper
      variant="outlined"
      sx={{
        display: "grid",
        gridTemplateRows: "auto minmax(0, 1fr)",
        minWidth: 0,
        overflow: "hidden",
        height: mobile ? "auto" : "100%",
      }}
    >
      <Stack spacing={0.35} sx={{ px: 1.25, py: 1, borderBottom: (theme) => `1px solid ${theme.palette.divider}` }}>
        <Typography variant="subtitle2">{title}</Typography>
        {subtitle ? (
          <Typography variant="body2" color="text.secondary">
            {subtitle}
          </Typography>
        ) : null}
      </Stack>
      <Stack spacing={0.9} sx={{ px: 1.25, py: 1.1, overflowY: "auto" }}>
        {sections.map((section) => (
          <Accordion
            key={section.key}
            disableGutters
            defaultExpanded={section.defaultExpanded ?? true}
            variant="outlined"
            sx={{ borderRadius: 1, "&:before": { display: "none" } }}
          >
            <SectionHeader title={section.title} summary={section.summary} countLabel={section.countLabel} />
            <AccordionDetails>{section.content}</AccordionDetails>
          </Accordion>
        ))}
      </Stack>
    </Paper>
  );
}
