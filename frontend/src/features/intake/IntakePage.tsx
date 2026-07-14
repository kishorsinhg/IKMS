import { useMemo, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { ui } from "../../app/ui";
import { listClients } from "../../api/clients";
import { uploadDocument } from "../../api/intake";

export function IntakePage() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [selectedClientId, setSelectedClientId] = useState("");
  const clientsQuery = useQuery({
    queryKey: ["clients", "intake-selector"],
    queryFn: () => listClients(""),
  });
  const uploadMutation = useMutation({
    mutationFn: () => uploadDocument(selectedFile!, selectedClientId || undefined),
  });

  const uploadResult = uploadMutation.data;
  const canUpload = useMemo(() => selectedFile !== null && !uploadMutation.isPending, [selectedFile, uploadMutation.isPending]);

  return (
    <section style={ui.page}>
      <div style={gridStyle}>
        <section style={cardStyle}>
          <h3 style={sectionTitleStyle}>Manual upload</h3>
          <div style={{ display: "grid", gap: "0.75rem" }}>
            <input
              type="file"
              accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
              onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
            />
            <label style={{ display: "grid", gap: "0.35rem" }}>
              <span>Route directly to client</span>
              <select value={selectedClientId} onChange={(event) => setSelectedClientId(event.target.value)}>
                <option value="">Leave unlinked for review</option>
                {clientsQuery.data?.map((client) => (
                  <option key={client.id} value={client.id}>{client.displayName}</option>
                ))}
              </select>
            </label>
            <button type="button" style={buttonStyle} disabled={!canUpload} onClick={() => uploadMutation.mutate()}>
              {uploadMutation.isPending ? "Uploading..." : "Upload document"}
            </button>
            <p style={{ margin: 0 }}>Upload PDF or DOCX knowledge items and route them to a client or review.</p>
          </div>
        </section>

        <section style={cardStyle}>
          <h3 style={sectionTitleStyle}>Duplicate result</h3>
          {uploadResult ? (
            <div style={{ display: "grid", gap: "0.35rem" }}>
              <strong>{uploadResult.outcome === "DUPLICATE" ? "Duplicate blocked" : "Upload accepted"}</strong>
              <span>Review status: {uploadResult.reviewStatus}</span>
              {uploadResult.documentId ? <span>Document ID: {uploadResult.documentId}</span> : null}
              {uploadResult.duplicateOfDocumentId ? <span>Duplicate of: {uploadResult.duplicateOfDocumentId}</span> : null}
            </div>
          ) : (
            <p>Exact duplicate outcomes will identify when an upload already exists.</p>
          )}
          {uploadMutation.isError ? <p style={errorStyle}>Upload failed. Confirm the file is PDF or DOCX and try again.</p> : null}
        </section>

        <section style={cardStyle}>
          <h3 style={sectionTitleStyle}>Automated intake status</h3>
          <div style={{ display: "grid", gap: "0.75rem" }}>
            <div style={statusCardStyle}>
              <strong>Shared folder</strong>
              <span>Worker is available in the backend pipeline.</span>
            </div>
            <div style={statusCardStyle}>
              <strong>IMAP mailbox</strong>
              <span>Worker is available in the backend pipeline.</span>
            </div>
          </div>
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
  ...ui.card,
};

const buttonStyle: React.CSSProperties = {
  ...ui.primaryButton,
};

const statusCardStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.25rem",
  padding: "0.9rem 1rem",
  borderRadius: "4px",
  background: "var(--ikms-panel-muted)",
  border: "1px solid var(--ikms-line)",
};

const errorStyle: React.CSSProperties = {
  marginBottom: 0,
  color: "var(--ikms-danger)",
};

const sectionTitleStyle: React.CSSProperties = {
  marginTop: 0,
  marginBottom: "1rem",
};
