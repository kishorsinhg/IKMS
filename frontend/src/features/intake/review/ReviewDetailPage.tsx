import ApprovalOutlinedIcon from "@mui/icons-material/ApprovalOutlined";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import DescriptionOutlinedIcon from "@mui/icons-material/DescriptionOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import LinkOutlinedIcon from "@mui/icons-material/LinkOutlined";
import OpenInNewOutlinedIcon from "@mui/icons-material/OpenInNewOutlined";
import TaskAltOutlinedIcon from "@mui/icons-material/TaskAltOutlined";
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  TextField,
  Typography,
  useMediaQuery,
} from "@mui/material";
import { useTheme } from "@mui/material/styles";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { KeyboardEvent as ReactKeyboardEvent, useEffect, useMemo, useState } from "react";
import { useNavigate, useOutletContext, useParams, useSearchParams } from "react-router-dom";
import { listDocumentTypes, listMetadataFields } from "../../../api/admin";
import { getClient, listClients } from "../../../api/clients";
import { DemoClientWorkspace, getDemoClientWorkspace, isDemoDataEnabled } from "../../../api/demo";
import {
  getClientKnowledgeTimeline,
  getClientRelatedKnowledge,
  getDocumentVersions,
  getSourceRelatedKnowledge,
  knowledgeQueryKeys,
  type CustomerKnowledgeTimelineEvent,
  type DocumentVersionSummary,
  type RelatedKnowledgeLink,
} from "../../../api/knowledge";
import {
  approveReviewItem,
  ClientDocumentSummary,
  ClientEmailSummary,
  correctReviewItemMetadata,
  getReviewQueueItem,
  linkReviewItemClient,
  listClientDocuments,
  listClientEmails,
  rejectReviewItem,
  retryReviewItem,
  ReviewQueueItem,
  ReviewQueueReason,
  ReviewQueueStatus,
} from "../../../api/intake";
import { useCurrentUser } from "../../../app/auth/useCurrentUser";
import { AssistantPanel } from "../../../app/components/assistant-panel/AssistantPanel";
import type {
  AssistantEvidenceReference,
  AssistantSourceReference,
  SuggestedQuestion,
} from "../../../app/components/assistant-panel/assistantTypes";
import { AISummary } from "../../../app/components/document-viewer/AISummary";
import { EvidenceCard } from "../../../app/components/document-viewer/EvidenceCard";
import {
  EnterpriseDocumentViewer,
  type EnterpriseDocumentViewerProps,
} from "../../../app/components/document-viewer/EnterpriseDocumentViewer";
import { MetadataGroup as EvidenceMetadataGroup } from "../../../app/components/document-viewer/MetadataGroup";
import { documentViewerPlaceholderLayers } from "../../../app/components/document-viewer/documentViewerLayers";
import {
  type AiSummaryState,
  type EvidenceWorkspaceSection,
  normalizeConfidence,
} from "../../../app/components/document-viewer/evidenceWorkspaceTypes";
import type { ContextSection } from "../../../app/components/RightContextPanel";
import { StatusBadge, StatusTone } from "../../../app/components/StatusBadge";
import { WorkspaceToolbar } from "../../../app/components/WorkspaceToolbar";
import {
  buildKnowledgeAssistantEvidenceReferences,
  buildKnowledgeAssistantSourceReferences,
  collectKnowledgeBusinessReferenceFields,
  humanizeKnowledgeRelationshipType,
  summarizeKnowledgeVersions,
} from "../../../app/knowledge/knowledgeContext";
import { useNotification } from "../../../app/providers/useNotification";
import type { IkmsShellOutletContext, ShellWorkspaceChrome } from "../../../app/shell/IkmsAppShell";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  RetryAction,
} from "../../../app/WorkspaceStates";

interface MetadataDraft {
  title: string;
  documentTypeId: string;
  metadataValues: Record<string, string>;
}

interface MetadataFieldDefinition {
  key: string;
  label: string;
  value: string;
}

interface MetadataGroup {
  key: string;
  title: string;
  fields: MetadataFieldDefinition[];
}

const reviewQueueKey = ["review-queue"] as const;

export function ReviewDetailPage() {
  const { reviewId } = useParams();
  const navigate = useNavigate();
  const theme = useTheme();
  const queryClient = useQueryClient();
  const { notify } = useNotification();
  const { setWorkspaceChrome, clearWorkspaceChrome } = useOutletContext<IkmsShellOutletContext>();
  const currentUserQuery = useCurrentUser();
  const [searchParams, setSearchParams] = useSearchParams();
  const isDesktopContext = useMediaQuery(theme.breakpoints.up("lg"));
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));
  const [editMode, setEditMode] = useState(false);
  const [linkDialogOpen, setLinkDialogOpen] = useState(false);
  const [approveDialogOpen, setApproveDialogOpen] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [contextDrawerOpen, setContextDrawerOpen] = useState(false);
  const [assistantDraft, setAssistantDraft] = useState("");
  const [selectedClientId, setSelectedClientId] = useState("");
  const [rejectReason, setRejectReason] = useState("");
  const [metadataDraft, setMetadataDraft] = useState<MetadataDraft>({
    title: "",
    documentTypeId: "",
    metadataValues: {},
  });

  const metadataQuery = searchParams.get("mq") ?? "";
  const backToQueueHref = useMemo(() => {
    const next = new URLSearchParams(searchParams);
    next.delete("mq");
    const query = next.toString();
    return `/review-queue${query ? `?${query}` : ""}`;
  }, [searchParams]);

  const detailQuery = useQuery({
    queryKey: reviewId ? [...reviewQueueKey, "detail", reviewId] : [...reviewQueueKey, "detail", "empty"],
    queryFn: () => getReviewQueueItem(reviewId!),
    enabled: Boolean(reviewId),
  });
  const documentTypesQuery = useQuery({
    queryKey: ["admin", "document-types", "review-detail"],
    queryFn: listDocumentTypes,
  });
  const metadataFieldsQuery = useQuery({
    queryKey: ["admin", "metadata-fields", "review-detail"],
    queryFn: listMetadataFields,
  });
  const clientsQuery = useQuery({
    queryKey: ["clients", "review-detail", "lookup"],
    queryFn: () => listClients(""),
  });

  const reviewItem = detailQuery.data;
  const clientId = reviewItem?.clientId ?? null;

  const clientQuery = useQuery({
    queryKey: clientId ? ["clients", "profile", clientId] : ["clients", "profile", "empty"],
    queryFn: () => getClient(clientId!),
    enabled: Boolean(clientId),
  });
  const documentsQuery = useQuery({
    queryKey: clientId ? ["clients", clientId, "documents", "review-detail"] : ["clients", "documents", "empty"],
    queryFn: () => listClientDocuments(clientId!),
    enabled: Boolean(clientId),
  });
  const emailsQuery = useQuery({
    queryKey: clientId ? ["clients", clientId, "emails", "review-detail"] : ["clients", "emails", "empty"],
    queryFn: () => listClientEmails(clientId!),
    enabled: Boolean(clientId),
  });
  const workspaceQuery = useQuery({
    queryKey: clientId ? ["clients", clientId, "workspace", "review-detail"] : ["clients", "workspace", "empty"],
    queryFn: () => getDemoClientWorkspace(clientId!),
    enabled: Boolean(clientId) && isDemoDataEnabled,
  });

  useEffect(() => {
    if (!reviewItem) {
      return;
    }

    setSelectedClientId(reviewItem.clientId ?? "");
    setMetadataDraft({
      title: reviewItem.title ?? "",
      documentTypeId: reviewItem.documentTypeId ?? "",
      metadataValues: reviewItem.metadataValues ?? {},
    });
    setRejectReason("");
    setEditMode(false);
  }, [reviewItem]);

  const currentUser = currentUserQuery.data;
  const linkedDocument = useMemo(
    () => matchReviewDocument(reviewItem, documentsQuery.data ?? []),
    [documentsQuery.data, reviewItem],
  );
  const linkedEmail = useMemo(
    () => matchReviewEmail(reviewItem, emailsQuery.data ?? []),
    [emailsQuery.data, reviewItem],
  );
  const knowledgeSourceType = linkedDocument ? "DOCUMENT" : linkedEmail ? "EMAIL" : null;
  const knowledgeSourceId = linkedDocument?.id ?? linkedEmail?.id ?? null;
  const timelineQuery = useQuery({
    queryKey: clientId ? knowledgeQueryKeys.clientTimeline(clientId, { limit: 5 }) : ["review-detail", "timeline", "empty"],
    queryFn: ({ signal }) => getClientKnowledgeTimeline(clientId!, { limit: 5 }, signal),
    enabled: Boolean(clientId),
  });
  const relatedKnowledgeQuery = useQuery({
    queryKey: clientId ? knowledgeQueryKeys.clientRelated(clientId, 8) : ["review-detail", "related", "empty"],
    queryFn: ({ signal }) => getClientRelatedKnowledge(clientId!, 8, signal),
    enabled: Boolean(clientId),
  });
  const sourceRelatedKnowledgeQuery = useQuery({
    queryKey: knowledgeSourceType && knowledgeSourceId
      ? knowledgeQueryKeys.sourceRelated(knowledgeSourceType, knowledgeSourceId, 8)
      : ["review-detail", "source-related", "empty"],
    queryFn: ({ signal }) => getSourceRelatedKnowledge(knowledgeSourceType!, knowledgeSourceId!, 8, signal),
    enabled: Boolean(knowledgeSourceType && knowledgeSourceId),
  });
  const versionHistoryQuery = useQuery({
    queryKey: linkedDocument?.id ? knowledgeQueryKeys.documentVersions(linkedDocument.id) : ["review-detail", "versions", "empty"],
    queryFn: ({ signal }) => getDocumentVersions(linkedDocument!.id, signal),
    enabled: Boolean(linkedDocument?.id),
  });
  const relatedKnowledgeLinks = useMemo(
    () =>
      sourceRelatedKnowledgeQuery.data?.links?.length
        ? sourceRelatedKnowledgeQuery.data.links
        : relatedKnowledgeQuery.data?.links ?? [],
    [relatedKnowledgeQuery.data?.links, sourceRelatedKnowledgeQuery.data?.links],
  );

  const canViewOriginal =
    currentUser?.permissions.includes("VIEW_ORIGINAL_DOCUMENTS") &&
    currentUser.permissions.includes("VIEW_PII");
  const canViewRedacted = currentUser?.permissions.includes("VIEW_REDACTED_DOCUMENTS");
  const previewHref = linkedDocument ? `/api/documents/${linkedDocument.id}/preview` : null;
  const canOpenDocument = Boolean(linkedDocument && (canViewOriginal || canViewRedacted));
  const canOpenCustomer = Boolean(clientQuery.data && currentUser?.permissions.includes("CLIENT_VIEW"));
  const relatedWorkspace = workspaceQuery.data;
  const previewAvailable = Boolean((!isDemoDataEnabled ? previewHref : null) && canOpenDocument);

  const metadataGroups = useMemo(
    () => buildMetadataGroups(metadataDraft, metadataFieldsQuery.data ?? [], metadataQuery),
    [metadataDraft, metadataFieldsQuery.data, metadataQuery],
  );
  const viewerEvidenceSections = useMemo(
    () =>
      buildReviewViewerEvidenceSections({
        item: reviewItem ?? null,
        client: clientQuery.data ?? null,
        document: linkedDocument,
        email: linkedEmail,
        workspace: relatedWorkspace,
        relatedLinks: relatedKnowledgeLinks,
        versions: versionHistoryQuery.data ?? [],
        metadataGroups,
        aiSummaryState: workspaceQuery.isLoading ? "loading" : workspaceQuery.isError ? "error" : relatedWorkspace?.aiSummaries[0] ? "ready" : "unavailable",
      }),
    [clientQuery.data, linkedDocument, linkedEmail, metadataGroups, relatedKnowledgeLinks, relatedWorkspace, reviewItem, versionHistoryQuery.data, workspaceQuery.isError, workspaceQuery.isLoading],
  );
  const activeFilters = metadataQuery.trim()
    ? [
        {
          key: "metadata-query",
          label: `Fields: ${metadataQuery.trim()}`,
          onDelete: () => setScopedSearchParams(setSearchParams, searchParams, { mq: null }),
        },
      ]
    : [];

  async function refreshReviewState() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: [...reviewQueueKey, "detail", reviewId] }),
      queryClient.invalidateQueries({ queryKey: reviewQueueKey }),
      clientId ? queryClient.invalidateQueries({ queryKey: ["clients", "profile", clientId] }) : Promise.resolve(),
      clientId ? queryClient.invalidateQueries({ queryKey: ["clients", clientId, "documents"] }) : Promise.resolve(),
      clientId ? queryClient.invalidateQueries({ queryKey: ["clients", clientId, "emails"] }) : Promise.resolve(),
    ]);
  }

  const metadataMutation = useMutation({
    mutationFn: () =>
      correctReviewItemMetadata(reviewItem!.id, {
        title: metadataDraft.title.trim(),
        documentTypeId: metadataDraft.documentTypeId || undefined,
        metadataValues: trimMetadataValues(metadataDraft.metadataValues),
        reviewerComment: "Reviewer metadata correction from Review Detail",
      }),
    onSuccess: async () => {
      notify({ severity: "success", message: "Review changes saved." });
      setEditMode(false);
      await refreshReviewState();
    },
    onError: () => notify({ severity: "error", message: "Unable to save review changes." }),
  });

  const approveMutation = useMutation({
    mutationFn: () => approveReviewItem(reviewItem!.id),
    onSuccess: async () => {
      notify({ severity: "success", message: "Review item approved." });
      await refreshReviewState();
    },
    onError: () => notify({ severity: "error", message: "Unable to approve the review item." }),
  });

  const rejectMutation = useMutation({
    mutationFn: () => rejectReviewItem(reviewItem!.id, rejectReason.trim()),
    onSuccess: async () => {
      notify({ severity: "warning", message: "Review item rejected." });
      setRejectDialogOpen(false);
      setRejectReason("");
      await refreshReviewState();
    },
    onError: () => notify({ severity: "error", message: "Unable to reject the review item." }),
  });

  const linkMutation = useMutation({
    mutationFn: () => linkReviewItemClient(reviewItem!.id, selectedClientId),
    onSuccess: async () => {
      notify({ severity: "success", message: "Review item linked to customer." });
      setLinkDialogOpen(false);
      await refreshReviewState();
    },
    onError: () => notify({ severity: "error", message: "Unable to link the review item." }),
  });

  const retryMutation = useMutation({
    mutationFn: () => retryReviewItem(reviewItem!.id, "Retry requested from Review Detail"),
    onSuccess: async () => {
      notify({ severity: "info", message: "Document processing retry requested." });
      await refreshReviewState();
    },
    onError: () => notify({ severity: "error", message: "Unable to retry document processing." }),
  });

  useEffect(() => {
    function handleKeyboardShortcuts(event: KeyboardEvent) {
      if (!reviewItem) {
        return;
      }

      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "s") {
        event.preventDefault();
        setEditMode(true);
        if (hasMetadataChanges(reviewItem, metadataDraft)) {
          metadataMutation.mutate();
        }
      }

      if ((event.metaKey || event.ctrlKey) && event.key === "Enter") {
        event.preventDefault();
        setApproveDialogOpen(true);
      }

      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "r") {
        event.preventDefault();
        setRejectDialogOpen(true);
      }
    }

    window.addEventListener("keydown", handleKeyboardShortcuts);
    return () => window.removeEventListener("keydown", handleKeyboardShortcuts);
  }, [approveMutation, metadataDraft, metadataMutation, reviewItem]);

  const contextSections = useMemo(
    () =>
      reviewItem
        ? [
            ...buildContextSections({
              item: reviewItem,
              client: clientQuery.data ?? null,
              document: linkedDocument,
              email: linkedEmail,
              workspace: relatedWorkspace,
              relatedLinks: relatedKnowledgeLinks,
              timelineEvents: timelineQuery.data?.events ?? [],
              versions: versionHistoryQuery.data ?? [],
              processingJob: reviewItem.processingJob ?? null,
              onReject: () => setRejectDialogOpen(true),
              onLinkCustomer: () => setLinkDialogOpen(true),
              onEditMetadata: () => setEditMode(true),
              onRetryProcessing: reviewItem.processingJob ? () => retryMutation.mutate() : undefined,
              onOpenCustomer: canOpenCustomer ? () => navigate(`/clients/${clientQuery.data!.id}`) : undefined,
              onOpenOriginal: canOpenDocument ? () => window.open(previewHref!, "_blank", "noopener,noreferrer") : undefined,
            }),
            {
              key: "assistant",
              title: "Assistant Workspace",
              content: (
                <AssistantPanel
                  title="Enterprise AI Assistant"
                  subtitle="Review detail context only. Conversation placeholders are shown where no assistant API is currently available."
                  conversationState="empty"
                  messages={[]}
                  suggestedQuestions={buildReviewAssistantQuestions(reviewItem, setAssistantDraft, relatedKnowledgeLinks, versionHistoryQuery.data ?? [])}
                  evidenceReferences={buildReviewAssistantEvidenceReferences(reviewItem, linkedDocument, linkedEmail, relatedKnowledgeLinks, timelineQuery.data?.events ?? [])}
                  sourceReferences={buildReviewAssistantSourceReferences(reviewItem, clientQuery.data ?? null, linkedDocument, linkedEmail, relatedKnowledgeLinks)}
                  emptyTitle="Conversation unavailable"
                  emptyMessage="Review Detail does not expose an assistant conversation endpoint in the current frontend contract."
                  composerContent={(
                    <Stack spacing={1}>
                      <TextField
                        size="small"
                        multiline
                        minRows={2}
                        value={assistantDraft}
                        onChange={(event) => setAssistantDraft(event.target.value)}
                        placeholder="Capture a review-specific assistant prompt"
                        aria-label="Review assistant prompt"
                      />
                      <Button variant="contained" disabled>
                        AI unavailable in Review Detail
                      </Button>
                    </Stack>
                  )}
                />
              ),
            },
          ]
        : [],
    [assistantDraft, canOpenCustomer, canOpenDocument, clientQuery.data, linkedDocument, linkedEmail, navigate, previewHref, relatedKnowledgeLinks, relatedWorkspace, retryMutation, reviewItem, timelineQuery.data?.events, versionHistoryQuery.data],
  );

  const chrome = useMemo<ShellWorkspaceChrome | null>(() => {
    if (!reviewItem) {
      return null;
    }

    return {
      title: reviewItem.title ?? "Review Detail",
      subtitle: `${formatReviewType(reviewItem.itemType)} · ${reviewItem.itemId}`,
      breadcrumbs: [
        { label: "IKMS" },
        { label: "Review", href: backToQueueHref },
        { label: "Review Detail" },
      ],
      secondaryActions: (
        <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
          {reviewItem.processingJob ? (
            <Button
              variant="text"
              color="inherit"
              onClick={() => retryMutation.mutate()}
              disabled={retryMutation.isPending}
            >
              {retryMutation.isPending ? "Retrying..." : "Retry processing"}
            </Button>
          ) : null}
          <Button
            variant="text"
            color="inherit"
            startIcon={<ArrowBackOutlinedIcon fontSize="small" />}
            onClick={() => navigate(backToQueueHref)}
          >
            Back to queue
          </Button>
        </Stack>
      ),
      primaryActions: (
        <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
          <Button
            variant="contained"
            startIcon={<TaskAltOutlinedIcon fontSize="small" />}
            onClick={() => setApproveDialogOpen(true)}
            disabled={approveMutation.isPending}
          >
            {approveMutation.isPending ? "Approving..." : "Approve"}
          </Button>
        </Stack>
      ),
      contextTitle: "Evidence Assistant",
      contextSections,
      contextWidth: 324,
    };
  }, [approveMutation.isPending, backToQueueHref, contextSections, navigate, retryMutation, reviewItem]);

  useEffect(() => {
    if (!chrome) {
      return undefined;
    }

    setWorkspaceChrome(chrome);
    return () => clearWorkspaceChrome();
  }, [chrome, clearWorkspaceChrome, setWorkspaceChrome]);

  if (!reviewId) {
    return (
      <EmptyState
        title="Review item not selected"
        message="Open a queue item from Review to continue processing."
        action={
          <Button variant="contained" onClick={() => navigate("/review-queue")}>
            Open review queue
          </Button>
        }
      />
    );
  }

  if (
    detailQuery.isLoading ||
    currentUserQuery.isLoading ||
    (clientId && (clientQuery.isLoading || documentsQuery.isLoading || emailsQuery.isLoading))
  ) {
    return (
      <LoadingState
        title="Loading review detail"
        message="Preparing the document, extracted metadata, and workflow context."
      />
    );
  }

  if (detailQuery.isError || currentUserQuery.isError || !reviewItem || !currentUser) {
    return (
      <ErrorState
        title="Unable to load review detail"
        message="The selected review item could not be opened."
        action={<RetryAction onClick={() => void detailQuery.refetch()} />}
      />
    );
  }

  const detailAlerts = buildDetailAlerts(reviewItem, clientQuery.data ?? null, linkedDocument, linkedEmail);
  const hasUnsavedChanges = hasMetadataChanges(reviewItem, metadataDraft);
  const toolbarSecondaryActions = [
    ...(!isDesktopContext
      ? [{
          key: "context",
          label: "Open context",
          onClick: () => setContextDrawerOpen(true),
        }]
      : []),
  ];

  return (
    <Stack spacing={1.25}>
      <WorkspaceToolbar
        searchPlaceholder="Filter extracted fields, references, or labels"
        searchValue={metadataQuery}
        onSearchChange={(value) => setScopedSearchParams(setSearchParams, searchParams, { mq: value || null })}
        onSearchKeyDown={(event: ReactKeyboardEvent<HTMLInputElement>) => {
          if (event.key === "Escape" && metadataQuery) {
            event.preventDefault();
            setScopedSearchParams(setSearchParams, searchParams, { mq: null });
          }
        }}
        searchAriaLabel="Filter extracted metadata"
        filters={(
          <Stack direction="row" spacing={0.75} alignItems="center" flexWrap="wrap" useFlexGap sx={{ minWidth: 0 }}>
            <StatusBadge label={formatStatus(reviewItem.status)} tone={mapStatusTone(reviewItem.status)} />
            <StatusBadge label={formatReason(reviewItem.reason)} tone={mapReasonTone(reviewItem.reason)} variant="outlined" />
            <StatusBadge label={formatReviewType(reviewItem.itemType)} tone="neutral" variant="outlined" />
            {editMode ? <StatusBadge label="Edit mode" tone="info" variant="outlined" /> : null}
          </Stack>
        )}
        activeFilters={activeFilters}
        onRefresh={() => void detailQuery.refetch()}
        secondaryActions={toolbarSecondaryActions.length > 0 ? toolbarSecondaryActions : undefined}
      />

      {detailAlerts.map((item) => (
        <Alert
          key={item.key}
          severity={item.severity}
          variant="outlined"
          action={item.action}
          sx={{ py: 0.25, alignItems: "center" }}
        >
          {item.message}
        </Alert>
      ))}

      <Box
        sx={{
          display: "grid",
          gap: 1.25,
          gridTemplateColumns: {
            xs: "1fr",
            lg: previewAvailable ? "1fr" : "minmax(0, 1.2fr)",
            xl: previewAvailable ? "minmax(0, 0.92fr) minmax(0, 1.08fr)" : "minmax(280px, 0.56fr) minmax(0, 1.44fr)",
          },
          alignItems: "start",
        }}
      >
        <DocumentViewerSurface
          item={reviewItem}
          document={linkedDocument}
          email={linkedEmail}
          previewHref={!isDemoDataEnabled ? previewHref : null}
          canOpenDocument={canOpenDocument}
          canViewOriginal={Boolean(canViewOriginal)}
          previewAvailable={previewAvailable}
          prioritizeMetadata={!previewAvailable}
          isMobile={isMobile}
          evidenceSections={viewerEvidenceSections}
        />

        <MetadataEditorPanel
          item={reviewItem}
          metadataDraft={metadataDraft}
          metadataGroups={metadataGroups}
          processingJob={reviewItem.processingJob ?? null}
          documentTypes={documentTypesQuery.data ?? []}
          editMode={editMode}
          onToggleEdit={() => setEditMode((current) => !current)}
          onDraftChange={setMetadataDraft}
          onSave={() => metadataMutation.mutate()}
          saving={metadataMutation.isPending}
          hasUnsavedChanges={hasUnsavedChanges}
          prioritizeMetadata={!previewAvailable}
          isMobile={isMobile}
        />
      </Box>

      {!isDesktopContext && reviewItem ? (
        <AssistantPanel
          title="Enterprise AI Assistant"
          subtitle="Review detail context only. Conversation placeholders are shown where no assistant API is currently available."
          variant={isMobile ? "sheet" : "drawer"}
          open={contextDrawerOpen}
          onClose={() => setContextDrawerOpen(false)}
          conversationState="empty"
          messages={[]}
          suggestedQuestions={buildReviewAssistantQuestions(reviewItem, setAssistantDraft, relatedKnowledgeLinks, versionHistoryQuery.data ?? [])}
          evidenceReferences={buildReviewAssistantEvidenceReferences(reviewItem, linkedDocument, linkedEmail, relatedKnowledgeLinks, timelineQuery.data?.events ?? [])}
          sourceReferences={buildReviewAssistantSourceReferences(reviewItem, clientQuery.data ?? null, linkedDocument, linkedEmail, relatedKnowledgeLinks)}
          emptyTitle="Conversation unavailable"
          emptyMessage="Review Detail does not expose an assistant conversation endpoint in the current frontend contract."
          composerContent={(
            <Stack spacing={1}>
              <TextField
                size="small"
                multiline
                minRows={2}
                value={assistantDraft}
                onChange={(event) => setAssistantDraft(event.target.value)}
                placeholder="Capture a review-specific assistant prompt"
                aria-label="Review assistant prompt"
              />
              <Button variant="contained" disabled>
                AI unavailable in Review Detail
              </Button>
            </Stack>
          )}
        />
      ) : null}

      <Dialog open={linkDialogOpen} onClose={() => setLinkDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Link review item to customer</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={1.5} sx={{ pt: 0.5 }}>
            <Typography variant="body2" color="text.secondary">
              Select the customer that should own this review item.
            </Typography>
            <FormControl size="small" fullWidth>
              <InputLabel id="review-detail-customer-label">Customer</InputLabel>
              <Select
                labelId="review-detail-customer-label"
                label="Customer"
                value={selectedClientId}
                onChange={(event) => setSelectedClientId(String(event.target.value))}
              >
                {(clientsQuery.data ?? []).map((client) => (
                  <MenuItem key={client.id} value={client.id}>
                    {client.displayName}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setLinkDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={() => linkMutation.mutate()}
            disabled={!selectedClientId || linkMutation.isPending}
          >
            {linkMutation.isPending ? "Linking..." : "Link customer"}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={approveDialogOpen} onClose={() => setApproveDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Approve review item</DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" color="text.secondary">
            Approve this item after confirming the linked customer, extracted metadata, and available source context.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setApproveDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={() => approveMutation.mutate()}
            disabled={approveMutation.isPending}
          >
            {approveMutation.isPending ? "Approving..." : "Approve"}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={rejectDialogOpen} onClose={() => setRejectDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Reject review item</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={1.25} sx={{ pt: 0.5 }}>
            <Alert severity="warning" variant="outlined">
              Rejection records a human decision and requires a clear reason for follow-up.
            </Alert>
            <TextField
              label="Rejection reason"
              value={rejectReason}
              onChange={(event) => setRejectReason(event.target.value)}
              autoFocus
              multiline
              minRows={3}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectDialogOpen(false)}>Cancel</Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => rejectMutation.mutate()}
            disabled={!rejectReason.trim() || rejectMutation.isPending}
          >
            {rejectMutation.isPending ? "Rejecting..." : "Reject"}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

function DocumentViewerSurface({
  item,
  document,
  email,
  previewHref,
  canOpenDocument,
  canViewOriginal,
  previewAvailable,
  prioritizeMetadata,
  isMobile,
  evidenceSections,
}: {
  item: ReviewQueueItem;
  document: ClientDocumentSummary | null;
  email: ClientEmailSummary | null;
  previewHref: string | null;
  canOpenDocument: boolean;
  canViewOriginal: boolean;
  previewAvailable: boolean;
  prioritizeMetadata: boolean;
  isMobile: boolean;
  evidenceSections: EnterpriseDocumentViewerProps["evidenceSections"];
}) {
  const viewerState = previewAvailable ? "ready" : canOpenDocument ? "unsupported" : item.itemType === "EMAIL" ? "unsupported" : "unsupported";
  const viewerDocument = {
    id: document?.id ?? email?.id ?? item.id,
    title: document?.title ?? email?.subject ?? item.title ?? item.itemId,
    subtitle: document
      ? `${document.source} · ${document.processingStatus}`
      : email
        ? `${email.sender} · ${email.processingStatus}`
        : `${formatReviewType(item.itemType)} · ${item.itemId}`,
    fileKind: item.itemType === "EMAIL" ? "email" as const : "pdf" as const,
    previewUrl: previewAvailable ? previewHref : null,
    downloadUrl: document ? `/api/documents/${document.id}/download` : null,
    originalUrl: canOpenDocument ? previewHref : null,
    originalActionLabel: canViewOriginal ? "Open original" : "Open document",
    pages: [{ id: `${item.id}-page-1`, label: "Page 1", pageNumber: 1 }],
    unsupportedReason:
      item.itemType === "EMAIL"
        ? "Inline email preview is not available in the current API."
        : "Inline preview is unavailable for this review item. Continue with metadata validation and linked business context.",
  };

  return (
    <Paper
      variant="outlined"
      sx={{
        order: prioritizeMetadata && isMobile ? 2 : 1,
        p: 1,
      }}
    >
      <EnterpriseDocumentViewer
        document={viewerDocument}
        state={viewerState}
        evidenceSections={evidenceSections}
        layers={documentViewerPlaceholderLayers()}
        embedded
        overflowActions={[
        ]}
      />
    </Paper>
  );
}

function MetadataEditorPanel({
  item,
  metadataDraft,
  metadataGroups,
  processingJob,
  documentTypes,
  editMode,
  onToggleEdit,
  onDraftChange,
  onSave,
  saving,
  hasUnsavedChanges,
  prioritizeMetadata,
  isMobile,
}: {
  item: ReviewQueueItem;
  metadataDraft: MetadataDraft;
  metadataGroups: MetadataGroup[];
  processingJob: ReviewQueueItem["processingJob"];
  documentTypes: Array<{ id: string; name: string }>;
  editMode: boolean;
  onToggleEdit: () => void;
  onDraftChange: React.Dispatch<React.SetStateAction<MetadataDraft>>;
  onSave: () => void;
  saving: boolean;
  hasUnsavedChanges: boolean;
  prioritizeMetadata: boolean;
  isMobile: boolean;
}) {
  return (
    <Paper variant="outlined" sx={{ p: 1.25, order: prioritizeMetadata ? 1 : isMobile ? 2 : 1 }}>
      <Stack spacing={1.25}>
        <Stack direction="row" alignItems="flex-start" justifyContent="space-between" spacing={1}>
          <Stack spacing={0.35}>
            <Typography variant="subtitle2">Extracted Metadata</Typography>
            <Typography variant="body2" color="text.secondary">
              Validate fields before approval. Confidence scores are not exposed by the current API, so every value requires manual review.
            </Typography>
          </Stack>
          <Stack direction="row" spacing={0.75} alignItems="center">
            {hasUnsavedChanges ? <StatusBadge label="Unsaved changes" tone="warning" variant="outlined" /> : null}
            <Button size="small" variant="outlined" startIcon={<EditOutlinedIcon fontSize="small" />} onClick={onToggleEdit}>
              {editMode ? "Stop editing" : "Edit metadata"}
            </Button>
          </Stack>
        </Stack>

        <Alert severity="info" variant="outlined" sx={{ py: 0.25 }}>
          Workflow status is {formatStatus(item.status).toLowerCase()}. Review reason is {formatReason(item.reason).toLowerCase()}.
        </Alert>

        {processingJob ? (
          <Alert severity={processingJob.findings.length > 0 ? "warning" : "info"} variant="outlined" sx={{ py: 0.25 }}>
            Processing stage {humanizeProcessingValue(processingJob.currentStage).toLowerCase()} with overall confidence{" "}
            {formatConfidenceValue(processingJob.overallConfidence).toLowerCase()}. {processingJob.findings.length} validation finding
            {processingJob.findings.length === 1 ? "" : "s"} returned by the processing pipeline.
          </Alert>
        ) : null}

        <Stack spacing={1.25}>
          <Stack spacing={1}>
            <Typography variant="subtitle2">Document Identity</Typography>
            <TextField
              size="small"
              label="Title"
              value={metadataDraft.title}
              onChange={(event) => onDraftChange((current) => ({ ...current, title: event.target.value }))}
              disabled={!editMode}
            />
            <FormControl size="small" fullWidth disabled={!editMode}>
              <InputLabel id="review-detail-document-type">Document Type</InputLabel>
              <Select
                labelId="review-detail-document-type"
                label="Document Type"
                value={metadataDraft.documentTypeId}
                onChange={(event) => onDraftChange((current) => ({ ...current, documentTypeId: String(event.target.value) }))}
              >
                <MenuItem value="">None</MenuItem>
                {documentTypes.map((documentType) => (
                  <MenuItem key={documentType.id} value={documentType.id}>
                    {documentType.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>

          {metadataGroups.map((group) => (
            <Box key={group.key}>
              <Divider sx={{ mb: 1.1 }} />
              <Stack spacing={1}>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                  <Typography variant="subtitle2">{group.title}</Typography>
                  <StatusBadge
                    label={`${group.fields.filter((field) => field.value.trim()).length}/${group.fields.length} captured`}
                    tone="neutral"
                    variant="outlined"
                  />
                </Stack>
                <Box
                  sx={{
                    display: "grid",
                    gap: 1,
                    gridTemplateColumns: { xs: "1fr", md: "repeat(2, minmax(0, 1fr))" },
                  }}
                >
                  {group.fields.map((field) => (
                    <TextField
                      key={field.key}
                      size="small"
                      label={field.label}
                      value={metadataDraft.metadataValues[field.key] ?? ""}
                      onChange={(event) =>
                        onDraftChange((current) => ({
                          ...current,
                          metadataValues: {
                            ...current.metadataValues,
                            [field.key]: event.target.value,
                          },
                        }))
                      }
                      disabled={!editMode}
                      helperText={field.value.trim() ? "Manual review required" : "No extracted value"}
                    />
                  ))}
                </Box>
              </Stack>
            </Box>
          ))}

          {metadataGroups.length === 0 ? (
            <EmptyState
              title="No extracted metadata"
              message="The current review item does not contain configurable extracted fields."
              compact
            />
          ) : null}

          {processingJob?.fields?.length ? (
            <Box>
              <Divider sx={{ mb: 1.1 }} />
              <Stack spacing={1}>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                  <Typography variant="subtitle2">Processing Evidence</Typography>
                  <StatusBadge
                    label={`${processingJob.fields.filter((field) => field.confidence != null).length}/${processingJob.fields.length} scored`}
                    tone="neutral"
                    variant="outlined"
                  />
                </Stack>
                <Box
                  sx={{
                    display: "grid",
                    gap: 1,
                    gridTemplateColumns: { xs: "1fr", md: "repeat(2, minmax(0, 1fr))" },
                  }}
                >
                  {processingJob.fields.slice(0, 8).map((field) => (
                    <TextField
                      key={field.fieldKey}
                      size="small"
                      label={field.fieldLabel}
                      value={field.correctedValue ?? field.extractedValue ?? ""}
                      disabled
                      helperText={`${humanizeProcessingValue(field.validationState)} · Confidence ${formatConfidenceValue(field.confidence)}`}
                    />
                  ))}
                </Box>
              </Stack>
            </Box>
          ) : null}

          {processingJob?.findings?.length ? (
            <Box>
              <Divider sx={{ mb: 1.1 }} />
              <Stack spacing={1}>
                <Typography variant="subtitle2">Validation Findings</Typography>
                <Stack spacing={0.75}>
                  {processingJob.findings.slice(0, 5).map((finding) => (
                    <Alert
                      key={`${finding.findingCode}-${finding.createdAt}`}
                      severity={finding.severity === "ERROR" ? "error" : finding.severity === "WARNING" ? "warning" : "info"}
                      variant="outlined"
                    >
                      <Typography variant="body2">{finding.message}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {humanizeProcessingValue(finding.stage)}{finding.fieldKey ? ` · ${humanizeFieldKey(finding.fieldKey)}` : ""}
                        {finding.confidence != null ? ` · Confidence ${formatConfidenceValue(finding.confidence)}` : ""}
                      </Typography>
                    </Alert>
                  ))}
                </Stack>
              </Stack>
            </Box>
          ) : null}
        </Stack>

        <Stack direction="row" spacing={0.75} justifyContent="flex-end" flexWrap="wrap" useFlexGap>
          <Button variant="text" color="inherit" onClick={onToggleEdit}>
            {editMode ? "Cancel edit mode" : "Review fields"}
          </Button>
          <Button
            variant="outlined"
            startIcon={<DescriptionOutlinedIcon fontSize="small" />}
            onClick={onSave}
            disabled={!editMode || !metadataDraft.title.trim() || !hasUnsavedChanges || saving}
          >
            {saving ? "Saving..." : "Save changes"}
          </Button>
        </Stack>
      </Stack>
    </Paper>
  );
}

function buildMetadataGroups(
  draft: MetadataDraft,
  configuredFields: Array<{ fieldKey: string; label: string }>,
  query: string,
): MetadataGroup[] {
  const configuredMap = new Map(configuredFields.map((field) => [field.fieldKey, field.label]));
  const combinedKeys = new Set([...configuredMap.keys(), ...Object.keys(draft.metadataValues)]);
  const normalizedQuery = query.trim().toLowerCase();

  const entries = [...combinedKeys].map<MetadataFieldDefinition>((key) => ({
    key,
    label: configuredMap.get(key) ?? humanizeFieldKey(key),
    value: draft.metadataValues[key] ?? "",
  }));

  const filtered = normalizedQuery
    ? entries.filter((field) =>
        [field.label, field.key, field.value].some((value) => value.toLowerCase().includes(normalizedQuery)),
      )
    : entries;

  const groups: MetadataGroup[] = [
    {
      key: "business-context",
      title: "Business Context",
      fields: filtered.filter((field) => /(client|customer|insured|applicant|contact)/i.test(field.key)),
    },
    {
      key: "references",
      title: "References",
      fields: filtered.filter((field) => /(policy|claim|reference|account|carrier|broker)/i.test(field.key)),
    },
    {
      key: "document-identity",
      title: "Document Identity",
      fields: filtered.filter((field) => /(type|class|lineOfBusiness|lob|submission)/i.test(field.key)),
    },
  ];

  const assigned = new Set(groups.flatMap((group) => group.fields.map((field) => field.key)));
  const additional = filtered.filter((field) => !assigned.has(field.key));

  if (additional.length > 0) {
    groups.push({
      key: "additional",
      title: "Additional Fields",
      fields: additional,
    });
  }

  return groups.filter((group) => group.fields.length > 0);
}

function buildContextSections({
  item,
  client,
  document,
  email,
  workspace,
  relatedLinks,
  timelineEvents,
  versions,
  processingJob,
  onReject,
  onLinkCustomer,
  onEditMetadata,
  onRetryProcessing,
  onOpenCustomer,
  onOpenOriginal,
}: {
  item: ReviewQueueItem | null;
  client: Awaited<ReturnType<typeof getClient>> | null;
  document: ClientDocumentSummary | null;
  email: ClientEmailSummary | null;
  workspace: DemoClientWorkspace | undefined;
  relatedLinks: RelatedKnowledgeLink[];
  timelineEvents: CustomerKnowledgeTimelineEvent[];
  versions: DocumentVersionSummary[];
  processingJob: ReviewQueueItem["processingJob"];
  onReject: () => void;
  onLinkCustomer: () => void;
  onEditMetadata: () => void;
  onRetryProcessing?: () => void;
  onOpenCustomer?: () => void;
  onOpenOriginal?: () => void;
}): ContextSection[] {
  if (!item) {
    return [];
  }

  const aiSummary = workspace?.aiSummaries[0];
  const businessReferences = collectKnowledgeBusinessReferenceFields(relatedLinks);
  const evidenceLines = Object.entries(item.metadataValues)
    .filter(([, value]) => value.trim())
    .slice(0, 4)
    .map(([key, value]) => `${humanizeFieldKey(key)}: ${value}`);

  return [
    {
      key: "workflow",
      title: "Workflow Status",
      content: (
        <Stack spacing={0.75}>
          <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
            <StatusBadge label={formatStatus(item.status)} tone={mapStatusTone(item.status)} />
            <StatusBadge label={formatReason(item.reason)} tone={mapReasonTone(item.reason)} variant="outlined" />
          </Stack>
          <Typography variant="body2" color="text.secondary">
            Assigned to {item.assignedTo ?? "unassigned"}.
          </Typography>
        </Stack>
      ),
    },
    {
      key: "customer",
      title: "Customer and Business Context",
      content: (
        <Stack spacing={0.5}>
          <Typography variant="body2">{client?.displayName ?? "No customer linked"}</Typography>
          <Typography variant="body2" color="text.secondary">
            {client ? `${client.clientId} · ${client.clientType}` : "Link a customer before final approval if ownership is unresolved."}
          </Typography>
          {client?.primaryEmail ? (
            <Typography variant="body2" color="text.secondary">
              {client.primaryEmail}
            </Typography>
          ) : null}
        </Stack>
      ),
    },
    ...(businessReferences.length > 0
      ? [
          {
            key: "business-references",
            title: "Business Reference Fields",
            content: (
              <Stack spacing={0.5}>
                {businessReferences.slice(0, 3).map((reference) => (
                  <Typography key={`${reference.key}-${reference.value}`} variant="body2" color="text.secondary">
                    {reference.label}: {reference.value}
                  </Typography>
                ))}
              </Stack>
            ),
          } satisfies ContextSection,
        ]
      : []),
    ...(relatedLinks.length > 0 || versions.length > 0 || timelineEvents.length > 0
      ? [
          {
            key: "related-knowledge",
            title: "Related Knowledge",
            content: (
              <Stack spacing={0.5}>
                {relatedLinks.slice(0, 3).map((link) => (
                  <Typography key={link.relationshipId} variant="body2" color="text.secondary">
                    {link.inferred ? "Possible related source" : "Related source"}: {link.relatedTitle} · {humanizeKnowledgeRelationshipType(link.relationshipType)}
                  </Typography>
                ))}
                {versions.length > 0 ? (
                  <Typography variant="body2" color="text.secondary">
                    Earlier versions: {summarizeKnowledgeVersions(versions)}
                  </Typography>
                ) : null}
                {timelineEvents.slice(0, 2).map((event) => (
                  <Typography key={event.eventId} variant="body2" color="text.secondary">
                    Recent customer knowledge: {event.title}
                  </Typography>
                ))}
              </Stack>
            ),
          } satisfies ContextSection,
        ]
      : []),
    ...(processingJob
      ? [
          {
            key: "processing-status",
            title: "Processing Status",
            content: (
              <Stack spacing={0.5}>
                <Typography variant="body2" color="text.secondary">
                  {humanizeProcessingValue(processingJob.status)} · {humanizeProcessingValue(processingJob.currentStage)}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Overall confidence: {formatConfidenceValue(processingJob.overallConfidence)}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Findings: {processingJob.findings.length}
                </Typography>
                {processingJob.lastErrorMessage ? (
                  <Typography variant="body2" color="text.secondary">
                    Last error: {processingJob.lastErrorMessage}
                  </Typography>
                ) : null}
              </Stack>
            ),
          } satisfies ContextSection,
        ]
      : []),
    {
      key: "ai",
      title: "AI Explanation",
      content: (
        <Stack spacing={0.75}>
          <Typography variant="body2" color="text.secondary">
            {buildAiExplanation(item.reason)}
          </Typography>
          {aiSummary ? (
            <Typography variant="body2" color="text.secondary">
              {aiSummary.summary}
            </Typography>
          ) : (
            <Typography variant="body2" color="text.secondary">
              No additional AI summary is available for this review item.
            </Typography>
          )}
        </Stack>
      ),
    },
    {
      key: "evidence",
      title: "Evidence Summary",
      content: (
        <Stack spacing={0.5}>
          {evidenceLines.length > 0 ? (
            evidenceLines.map((line) => (
              <Typography key={line} variant="body2" color="text.secondary">
                {line}
              </Typography>
            ))
          ) : (
            <Typography variant="body2" color="text.secondary">
              No extracted evidence values are currently available.
            </Typography>
          )}
          {document ? (
            <Typography variant="body2" color="text.secondary">
              Source document: {document.title}
            </Typography>
          ) : email ? (
            <Typography variant="body2" color="text.secondary">
              Source email: {email.subject}
            </Typography>
          ) : null}
        </Stack>
      ),
    },
    {
      key: "actions",
      title: "Quick Actions",
      content: (
        <Stack spacing={0.75}>
          <Button size="small" variant="outlined" startIcon={<LinkOutlinedIcon fontSize="small" />} onClick={onLinkCustomer}>
            Link customer
          </Button>
          <Button size="small" variant="text" color="inherit" startIcon={<EditOutlinedIcon fontSize="small" />} onClick={onEditMetadata}>
            Edit metadata
          </Button>
          {onOpenCustomer ? (
            <Button size="small" variant="text" color="inherit" startIcon={<ApprovalOutlinedIcon fontSize="small" />} onClick={onOpenCustomer}>
              Open Customer360
            </Button>
          ) : null}
          {onOpenOriginal ? (
            <Button size="small" variant="text" color="inherit" startIcon={<OpenInNewOutlinedIcon fontSize="small" />} onClick={onOpenOriginal}>
              Open original document
            </Button>
          ) : null}
          {onRetryProcessing ? (
            <Button size="small" variant="text" color="inherit" onClick={onRetryProcessing}>
              Retry processing
            </Button>
          ) : null}
          <Button size="small" color="error" variant="text" onClick={onReject}>
            Reject
          </Button>
        </Stack>
      ),
    },
  ];
}

function buildReviewAssistantQuestions(
  item: ReviewQueueItem,
  setDraft: (value: string) => void,
  relatedLinks: RelatedKnowledgeLink[],
  versions: DocumentVersionSummary[],
): SuggestedQuestion[] {
  const labels = [
    "Why is review required?",
    relatedLinks.length > 0 ? "Show related knowledge" : "Show missing metadata",
    versions.length > 1 ? "Find earlier versions" : item.itemType === "DOCUMENT" ? "Explain this document" : "Summarize this intake item",
    "What should be verified before approval?",
  ];

  return labels.map((label) => ({
    key: `${item.id}-${label}`,
    label,
    onSelect: () => setDraft(label),
  }));
}

function buildReviewAssistantEvidenceReferences(
  item: ReviewQueueItem,
  document: ClientDocumentSummary | null,
  email: ClientEmailSummary | null,
  relatedLinks: RelatedKnowledgeLink[],
  timelineEvents: CustomerKnowledgeTimelineEvent[],
): AssistantEvidenceReference[] {
  const title = document?.title ?? email?.subject ?? item.title ?? item.itemId;
  return buildKnowledgeAssistantEvidenceReferences(relatedLinks, timelineEvents, {
    key: `${item.id}-source`,
    label: title,
    target: document ? "page" : "metadata",
    detail: item.reason,
    disabled: true,
  });
}

function buildReviewAssistantSourceReferences(
  item: ReviewQueueItem,
  client: Awaited<ReturnType<typeof getClient>> | null,
  document: ClientDocumentSummary | null,
  email: ClientEmailSummary | null,
  relatedLinks: RelatedKnowledgeLink[],
): AssistantSourceReference[] {
  return buildKnowledgeAssistantSourceReferences({
    customerLabel: client?.displayName,
    primarySource: document
      ? { key: `${item.id}-document`, label: document.title, sourceType: "DOCUMENT" }
      : email
        ? { key: `${item.id}-email`, label: email.subject, sourceType: "EMAIL" }
        : { key: `${item.id}-review`, label: item.title ?? item.itemId, sourceType: "METADATA", detail: item.reason },
    relatedLinks,
  });
}

function buildDetailAlerts(
  item: ReviewQueueItem,
  client: Awaited<ReturnType<typeof getClient>> | null,
  document: ClientDocumentSummary | null,
  email: ClientEmailSummary | null,
) {
  const alerts: Array<{
    key: string;
    severity: "warning" | "info";
    message: string;
    action?: React.ReactNode;
  }> = [];

  if (!client) {
    alerts.push({
      key: "link",
      severity: "warning",
      message: "This review item is not linked to a customer. Confirm ownership before approval.",
    });
  }

  if (!document && !email) {
    alerts.push({
      key: "source",
      severity: "info",
      message: "Source preview data is limited for this item. Use extracted metadata and linked records to complete review.",
    });
  }

  if (item.reason === "LOW_EXTRACTION_CONFIDENCE") {
    alerts.push({
      key: "confidence",
      severity: "warning",
      message: "Extraction confidence is low for this review item. Verify all field values against the available source context.",
    });
  }

  if (!document && !email && !alerts.some((alert) => alert.key === "source")) {
    alerts.push({
      key: "source",
      severity: "info",
      message: "Inline preview is unavailable. Continue with metadata validation and linked customer context.",
    });
  }

  return alerts;
}

function matchReviewDocument(item: ReviewQueueItem | undefined, documents: ClientDocumentSummary[]) {
  if (!item) {
    return null;
  }

  if (item.itemType === "DOCUMENT") {
    return documents.find((document) => document.id === item.itemId) ?? null;
  }

  if (item.itemType === "DOCUMENT_VERSION") {
    return documents.find((document) => document.currentVersionId === item.itemId) ?? null;
  }

  return null;
}

function matchReviewEmail(item: ReviewQueueItem | undefined, emails: ClientEmailSummary[]) {
  if (!item || item.itemType !== "EMAIL") {
    return null;
  }

  return emails.find((email) => email.id === item.itemId) ?? null;
}

function hasMetadataChanges(item: ReviewQueueItem, draft: MetadataDraft) {
  return (
    (item.title ?? "") !== draft.title ||
    (item.documentTypeId ?? "") !== draft.documentTypeId ||
    JSON.stringify(trimMetadataValues(item.metadataValues)) !== JSON.stringify(trimMetadataValues(draft.metadataValues))
  );
}

function trimMetadataValues(values: Record<string, string>) {
  return Object.fromEntries(
    Object.entries(values)
      .map(([key, value]) => [key, value.trim()])
      .filter(([, value]) => value.length > 0),
  );
}

function buildReviewViewerEvidenceSections({
  item,
  client,
  document,
  email,
  workspace,
  relatedLinks,
  versions,
  metadataGroups,
  aiSummaryState,
}: {
  item: ReviewQueueItem | null;
  client: Awaited<ReturnType<typeof getClient>> | null;
  document: ClientDocumentSummary | null;
  email: ClientEmailSummary | null;
  workspace: DemoClientWorkspace | undefined;
  relatedLinks: RelatedKnowledgeLink[];
  versions: DocumentVersionSummary[];
  metadataGroups: MetadataGroup[];
  aiSummaryState: AiSummaryState;
}): EvidenceWorkspaceSection[] {
  if (!item) {
    return [];
  }

  const sections: EvidenceWorkspaceSection[] = [];
  const metadataLines = Object.entries(item.metadataValues).filter(([, value]) => value.trim());
  const aiSummary = workspace?.aiSummaries[0];
  const processingJob = item.processingJob ?? null;
  const processingFields = processingJob?.fields ?? [];
  const processingFindings = processingJob?.findings ?? [];
  const evidenceConfidence = mapNumericConfidenceToLevel(processingJob?.metadataConfidence ?? processingJob?.overallConfidence);
  const businessReferences = collectKnowledgeBusinessReferenceFields(relatedLinks);
  const relatedDocuments = relatedLinks.filter((link) => link.relatedSourceType === "DOCUMENT" || link.relatedSourceType === "DOCUMENT_VERSION");
  const relatedEmails = relatedLinks.filter((link) => link.relatedSourceType === "EMAIL" || link.sourceType === "EMAIL");

  sections.push({
    key: "ai-summary",
    title: "AI Summary",
    defaultExpanded: true,
    summary: aiSummary ? "Operational AI summary available" : "No AI summary payload",
    searchText: aiSummary?.summary,
    content: (
      <AISummary
        state={aiSummaryState}
        summary={aiSummary?.summary}
        confidence={normalizeConfidence(aiSummary?.confidence)}
        supportingNotes={aiSummary?.evidence ?? []}
      />
    ),
  });

  sections.push({
    key: "evidence",
    title: "Evidence",
    defaultExpanded: true,
    summary: processingFields.length > 0 ? "Processing citations, validations, and extracted values" : metadataLines.length > 0 ? "Extracted citations and supporting values" : "No evidence items available",
    countLabel: processingFields.length > 0 ? `${processingFields.length} fields` : metadataLines.length > 0 ? `${metadataLines.length} items` : undefined,
    searchText: [...metadataLines.map(([key, value]) => `${key} ${value}`), ...processingFields.map((field) => `${field.fieldKey} ${field.extractedValue ?? ""}`)].join(" "),
    content: processingFields.length > 0 ? (
      <Stack spacing={0.75}>
        {processingFields.map((field) => (
          <EvidenceCard
            key={field.fieldKey}
            title={field.fieldLabel}
            excerpt={field.correctedValue ?? field.extractedValue ?? field.approvedValue ?? "No value extracted"}
            citation={document?.title ? `${document.title}${field.sourcePage ? ` · Page ${field.sourcePage}` : ""}` : email?.subject}
            source={field.sourceType}
            type={field.businessReferenceType ? "Business reference field" : "Extracted evidence"}
            confidence={mapNumericConfidenceToLevel(field.confidence)}
            metadata={[
              `Method: ${humanizeProcessingValue(field.extractionMethod)}`,
              `Validation: ${humanizeProcessingValue(field.validationState)}`,
            ]}
            navigationActions={[
              { key: `${field.fieldKey}-page`, label: "Jump to page", kind: "page", disabled: true },
              { key: `${field.fieldKey}-ocr`, label: "Jump to OCR region", kind: "ocr-region", disabled: true },
              { key: `${field.fieldKey}-highlight`, label: "Jump to highlight", kind: "highlight", disabled: true },
            ]}
          />
        ))}
      </Stack>
    ) : metadataLines.length > 0 ? (
      <Stack spacing={0.75}>
        {metadataLines.map(([key, value]) => (
          <EvidenceCard
            key={key}
            title={humanizeFieldKey(key)}
            excerpt={value}
            citation={document?.title ? `${document.title}${document.createdAt ? ` · ${formatDateTime(document.createdAt)}` : ""}` : email?.subject}
            source={document?.source ?? email?.sender ?? formatReviewType(item.itemType)}
            type="Extracted evidence"
            confidence={evidenceConfidence}
            metadata={[`Reason: ${formatReason(item.reason)}`, `Status: ${formatStatus(item.status)}`]}
            navigationActions={[
              { key: `${key}-page`, label: "Jump to page", kind: "page", disabled: true },
              { key: `${key}-ocr`, label: "Jump to OCR region", kind: "ocr-region", disabled: true },
              { key: `${key}-highlight`, label: "Jump to highlight", kind: "highlight", disabled: true },
            ]}
          />
        ))}
      </Stack>
    ) : (
      <Typography variant="body2" color="text.secondary">
        No extracted evidence values are currently available.
      </Typography>
    ),
  });

  sections.push({
    key: "fields",
      title: "Extracted Fields",
    summary: processingFields.length > 0 ? "Pipeline fields with extracted, corrected, and approved states" : metadataLines.length > 0 ? "Grouped fields require manual validation" : "No extracted fields captured",
    searchText: [...metadataLines.map(([key, value]) => `${key} ${value}`), ...processingFields.map((field) => `${field.fieldLabel} ${field.extractedValue ?? ""} ${field.correctedValue ?? ""}`)].join(" "),
    content: (
      <Stack spacing={1}>
        {processingFields.length > 0 ? (
          <EvidenceMetadataGroup
            title="Processing pipeline fields"
            fields={processingFields.map((field) => ({
              key: field.fieldKey,
              label: field.fieldLabel,
              value: field.correctedValue ?? field.extractedValue ?? field.approvedValue ?? "Missing value",
              state: mapProcessingFieldState(field.validationState, field.correctedValue ?? field.extractedValue ?? field.approvedValue),
              helperText: `${humanizeProcessingValue(field.extractionMethod)} · ${field.required ? "Required" : "Optional"}`,
              confidence: mapNumericConfidenceToLevel(field.confidence),
            }))}
          />
        ) : null}
        {metadataGroups.map((group) => (
          <EvidenceMetadataGroup
            key={group.key}
            title={group.title}
            fields={group.fields.map((field) => ({
              key: field.key,
              label: field.label,
              value: field.value,
              state: field.value.trim() ? "NEEDS_REVIEW" : "MISSING",
              helperText: field.value.trim() ? "Manual review required" : "Missing value",
              confidence: field.value.trim() ? evidenceConfidence : "UNKNOWN",
            }))}
          />
        ))}
      </Stack>
    ),
  });

  if (client) {
    sections.push({
      key: "business-context",
      title: "Related Business Context",
      summary: businessReferences.length > 0 ? "Linked customer and external reference fields" : "Linked customer context",
      content: (
        <Stack spacing={1}>
          <EvidenceMetadataGroup
            title="Customer"
            fields={[
              {
                key: "customer-name",
                label: "Customer",
                value: client.displayName,
                state: "VERIFIED",
                helperText: "Linked customer record",
                confidence: "HIGH",
              },
              {
                key: "customer-id",
                label: "Customer ID",
                value: `${client.clientId} · ${client.clientType}`,
                state: "READ_ONLY",
                helperText: "Read-only linked record",
              },
            ]}
          />
          {businessReferences.length > 0 ? (
            <EvidenceMetadataGroup
              title="Business Reference Fields"
              fields={businessReferences.map((reference, index) => ({
                key: `review-reference-${index}`,
                label: reference.label,
                value: reference.value,
                state: "VERIFIED" as const,
                helperText: "Structured searchable metadata",
                confidence: "HIGH" as const,
              }))}
            />
          ) : null}
          {relatedDocuments.length > 0 ? (
            <EvidenceMetadataGroup
              title="Related Documents"
              fields={relatedDocuments.slice(0, 4).map((link, index) => ({
                key: `related-document-${index}`,
                label: humanizeKnowledgeRelationshipType(link.relationshipType),
                value: `${link.relatedTitle} · ${link.explanation}`,
                state: link.inferred ? "NEEDS_REVIEW" : "READ_ONLY",
              }))}
            />
          ) : null}
          {relatedEmails.length > 0 ? (
            <EvidenceMetadataGroup
              title="Related Emails"
              fields={relatedEmails.slice(0, 3).map((link, index) => ({
                key: `related-email-${index}`,
                label: humanizeKnowledgeRelationshipType(link.relationshipType),
                value: link.sourceType === "EMAIL" ? link.sourceTitle : link.relatedTitle,
                state: link.inferred ? "NEEDS_REVIEW" : "READ_ONLY",
              }))}
            />
          ) : null}
          {(document || email) ? (
            <EvidenceMetadataGroup
              title="Related Documents"
              fields={[
                {
                  key: "related-record",
                  label: document ? "Document" : "Email",
                  value: document?.title ?? email?.subject,
                  state: "READ_ONLY",
                  helperText: "Linked source record",
                },
              ]}
            />
          ) : null}
        </Stack>
      ),
    });
  }

  sections.push({
    key: "document-information",
    title: "Document Information",
    summary: "Version, dates, and source information",
    content: (
      <EvidenceMetadataGroup
        title="Document information"
        fields={[
          {
            key: "document-type",
            label: "Document type",
            value: item.documentTypeId || formatReviewType(item.itemType),
            state: "READ_ONLY",
          },
          {
            key: "version",
            label: "Version",
            value: document?.currentVersionId ?? "Current",
            state: "READ_ONLY",
          },
          {
            key: "created",
            label: "Created",
            value: document?.createdAt ? formatDateTime(document.createdAt) : email?.receivedAt ? formatDateTime(email.receivedAt) : undefined,
            state: document?.createdAt || email?.receivedAt ? "READ_ONLY" : "MISSING",
          },
          {
            key: "source",
            label: "Source",
            value: document?.source ?? email?.sender ?? formatReviewType(item.itemType),
            state: "READ_ONLY",
          },
          {
            key: "version-history",
            label: "Version history",
            value: summarizeKnowledgeVersions(versions),
            state: "READ_ONLY",
          },
        ]}
      />
    ),
  });

  sections.push({
    key: "activity",
    title: "Activity",
    summary: "Review actions, history, and comments",
    content: (
      <Stack spacing={1}>
        <EvidenceMetadataGroup
          title="Review actions"
          fields={[
            {
              key: "status",
              label: "Workflow status",
              value: formatStatus(item.status),
              state: "READ_ONLY",
            },
            {
              key: "reason",
              label: "Review reason",
              value: formatReason(item.reason),
              state: item.reason === "LOW_EXTRACTION_CONFIDENCE" ? "NEEDS_REVIEW" : "READ_ONLY",
              helperText: item.reason === "LOW_EXTRACTION_CONFIDENCE" ? "Low-confidence extraction requires manual review" : "Operational review context",
              confidence: evidenceConfidence,
            },
            ...(processingJob
              ? [
                  {
                    key: "processing-stage",
                    label: "Processing stage",
                    value: humanizeProcessingValue(processingJob.currentStage),
                    state: "READ_ONLY" as const,
                    helperText: `Retry count ${processingJob.retryCount}`,
                    confidence: mapNumericConfidenceToLevel(processingJob.overallConfidence),
                  },
                ]
              : []),
            {
              key: "assigned",
              label: "Assigned to",
              value: item.assignedTo ?? "Unassigned",
              state: "READ_ONLY",
            },
          ]}
        />
        <EvidenceMetadataGroup
          title="History and comments"
          fields={[
            {
              key: "history",
              label: "Latest source event",
              value: document?.createdAt
                ? `Document received ${formatDateTime(document.createdAt)}`
                : email?.receivedAt
                  ? `Email received ${formatDateTime(email.receivedAt)}`
                  : "No source history available",
              state: "READ_ONLY",
            },
            {
              key: "comments",
              label: "Reviewer comments",
              value: processingJob?.reviewerComment ?? "No read-only comments are available from the current API.",
              state: "READ_ONLY",
            },
            {
              key: "related-knowledge",
              label: "Related knowledge",
              value: relatedLinks.length > 0 ? `${relatedLinks.length} related sources available` : "No related sources returned by the current API.",
              state: "READ_ONLY",
            },
            ...(processingFindings.length > 0
              ? [
                  {
                    key: "validation-findings",
                    label: "Validation findings",
                    value: processingFindings.map((finding) => finding.message).join(" · "),
                    state: "NEEDS_REVIEW" as const,
                    helperText: "Processing findings require explicit reviewer confirmation",
                    confidence: mapNumericConfidenceToLevel(processingJob?.validationConfidence),
                  },
                ]
              : []),
          ]}
        />
      </Stack>
    ),
  });

  return sections;
}

function setScopedSearchParams(
  setSearchParams: ReturnType<typeof useSearchParams>[1],
  currentParams: URLSearchParams,
  updates: Record<string, string | null>,
) {
  const next = new URLSearchParams(currentParams);
  for (const [key, value] of Object.entries(updates)) {
    if (!value) {
      next.delete(key);
      continue;
    }
    next.set(key, value);
  }
  setSearchParams(next, { replace: true });
}

function formatReviewType(value: ReviewQueueItem["itemType"]) {
  switch (value) {
    case "DOCUMENT":
      return "Document";
    case "EMAIL":
      return "Email";
    case "DOCUMENT_VERSION":
      return "Document version";
  }
}

function formatStatus(value: ReviewQueueStatus) {
  switch (value) {
    case "OPEN":
      return "Open";
    case "IN_PROGRESS":
      return "In review";
    case "RESOLVED":
      return "Approved";
    case "REJECTED":
      return "Rejected";
  }
}

function formatReason(value: ReviewQueueReason) {
  return value
    .toLowerCase()
    .split("_")
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(" ");
}

function humanizeProcessingValue(value: string) {
  return value
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (character) => character.toUpperCase());
}

function formatConfidenceValue(value: number | null | undefined) {
  return typeof value === "number" ? `${Math.round(value * 100)}%` : "Unavailable";
}

function mapProcessingFieldState(validationState: string, value: string | null | undefined) {
  switch (validationState) {
    case "VERIFIED":
      return "VERIFIED" as const;
    case "READ_ONLY":
      return "READ_ONLY" as const;
    case "MISSING":
      return "MISSING" as const;
    default:
      return value ? "NEEDS_REVIEW" as const : "MISSING" as const;
  }
}

function mapNumericConfidenceToLevel(value: number | null | undefined) {
  if (typeof value !== "number") {
    return "UNKNOWN" as const;
  }
  if (value >= 0.85) {
    return "HIGH" as const;
  }
  if (value >= 0.6) {
    return "MEDIUM" as const;
  }
  return "LOW" as const;
}

function mapStatusTone(value: ReviewQueueStatus): StatusTone {
  switch (value) {
    case "OPEN":
      return "warning";
    case "IN_PROGRESS":
      return "info";
    case "RESOLVED":
      return "success";
    case "REJECTED":
      return "error";
  }
}

function mapReasonTone(value: ReviewQueueReason): StatusTone {
  switch (value) {
    case "LOW_CLIENT_CONFIDENCE":
    case "LOW_CLASSIFICATION_CONFIDENCE":
    case "LOW_EXTRACTION_CONFIDENCE":
      return "warning";
    case "PROCESSING_FAILED":
    case "PROMPT_INJECTION_RISK":
    case "REDACTION_FAILED":
      return "error";
    case "DUPLICATE_UNCERTAINTY":
      return "info";
    case "UNLINKED":
      return "neutral";
  }
}

function buildAiExplanation(reason: ReviewQueueReason) {
  switch (reason) {
    case "UNLINKED":
      return "The intake pipeline could not confirm a customer owner for this record.";
    case "LOW_CLIENT_CONFIDENCE":
      return "Customer matching evidence is present but does not meet the confidence threshold for automatic linkage.";
    case "LOW_CLASSIFICATION_CONFIDENCE":
      return "Document classification is uncertain and requires a human decision before release.";
    case "LOW_EXTRACTION_CONFIDENCE":
      return "One or more extracted values are below the confidence threshold and must be manually verified.";
    case "DUPLICATE_UNCERTAINTY":
      return "Potential duplicate indicators were detected, but the system could not safely collapse the records.";
    case "REDACTION_FAILED":
      return "Protected content could not be redacted confidently, so human review is required.";
    case "PROMPT_INJECTION_RISK":
      return "The system detected language that may attempt to manipulate AI processing and routed the item for manual review.";
    case "PROCESSING_FAILED":
      return "Automated processing did not complete successfully and needs manual correction.";
  }
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(value));
}

function humanizeFieldKey(value: string) {
  return value
    .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
    .replace(/[_-]+/g, " ")
    .replace(/\b\w/g, (segment) => segment.toUpperCase());
}
