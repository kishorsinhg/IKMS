import { Permission } from "../../../api/auth";
import { ui } from "../../../app/ui";
import { ClientDocumentSummary, ClientEmailSummary } from "../../../api/intake";

export function DocumentsSection({
  documents,
  permissions,
}: {
  documents: ClientDocumentSummary[] | undefined;
  permissions: Permission[];
}) {
  const canViewOriginal = permissions.includes("VIEW_ORIGINAL_DOCUMENTS") && permissions.includes("VIEW_PII");
  const canViewRedacted = permissions.includes("VIEW_REDACTED_DOCUMENTS");

  return (
    <section style={cardStyle}>
      <h3 style={{ marginTop: 0 }}>Documents</h3>
      <div style={listStyle}>
        {documents?.map((document) => (
          <article key={document.id} style={itemStyle}>
            <strong>{document.title}</strong>
            <span>{document.source} · {document.processingStatus}</span>
            <span>{document.reviewStatus} · {formatDateTime(document.createdAt)}</span>
            <div style={actionRowStyle}>
              {document.containsPii ? (
                canViewOriginal ? (
                  <>
                    <a href={`/api/documents/${document.id}/preview`} style={actionLinkStyle}>Preview original</a>
                    <a href={`/api/documents/${document.id}/download`} style={actionLinkStyle}>Download original</a>
                  </>
                ) : canViewRedacted && document.redactionStatus === "AVAILABLE" ? (
                  <>
                    <a href={`/api/documents/${document.id}/preview`} style={actionLinkStyle}>Preview redacted</a>
                    <a href={`/api/documents/${document.id}/download`} style={actionLinkStyle}>Download redacted</a>
                  </>
                ) : (
                  <span>Redacted copy unavailable</span>
                )
              ) : (
                <>
                  <a href={`/api/documents/${document.id}/preview`} style={actionLinkStyle}>Preview document</a>
                  <a href={`/api/documents/${document.id}/download`} style={actionLinkStyle}>Download document</a>
                </>
              )}
            </div>
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
  ...ui.card,
};

const listStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.75rem",
};

const itemStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.28rem",
  padding: "0.95rem 1rem",
  borderRadius: "0.95rem",
  background: "var(--panel-muted)",
  border: "1px solid rgba(191, 208, 226, 0.72)",
};

const actionRowStyle: React.CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  gap: "0.75rem",
  marginTop: "0.35rem",
};

const actionLinkStyle: React.CSSProperties = {
  color: "var(--accent)",
  fontWeight: 700,
  textDecoration: "none",
};
