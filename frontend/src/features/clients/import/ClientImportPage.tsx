import { FormEvent, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { ui } from "../../../app/ui";
import { ClientImportResult, importClients } from "../../../api/clients";
import { ApiClientError } from "../../../api/client";

export function ClientImportPage() {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<ClientImportResult | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const importMutation = useMutation({
    mutationFn: importClients,
    onSuccess: (response) => {
      setResult(response);
      setErrorMessage(null);
    },
    onError: (error: ApiClientError) => {
      setErrorMessage(error.message);
    },
  });

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file) {
      setErrorMessage("Select a CSV file to import.");
      return;
    }
    importMutation.mutate(file);
  }

  return (
    <section style={ui.page}>
      <form onSubmit={handleSubmit} style={{ ...ui.card, display: "grid", gap: "0.75rem", maxWidth: "620px" }}>
        <input
          type="file"
          accept=".csv,text/csv"
          onChange={(event) => setFile(event.target.files?.[0] ?? null)}
        />
        <button type="submit" disabled={importMutation.isPending} style={buttonStyle}>
          {importMutation.isPending ? "Importing..." : "Import CSV"}
        </button>
      </form>

      {errorMessage ? <div style={{ color: "var(--ikms-danger)", fontWeight: 600 }}>{errorMessage}</div> : null}

      {result ? (
        <section style={{ display: "grid", gap: "1rem" }}>
          <div style={summaryStyle}>
            <strong>{result.filename}</strong>
            <span>{result.acceptedRows} accepted</span>
            <span>{result.warningCount} warnings</span>
            <span>{result.errorCount} errors</span>
          </div>

          {result.fileErrors.length > 0 ? (
            <div style={{ color: "var(--ikms-danger)", fontWeight: 600 }}>{result.fileErrors.join(" ")}</div>
          ) : null}

          <div style={{ overflowX: "auto" }}>
            <table style={tableStyle}>
              <thead>
                <tr>
                  <th>Line</th>
                  <th>Client ID</th>
                  <th>Name</th>
                  <th>Status</th>
                  <th>Warnings</th>
                  <th>Errors</th>
                </tr>
              </thead>
              <tbody>
                {result.rows.map((row) => (
                  <tr key={row.lineNumber}>
                    <td>{row.lineNumber}</td>
                    <td>{row.clientId || "Missing"}</td>
                    <td>{row.displayName || "Missing"}</td>
                    <td>{row.accepted ? "Accepted" : "Rejected"}</td>
                    <td>{row.warnings.join(", ") || "-"}</td>
                    <td>{row.errors.join(", ") || "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}
    </section>
  );
}

const buttonStyle: React.CSSProperties = {
  ...ui.primaryButton,
};

const summaryStyle: React.CSSProperties = {
  display: "flex",
  gap: "1rem",
  flexWrap: "wrap",
  padding: "1rem",
  borderRadius: "4px",
  background: "var(--ikms-panel-muted)",
  border: "1px solid var(--ikms-line)",
};

const tableStyle: React.CSSProperties = {
  width: "100%",
  borderCollapse: "collapse",
};
