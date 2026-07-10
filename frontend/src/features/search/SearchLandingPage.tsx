import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { listClients } from "../../api/clients";

export function SearchLandingPage() {
  const clientsQuery = useQuery({
    queryKey: ["clients", "search-landing"],
    queryFn: () => listClients(""),
  });

  return (
    <section style={{ display: "grid", gap: "1rem" }}>
      <header>
        <h2 style={{ marginBottom: "0.35rem" }}>Client search and AI Q&A</h2>
        <p style={{ margin: 0, color: "#6d6253" }}>
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
  width: "fit-content",
  color: "#1f1c18",
  fontWeight: 700,
  textDecoration: "none",
};
