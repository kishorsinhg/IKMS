import ChatOutlinedIcon from "@mui/icons-material/ChatOutlined";
import { Button, Stack, Typography } from "@mui/material";
import type { SuggestedQuestion } from "./assistantTypes";

export function SuggestedQuestionList({
  questions,
}: {
  questions: SuggestedQuestion[];
}) {
  if (questions.length === 0) {
    return (
      <Typography variant="body2" color="text.secondary">
        No context-aware prompts are available for the current workspace state.
      </Typography>
    );
  }

  return (
    <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
      {questions.map((question) => (
        <Button
          key={question.key}
          size="small"
          variant="outlined"
          color="inherit"
          startIcon={<ChatOutlinedIcon fontSize="small" />}
          onClick={question.onSelect}
          disabled={question.disabled}
          aria-label={question.label}
        >
          {question.label}
        </Button>
      ))}
    </Stack>
  );
}
