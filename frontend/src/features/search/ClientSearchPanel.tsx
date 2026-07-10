import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
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
  padding: "1rem",
  borderRadius: "1rem",
  background: "#fff8ee",
  border: "1px solid rgba(31, 28, 24, 0.1)",
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
  gap: "0.2rem",
  padding: "0.75rem",
  borderRadius: "0.85rem",
  background: "#f7efe0",
};

const answerCardStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.65rem",
  padding: "0.9rem",
  borderRadius: "0.9rem",
  background: "#f2e8d6",
};

const citationStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.2rem",
  padding: "0.65rem",
  borderRadius: "0.75rem",
  background: "#fffaf0",
};

const feedbackRowStyle: React.CSSProperties = {
  display: "flex",
  gap: "0.75rem",
};

const buttonStyle: React.CSSProperties = {
  width: "fit-content",
  padding: "0.7rem 1rem",
  borderRadius: "999px",
  border: "none",
  background: "#1f1c18",
  color: "#fffaf0",
  fontWeight: 700,
  cursor: "pointer",
};

const secondaryButtonStyle: React.CSSProperties = {
  ...buttonStyle,
  background: "#6d6253",
};
