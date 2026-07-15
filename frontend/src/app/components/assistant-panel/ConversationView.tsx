import { Stack } from "@mui/material";
import { EmptyState, ErrorState, LoadingState, RetryAction } from "../../WorkspaceStates";
import { ConversationMessage } from "./ConversationMessage";
import { ThinkingIndicator } from "./ThinkingIndicator";
import type { AssistantConversationState, AssistantMessage } from "./assistantTypes";

export function ConversationView({
  state,
  messages,
  errorMessage,
  loadingMessage,
  emptyTitle = "Conversation not started",
  emptyMessage = "Select a suggested question or enter a workspace-specific prompt to begin.",
  onRetry,
  onCopyMessage,
}: {
  state: AssistantConversationState;
  messages: AssistantMessage[];
  errorMessage?: string;
  loadingMessage?: string;
  emptyTitle?: string;
  emptyMessage?: string;
  onRetry?: () => void;
  onCopyMessage?: (message: AssistantMessage) => void;
}) {
  if (state === "loading") {
    return (
      <LoadingState
        title="Loading conversation"
        message={loadingMessage ?? "Preparing the assistant workspace."}
      />
    );
  }

  if (state === "error") {
    return (
      <ErrorState
        title="Assistant unavailable"
        message={errorMessage ?? "The assistant conversation could not be loaded."}
        action={onRetry ? <RetryAction onClick={onRetry} /> : undefined}
      />
    );
  }

  if (messages.length === 0) {
    return (
      <EmptyState
        title={emptyTitle}
        message={emptyMessage}
        compact
      />
    );
  }

  return (
    <Stack spacing={1}>
      {messages.map((message) => (
        <ConversationMessage key={message.id} message={message} onCopy={onCopyMessage} />
      ))}
      {messages.some((message) => message.status === "streaming") ? (
        <ThinkingIndicator />
      ) : null}
    </Stack>
  );
}
