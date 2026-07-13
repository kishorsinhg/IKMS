import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { ui } from "../../app/ui";
import { askClientQuestion, searchClientKnowledge, sendAiFeedback } from "../../api/search";

export function ClientSearchPanel({ clientId }: { clientId: string }) {
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
    <section style={cardStyle}>
      <h3 style={{ marginTop: 0 }}>AI Q&A</h3>
      <div style={stackStyle}>
        <label style={fieldStyle}>
          <span>Search this client</span>
          <input value={searchQuery} onChange={(event) => setSearchQuery(event.target.value)} placeholder="Search documents, emails, and notes" />
        </label>
        <div style={resultListStyle}>
          {searchResultsQuery.data?.map((result) => (
            <article key={`${result.sourceType}-${result.sourceId}`} style={resultCardStyle}>
              <strong>{result.title}</strong>
              <span>{result.excerpt}</span>
              <small>{result.citation}</small>
            </article>
          ))}
          {searchQuery.trim() && searchResultsQuery.data?.length === 0 ? <span>No matching client knowledge.</span> : null}
        </div>

        <label style={fieldStyle}>
          <span>Ask a question</span>
          <textarea rows={4} value={question} onChange={(event) => setQuestion(event.target.value)} placeholder="Ask an evidence-based question about this client" />
        </label>
        <button
          type="button"
          style={buttonStyle}
          disabled={!question.trim() || askMutation.isPending}
          onClick={() => askMutation.mutate(question.trim())}
        >
          {askMutation.isPending ? "Answering..." : "Ask client AI"}
        </button>

        {askMutation.data ? (
          <article style={answerCardStyle}>
            <strong>{askMutation.data.status}</strong>
            <p style={{ margin: 0 }}>{askMutation.data.answer}</p>
            <div style={resultListStyle}>
              {askMutation.data.citations.map((citation) => (
                <div key={`${citation.sourceType}-${citation.sourceId}`} style={citationStyle}>
                  <strong>{citation.title}</strong>
                  <span>{citation.excerpt}</span>
                </div>
              ))}
            </div>
            {askMutation.data.status === "Answered" ? (
              <div style={feedbackRowStyle}>
                <button type="button" style={secondaryButtonStyle} onClick={() => feedbackMutation.mutate({ interactionId: askMutation.data!.interactionId, helpful: true })}>
                  Helpful
                </button>
                <button type="button" style={secondaryButtonStyle} onClick={() => feedbackMutation.mutate({ interactionId: askMutation.data!.interactionId, helpful: false })}>
                  Not helpful
                </button>
              </div>
            ) : null}
          </article>
        ) : null}
      </div>
    </section>
  );
}

const cardStyle: React.CSSProperties = {
  ...ui.card,
};

const stackStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.85rem",
};

const fieldStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.35rem",
};

const resultListStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.6rem",
};

const resultCardStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.28rem",
  padding: "0.85rem 0.95rem",
  borderRadius: "0.95rem",
  background: "var(--panel-muted)",
  border: "1px solid rgba(191, 208, 226, 0.72)",
};

const answerCardStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.65rem",
  padding: "1rem 1.05rem",
  borderRadius: "1rem",
  background: "linear-gradient(180deg, #eff6ff 0%, #f6f9fc 100%)",
  border: "1px solid rgba(176, 204, 239, 0.8)",
};

const citationStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.2rem",
  padding: "0.75rem 0.8rem",
  borderRadius: "0.85rem",
  background: "#ffffff",
  border: "1px solid var(--line)",
};

const feedbackRowStyle: React.CSSProperties = {
  display: "flex",
  gap: "0.75rem",
};

const buttonStyle: React.CSSProperties = {
  ...ui.primaryButton,
};

const secondaryButtonStyle: React.CSSProperties = {
  ...ui.secondaryButton,
};
