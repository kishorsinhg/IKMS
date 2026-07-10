import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
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
    <section style={{ display: "grid", gap: "1.5rem" }}>
      <header>
        <h2 style={{ marginBottom: "0.35rem" }}>Clients workspace</h2>
        <p style={{ margin: 0, color: "#6d6253" }}>
          Create clients, generate temporary IDs when needed, and open the client profile workspace.
        </p>
      </header>

      <div style={gridStyle}>
        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>Create client</h3>
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
          <h3 style={{ marginTop: 0 }}>Find client</h3>
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
  gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
  gap: "1rem",
};

const cardStyle: React.CSSProperties = {
  padding: "1.25rem",
  borderRadius: "1rem",
  background: "#fff8ee",
  border: "1px solid rgba(31, 28, 24, 0.1)",
};

const buttonStyle: React.CSSProperties = {
  width: "fit-content",
  padding: "0.75rem 1rem",
  borderRadius: "999px",
  border: "none",
  background: "#1f1c18",
  color: "#fffaf0",
  fontWeight: 700,
  cursor: "pointer",
};

const linkStyle: React.CSSProperties = {
  display: "inline-block",
  marginTop: "1rem",
  color: "#1f1c18",
  fontWeight: 700,
  textDecoration: "none",
};

const clientLinkStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.2rem",
  textDecoration: "none",
  color: "#1f1c18",
  padding: "0.9rem 1rem",
  borderRadius: "0.8rem",
  background: "#f7efe0",
};
