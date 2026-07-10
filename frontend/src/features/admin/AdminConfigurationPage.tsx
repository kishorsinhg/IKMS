import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
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
  const [sharedFolderPath, setSharedFolderPath] = useState("");
  const [mailboxName, setMailboxName] = useState("");
  const [mailboxHost, setMailboxHost] = useState("");
  const [mailboxUsername, setMailboxUsername] = useState("");
  const [reviewMode, setReviewMode] = useState("confidence");
  const [reviewThreshold, setReviewThreshold] = useState("0.75");
  const [providerName, setProviderName] = useState("mistral");
  const [modelName, setModelName] = useState("mistral-small");
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
    mutationFn: () => createMetadataField({ fieldKey: metadataFieldKey, label: metadataLabel, pii: true, active: true }),
    onSuccess: async () => {
      setMetadataFieldKey("");
      setMetadataLabel("");
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
    mutationFn: () => updateAiSetting({ providerName, modelName, ocrProvider, active: true }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin", "ai-setting"] });
    },
  });

  function submit(event: FormEvent, action: () => void) {
    event.preventDefault();
    action();
  }

  return (
    <section style={{ display: "grid", gap: "1.5rem" }}>
      <header>
        <h2 style={{ marginBottom: "0.35rem" }}>Administration</h2>
        <p style={{ margin: 0, color: "#6d6253" }}>
          Configure broker knowledge rules, intake paths, review behavior, and AI providers.
        </p>
      </header>

      <div style={gridStyle}>
        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>Users and roles</h3>
          <div style={listStyle}>
            {usersQuery.data?.map((user) => (
              <div key={user.id} style={itemStyle}>
                <strong>{user.displayName}</strong>
                <span>{user.username} · {user.roles.join(", ")}</span>
              </div>
            ))}
          </div>
        </section>

        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>Document types</h3>
          <form onSubmit={(event) => submit(event, () => docTypeMutation.mutate())} style={formStyle}>
            <input value={documentTypeName} onChange={(event) => setDocumentTypeName(event.target.value)} placeholder="New document type" />
            <button type="submit" style={buttonStyle}>Add document type</button>
          </form>
          <div style={listStyle}>
            {documentTypesQuery.data?.map((item) => <div key={item.id} style={itemStyle}>{item.name}</div>)}
          </div>
        </section>

        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>Metadata fields</h3>
          <form onSubmit={(event) => submit(event, () => metadataMutation.mutate())} style={formStyle}>
            <input value={metadataFieldKey} onChange={(event) => setMetadataFieldKey(event.target.value)} placeholder="Field key" />
            <input value={metadataLabel} onChange={(event) => setMetadataLabel(event.target.value)} placeholder="Field label" />
            <button type="submit" style={buttonStyle}>Add metadata field</button>
          </form>
          <div style={listStyle}>
            {metadataFieldsQuery.data?.map((item) => <div key={item.id} style={itemStyle}>{item.label} · {item.pii ? "PII" : "Standard"}</div>)}
          </div>
        </section>

        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>Shared folder paths</h3>
          <form onSubmit={(event) => submit(event, () => folderMutation.mutate())} style={formStyle}>
            <input value={sharedFolderPath} onChange={(event) => setSharedFolderPath(event.target.value)} placeholder="/network/share/incoming" />
            <button type="submit" style={buttonStyle}>Add shared folder</button>
          </form>
          <div style={listStyle}>
            {sharedFoldersQuery.data?.map((item) => <div key={item.id} style={itemStyle}>{item.path}</div>)}
          </div>
        </section>

        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>IMAP mailboxes</h3>
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
          <h3 style={{ marginTop: 0 }}>Review mode</h3>
          <form onSubmit={(event) => submit(event, () => reviewMutation.mutate())} style={formStyle}>
            <input value={reviewMode} onChange={(event) => setReviewMode(event.target.value)} placeholder="confidence" />
            <input value={reviewThreshold} onChange={(event) => setReviewThreshold(event.target.value)} placeholder="0.75" />
            <button type="submit" style={buttonStyle}>Save review settings</button>
          </form>
          {reviewSettingQuery.data ? <div style={itemStyle}>{reviewSettingQuery.data.mode} · {reviewSettingQuery.data.lowConfidenceThreshold}</div> : null}
        </section>

        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>AI/OCR provider settings</h3>
          <form onSubmit={(event) => submit(event, () => aiMutation.mutate())} style={formStyle}>
            <input value={providerName} onChange={(event) => setProviderName(event.target.value)} placeholder="AI provider" />
            <input value={modelName} onChange={(event) => setModelName(event.target.value)} placeholder="Model" />
            <input value={ocrProvider} onChange={(event) => setOcrProvider(event.target.value)} placeholder="OCR provider" />
            <button type="submit" style={buttonStyle}>Save AI settings</button>
          </form>
          {aiSettingQuery.data ? <div style={itemStyle}>{aiSettingQuery.data.providerName} · {aiSettingQuery.data.modelName}</div> : null}
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
  padding: "1rem",
  borderRadius: "1rem",
  background: "#fff8ee",
  border: "1px solid rgba(31, 28, 24, 0.1)",
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
  padding: "0.75rem",
  borderRadius: "0.85rem",
  background: "#f7efe0",
};

const buttonStyle: React.CSSProperties = {
  width: "fit-content",
  padding: "0.7rem 1rem",
  borderRadius: "999px",
  border: "none",
  background: "#1f1c18",
  color: "#fffaf0",
  fontWeight: 700,
  cursor: "pointer",
};
