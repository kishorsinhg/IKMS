import ApprovalOutlinedIcon from "@mui/icons-material/ApprovalOutlined";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import DescriptionOutlinedIcon from "@mui/icons-material/DescriptionOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import LinkOutlinedIcon from "@mui/icons-material/LinkOutlined";
import OpenInNewOutlinedIcon from "@mui/icons-material/OpenInNewOutlined";
import TaskAltOutlinedIcon from "@mui/icons-material/TaskAltOutlined";
import ZoomInOutlinedIcon from "@mui/icons-material/ZoomInOutlined";
import ZoomOutOutlinedIcon from "@mui/icons-material/ZoomOutOutlined";
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Drawer,
  FormControl,
  IconButton,
  InputLabel,
  List,
  ListItemButton,
  ListItemText,
  MenuItem,
  Paper,
  Select,
  Stack,
  TextField,
  Tooltip,
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
  approveReviewItem,
  ClientDocumentSummary,
  ClientEmailSummary,
  correctReviewItemMetadata,
  getReviewQueueItem,
  linkReviewItemClient,
  listClientDocuments,
  listClientEmails,
  rejectReviewItem,
  ReviewQueueItem,
  ReviewQueueReason,
  ReviewQueueStatus,
} from "../../../api/intake";
import { useCurrentUser } from "../../../app/auth/useCurrentUser";
import type { ContextSection } from "../../../app/components/RightContextPanel";
import { StatusBadge, StatusTone } from "../../../app/components/StatusBadge";
import { WorkspaceToolbar } from "../../../app/components/WorkspaceToolbar";
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
  const [selectedClientId, setSelectedClientId] = useState("");
  const [rejectReason, setRejectReason] = useState("");
  const [viewerZoom, setViewerZoom] = useState(100);
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
      buildContextSections({
        item: reviewItem ?? null,
        client: clientQuery.data ?? null,
        document: linkedDocument,
        email: linkedEmail,
        workspace: relatedWorkspace,
        onReject: () => setRejectDialogOpen(true),
        onLinkCustomer: () => setLinkDialogOpen(true),
        onEditMetadata: () => setEditMode(true),
        onOpenCustomer: canOpenCustomer ? () => navigate(`/clients/${clientQuery.data!.id}`) : undefined,
        onOpenOriginal: canOpenDocument ? () => window.open(previewHref!, "_blank", "noopener,noreferrer") : undefined,
      }),
    [
      canOpenCustomer,
      canOpenDocument,
      clientQuery.data,
      linkedDocument,
      linkedEmail,
      navigate,
      previewHref,
      relatedWorkspace,
      reviewItem,
    ],
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
        <Button
          variant="text"
          color="inherit"
          startIcon={<ArrowBackOutlinedIcon fontSize="small" />}
          onClick={() => navigate(backToQueueHref)}
        >
          Back to queue
        </Button>
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
  }, [approveMutation.isPending, backToQueueHref, contextSections, navigate, reviewItem]);

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
          zoom={viewerZoom}
          onZoomChange={setViewerZoom}
          previewAvailable={previewAvailable}
          prioritizeMetadata={!previewAvailable}
          isMobile={isMobile}
        />

        <MetadataEditorPanel
          item={reviewItem}
          metadataDraft={metadataDraft}
          metadataGroups={metadataGroups}
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

      {!isDesktopContext ? (
        <Drawer
          anchor="right"
          open={contextDrawerOpen}
          onClose={() => setContextDrawerOpen(false)}
          PaperProps={{ sx: { width: { xs: "100%", sm: 360 } } }}
        >
          <Stack spacing={1} sx={{ p: 2 }}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
              <Typography variant="subtitle2">Evidence Assistant</Typography>
              <Button size="small" onClick={() => setContextDrawerOpen(false)}>
                Close
              </Button>
            </Stack>
            {contextSections.map((section) => (
              <Paper key={section.key} variant="outlined" sx={{ p: 1.25 }}>
                <Typography variant="subtitle2" gutterBottom>
                  {section.title}
                </Typography>
                {section.content}
              </Paper>
            ))}
          </Stack>
        </Drawer>
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
  zoom,
  onZoomChange,
  previewAvailable,
  prioritizeMetadata,
  isMobile,
}: {
  item: ReviewQueueItem;
  document: ClientDocumentSummary | null;
  email: ClientEmailSummary | null;
  previewHref: string | null;
  canOpenDocument: boolean;
  canViewOriginal: boolean;
  zoom: number;
  onZoomChange: (next: number) => void;
  previewAvailable: boolean;
  prioritizeMetadata: boolean;
  isMobile: boolean;
}) {
  return (
    <Paper
      variant="outlined"
      sx={{
        display: "grid",
        gridTemplateRows: previewAvailable ? "auto minmax(0, 1fr)" : "auto",
        minHeight: previewAvailable ? { xs: 360, lg: 560 } : undefined,
        order: prioritizeMetadata && isMobile ? 2 : 1,
        overflow: "hidden",
      }}
    >
      <Stack
        spacing={previewAvailable ? 1 : 0.75}
        sx={{
          px: 1.25,
          py: previewAvailable ? 1.1 : 0.95,
          borderBottom: previewAvailable ? (theme) => `1px solid ${theme.palette.divider}` : undefined,
        }}
      >
        <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={1}>
          <Stack spacing={0.25}>
            <Typography variant="subtitle2">Document Viewer</Typography>
            <Typography variant="body2" color="text.secondary" noWrap>
              {document?.title ?? email?.subject ?? item.title ?? item.itemId}
            </Typography>
          </Stack>
          {canOpenDocument && previewHref ? (
            <Button
              size="small"
              variant="outlined"
              startIcon={<OpenInNewOutlinedIcon fontSize="small" />}
              component="a"
              href={previewHref!}
              target="_blank"
              rel="noreferrer"
            >
              {canViewOriginal ? "Open original" : "Open document"}
            </Button>
          ) : null}
        </Stack>

        {previewAvailable ? (
          <Stack direction="row" spacing={0.5} alignItems="center" flexWrap="wrap" useFlexGap>
            <Tooltip title="Zoom out">
              <span>
                <IconButton size="small" onClick={() => onZoomChange(Math.max(75, zoom - 10))}>
                  <ZoomOutOutlinedIcon fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>
            <Tooltip title="Zoom in">
              <span>
                <IconButton size="small" onClick={() => onZoomChange(Math.min(150, zoom + 10))}>
                  <ZoomInOutlinedIcon fontSize="small" />
                </IconButton>
              </span>
            </Tooltip>
            <Button size="small" variant="text" color="inherit" onClick={() => onZoomChange(100)}>
              Fit width
            </Button>
            <Button size="small" variant="text" color="inherit" onClick={() => onZoomChange(90)}>
              Fit page
            </Button>
            <Typography variant="caption" color="text.secondary">
              {zoom}%
            </Typography>
          </Stack>
        ) : null}
      </Stack>

      {previewAvailable ? (
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: "72px minmax(0, 1fr)",
            minHeight: 0,
          }}
        >
          <Box sx={{ borderRight: (theme) => `1px solid ${theme.palette.divider}`, p: 0.75, backgroundColor: "background.default" }}>
            <List disablePadding sx={{ display: "grid", gap: 0.5 }}>
              <ListItemButton selected dense sx={{ borderRadius: 1, px: 1, py: 0.75 }}>
                <ListItemText
                  primary={<Typography variant="caption">1</Typography>}
                  secondary={<Typography variant="caption" color="text.secondary">{item.itemType === "EMAIL" ? "Message" : "Page"}</Typography>}
                />
              </ListItemButton>
            </List>
          </Box>

          <Box sx={{ minWidth: 0, minHeight: 0, overflow: "auto", backgroundColor: "background.default" }}>
            <Box sx={{ p: 1.25 }}>
              <Box
                sx={{
                  transform: `scale(${zoom / 100})`,
                  transformOrigin: "top center",
                  transition: "transform 120ms ease",
                }}
              >
                <Box
                  component="iframe"
                  title="Review document preview"
                  src={previewHref!}
                  sx={{
                    display: "block",
                    width: "100%",
                    minHeight: 520,
                    border: 0,
                    backgroundColor: "common.white",
                  }}
                />
              </Box>
            </Box>
          </Box>
        </Box>
      ) : (
        <Box sx={{ px: 1.25, pb: 1.1 }}>
          <Paper variant="outlined" sx={{ px: 1.25, py: 1 }}>
            {item.itemType === "EMAIL" && email ? (
              <Stack spacing={0.75}>
                <Typography variant="body2" color="text.secondary">
                  Inline email preview is not available in the current API.
                </Typography>
                <Stack spacing={0.5}>
                  <MetadataReadout label="Sender" value={email.sender} />
                  <MetadataReadout label="Received" value={formatDateTime(email.receivedAt)} />
                </Stack>
              </Stack>
            ) : (
              <Stack spacing={0.75}>
                <Typography variant="body2" color="text.secondary">
                  Inline preview is unavailable for this review item.
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Continue with metadata validation and linked business context.
                </Typography>
              </Stack>
            )}
          </Paper>
        </Box>
      )}
    </Paper>
  );
}

function MetadataEditorPanel({
  item,
  metadataDraft,
  metadataGroups,
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

function MetadataReadout({ label, value }: { label: string; value: string }) {
  return (
    <Stack spacing={0.35}>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="body2">{value}</Typography>
    </Stack>
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
  onReject,
  onLinkCustomer,
  onEditMetadata,
  onOpenCustomer,
  onOpenOriginal,
}: {
  item: ReviewQueueItem | null;
  client: Awaited<ReturnType<typeof getClient>> | null;
  document: ClientDocumentSummary | null;
  email: ClientEmailSummary | null;
  workspace: DemoClientWorkspace | undefined;
  onReject: () => void;
  onLinkCustomer: () => void;
  onEditMetadata: () => void;
  onOpenCustomer?: () => void;
  onOpenOriginal?: () => void;
}): ContextSection[] {
  if (!item) {
    return [];
  }

  const policy = workspace?.policyReferences[0];
  const claim = workspace?.claimReferences[0];
  const aiSummary = workspace?.aiSummaries[0];
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
    ...(policy
      ? [
          {
            key: "policy",
            title: "Policy",
            content: (
              <Stack spacing={0.5}>
                <Typography variant="body2">{policy.policyNumber}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {policy.lineOfBusiness} · {policy.carrier}
                </Typography>
              </Stack>
            ),
          } satisfies ContextSection,
        ]
      : []),
    ...(claim
      ? [
          {
            key: "claim",
            title: "Claim",
            content: (
              <Stack spacing={0.5}>
                <Typography variant="body2">{claim.claimNumber}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {claim.status} · {claim.carrier}
                </Typography>
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
          <Button size="small" color="error" variant="text" onClick={onReject}>
            Reject
          </Button>
        </Stack>
      ),
    },
  ];
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
