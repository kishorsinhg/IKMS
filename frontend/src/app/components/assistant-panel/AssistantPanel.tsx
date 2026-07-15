import {
  Box,
  Divider,
  Drawer,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import type { ReactNode } from "react";
import { AssistantToolbar } from "./AssistantToolbar";
import { ConversationView } from "./ConversationView";
import { EvidenceReference } from "./EvidenceReference";
import { SourceChip } from "./SourceChip";
import { SuggestedQuestionList } from "./SuggestedQuestionList";
import type {
  AssistantConversationState,
  AssistantEvidenceReference,
  AssistantMessage,
  AssistantSourceReference,
  AssistantSurfaceVariant,
  SuggestedQuestion,
} from "./assistantTypes";

export function AssistantPanel({
  title,
  subtitle,
  variant = "embedded",
  open = true,
  onClose,
  conversationState,
  messages,
  suggestedQuestions,
  evidenceReferences = [],
  sourceReferences = [],
  errorMessage,
  loadingMessage,
  emptyTitle,
  emptyMessage,
  onRetry,
  onCopyMessage,
  toolbarActions,
  toolbarContent,
  composerContent,
  sourcesContent,
}: {
  title: string;
  subtitle?: string;
  variant?: AssistantSurfaceVariant;
  open?: boolean;
  onClose?: () => void;
  conversationState: AssistantConversationState;
  messages: AssistantMessage[];
  suggestedQuestions: SuggestedQuestion[];
  evidenceReferences?: AssistantEvidenceReference[];
  sourceReferences?: AssistantSourceReference[];
  errorMessage?: string;
  loadingMessage?: string;
  emptyTitle?: string;
  emptyMessage?: string;
  onRetry?: () => void;
  onCopyMessage?: (message: AssistantMessage) => void;
  toolbarActions?: ReactNode;
  toolbarContent?: ReactNode;
  composerContent?: ReactNode;
  sourcesContent?: ReactNode;
}) {
  const content = (
    <Stack spacing={0} sx={{ height: "100%" }}>
      <AssistantToolbar title={title} subtitle={subtitle} actions={toolbarActions} onRetry={onRetry} onClose={variant === "embedded" ? undefined : onClose} />
      <Box sx={{ overflowY: "auto", p: 1.5 }}>
        <Stack spacing={1.5}>
          {toolbarContent}
          {composerContent}

          <Section title="Conversation">
            <ConversationView
              state={conversationState}
              messages={messages}
              errorMessage={errorMessage}
              loadingMessage={loadingMessage}
              emptyTitle={emptyTitle}
              emptyMessage={emptyMessage}
              onRetry={onRetry}
              onCopyMessage={onCopyMessage}
            />
          </Section>

          <Divider />

          <Section title="Suggested Questions">
            <SuggestedQuestionList questions={suggestedQuestions} />
          </Section>

          <Divider />

          <Section title="Evidence References">
            {evidenceReferences.length > 0 ? (
              <Stack spacing={0.25}>
                {evidenceReferences.map((reference) => (
                  <EvidenceReference key={reference.key} reference={reference} />
                ))}
              </Stack>
            ) : (
              <Typography variant="body2" color="text.secondary">
                Evidence references will appear when assistant responses include traceable citations.
              </Typography>
            )}
          </Section>

          <Divider />

          <Section title="Sources">
            {sourcesContent ?? (
              sourceReferences.length > 0 ? (
                <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                  {sourceReferences.map((source) => (
                    <SourceChip key={source.key} source={source} />
                  ))}
                </Stack>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  Source references will appear when the current workspace exposes linked records.
                </Typography>
              )
            )}
          </Section>
        </Stack>
      </Box>
    </Stack>
  );

  if (variant === "embedded") {
    return (
      <Paper
        variant="outlined"
        aria-label={title}
        sx={{ borderRadius: 1, backgroundColor: "background.paper", overflow: "hidden" }}
      >
        {content}
      </Paper>
    );
  }

  return (
    <Drawer
      anchor={variant === "sheet" ? "bottom" : "right"}
      open={open}
      onClose={onClose}
      PaperProps={{
        sx: variant === "sheet"
          ? {
              height: "min(85vh, 720px)",
              borderTopLeftRadius: 16,
              borderTopRightRadius: 16,
            }
          : {
              width: { xs: "100%", sm: 420 },
              maxWidth: "100%",
            },
      }}
    >
      {content}
    </Drawer>
  );
}

function Section({
  title,
  children,
}: {
  title: string;
  children: ReactNode;
}) {
  return (
    <Stack spacing={0.75}>
      <Typography variant="caption" color="text.secondary" sx={{ textTransform: "uppercase", letterSpacing: "0.04em" }}>
        {title}
      </Typography>
      {children}
    </Stack>
  );
}
