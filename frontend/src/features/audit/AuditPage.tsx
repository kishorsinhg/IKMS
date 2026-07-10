import { FormEvent, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { getCurrentUser } from "../../api/auth";
import { AuditFilters, exportAuditLogs, searchAuditLogs } from "../../api/audit";

export function AuditPage() {
  const [filters, setFilters] = useState<AuditFilters>({});
  const [draftActor, setDraftActor] = useState("");
  const [draftAction, setDraftAction] = useState("");
  const [draftClientId, setDraftClientId] = useState("");
  const [draftFrom, setDraftFrom] = useState("");
  const [draftTo, setDraftTo] = useState("");
  const currentUserQuery = useQuery({ queryKey: ["auth", "me"], queryFn: getCurrentUser, retry: false });
  const auditQuery = useQuery({
    queryKey: ["audit", filters],
    queryFn: () => searchAuditLogs(filters),
  });
  const exportMutation = useMutation({
    mutationFn: () => exportAuditLogs(filters),
  });

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFilters({
      actor: draftActor.trim() || undefined,
      action: draftAction.trim() || undefined,
      clientId: draftClientId.trim() || undefined,
      from: draftFrom || undefined,
      to: draftTo || undefined,
    });
  }

  const canExport = currentUserQuery.data?.permissions.includes("EXPORT_AUDIT") ?? false;

  return (
    <section style={{ display: "grid", gap: "1.25rem" }}>
      <header>
        <h2 style={{ marginBottom: "0.35rem" }}>Audit and governance</h2>
        <p style={{ margin: 0, color: "#6d6253" }}>
          Search operational activity across authentication, intake, review, document access, PII access, configuration, and AI usage.
        </p>
      </header>

      <section style={cardStyle}>
        <form onSubmit={applyFilters} style={filterGridStyle}>
          <input value={draftActor} onChange={(event) => setDraftActor(event.target.value)} placeholder="Actor username" />
          <input value={draftAction} onChange={(event) => setDraftAction(event.target.value)} placeholder="Action" />
          <input value={draftClientId} onChange={(event) => setDraftClientId(event.target.value)} placeholder="Client UUID" />
          <input value={draftFrom} onChange={(event) => setDraftFrom(event.target.value)} placeholder="2026-07-10T00:00:00Z" />
          <input value={draftTo} onChange={(event) => setDraftTo(event.target.value)} placeholder="2026-07-10T23:59:59Z" />
          <div style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap" }}>
            <button type="submit" style={buttonStyle}>Search audit</button>
            <button
              type="button"
              style={secondaryButtonStyle}
              disabled={!canExport || exportMutation.isPending}
              onClick={() => exportMutation.mutate()}
            >
              {exportMutation.isPending ? "Exporting..." : "Export CSV"}
            </button>
          </div>
        </form>
        {!canExport ? (
          <p style={{ marginBottom: 0, color: "#8a4b24" }}>
            CSV export requires the `EXPORT_AUDIT` permission.
          </p>
        ) : null}
      </section>

      <section style={cardStyle}>
        <h3 style={{ marginTop: 0 }}>Audit events</h3>
        {auditQuery.isLoading ? <p>Loading audit events...</p> : null}
        <div style={listStyle}>
          {auditQuery.data?.map((entry) => (
            <article key={entry.id} style={itemStyle}>
              <strong>{entry.action}</strong>
              <span>{entry.actorUsername ?? "system"} · {entry.category} · {entry.outcome}</span>
              <span>{entry.occurredAt}</span>
              <span>Client: {entry.clientId ?? "n/a"} · PII: {entry.piiAccess ? "Yes" : "No"}</span>
              <span>Details: {Object.entries(entry.details).map(([key, value]) => `${key}=${value}`).join(", ") || "none"}</span>
            </article>
          ))}
        </div>
      </section>

      {exportMutation.data ? (
        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>CSV preview</h3>
          <pre style={previewStyle}>{exportMutation.data}</pre>
        </section>
      ) : null}
    </section>
  );
}

const cardStyle: React.CSSProperties = {
  padding: "1rem",
  borderRadius: "1rem",
  background: "#fff8ee",
  border: "1px solid rgba(31, 28, 24, 0.1)",
};

const filterGridStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.75rem",
  gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
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
  background: "#b46a31",
};

const previewStyle: React.CSSProperties = {
  margin: 0,
  whiteSpace: "pre-wrap",
  overflowX: "auto",
};
