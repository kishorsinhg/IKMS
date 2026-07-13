import { FormEvent, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { ui } from "../../app/ui";
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
    <section style={ui.page}>
      <header style={ui.pageHeader}>
        <p style={ui.eyebrow}>Governance / Audit explorer</p>
        <h2 style={ui.pageTitle}>Audit and governance</h2>
        <p style={ui.pageDescription}>
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
          <p style={{ marginBottom: 0, color: "var(--warning)", fontWeight: 600 }}>
            CSV export requires the `EXPORT_AUDIT` permission.
          </p>
        ) : null}
      </section>

      <section style={cardStyle}>
        <h3 style={{ marginTop: 0 }}>Audit events</h3>
        {auditQuery.isLoading ? <p>Loading audit events...</p> : null}
        <div style={tableWrapStyle}>
          <table>
            <thead>
              <tr>
                <th>Occurred</th>
                <th>Actor</th>
                <th>Category</th>
                <th>Action</th>
                <th>Outcome</th>
                <th>Client</th>
                <th>PII</th>
                <th>Details</th>
              </tr>
            </thead>
            <tbody>
              {auditQuery.data?.map((entry) => (
                <tr key={entry.id}>
                  <td>{entry.occurredAt}</td>
                  <td>{entry.actorUsername ?? "system"}</td>
                  <td>{entry.category}</td>
                  <td><strong>{entry.action}</strong></td>
                  <td><span style={ui.statusBadge}>{entry.outcome}</span></td>
                  <td>{entry.clientId ?? "n/a"}</td>
                  <td>{entry.piiAccess ? "Yes" : "No"}</td>
                  <td>{`Details: ${Object.entries(entry.details).map(([key, value]) => `${key}=${value}`).join(", ") || "none"}`}</td>
                </tr>
              ))}
            </tbody>
          </table>
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
  ...ui.card,
};

const filterGridStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.75rem",
  gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
};

const buttonStyle: React.CSSProperties = {
  ...ui.primaryButton,
};

const secondaryButtonStyle: React.CSSProperties = {
  ...ui.secondaryButton,
};

const previewStyle: React.CSSProperties = {
  margin: 0,
  whiteSpace: "pre-wrap",
  overflowX: "auto",
};

const tableWrapStyle: React.CSSProperties = {
  overflowX: "auto",
};
