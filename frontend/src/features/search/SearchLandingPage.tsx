import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { ui } from "../../app/ui";
import { listClients } from "../../api/clients";

export function SearchLandingPage() {
  const clientsQuery = useQuery({
    queryKey: ["clients", "search-landing"],
    queryFn: () => listClients(""),
  });

  return (
    <section style={ui.page}>
      <header style={ui.pageHeader}>
        <h2 style={ui.pageTitle}>Client search and AI Q&A</h2>
        <p style={ui.pageDescription}>
          Search and AI Q&A stay within one selected client profile in V1.
        </p>
      </header>
      <div style={{ display: "grid", gap: "0.75rem" }}>
        {clientsQuery.data?.map((client) => (
          <Link key={client.id} to={`/clients/${client.id}`} style={linkStyle}>
            Open {client.displayName}
          </Link>
        ))}
        {clientsQuery.data?.length === 0 ? <span>No clients available yet.</span> : null}
      </div>
    </section>
  );
}

const linkStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.25rem",
  color: "var(--text)",
  fontWeight: 700,
  textDecoration: "none",
  padding: "1rem 1.05rem",
  borderRadius: "1rem",
  background: "var(--panel)",
  border: "1px solid var(--line)",
  boxShadow: "0 8px 18px rgba(15, 23, 40, 0.04)",
};
