import ContentCopyOutlinedIcon from "@mui/icons-material/ContentCopyOutlined";
import ExpandLessOutlinedIcon from "@mui/icons-material/ExpandLessOutlined";
import ExpandMoreOutlinedIcon from "@mui/icons-material/ExpandMoreOutlined";
import PersonOutlinedIcon from "@mui/icons-material/PersonOutlined";
import SmartToyOutlinedIcon from "@mui/icons-material/SmartToyOutlined";
import WarningAmberOutlinedIcon from "@mui/icons-material/WarningAmberOutlined";
import {
  Alert,
  Box,
  Button,
  Chip,
  Stack,
  Typography,
} from "@mui/material";
import { useState } from "react";
import { EvidenceReference } from "./EvidenceReference";
import { SourceChip } from "./SourceChip";
import { ThinkingIndicator } from "./ThinkingIndicator";
import type { AssistantMessage } from "./assistantTypes";

export function ConversationMessage({
  message,
  onCopy,
}: {
  message: AssistantMessage;
  onCopy?: (message: AssistantMessage) => void;
}) {
  const [expanded, setExpanded] = useState(false);
  const canExpand = (message.content?.length ?? 0) > 240;
  const roleLabel = message.role === "user" ? "You" : "Assistant";

  return (
    <Box
      sx={{
        border: (theme) => `1px solid ${theme.palette.divider}`,
        borderRadius: 1,
        p: 1.25,
        backgroundColor: message.role === "assistant" ? "background.paper" : "background.default",
      }}
    >
      <Stack spacing={1}>
        <Stack direction="row" justifyContent="space-between" alignItems="center" flexWrap="wrap" useFlexGap>
          <Stack direction="row" spacing={0.75} alignItems="center">
            {message.role === "assistant" ? <SmartToyOutlinedIcon fontSize="small" color="action" /> : <PersonOutlinedIcon fontSize="small" color="action" />}
            <Typography variant="subtitle2">{roleLabel}</Typography>
            {message.status === "streaming" ? <Chip size="small" label="Thinking" /> : null}
            {message.status === "error" ? <Chip size="small" color="error" label="Error" /> : null}
          </Stack>
          {message.timestamp ? (
            <Typography variant="caption" color="text.secondary">
              {message.timestamp}
            </Typography>
          ) : null}
        </Stack>

        {message.status === "streaming" ? (
          <ThinkingIndicator label="Streaming placeholder active" />
        ) : (
          <Typography
            variant="body2"
            sx={canExpand && !expanded ? {
              display: "-webkit-box",
              overflow: "hidden",
              WebkitBoxOrient: "vertical",
              WebkitLineClamp: 4,
            } : undefined}
          >
            {message.content}
          </Typography>
        )}

        {message.warnings?.length ? (
          <Stack spacing={0.75}>
            {message.warnings.map((warning) => (
              <Alert key={warning} severity="warning" variant="outlined" icon={<WarningAmberOutlinedIcon fontSize="small" />}>
                {warning}
              </Alert>
            ))}
          </Stack>
        ) : null}

        {message.evidenceReferences?.length ? (
          <Stack spacing={0.5}>
            <Typography variant="caption" color="text.secondary" sx={{ textTransform: "uppercase", letterSpacing: "0.04em" }}>
              Evidence References
            </Typography>
            <Stack spacing={0.25}>
              {message.evidenceReferences.map((reference) => (
                <EvidenceReference key={reference.key} reference={reference} />
              ))}
            </Stack>
          </Stack>
        ) : null}

        {message.sourceReferences?.length ? (
          <Stack spacing={0.5}>
            <Typography variant="caption" color="text.secondary" sx={{ textTransform: "uppercase", letterSpacing: "0.04em" }}>
              Sources
            </Typography>
            <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
              {message.sourceReferences.map((source) => (
                <SourceChip key={source.key} source={source} />
              ))}
            </Stack>
          </Stack>
        ) : null}

        <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
          {message.role === "assistant" && onCopy && message.content ? (
            <Button
              size="small"
              variant="text"
              color="inherit"
              startIcon={<ContentCopyOutlinedIcon fontSize="small" />}
              onClick={() => onCopy(message)}
            >
              Copy response
            </Button>
          ) : null}
          {canExpand ? (
            <Button
              size="small"
              variant="text"
              color="inherit"
              startIcon={expanded ? <ExpandLessOutlinedIcon fontSize="small" /> : <ExpandMoreOutlinedIcon fontSize="small" />}
              onClick={() => setExpanded((current) => !current)}
            >
              {expanded ? "Collapse" : "Expand"}
            </Button>
          ) : null}
        </Stack>
      </Stack>
    </Box>
  );
}
