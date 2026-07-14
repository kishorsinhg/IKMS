import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import SmartToyOutlinedIcon from "@mui/icons-material/SmartToyOutlined";
import ThumbDownOutlinedIcon from "@mui/icons-material/ThumbDownOutlined";
import ThumbUpOutlinedIcon from "@mui/icons-material/ThumbUpOutlined";
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  InputAdornment,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { askClientQuestion, searchClientKnowledge, sendAiFeedback } from "../../api/search";
import { StatusBadge } from "../../app/components/StatusBadge";
import { EmptyState } from "../../app/WorkspaceStates";

export function ClientSearchPanel({
  clientId,
  compact = false,
}: {
  clientId: string;
  compact?: boolean;
}) {
  const [searchQuery, setSearchQuery] = useState("");
  const [question, setQuestion] = useState("");
  const searchResultsQuery = useQuery({
    queryKey: ["clients", clientId, "search", searchQuery],
    queryFn: () => searchClientKnowledge(clientId, searchQuery),
    enabled: searchQuery.trim().length > 0,
  });

  const askMutation = useMutation({
    mutationFn: (value: string) => askClientQuestion(clientId, value),
  });

  const feedbackMutation = useMutation({
    mutationFn: ({ interactionId, helpful }: { interactionId: string; helpful: boolean }) => sendAiFeedback(interactionId, helpful),
  });

  return (
    <Box
      sx={{
        border: compact ? "none" : (theme) => `1px solid ${theme.palette.divider}`,
        borderRadius: compact ? 0 : 1,
        backgroundColor: compact ? "transparent" : "background.paper",
        px: compact ? 0 : 2,
        py: compact ? 0 : 1.5,
      }}
    >
      <Stack spacing={1.5}>
        <Stack spacing={0.25}>
          <Typography variant="subtitle2">AI Q&A</Typography>
          {!compact ? (
            <Typography variant="body2" color="text.secondary">
              Search client-linked evidence and ask evidence-based questions without leaving Customer360.
            </Typography>
          ) : null}
        </Stack>

        <Stack spacing={1}>
          <TextField
            size="small"
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            placeholder="Search documents, emails, and notes"
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchOutlinedIcon fontSize="small" />
                </InputAdornment>
              ),
            }}
          />

          {searchResultsQuery.isLoading ? (
            <Stack direction="row" spacing={1} alignItems="center">
              <CircularProgress size={18} />
              <Typography variant="body2" color="text.secondary">
                Searching client evidence
              </Typography>
            </Stack>
          ) : searchQuery.trim() && (searchResultsQuery.data?.length ?? 0) === 0 ? (
            <EmptyState
              title="No matching client knowledge"
              message="Try a broader term or switch to a direct question."
              compact
            />
          ) : (
            <Stack spacing={1}>
              {searchResultsQuery.data?.map((result) => (
                <Box
                  key={`${result.sourceType}-${result.sourceId}`}
                  sx={{
                    border: (theme) => `1px solid ${theme.palette.divider}`,
                    borderRadius: 1,
                    p: 1.25,
                    backgroundColor: "background.default",
                  }}
                >
                  <Stack spacing={0.5}>
                    <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                      <Typography variant="body2" fontWeight={600}>
                        {result.title}
                      </Typography>
                      <StatusBadge label={result.citationQuality} tone={result.citationQuality === "HIGH" ? "success" : result.citationQuality === "MEDIUM" ? "warning" : "error"} />
                      <Chip size="small" variant="outlined" label={result.retrievalPath} />
                    </Stack>
                    <Typography variant="body2" color="text.secondary">
                      {result.excerpt}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {result.pageNumber ? `Page ${result.pageNumber}` : result.sourceSection ?? result.sourceType}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {result.citation}
                    </Typography>
                  </Stack>
                </Box>
              ))}
            </Stack>
          )}
        </Stack>

        <Divider />

        <Stack spacing={1}>
          <TextField
            multiline
            minRows={compact ? 2 : 3}
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
            placeholder="Ask an evidence-based question about this client"
          />

          <Stack direction="row" spacing={1}>
            <Button
              variant="contained"
              startIcon={<SmartToyOutlinedIcon fontSize="small" />}
              disabled={!question.trim() || askMutation.isPending}
              onClick={() => askMutation.mutate(question.trim())}
            >
              {askMutation.isPending ? "Answering..." : "Ask client AI"}
            </Button>
          </Stack>

          {askMutation.data ? (
            <Box
              sx={{
                border: (theme) => `1px solid ${theme.palette.divider}`,
                borderRadius: 1,
                p: 1.5,
                backgroundColor: "background.default",
              }}
            >
              <Stack spacing={1}>
                <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                  <StatusBadge label={askMutation.data.status} tone={askMutation.data.status === "Answered" ? "success" : askMutation.data.status === "NoEvidence" ? "warning" : "error"} />
                  <Chip size="small" variant="outlined" label={askMutation.data.retrievalMode} />
                </Stack>
                <Typography variant="body2">{askMutation.data.answer}</Typography>

                {askMutation.data.warnings.length > 0 ? (
                  <Stack spacing={0.75}>
                    {askMutation.data.warnings.map((warning) => (
                      <Alert key={warning} severity="warning" variant="outlined">
                        {warning}
                      </Alert>
                    ))}
                  </Stack>
                ) : null}

                <Stack spacing={0.75}>
                  {askMutation.data.citations.map((citation) => (
                    <Box
                      key={`${citation.sourceType}-${citation.sourceId}`}
                      sx={{
                        border: (theme) => `1px solid ${theme.palette.divider}`,
                        borderRadius: 1,
                        p: 1,
                        backgroundColor: "background.paper",
                      }}
                    >
                      <Typography variant="body2" fontWeight={600}>
                        {citation.title}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {citation.pageNumber ? `Page ${citation.pageNumber}` : citation.sourceSection ?? citation.sourceType}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {citation.excerpt}
                      </Typography>
                    </Box>
                  ))}
                </Stack>

                {askMutation.data.status === "Answered" ? (
                  <Stack direction="row" spacing={1}>
                    <Button
                      size="small"
                      variant="outlined"
                      startIcon={<ThumbUpOutlinedIcon fontSize="small" />}
                      disabled={feedbackMutation.isPending}
                      onClick={() => feedbackMutation.mutate({ interactionId: askMutation.data!.interactionId, helpful: true })}
                    >
                      Helpful
                    </Button>
                    <Button
                      size="small"
                      variant="outlined"
                      startIcon={<ThumbDownOutlinedIcon fontSize="small" />}
                      disabled={feedbackMutation.isPending}
                      onClick={() => feedbackMutation.mutate({ interactionId: askMutation.data!.interactionId, helpful: false })}
                    >
                      Not helpful
                    </Button>
                  </Stack>
                ) : null}
              </Stack>
            </Box>
          ) : null}
        </Stack>
      </Stack>
    </Box>
  );
}
