import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { ui } from "../../app/ui";
import { ClientType, createClient, listClients } from "../../api/clients";

const clientsQueryKey = ["clients"];

export function ClientsWorkspacePage() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [clientType, setClientType] = useState<ClientType>("BUSINESS");
  const [requestedClientId, setRequestedClientId] = useState("");

  const clientsQuery = useQuery({
    queryKey: [...clientsQueryKey, query],
    queryFn: () => listClients(query),
  });

  const createClientMutation = useMutation({
    mutationFn: createClient,
    onSuccess: async () => {
      setDisplayName("");
      setRequestedClientId("");
      await queryClient.invalidateQueries({ queryKey: clientsQueryKey });
    },
  });

  function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    createClientMutation.mutate({
      clientId: requestedClientId || undefined,
      clientType,
      displayName,
    });
  }

  return (
    <section style={ui.page}>
      <header style={ui.pageHeader}>
        <h2 style={ui.pageTitle}>Clients workspace</h2>
        <p style={ui.pageDescription}>
          Create clients, generate temporary IDs when needed, and open the client profile workspace.
        </p>
      </header>

      <section style={ui.heroCard}>
        <div style={ui.metricRow}>
          <div style={ui.metricCard}>
            <strong style={metricValueStyle}>{clientsQuery.data?.length ?? 0}</strong>
            <span style={metricLabelStyle}>Visible clients</span>
          </div>
          <div style={ui.metricCard}>
            <strong style={metricValueStyle}>{clientsQuery.data?.filter((client) => client.clientIdTemporary).length ?? 0}</strong>
            <span style={metricLabelStyle}>Temporary IDs</span>
          </div>
          <div style={ui.metricCard}>
            <strong style={metricValueStyle}>{clientsQuery.data?.filter((client) => client.status === "ACTIVE").length ?? 0}</strong>
            <span style={metricLabelStyle}>Active records</span>
          </div>
        </div>
      </section>

      <div style={gridStyle}>
        <section style={cardStyle}>
          <h3 style={sectionTitleStyle}>Create client</h3>
          <form onSubmit={handleCreate} style={{ display: "grid", gap: "0.75rem" }}>
            <input
              placeholder="Display name"
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
            />
            <input
              placeholder="Actual ClientID (optional)"
              value={requestedClientId}
              onChange={(event) => setRequestedClientId(event.target.value)}
            />
            <select value={clientType} onChange={(event) => setClientType(event.target.value as ClientType)}>
              <option value="BUSINESS">Business</option>
              <option value="INDIVIDUAL">Individual</option>
            </select>
            <button type="submit" style={buttonStyle} disabled={createClientMutation.isPending}>
              {createClientMutation.isPending ? "Creating..." : "Create client"}
            </button>
          </form>
          <Link to="/clients/import" style={linkStyle}>
            Open CSV import
          </Link>
        </section>

        <section style={cardStyle}>
          <h3 style={sectionTitleStyle}>Find client</h3>
          <input
            placeholder="Search by name or ClientID"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          <div style={{ display: "grid", gap: "0.75rem", marginTop: "1rem" }}>
            {clientsQuery.data?.map((client) => (
              <Link key={client.id} to={`/clients/${client.id}`} style={clientLinkStyle}>
                <strong>{client.displayName}</strong>
                <span>{client.clientId}{client.clientIdTemporary ? " (Temporary)" : ""}</span>
              </Link>
            ))}
            {clientsQuery.data?.length === 0 ? <span>No clients found yet.</span> : null}
          </div>
        </section>
      </div>
    </section>
  );
}

const gridStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(320px, 380px) minmax(0, 1fr)",
  gap: "1rem",
};

const cardStyle: React.CSSProperties = {
  ...ui.card,
  padding: "1.25rem",
};

const buttonStyle: React.CSSProperties = {
  ...ui.primaryButton,
};

const linkStyle: React.CSSProperties = {
  display: "inline-block",
  marginTop: "1rem",
  color: "var(--accent)",
  fontWeight: 700,
  textDecoration: "none",
};

const clientLinkStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.28rem",
  textDecoration: "none",
  color: "var(--text)",
  padding: "1rem 1.05rem",
  borderRadius: "1rem",
  background: "var(--panel-muted)",
  border: "1px solid rgba(191, 208, 226, 0.72)",
};

const sectionTitleStyle: React.CSSProperties = {
  marginTop: 0,
  marginBottom: "1rem",
};

const metricValueStyle: React.CSSProperties = {
  fontSize: "1.5rem",
  letterSpacing: "-0.03em",
};

const metricLabelStyle: React.CSSProperties = {
  color: "var(--muted)",
  fontSize: "0.84rem",
};
