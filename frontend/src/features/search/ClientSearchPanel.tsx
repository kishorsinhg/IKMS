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
import { useMemo, useState } from "react";
import { askClientQuestion, searchClientKnowledge, sendAiFeedback } from "../../api/search";
import { AssistantPanel } from "../../app/components/assistant-panel/AssistantPanel";
import type { AssistantEvidenceReference, AssistantMessage, AssistantSourceReference, SuggestedQuestion } from "../../app/components/assistant-panel/assistantTypes";
import { StatusBadge } from "../../app/components/StatusBadge";
import { EmptyState } from "../../app/WorkspaceStates";

export function ClientSearchPanel({
  clientId,
  compact = false,
  suggestedPrompts = [],
  sourceReferencesOverride = [],
  evidenceReferencesOverride = [],
}: {
  clientId: string;
  compact?: boolean;
  suggestedPrompts?: string[];
  sourceReferencesOverride?: AssistantSourceReference[];
  evidenceReferencesOverride?: AssistantEvidenceReference[];
}) {
  const [searchQuery, setSearchQuery] = useState("");
  const [question, setQuestion] = useState("");
  const [lastSubmittedQuestion, setLastSubmittedQuestion] = useState("");
  const [messages, setMessages] = useState<AssistantMessage[]>([]);
  const searchResultsQuery = useQuery({
    queryKey: ["clients", clientId, "search", searchQuery],
    queryFn: () => searchClientKnowledge(clientId, searchQuery),
    enabled: searchQuery.trim().length > 0,
  });

  const askMutation = useMutation({
    mutationFn: (value: string) => askClientQuestion(clientId, value),
    onMutate: (value) => {
      const userMessageId = `user-${Date.now()}`;
      const assistantMessageId = `assistant-${Date.now()}`;
      setLastSubmittedQuestion(value);
      setMessages((current) => [
        ...current,
        {
          id: userMessageId,
          role: "user",
          content: value,
          status: "ready",
        },
        {
          id: assistantMessageId,
          role: "assistant",
          content: "",
          status: "streaming",
        },
      ]);
      setQuestion("");
      return { assistantMessageId };
    },
    onSuccess: (response, _value, context) => {
      setMessages((current) =>
        current.map((message) =>
          message.id === context?.assistantMessageId
            ? {
                ...message,
                content: response.answer,
                status: "ready",
                warnings: response.warnings,
                sourceReferences: response.citations.map((citation) => buildCitationSource(citation)),
                evidenceReferences: response.citations.map((citation, index) => buildCitationEvidence(citation, index)),
              }
            : message,
        ),
      );
    },
    onError: () => {
      setMessages((current) =>
        current.map((message) =>
          message.id.startsWith("assistant-") && message.status === "streaming"
            ? {
                ...message,
                content: "No assistant response is available.",
                status: "error",
              }
            : message,
        ),
      );
    },
  });

  const feedbackMutation = useMutation({
    mutationFn: ({ interactionId, helpful }: { interactionId: string; helpful: boolean }) => sendAiFeedback(interactionId, helpful),
  });

  const suggestedQuestions = useMemo<SuggestedQuestion[]>(
    () => Array.from(new Set([
      "Summarize customer",
      "Explain this document",
      "Show missing metadata",
      "Compare with previous version",
      ...suggestedPrompts,
    ])).map((label) => ({
      key: label,
      label,
      onSelect: () => setQuestion(label),
    })),
    [suggestedPrompts],
  );

  const sourceReferences = useMemo<AssistantSourceReference[]>(
    () => [
      ...sourceReferencesOverride,
      ...(searchResultsQuery.data ?? []).map((result) => ({
        key: `${result.sourceType}-${result.sourceId}`,
        label: result.title,
        kind: normalizeSourceKind(result.sourceType),
        detail: result.citation,
      })),
    ],
    [searchResultsQuery.data, sourceReferencesOverride],
  );

  const evidenceReferences = useMemo<AssistantEvidenceReference[]>(
    () => [
      ...evidenceReferencesOverride,
      ...(searchResultsQuery.data ?? []).map((result, index) => ({
        key: `${result.sourceId}-${index}`,
        label: result.pageNumber ? `${result.title} · Page ${result.pageNumber}` : result.title,
        target: result.pageNumber ? "page" : "metadata",
        detail: result.sourceSection ?? result.citation,
        disabled: true,
      } satisfies AssistantEvidenceReference)),
    ],
    [evidenceReferencesOverride, searchResultsQuery.data],
  );

  return (
    <Box
      sx={{
        border: compact ? "none" : undefined,
      }}
    >
      <AssistantPanel
        title="Evidence Assistant"
        subtitle="Search client-linked evidence and ask evidence-based questions without leaving Customer360."
        conversationState={askMutation.isError && messages.length === 0 ? "error" : messages.length > 0 ? "ready" : "empty"}
        messages={messages}
        suggestedQuestions={suggestedQuestions}
        evidenceReferences={evidenceReferences}
        sourceReferences={sourceReferences}
        errorMessage="The assistant could not answer this client question."
        emptyTitle="Conversation not started"
        emptyMessage="Use a suggested question or ask a customer-specific evidence question."
        onRetry={lastSubmittedQuestion ? () => askMutation.mutate(lastSubmittedQuestion) : undefined}
        onCopyMessage={(message) => {
          void navigator.clipboard?.writeText(message.content ?? "");
        }}
        toolbarContent={(
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
              aria-label="Search client-linked evidence"
            />
          </Stack>
        )}
        composerContent={(
          <Stack spacing={1}>
            <TextField
              multiline
              minRows={compact ? 2 : 3}
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              placeholder="Ask an evidence-based question about this client"
              aria-label="Ask an evidence-based question about this client"
            />
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap alignItems="center">
              <Button
                variant="contained"
                startIcon={<SmartToyOutlinedIcon fontSize="small" />}
                disabled={!question.trim() || askMutation.isPending}
                onClick={() => askMutation.mutate(question.trim())}
              >
                {askMutation.isPending ? "Answering..." : "Ask client AI"}
              </Button>
              {messages.length > 0 && askMutation.data?.status === "Answered" ? (
                <>
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
                </>
              ) : null}
            </Stack>
            {askMutation.data ? (
              <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                <StatusBadge
                  label={askMutation.data.status}
                  tone={askMutation.data.status === "Answered" ? "success" : askMutation.data.status === "NoEvidence" ? "warning" : "error"}
                />
                <Chip size="small" variant="outlined" label={askMutation.data.retrievalMode} />
              </Stack>
            ) : null}
          </Stack>
        )}
        sourcesContent={(
          <Stack spacing={1}>
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
            ) : searchResultsQuery.data?.length ? (
              <Stack spacing={1}>
                {searchResultsQuery.data.map((result) => (
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
                      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                        <Typography variant="body2" fontWeight={600}>
                          {result.title}
                        </Typography>
                        <StatusBadge
                          label={result.citationQuality}
                          tone={result.citationQuality === "HIGH" ? "success" : result.citationQuality === "MEDIUM" ? "warning" : "error"}
                        />
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
            ) : (
              <Typography variant="body2" color="text.secondary">
                Search results will appear here when you search linked evidence for this customer.
              </Typography>
            )}
            {askMutation.data?.warnings.length ? (
              <>
                <Divider />
                <Stack spacing={0.75}>
                  {askMutation.data.warnings.map((warning) => (
                    <Alert key={warning} severity="warning" variant="outlined">
                      {warning}
                    </Alert>
                  ))}
                </Stack>
              </>
            ) : null}
          </Stack>
        )}
      />
    </Box>
  );
}

function buildCitationSource(citation: {
  sourceType: string;
  sourceId: string;
  title: string;
}) {
  return {
    key: `${citation.sourceType}-${citation.sourceId}`,
    label: citation.title,
    kind: normalizeSourceKind(citation.sourceType),
    referenceType: normalizeReferenceType(citation.sourceType),
    disabled: true,
  } satisfies AssistantSourceReference;
}

function buildCitationEvidence(
  citation: {
    title: string;
    pageNumber: number | null;
    sourceSection: string | null;
  },
  index: number,
) {
  return {
    key: `${citation.title}-${index}`,
    label: citation.pageNumber ? `${citation.title} · Page ${citation.pageNumber}` : citation.title,
    target: citation.pageNumber ? "page" : "metadata",
    detail: citation.sourceSection ?? "Reference location unavailable",
    disabled: true,
  } satisfies AssistantEvidenceReference;
}

function normalizeSourceKind(value: string): AssistantSourceReference["kind"] {
  switch (value.toUpperCase()) {
    case "DOCUMENT":
      return "DOCUMENT";
    case "EMAIL":
      return "EMAIL";
    case "POLICY":
    case "CLAIM":
    case "POLICY_REFERENCE":
    case "CLAIM_REFERENCE":
    case "METADATA":
      return "METADATA";
    case "CUSTOMER":
      return "CUSTOMER";
    case "NOTE":
      return "NOTE";
    default:
      return "UNKNOWN";
  }
}

function normalizeReferenceType(value: string): AssistantSourceReference["referenceType"] | undefined {
  switch (value.toUpperCase()) {
    case "POLICY":
    case "POLICY_REFERENCE":
      return "policy";
    case "CLAIM":
    case "CLAIM_REFERENCE":
      return "claim";
    default:
      return undefined;
  }
}
