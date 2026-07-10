import { ClientDocumentSummary, ClientEmailSummary } from "../../../api/intake";

export function DocumentsSection({ documents }: { documents: ClientDocumentSummary[] | undefined }) {
  return (
    <section style={cardStyle}>
      <h3 style={{ marginTop: 0 }}>Documents</h3>
      <div style={listStyle}>
        {documents?.map((document) => (
          <article key={document.id} style={itemStyle}>
            <strong>{document.title}</strong>
            <span>{document.source} · {document.processingStatus}</span>
            <span>{document.reviewStatus} · {formatDateTime(document.createdAt)}</span>
          </article>
        ))}
        {documents?.length === 0 ? <span>No client documents yet.</span> : null}
      </div>
    </section>
  );
}

export function EmailsSection({ emails }: { emails: ClientEmailSummary[] | undefined }) {
  return (
    <section style={cardStyle}>
      <h3 style={{ marginTop: 0 }}>Emails</h3>
      <div style={listStyle}>
        {emails?.map((email) => (
          <article key={email.id} style={itemStyle}>
            <strong>{email.subject}</strong>
            <span>{email.sender}</span>
            <span>{email.reviewStatus} · {formatDateTime(email.receivedAt)}</span>
          </article>
        ))}
        {emails?.length === 0 ? <span>No client emails yet.</span> : null}
      </div>
    </section>
  );
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString();
}

const cardStyle: React.CSSProperties = {
  padding: "1rem",
  borderRadius: "1rem",
  background: "#fff8ee",
  border: "1px solid rgba(31, 28, 24, 0.1)",
};

const listStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.75rem",
};

const itemStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.2rem",
  padding: "0.85rem",
  borderRadius: "0.85rem",
  background: "#f7efe0",
};
