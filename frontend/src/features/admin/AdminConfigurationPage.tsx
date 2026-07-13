import { FormEvent, useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ui } from "../../app/ui";
import {
  createDocumentType,
  createMailbox,
  createMetadataField,
  createSharedFolder,
  getAiSetting,
  getReviewSetting,
  listAdminUsers,
  listDocumentTypes,
  listMailboxes,
  listMetadataFields,
  listSharedFolders,
  updateAiSetting,
  updateReviewSetting,
} from "../../api/admin";

export function AdminConfigurationPage() {
  const queryClient = useQueryClient();
  const [documentTypeName, setDocumentTypeName] = useState("");
  const [metadataFieldKey, setMetadataFieldKey] = useState("");
  const [metadataLabel, setMetadataLabel] = useState("");
  const [metadataFieldPii, setMetadataFieldPii] = useState(false);
  const [sharedFolderPath, setSharedFolderPath] = useState("");
  const [mailboxName, setMailboxName] = useState("");
  const [mailboxHost, setMailboxHost] = useState("");
  const [mailboxUsername, setMailboxUsername] = useState("");
  const [reviewMode, setReviewMode] = useState("confidence");
  const [reviewThreshold, setReviewThreshold] = useState("0.75");
  const [providerName, setProviderName] = useState("mistral");
  const [modelName, setModelName] = useState("mistral-small");
  const [apiBaseUrl, setApiBaseUrl] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [ocrProvider, setOcrProvider] = useState("tesseract");

  const usersQuery = useQuery({ queryKey: ["admin", "users"], queryFn: listAdminUsers });
  const documentTypesQuery = useQuery({ queryKey: ["admin", "document-types"], queryFn: listDocumentTypes });
  const metadataFieldsQuery = useQuery({ queryKey: ["admin", "metadata-fields"], queryFn: listMetadataFields });
  const sharedFoldersQuery = useQuery({ queryKey: ["admin", "shared-folders"], queryFn: listSharedFolders });
  const mailboxesQuery = useQuery({ queryKey: ["admin", "mailboxes"], queryFn: listMailboxes });
  const reviewSettingQuery = useQuery({ queryKey: ["admin", "review-setting"], queryFn: getReviewSetting });
  const aiSettingQuery = useQuery({ queryKey: ["admin", "ai-setting"], queryFn: getAiSetting });

  const docTypeMutation = useMutation({
    mutationFn: () => createDocumentType({ name: documentTypeName, active: true }),
    onSuccess: async () => {
      setDocumentTypeName("");
      await queryClient.invalidateQueries({ queryKey: ["admin", "document-types"] });
    },
  });
  const metadataMutation = useMutation({
    mutationFn: () => createMetadataField({ fieldKey: metadataFieldKey, label: metadataLabel, pii: metadataFieldPii, active: true }),
    onSuccess: async () => {
      setMetadataFieldKey("");
      setMetadataLabel("");
      setMetadataFieldPii(false);
      await queryClient.invalidateQueries({ queryKey: ["admin", "metadata-fields"] });
    },
  });
  const folderMutation = useMutation({
    mutationFn: () => createSharedFolder({ path: sharedFolderPath, active: true }),
    onSuccess: async () => {
      setSharedFolderPath("");
      await queryClient.invalidateQueries({ queryKey: ["admin", "shared-folders"] });
    },
  });
  const mailboxMutation = useMutation({
    mutationFn: () => createMailbox({ name: mailboxName, host: mailboxHost, username: mailboxUsername, active: true }),
    onSuccess: async () => {
      setMailboxName("");
      setMailboxHost("");
      setMailboxUsername("");
      await queryClient.invalidateQueries({ queryKey: ["admin", "mailboxes"] });
    },
  });
  const reviewMutation = useMutation({
    mutationFn: () => updateReviewSetting({ mode: reviewMode, lowConfidenceThreshold: Number(reviewThreshold) }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin", "review-setting"] });
    },
  });
  const aiMutation = useMutation({
    mutationFn: () => updateAiSetting({ providerName, modelName, apiBaseUrl, apiKey, ocrProvider, active: true }),
    onSuccess: async () => {
      setApiKey("");
      await queryClient.invalidateQueries({ queryKey: ["admin", "ai-setting"] });
    },
  });

  useEffect(() => {
    if (reviewSettingQuery.data) {
      setReviewMode(reviewSettingQuery.data.mode);
      setReviewThreshold(String(reviewSettingQuery.data.lowConfidenceThreshold));
    }
  }, [reviewSettingQuery.data]);

  useEffect(() => {
    if (aiSettingQuery.data) {
      setProviderName(aiSettingQuery.data.providerName);
      setModelName(aiSettingQuery.data.modelName);
      setApiBaseUrl(aiSettingQuery.data.apiBaseUrl ?? "");
      setOcrProvider(aiSettingQuery.data.ocrProvider);
    }
  }, [aiSettingQuery.data]);

  function submit(event: FormEvent, action: () => void) {
    event.preventDefault();
    action();
  }

  return (
    <section style={ui.page}>
      <header style={ui.pageHeader}>
        <p style={ui.eyebrow}>Administration / Configuration</p>
        <h2 style={ui.pageTitle}>Administration</h2>
        <p style={ui.pageDescription}>
          Configure broker knowledge rules, intake paths, review behavior, and AI providers.
        </p>
      </header>

      <section style={ui.heroCard}>
        <div style={ui.metricRow}>
          <div style={ui.metricCard}>
            <strong style={metricValueStyle}>{usersQuery.data?.length ?? 0}</strong>
            <span style={metricLabelStyle}>Provisioned users</span>
          </div>
          <div style={ui.metricCard}>
            <strong style={metricValueStyle}>{documentTypesQuery.data?.length ?? 0}</strong>
            <span style={metricLabelStyle}>Configured types</span>
          </div>
          <div style={ui.metricCard}>
            <strong style={metricValueStyle}>{metadataFieldsQuery.data?.filter((item) => item.pii).length ?? 0}</strong>
            <span style={metricLabelStyle}>PII fields</span>
          </div>
          <div style={ui.metricCard}>
            <strong style={metricValueStyle}>{mailboxesQuery.data?.length ?? 0}</strong>
            <span style={metricLabelStyle}>Mailboxes</span>
          </div>
        </div>
      </section>

      <div style={gridStyle}>
        <section style={cardStyle}>
          <h3 style={sectionTitleStyle}>Users and roles</h3>
          <div style={tableWrapStyle}>
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Username</th>
                  <th>Status</th>
                  <th>Roles</th>
                </tr>
              </thead>
              <tbody>
                {usersQuery.data?.map((user) => (
                  <tr key={user.id}>
                    <td>{user.displayName}</td>
                    <td>{user.username}</td>
                    <td><span style={ui.statusBadge}>{user.status}</span></td>
                    <td>{user.roles.join(", ")}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section style={cardStyle}>
          <h3 style={sectionTitleStyle}>Document types</h3>
          <form onSubmit={(event) => submit(event, () => docTypeMutation.mutate())} style={formStyle}>
            <input value={documentTypeName} onChange={(event) => setDocumentTypeName(event.target.value)} placeholder="New document type" />
            <button type="submit" style={buttonStyle}>Add document type</button>
          </form>
          <div style={listStyle}>
            {documentTypesQuery.data?.map((item) => <div key={item.id} style={itemStyle}>{item.name}</div>)}
          </div>
        </section>

        <section style={cardStyle}>
          <h3 style={sectionTitleStyle}>Metadata fields</h3>
          <form onSubmit={(event) => submit(event, () => metadataMutation.mutate())} style={formStyle}>
            <input value={metadataFieldKey} onChange={(event) => setMetadataFieldKey(event.target.value)} placeholder="Field key" />
            <input value={metadataLabel} onChange={(event) => setMetadataLabel(event.target.value)} placeholder="Field label" />
            <label style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
              <input type="checkbox" checked={metadataFieldPii} onChange={(event) => setMetadataFieldPii(event.target.checked)} />
              <span>Mark as PII</span>
            </label>
            <button type="submit" style={buttonStyle}>Add metadata field</button>
          </form>
          <div style={listStyle}>
            {metadataFieldsQuery.data?.map((item) => <div key={item.id} style={itemStyle}>{item.label} · {item.pii ? "PII" : "Standard"}</div>)}
          </div>
        </section>

        <section style={cardStyle}>
          <h3 style={sectionTitleStyle}>Shared folder paths</h3>
          <form onSubmit={(event) => submit(event, () => folderMutation.mutate())} style={formStyle}>
            <input value={sharedFolderPath} onChange={(event) => setSharedFolderPath(event.target.value)} placeholder="/network/share/incoming" />
            <button type="submit" style={buttonStyle}>Add shared folder</button>
          </form>
          <div style={listStyle}>
            {sharedFoldersQuery.data?.map((item) => <div key={item.id} style={itemStyle}>{item.path}</div>)}
          </div>
        </section>

        <section style={cardStyle}>
          <h3 style={sectionTitleStyle}>IMAP mailboxes</h3>
          <form onSubmit={(event) => submit(event, () => mailboxMutation.mutate())} style={formStyle}>
            <input value={mailboxName} onChange={(event) => setMailboxName(event.target.value)} placeholder="Mailbox name" />
            <input value={mailboxHost} onChange={(event) => setMailboxHost(event.target.value)} placeholder="imap.example.com" />
            <input value={mailboxUsername} onChange={(event) => setMailboxUsername(event.target.value)} placeholder="mailbox user" />
            <button type="submit" style={buttonStyle}>Add mailbox</button>
          </form>
          <div style={listStyle}>
            {mailboxesQuery.data?.map((item) => <div key={item.id} style={itemStyle}>{item.name} · {item.host}</div>)}
          </div>
        </section>

        <section style={cardStyle}>
          <h3 style={sectionTitleStyle}>Review mode</h3>
          <form onSubmit={(event) => submit(event, () => reviewMutation.mutate())} style={formStyle}>
            <input value={reviewMode} onChange={(event) => setReviewMode(event.target.value)} placeholder="confidence" />
            <input value={reviewThreshold} onChange={(event) => setReviewThreshold(event.target.value)} placeholder="0.75" />
            <button type="submit" style={buttonStyle}>Save review settings</button>
          </form>
          {reviewSettingQuery.data ? <div style={itemStyle}>{reviewSettingQuery.data.mode} · {reviewSettingQuery.data.lowConfidenceThreshold}</div> : null}
        </section>

        <section style={cardStyle}>
          <h3 style={sectionTitleStyle}>AI/OCR provider settings</h3>
          <form onSubmit={(event) => submit(event, () => aiMutation.mutate())} style={formStyle}>
            <input value={providerName} onChange={(event) => setProviderName(event.target.value)} placeholder="AI provider" />
            <input value={modelName} onChange={(event) => setModelName(event.target.value)} placeholder="Model" />
            <input value={apiBaseUrl} onChange={(event) => setApiBaseUrl(event.target.value)} placeholder="https://api.provider.com/v1" />
            <input
              value={apiKey}
              onChange={(event) => setApiKey(event.target.value)}
              placeholder={aiSettingQuery.data?.apiKeyConfigured ? "Configured - enter new key to rotate" : "API key"}
              type="password"
            />
            <input value={ocrProvider} onChange={(event) => setOcrProvider(event.target.value)} placeholder="OCR provider" />
            <button type="submit" style={buttonStyle}>Save AI settings</button>
          </form>
          {aiSettingQuery.data ? (
            <div style={itemStyle}>
              {aiSettingQuery.data.providerName} · {aiSettingQuery.data.modelName}
              <span>{aiSettingQuery.data.apiBaseUrl || "No API base URL configured"}</span>
              <span>{aiSettingQuery.data.apiKeyConfigured ? "API key configured" : "API key not configured"}</span>
            </div>
          ) : null}
        </section>
      </div>
    </section>
  );
}

const gridStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))",
  gap: "1rem",
};

const cardStyle: React.CSSProperties = {
  ...ui.card,
};

const formStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.5rem",
  marginBottom: "0.75rem",
};

const listStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.5rem",
};

const itemStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.2rem",
  padding: "0.85rem 0.95rem",
  borderRadius: "0.95rem",
  background: "var(--panel-muted)",
  border: "1px solid rgba(191, 208, 226, 0.72)",
};

const buttonStyle: React.CSSProperties = {
  ...ui.primaryButton,
};

const tableWrapStyle: React.CSSProperties = {
  overflowX: "auto",
};

const sectionTitleStyle: React.CSSProperties = {
  marginTop: 0,
  marginBottom: "1rem",
};

const metricValueStyle: React.CSSProperties = {
  fontSize: "1.35rem",
  letterSpacing: "-0.03em",
};

const metricLabelStyle: React.CSSProperties = {
  color: "var(--muted)",
  fontSize: "0.84rem",
};
