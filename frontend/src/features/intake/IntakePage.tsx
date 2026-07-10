export function IntakePage() {
  return (
    <section style={{ display: "grid", gap: "1.5rem" }}>
      <header>
        <h2 style={{ marginBottom: "0.35rem" }}>Intake operations</h2>
        <p style={{ margin: 0, color: "#6d6253" }}>
          Manual upload, duplicate outcomes, and automated intake status will be managed here.
        </p>
      </header>

      <div style={gridStyle}>
        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>Manual upload</h3>
          <input type="file" accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document" />
          <p>Upload PDF or DOCX knowledge items and route them to a client or review.</p>
        </section>

        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>Duplicate result</h3>
          <p>Exact duplicate outcomes will identify when an upload already exists.</p>
        </section>

        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>Automated intake status</h3>
          <p>Shared-folder and IMAP processing status will appear here for authorized users.</p>
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
