import ExpandMoreOutlinedIcon from "@mui/icons-material/ExpandMoreOutlined";
import PlaceOutlinedIcon from "@mui/icons-material/PlaceOutlined";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Button,
  Chip,
  Stack,
  Typography,
} from "@mui/material";
import { ConfidenceIndicator } from "./ConfidenceIndicator";
import type { ConfidenceLevel, EvidenceNavigationAction } from "./evidenceWorkspaceTypes";

export function EvidenceCard({
  title,
  excerpt,
  citation,
  source,
  type,
  confidence,
  metadata = [],
  navigationActions = [],
  defaultExpanded = false,
}: {
  title: string;
  excerpt: string;
  citation?: string;
  source?: string;
  type?: string;
  confidence: ConfidenceLevel;
  metadata?: string[];
  navigationActions?: EvidenceNavigationAction[];
  defaultExpanded?: boolean;
}) {
  return (
    <Accordion disableGutters defaultExpanded={defaultExpanded} variant="outlined" sx={{ borderRadius: 1, "&:before": { display: "none" } }}>
      <AccordionSummary expandIcon={<ExpandMoreOutlinedIcon fontSize="small" />} aria-label={`Expand evidence for ${title}`}>
        <Stack spacing={0.75} sx={{ minWidth: 0, width: "100%" }}>
          <Stack direction="row" spacing={0.75} alignItems="center" flexWrap="wrap" useFlexGap>
            <Typography variant="body2" fontWeight={600}>
              {title}
            </Typography>
            <ConfidenceIndicator value={confidence} compact />
            {type ? <Chip size="small" variant="outlined" label={type} /> : null}
          </Stack>
          <Typography variant="body2" color="text.secondary" noWrap>
            {excerpt}
          </Typography>
        </Stack>
      </AccordionSummary>
      <AccordionDetails>
        <Stack spacing={1}>
          <Typography variant="body2" color="text.secondary">
            {excerpt}
          </Typography>
          {citation ? (
            <Stack direction="row" spacing={0.5} alignItems="center">
              <PlaceOutlinedIcon fontSize="small" color="action" />
              <Typography variant="caption" color="text.secondary">
                {citation}
              </Typography>
            </Stack>
          ) : null}
          {source ? (
            <Typography variant="caption" color="text.secondary">
              Source: {source}
            </Typography>
          ) : null}
          {metadata.length > 0 ? (
            <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
              {metadata.map((item) => (
                <Chip key={item} size="small" label={item} variant="outlined" />
              ))}
            </Stack>
          ) : null}
          {navigationActions.length > 0 ? (
            <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
              {navigationActions.map((action) => (
                <Button
                  key={action.key}
                  size="small"
                  variant="text"
                  color="inherit"
                  onClick={action.onClick}
                  disabled={action.disabled ?? !action.onClick}
                  aria-label={action.label}
                >
                  {action.label}
                </Button>
              ))}
            </Stack>
          ) : null}
        </Stack>
      </AccordionDetails>
    </Accordion>
  );
}
