import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
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

  useEffect(() => {
    const firstItemId = itemsQuery.data?.[0]?.id ?? "";
    if (!selectedItemId || !itemsQuery.data?.some((item) => item.id === selectedItemId)) {
      setSelectedItemId(firstItemId);
    }
  }, [itemsQuery.data, selectedItemId]);

  const refreshQueue = async () => {
    await queryClient.invalidateQueries({ queryKey: ["review-queue"] });
  };

  const linkMutation = useMutation({
    mutationFn: () => linkReviewItemClient(selectedItemId, selectedClientId),
    onSuccess: refreshQueue,
  });
  const metadataMutation = useMutation({
    mutationFn: () => correctReviewItemMetadata(selectedItemId, title),
    onSuccess: async () => {
      setTitle("");
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

  const selectedItem = detailQuery.data;

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
                <option value="LOW_CONFIDENCE">Low confidence</option>
                <option value="MISSING_METADATA">Missing metadata</option>
                <option value="UNSUPPORTED">Unsupported</option>
              </select>
            </label>
            <div style={{ display: "grid", gap: "0.5rem" }}>
              {itemsQuery.data?.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  style={item.id === selectedItemId ? selectedItemButtonStyle : itemButtonStyle}
                  onClick={() => setSelectedItemId(item.id)}
                >
                  {item.itemType} · {item.reason} · {item.status}
                </button>
              ))}
              {itemsQuery.data?.length === 0 ? <span>No matching review items.</span> : null}
            </div>
          </div>
        </section>
        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>Open review item</h3>
          {selectedItem ? (
            <div style={{ display: "grid", gap: "0.35rem" }}>
              <strong>{selectedItem.itemType}</strong>
              <span>Queue ID: {selectedItem.id}</span>
              <span>Item ID: {selectedItem.itemId}</span>
              <span>{selectedItem.reason} · {selectedItem.status}</span>
            </div>
          ) : (
            <p>Reviewers will inspect extracted details and intake evidence here.</p>
          )}
        </section>
        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>Resolve actions</h3>
          <div style={{ display: "grid", gap: "0.75rem" }}>
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

const dangerButtonStyle: React.CSSProperties = {
  ...buttonStyle,
  background: "#9c2f1f",
};

const itemButtonStyle: React.CSSProperties = {
  textAlign: "left",
  padding: "0.75rem",
  borderRadius: "0.85rem",
  border: "1px solid rgba(31, 28, 24, 0.12)",
  background: "#f7efe0",
  cursor: "pointer",
};

const selectedItemButtonStyle: React.CSSProperties = {
  ...itemButtonStyle,
  background: "#1f1c18",
  color: "#fffaf0",
};
