export function ReviewQueuePage() {
  return (
    <section style={{ display: "grid", gap: "1.5rem" }}>
      <header>
        <h2 style={{ marginBottom: "0.35rem" }}>Review queue</h2>
        <p style={{ margin: 0, color: "#6d6253" }}>
          Resolve unlinked or low-confidence intake items without administrator assistance.
        </p>
      </header>

      <div style={gridStyle}>
        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>Filters</h3>
          <p>Reason, status, source, date, and confidence filters will narrow review items.</p>
        </section>
        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>Open review item</h3>
          <p>Reviewers will inspect extracted details and intake evidence here.</p>
        </section>
        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>Resolve actions</h3>
          <p>Select a client, correct metadata, approve, reject, or leave the item pending.</p>
        </section>
      </div>
    </section>
  );
}

const gridStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
  gap: "1rem",
};

const cardStyle: React.CSSProperties = {
  padding: "1rem",
  borderRadius: "1rem",
  background: "#fff8ee",
  border: "1px solid rgba(31, 28, 24, 0.1)",
};
