import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ui } from "../../../app/ui";
import { listDocumentTypes, listMetadataFields } from "../../../api/admin";
import { listClients } from "../../../api/clients";
import {
  approveReviewItem,
  correctReviewItemMetadata,
  getReviewQueueItem,
  linkReviewItemClient,
  listReviewQueue,
  rejectReviewItem,
  ReviewQueueReason,
  ReviewQueueStatus,
} from "../../../api/intake";

export function ReviewQueuePage() {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<ReviewQueueStatus | "">("OPEN");
  const [reasonFilter, setReasonFilter] = useState<ReviewQueueReason | "">("");
  const [selectedItemId, setSelectedItemId] = useState("");
  const [selectedClientId, setSelectedClientId] = useState("");
  const [title, setTitle] = useState("");
  const [documentTypeId, setDocumentTypeId] = useState("");
  const [carrier, setCarrier] = useState("");
  const [policyNumber, setPolicyNumber] = useState("");
  const [rejectReason, setRejectReason] = useState("");

  const itemsQuery = useQuery({
    queryKey: ["review-queue", statusFilter, reasonFilter],
    queryFn: () => listReviewQueue(statusFilter, reasonFilter),
  });
  const detailQuery = useQuery({
    queryKey: ["review-queue", "detail", selectedItemId],
    queryFn: () => getReviewQueueItem(selectedItemId),
    enabled: Boolean(selectedItemId),
  });
  const clientsQuery = useQuery({
    queryKey: ["clients", "review-selector"],
    queryFn: () => listClients(""),
  });
  const documentTypesQuery = useQuery({
    queryKey: ["admin", "document-types", "review"],
    queryFn: listDocumentTypes,
  });
  const metadataFieldsQuery = useQuery({
    queryKey: ["admin", "metadata-fields", "review"],
    queryFn: listMetadataFields,
  });
  const selectedItem = detailQuery.data;

  useEffect(() => {
    const firstItemId = itemsQuery.data?.[0]?.id ?? "";
    if (!selectedItemId || !itemsQuery.data?.some((item) => item.id === selectedItemId)) {
      setSelectedItemId(firstItemId);
    }
  }, [itemsQuery.data, selectedItemId]);

  useEffect(() => {
    if (!selectedItem) {
      setTitle("");
      setDocumentTypeId("");
      setCarrier("");
      setPolicyNumber("");
      return;
    }

    setTitle(selectedItem.title ?? "");
    setDocumentTypeId(selectedItem.documentTypeId ?? "");
    setCarrier(selectedItem.metadataValues?.carrier ?? "");
    setPolicyNumber(selectedItem.metadataValues?.policyNumber ?? "");
  }, [selectedItem]);

  const refreshQueue = async () => {
    await queryClient.invalidateQueries({ queryKey: ["review-queue"] });
  };

  const linkMutation = useMutation({
    mutationFn: () => linkReviewItemClient(selectedItemId, selectedClientId),
    onSuccess: refreshQueue,
  });
  const metadataMutation = useMutation({
    mutationFn: () => correctReviewItemMetadata(selectedItemId, {
      title,
      documentTypeId: documentTypeId || undefined,
      metadataValues: {
        ...(carrier.trim() ? { carrier: carrier.trim() } : {}),
        ...(policyNumber.trim() ? { policyNumber: policyNumber.trim() } : {}),
      },
    }),
    onSuccess: async () => {
      await refreshQueue();
    },
  });
  const approveMutation = useMutation({
    mutationFn: () => approveReviewItem(selectedItemId),
    onSuccess: refreshQueue,
  });
  const rejectMutation = useMutation({
    mutationFn: () => rejectReviewItem(selectedItemId, rejectReason),
    onSuccess: async () => {
      setRejectReason("");
      await refreshQueue();
    },
  });

  return (
    <section style={ui.page}>
      <header style={ui.pageHeader}>
        <p style={ui.eyebrow}>Indexer workspace / Review queue</p>
        <h2 style={ui.pageTitle}>Review queue</h2>
        <p style={ui.pageDescription}>
          Resolve unlinked or low-confidence intake items without administrator assistance.
        </p>
      </header>

      <section style={ui.heroCard}>
        <div style={ui.metricRow}>
          <div style={ui.metricCard}>
            <strong style={metricValueStyle}>{itemsQuery.data?.length ?? 0}</strong>
            <span style={metricLabelStyle}>Visible items</span>
          </div>
          <div style={ui.metricCard}>
            <strong style={metricValueStyle}>{itemsQuery.data?.filter((item) => item.status === "OPEN").length ?? 0}</strong>
            <span style={metricLabelStyle}>Open</span>
          </div>
          <div style={ui.metricCard}>
            <strong style={metricValueStyle}>{itemsQuery.data?.filter((item) => lowConfidenceReasons.has(item.reason)).length ?? 0}</strong>
            <span style={metricLabelStyle}>Low confidence</span>
          </div>
        </div>
      </section>

      <div style={layoutStyle}>
        <section style={cardStyle}>
          <h3 style={sectionTitleStyle}>Filters</h3>
          <div style={{ display: "grid", gap: "0.75rem" }}>
            <label style={{ display: "grid", gap: "0.35rem" }}>
              <span>Status</span>
              <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as ReviewQueueStatus | "")}>
                <option value="">All statuses</option>
                <option value="OPEN">Open</option>
                <option value="IN_PROGRESS">In progress</option>
                <option value="RESOLVED">Resolved</option>
                <option value="REJECTED">Rejected</option>
              </select>
            </label>
            <label style={{ display: "grid", gap: "0.35rem" }}>
              <span>Reason</span>
              <select value={reasonFilter} onChange={(event) => setReasonFilter(event.target.value as ReviewQueueReason | "")}>
                <option value="">All reasons</option>
                <option value="UNLINKED">Unlinked</option>
                <option value="LOW_CLIENT_CONFIDENCE">Low client confidence</option>
                <option value="LOW_CLASSIFICATION_CONFIDENCE">Low classification confidence</option>
                <option value="LOW_EXTRACTION_CONFIDENCE">Low extraction confidence</option>
                <option value="DUPLICATE_UNCERTAINTY">Duplicate uncertainty</option>
                <option value="REDACTION_FAILED">Redaction failed</option>
                <option value="PROMPT_INJECTION_RISK">Prompt injection risk</option>
                <option value="PROCESSING_FAILED">Processing failed</option>
              </select>
            </label>
            <div style={tableWrapStyle}>
              <table>
                <thead>
                  <tr>
                    <th>Type</th>
                    <th>Reason</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {itemsQuery.data?.map((item) => (
                    <tr key={item.id}>
                      <td>{item.itemType}</td>
                      <td>{item.reason}</td>
                      <td><span style={ui.statusBadge}>{item.status}</span></td>
                      <td>
                        <button
                          type="button"
                          style={item.id === selectedItemId ? selectedItemButtonStyle : itemButtonStyle}
                          onClick={() => setSelectedItemId(item.id)}
                        >
                          {`${item.itemType} · ${item.reason} · ${item.status}`}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {itemsQuery.data?.length === 0 ? <span>No matching review items.</span> : null}
          </div>
        </section>

        <div style={detailColumnStyle}>
          <section style={cardStyle}>
          <h3 style={sectionTitleStyle}>Open review item</h3>
          {selectedItem ? (
            <div style={{ display: "grid", gap: "0.35rem" }}>
              <strong>{selectedItem.itemType}</strong>
              <span>Queue ID: {selectedItem.id}</span>
              <span>Item ID: {selectedItem.itemId}</span>
              <span>Title: {selectedItem.title ?? "n/a"}</span>
              <span>{selectedItem.reason} · <span style={ui.statusBadge}>{selectedItem.status}</span></span>
            </div>
          ) : (
            <p>Reviewers will inspect extracted details and intake evidence here.</p>
          )}
          </section>
          <section style={cardStyle}>
            <h3 style={sectionTitleStyle}>Resolve actions</h3>
            <div style={actionGridStyle}>
              <label style={{ display: "grid", gap: "0.35rem" }}>
                <span>Link to client</span>
                <select value={selectedClientId} onChange={(event) => setSelectedClientId(event.target.value)}>
                  <option value="">Select client</option>
                  {clientsQuery.data?.map((client) => (
                    <option key={client.id} value={client.id}>{client.displayName}</option>
                  ))}
                </select>
              </label>
              <button type="button" style={buttonStyle} disabled={!selectedItemId || !selectedClientId} onClick={() => linkMutation.mutate()}>
                Link client
              </button>
              <label style={{ display: "grid", gap: "0.35rem" }}>
                <span>Correct title</span>
                <input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Updated title" />
              </label>
              <label style={{ display: "grid", gap: "0.35rem" }}>
                <span>Document type</span>
                <select value={documentTypeId} onChange={(event) => setDocumentTypeId(event.target.value)}>
                  <option value="">Select document type</option>
                  {documentTypesQuery.data?.map((item) => (
                    <option key={item.id} value={item.id}>{item.name}</option>
                  ))}
                </select>
              </label>
              <label style={{ display: "grid", gap: "0.35rem" }}>
                <span>Carrier metadata</span>
                <input value={carrier} onChange={(event) => setCarrier(event.target.value)} placeholder="Carrier" />
              </label>
              <label style={{ display: "grid", gap: "0.35rem" }}>
                <span>Policy number metadata</span>
                <input value={policyNumber} onChange={(event) => setPolicyNumber(event.target.value)} placeholder="Policy number" />
              </label>
              {metadataFieldsQuery.data?.length ? (
                <span style={ui.pageDescription}>
                  Configured metadata fields: {metadataFieldsQuery.data.map((item) => item.fieldKey).join(", ")}
                </span>
              ) : null}
              <button type="button" style={buttonStyle} disabled={!selectedItemId || !title.trim()} onClick={() => metadataMutation.mutate()}>
                Save metadata
              </button>
              <button type="button" style={buttonStyle} disabled={!selectedItemId} onClick={() => approveMutation.mutate()}>
                Approve
              </button>
              <label style={{ display: "grid", gap: "0.35rem" }}>
                <span>Reject reason</span>
                <input value={rejectReason} onChange={(event) => setRejectReason(event.target.value)} placeholder="Reason for rejection" />
              </label>
              <button type="button" style={dangerButtonStyle} disabled={!selectedItemId} onClick={() => rejectMutation.mutate()}>
                Reject
              </button>
            </div>
          </section>
        </div>
      </div>
    </section>
  );
}

const layoutStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(420px, 1.1fr) minmax(320px, 0.9fr)",
  gap: "1rem",
};

const detailColumnStyle: React.CSSProperties = {
  display: "grid",
  gap: "1rem",
};

const cardStyle: React.CSSProperties = {
  ...ui.card,
};

const buttonStyle: React.CSSProperties = {
  ...ui.primaryButton,
};

const dangerButtonStyle: React.CSSProperties = {
  ...ui.dangerButton,
};

const itemButtonStyle: React.CSSProperties = {
  padding: "0.45rem 0.7rem",
  borderRadius: "0.75rem",
  border: "1px solid rgba(191, 208, 226, 0.72)",
  background: "var(--panel-solid)",
  cursor: "pointer",
};

const selectedItemButtonStyle: React.CSSProperties = {
  ...itemButtonStyle,
  background: "linear-gradient(180deg, var(--accent) 0%, var(--accent-strong) 100%)",
  border: "1px solid transparent",
  color: "#fffaf0",
};

const sectionTitleStyle: React.CSSProperties = {
  marginTop: 0,
  marginBottom: "1rem",
};

const tableWrapStyle: React.CSSProperties = {
  overflowX: "auto",
};

const actionGridStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.75rem",
};

const metricValueStyle: React.CSSProperties = {
  fontSize: "1.35rem",
  letterSpacing: "-0.03em",
};

const metricLabelStyle: React.CSSProperties = {
  color: "var(--muted)",
  fontSize: "0.84rem",
};
  const lowConfidenceReasons = new Set<ReviewQueueReason>([
    "LOW_CLIENT_CONFIDENCE",
    "LOW_CLASSIFICATION_CONFIDENCE",
    "LOW_EXTRACTION_CONFIDENCE",
  ]);
