import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import DownloadOutlinedIcon from "@mui/icons-material/DownloadOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import ExpandMoreOutlinedIcon from "@mui/icons-material/ExpandMoreOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import VisibilityOutlinedIcon from "@mui/icons-material/VisibilityOutlined";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Stack,
  Tab,
  Tabs,
  TextField,
  Link,
  Tooltip,
  Typography,
  useMediaQuery,
} from "@mui/material";
import { useTheme } from "@mui/material/styles";
import { GridColDef, GridSortModel } from "@mui/x-data-grid";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { KeyboardEvent, ReactElement, useEffect, useMemo, useState } from "react";
import { useNavigate, useOutletContext, useParams, useSearchParams } from "react-router-dom";
import {
  createNote,
  deleteNote,
  getClient,
  listDocumentVersions,
  listKnowledgeTimeline,
  listNotes,
  listRelatedKnowledge,
  listSourceRelatedKnowledge,
  type DocumentVersionSummary,
  type CustomerKnowledgeTimelineEvent,
  type RelatedKnowledgeLink,
  Note,
  updateNote,
} from "../../api/clients";
import { DemoClientWorkspace, getDemoClientWorkspace, isDemoDataEnabled } from "../../api/demo";
import { ClientDocumentSummary, ClientEmailSummary, listClientDocuments, listClientEmails } from "../../api/intake";
import { useCurrentUser } from "../../app/auth/useCurrentUser";
import { AISummary } from "../../app/components/document-viewer/AISummary";
import { DocumentViewerDialog } from "../../app/components/document-viewer/DocumentViewerDialog";
import { EvidenceCard } from "../../app/components/document-viewer/EvidenceCard";
import { type EnterpriseDocumentViewerProps } from "../../app/components/document-viewer/EnterpriseDocumentViewer";
import { MetadataGroup as EvidenceMetadataGroup } from "../../app/components/document-viewer/MetadataGroup";
import { documentViewerPlaceholderLayers } from "../../app/components/document-viewer/documentViewerLayers";
import { type EvidenceWorkspaceSection, normalizeConfidence } from "../../app/components/document-viewer/evidenceWorkspaceTypes";
import { EntityGrid } from "../../app/components/EntityGrid";
import { StatusBadge, StatusTone } from "../../app/components/StatusBadge";
import { WorkspaceToolbar } from "../../app/components/WorkspaceToolbar";
import { useNotification } from "../../app/providers/useNotification";
import type { ContextSection } from "../../app/components/RightContextPanel";
import type { IkmsShellOutletContext, ShellWorkspaceChrome } from "../../app/shell/IkmsAppShell";
import { ClientSearchPanel } from "../search/ClientSearchPanel";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  RetryAction,
} from "../../app/WorkspaceStates";

type CustomerTab = "documents" | "emails" | "notes" | "relationships" | "policies" | "claims" | "timeline";

interface DocumentRow extends Record<string, unknown> {
  id: string;
  title: string;
  category: string;
  version: string;
  modified: string;
  owner: string;
  status: string;
  statusTone: StatusTone;
  previewHref: string;
  downloadHref: string;
  permissionLabel: string;
}

interface EmailRow extends Record<string, unknown> {
  id: string;
  subject: string;
  sender: string;
  date: string;
  attachment: string;
  status: string;
  statusTone: StatusTone;
  recipients: string;
}

interface NoteRow extends Record<string, unknown> {
  id: string;
  title: string;
  author: string;
  created: string;
  updated: string;
  text: string;
}

interface RelationshipRow extends Record<string, unknown> {
  id: string;
  relatedCustomer: string;
  relationship: string;
  source: string;
  status: string;
  explanation: string;
  score: string;
  derivationType: string;
}

interface PolicyReferenceRow extends Record<string, unknown> {
  id: string;
  policyNumber: string;
  product: string;
  insurer: string;
  status: string;
  expiry: string;
  summary: string;
}

interface ClaimReferenceRow extends Record<string, unknown> {
  id: string;
  claimNumber: string;
  carrier: string;
  status: string;
  opened: string;
  updated: string;
  summary: string;
}

interface TimelineRow extends Record<string, unknown> {
  id: string;
  event: string;
  type: string;
  occurredAt: string;
  actor: string;
  detail: string;
  status: string;
  statusTone: StatusTone;
  sourceType?: string;
  businessReferences?: string;
}

interface MobileListItem {
  id: string;
  title: string;
  subtitle: string;
  meta: string;
  status?: { label: string; tone: StatusTone };
  actions?: Array<{
    key: string;
    label: string;
    icon?: ReactElement;
    href?: string;
    onClick?: () => void;
  }>;
}

const tabs: Array<{ key: CustomerTab; label: string; mobileLabel: string }> = [
  { key: "documents", label: "Documents", mobileLabel: "Docs" },
  { key: "emails", label: "Emails", mobileLabel: "Email" },
  { key: "notes", label: "Notes", mobileLabel: "Notes" },
  { key: "relationships", label: "Relationships", mobileLabel: "Related" },
  { key: "policies", label: "Policy References", mobileLabel: "Policies" },
  { key: "claims", label: "Claim References", mobileLabel: "Claims" },
  { key: "timeline", label: "Timeline", mobileLabel: "Timeline" },
];

const clientQueryKey = (clientId: string) => ["clients", "profile", clientId] as const;
const notesQueryKey = (clientId: string) => ["clients", clientId, "notes"] as const;
const documentsQueryKey = (clientId: string) => ["clients", clientId, "documents"] as const;
const emailsQueryKey = (clientId: string) => ["clients", clientId, "emails"] as const;
const timelineQueryKey = (clientId: string) => ["clients", clientId, "knowledge", "timeline"] as const;
const relatedKnowledgeQueryKey = (clientId: string) => ["clients", clientId, "knowledge", "related"] as const;
const sourceRelatedKnowledgeQueryKey = (sourceType: string, sourceId: string) =>
  ["knowledge", "sources", sourceType, sourceId, "related"] as const;
const documentVersionsQueryKey = (documentId: string) => ["documents", documentId, "versions"] as const;
const workspaceQueryKey = (clientId: string) => ["clients", clientId, "demo-workspace"] as const;

export function ClientProfilePage() {
  const { clientId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { notify } = useNotification();
  const { setWorkspaceChrome, clearWorkspaceChrome } = useOutletContext<IkmsShellOutletContext>();
  const [searchParams, setSearchParams] = useSearchParams();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));
  const isTabletDown = useMediaQuery(theme.breakpoints.down("lg"));
  const [noteDraft, setNoteDraft] = useState("");
  const [editingNoteId, setEditingNoteId] = useState<string | null>(null);
  const [noteDrawerOpen, setNoteDrawerOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [viewerDocumentId, setViewerDocumentId] = useState<string | null>(null);
  const currentUserQuery = useCurrentUser();

  const activeTab = parseTab(searchParams.get("tab"));
  const currentTabQuery = searchParams.get(tabParam(activeTab, "q")) ?? "";
  const selectedRowId = searchParams.get(tabParam(activeTab, "selected"));
  const sortField = searchParams.get(tabParam(activeTab, "sort")) ?? "";
  const sortDirection = searchParams.get(tabParam(activeTab, "dir")) === "desc" ? "desc" : "asc";
  const [toolbarQuery, setToolbarQuery] = useState(currentTabQuery);

  const clientQuery = useQuery({
    queryKey: clientId ? clientQueryKey(clientId) : ["clients", "profile", "empty"],
    queryFn: () => getClient(clientId!),
    enabled: Boolean(clientId),
  });
  const notesQuery = useQuery({
    queryKey: clientId ? notesQueryKey(clientId) : ["clients", "notes", "empty"],
    queryFn: () => listNotes(clientId!),
    enabled: Boolean(clientId),
  });
  const documentsQuery = useQuery({
    queryKey: clientId ? documentsQueryKey(clientId) : ["clients", "documents", "empty"],
    queryFn: () => listClientDocuments(clientId!),
    enabled: Boolean(clientId),
  });
  const emailsQuery = useQuery({
    queryKey: clientId ? emailsQueryKey(clientId) : ["clients", "emails", "empty"],
    queryFn: () => listClientEmails(clientId!),
    enabled: Boolean(clientId),
  });
  const timelineQuery = useQuery({
    queryKey: clientId ? timelineQueryKey(clientId) : ["clients", "timeline", "empty"],
    queryFn: () => listKnowledgeTimeline(clientId!, { limit: 50 }),
    enabled: Boolean(clientId) && !isDemoDataEnabled,
  });
  const relatedKnowledgeQuery = useQuery({
    queryKey: clientId ? relatedKnowledgeQueryKey(clientId) : ["clients", "related-knowledge", "empty"],
    queryFn: () => listRelatedKnowledge(clientId!, 20),
    enabled: Boolean(clientId) && !isDemoDataEnabled,
  });
  const workspaceQuery = useQuery({
    queryKey: clientId ? workspaceQueryKey(clientId) : ["clients", "workspace", "empty"],
    queryFn: () => getDemoClientWorkspace(clientId!),
    enabled: Boolean(clientId) && isDemoDataEnabled,
  });

  const createNoteMutation = useMutation({
    mutationFn: (text: string) => createNote(clientId!, { noteText: text }),
    onSuccess: async () => {
      closeNoteDrawer();
      notify({ severity: "success", message: "Note saved." });
      await queryClient.invalidateQueries({ queryKey: notesQueryKey(clientId!) });
    },
    onError: () => notify({ severity: "error", message: "Unable to save note." }),
  });
  const updateNoteMutation = useMutation({
    mutationFn: ({ noteId, text }: { noteId: string; text: string }) => updateNote(noteId, { noteText: text }),
    onSuccess: async () => {
      closeNoteDrawer();
      notify({ severity: "success", message: "Note updated." });
      await queryClient.invalidateQueries({ queryKey: notesQueryKey(clientId!) });
    },
    onError: () => notify({ severity: "error", message: "Unable to update note." }),
  });
  const deleteNoteMutation = useMutation({
    mutationFn: (noteId: string) => deleteNote(noteId),
    onSuccess: async () => {
      setDeleteDialogOpen(false);
      setEditingNoteId(null);
      notify({ severity: "success", message: "Note deleted." });
      await queryClient.invalidateQueries({ queryKey: notesQueryKey(clientId!) });
    },
    onError: () => notify({ severity: "error", message: "Unable to delete note." }),
  });

  useEffect(() => {
    setToolbarQuery(currentTabQuery);
  }, [currentTabQuery, activeTab]);

  useEffect(() => {
    function handleTabShortcuts(event: globalThis.KeyboardEvent) {
      if (!(event.ctrlKey || event.metaKey)) {
        return;
      }

      const next = shortcutToTab(event.key);
      if (!next) {
        return;
      }

      event.preventDefault();
      setScopedSearchParams(setSearchParams, searchParams, { tab: next });
    }

    window.addEventListener("keydown", handleTabShortcuts);
    return () => window.removeEventListener("keydown", handleTabShortcuts);
  }, [searchParams, setSearchParams]);

  const client = clientQuery.data;
  const workspace = workspaceQuery.data;
  const currentUser = currentUserQuery.data;
  const reviewQueueCount = workspace?.reviewQueue.length ?? 0;

  const documentRows = useMemo(
    () => mapDocumentRows(documentsQuery.data ?? [], currentUser?.permissions ?? []),
    [documentsQuery.data, currentUser?.permissions],
  );
  const emailRows = useMemo(() => mapEmailRows(emailsQuery.data ?? []), [emailsQuery.data]);
  const noteRows = useMemo(() => mapNoteRows(notesQuery.data ?? [], currentUser?.displayName ?? "Current user"), [notesQuery.data, currentUser?.displayName]);
  const relationshipRows = useMemo(
    () => mapRelationshipRows(relatedKnowledgeQuery.data?.links ?? []),
    [relatedKnowledgeQuery.data?.links],
  );
  const policyRows = useMemo(() => mapPolicyRows(workspace), [workspace]);
  const claimRows = useMemo(() => mapClaimRows(workspace), [workspace]);
  const timelineRows = useMemo(
    () =>
      isDemoDataEnabled
        ? mapTimelineRows({
            notes: notesQuery.data ?? [],
            documents: documentsQuery.data ?? [],
            emails: emailsQuery.data ?? [],
            workspace,
          })
        : mapKnowledgeTimelineRows(timelineQuery.data?.events ?? []),
    [documentsQuery.data, emailsQuery.data, notesQuery.data, timelineQuery.data?.events, workspace],
  );

  const tabRows = useMemo(() => {
    const rowsByTab = {
      documents: documentRows,
      emails: emailRows,
      notes: noteRows,
      relationships: relationshipRows,
      policies: policyRows,
      claims: claimRows,
      timeline: timelineRows,
    };
    return rowsByTab[activeTab];
  }, [activeTab, claimRows, documentRows, emailRows, noteRows, policyRows, relationshipRows, timelineRows]);

  const filteredRows = useMemo(
    () => filterRows(activeTab, tabRows, currentTabQuery),
    [activeTab, currentTabQuery, tabRows],
  );

  const sortModel = useMemo<GridSortModel>(
    () => (sortField ? [{ field: sortField, sort: sortDirection }] : []),
    [sortDirection, sortField],
  );
  const sortedRows = useMemo(
    () => sortRows(filteredRows, sortModel),
    [filteredRows, sortModel],
  );
  const selectedRow = useMemo(
    () => sortedRows.find((row) => String(row.id) === selectedRowId) ?? null,
    [selectedRowId, sortedRows],
  );
  const viewerDocument = useMemo(
    () => (viewerDocumentId ? (documentsQuery.data ?? []).find((document) => document.id === viewerDocumentId) ?? null : null),
    [documentsQuery.data, viewerDocumentId],
  );
  const viewerRelatedKnowledgeQuery = useQuery({
    queryKey: viewerDocumentId ? sourceRelatedKnowledgeQueryKey("DOCUMENT", viewerDocumentId) : ["knowledge", "source-related", "empty"],
    queryFn: () => listSourceRelatedKnowledge("DOCUMENT", viewerDocumentId!, 12),
    enabled: Boolean(viewerDocumentId) && !isDemoDataEnabled,
  });
  const documentVersionsQuery = useQuery({
    queryKey: viewerDocumentId ? documentVersionsQueryKey(viewerDocumentId) : ["documents", "versions", "empty"],
    queryFn: () => listDocumentVersions(viewerDocumentId!),
    enabled: Boolean(viewerDocumentId) && !isDemoDataEnabled,
  });
  const viewerDocumentConfig = useMemo(
    () =>
      client && viewerDocument
        ? buildCustomerViewerDocument({
            client,
            document: viewerDocument,
            workspace,
            permissions: currentUser?.permissions ?? [],
          })
        : null,
    [client, currentUser?.permissions, viewerDocument, workspace],
  );
  const viewerEvidenceSections = useMemo<EnterpriseDocumentViewerProps["evidenceSections"]>(
    () =>
      client && viewerDocument
        ? buildCustomerViewerEvidenceSections({
            client,
            document: viewerDocument,
            workspace,
            relatedLinks: viewerRelatedKnowledgeQuery.data?.links ?? [],
            versions: documentVersionsQuery.data ?? [],
          })
        : [],
    [client, documentVersionsQuery.data, viewerDocument, viewerRelatedKnowledgeQuery.data?.links, workspace],
  );

  useEffect(() => {
    if (selectedRowId && !sortedRows.some((row) => String(row.id) === selectedRowId)) {
      setScopedSearchParams(setSearchParams, searchParams, { [tabParam(activeTab, "selected")]: null });
    }
  }, [activeTab, searchParams, selectedRowId, setSearchParams, sortedRows]);

  const pageError =
    clientQuery.isError
    || notesQuery.isError
    || documentsQuery.isError
    || emailsQuery.isError
    || timelineQuery.isError
    || relatedKnowledgeQuery.isError
    || currentUserQuery.isError;
  const pageLoading =
    clientQuery.isLoading
    || notesQuery.isLoading
    || documentsQuery.isLoading
    || emailsQuery.isLoading
    || timelineQuery.isLoading
    || relatedKnowledgeQuery.isLoading
    || currentUserQuery.isLoading;
  const hasAiAssistant = currentUser?.permissions.includes("ASK_CLIENT_AI") ?? false;
  const tabMeta = getTabMeta(activeTab);
  const assistantSuggestedPrompts = useMemo(
    () => [
      "What happened recently for this customer?",
      "Show documents related to this item.",
      "Find earlier versions.",
      "What other documents mention this policy reference?",
      "What correspondence mentions this claim reference?",
      "Explain the sequence leading to this review item.",
    ],
    [],
  );
  const assistantSourceReferences = useMemo(
    () =>
      (relatedKnowledgeQuery.data?.links ?? []).slice(0, 6).map((link) => ({
        key: link.relationshipId,
        label: link.relatedTitle,
        kind: normalizeAssistantSourceKind(link.relatedSourceType),
        detail: link.explanation,
        disabled: true,
      })),
    [relatedKnowledgeQuery.data?.links],
  );
  const assistantEvidenceReferences = useMemo(
    () =>
      (timelineQuery.data?.events ?? []).slice(0, 4).map((event) => ({
        key: event.eventId,
        label: event.title,
        target: "metadata" as const,
        detail: event.summary,
        disabled: true,
      })),
    [timelineQuery.data?.events],
  );

  const workspaceChrome = useMemo<ShellWorkspaceChrome | null>(() => {
    if (!client || !currentUser) {
      return null;
    }

    const sections = buildContextSections({
      workspace,
      activeTab,
      selectedRow,
      reviewQueueCount,
      onOpenCustomerList: () => navigate("/clients"),
    });

    return {
      title: client.displayName,
      subtitle: `${client.clientId}${client.clientIdTemporary ? " (Temporary ID)" : ""} · ${client.clientType} · ${client.status}`,
      breadcrumbs: [
        { label: "IKMS" },
        { label: "Customer Access", href: "/clients" },
        { label: "Customer360" },
      ],
      secondaryActions: (
        <Button
          variant="text"
          color="inherit"
          startIcon={<ArrowBackOutlinedIcon fontSize="small" />}
          onClick={() => navigate("/clients")}
        >
          Customer list
        </Button>
      ),
      primaryActions: (
        <Stack direction="row" spacing={1} flexWrap="wrap">
          {activeTab === "notes" ? (
            <Button
              variant="contained"
              startIcon={<AddOutlinedIcon fontSize="small" />}
              onClick={() => openCreateNote()}
            >
              Add note
            </Button>
          ) : null}
        </Stack>
      ),
      contextTitle: selectedRow ? `${tabMeta.label} Record` : "Customer Context",
      contextSections: sections,
      contextWidth: 320,
    };
  }, [activeTab, client, currentUser, navigate, reviewQueueCount, selectedRow, tabMeta.label, workspace]);

  useEffect(() => {
    if (!workspaceChrome) {
      return undefined;
    }

    setWorkspaceChrome(workspaceChrome);
    return () => clearWorkspaceChrome();
  }, [clearWorkspaceChrome, setWorkspaceChrome, workspaceChrome]);

  function openCreateNote() {
    setEditingNoteId(null);
    setNoteDraft("");
    setNoteDrawerOpen(true);
  }

  function openEditNote(note: Note) {
    setEditingNoteId(note.id);
    setNoteDraft(note.noteText);
    setNoteDrawerOpen(true);
  }

  function closeNoteDrawer() {
    setNoteDrawerOpen(false);
    setEditingNoteId(null);
    setNoteDraft("");
  }

  function applyToolbarSearch() {
    setScopedSearchParams(setSearchParams, searchParams, {
      [tabParam(activeTab, "q")]: toolbarQuery.trim() || null,
      [tabParam(activeTab, "selected")]: null,
    });
  }

  function clearToolbarSearch() {
    setToolbarQuery("");
    setScopedSearchParams(setSearchParams, searchParams, {
      [tabParam(activeTab, "q")]: null,
      [tabParam(activeTab, "selected")]: null,
    });
  }

  function handleToolbarKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Enter") {
      event.preventDefault();
      applyToolbarSearch();
    }
  }

  function handleTabChange(_: React.SyntheticEvent, value: CustomerTab) {
    setScopedSearchParams(setSearchParams, searchParams, { tab: value });
  }

  function handleSortModelChange(model: GridSortModel) {
    const next = model[0];
    setScopedSearchParams(setSearchParams, searchParams, {
      [tabParam(activeTab, "sort")]: next?.field ?? null,
      [tabParam(activeTab, "dir")]: next?.sort ?? null,
    });
  }

  function handleRowSelect(rowId: string) {
    setScopedSearchParams(setSearchParams, searchParams, {
      [tabParam(activeTab, "selected")]: rowId,
    });
  }

  function handleNoteSubmit() {
    const trimmed = noteDraft.trim();
    if (!trimmed) {
      return;
    }

    if (editingNoteId) {
      updateNoteMutation.mutate({ noteId: editingNoteId, text: trimmed });
      return;
    }

    createNoteMutation.mutate(trimmed);
  }

  if (!clientId) {
    return (
      <EmptyState
        title="Customer not selected"
        message="Open a customer from Search or Customer Access to start Customer360."
        action={
          <Button variant="contained" onClick={() => navigate("/clients")}>
            Open customer list
          </Button>
        }
      />
    );
  }

  if (pageLoading) {
    return (
      <LoadingState
        title="Loading Customer360"
        message="Retrieving customer profile, related knowledge, and workspace permissions."
      />
    );
  }

  if (pageError || !client || !currentUser) {
    return (
      <ErrorState
        title="Unable to load Customer360"
        message="The selected customer workspace could not be opened."
        action={<RetryAction onClick={() => window.location.reload()} />}
      />
    );
  }

  const activeFilters = currentTabQuery
    ? [
        {
          key: `${activeTab}-query`,
          label: `Search: ${currentTabQuery}`,
          onDelete: clearToolbarSearch,
        },
      ]
    : [];

  const activeColumns = buildColumns({
    activeTab,
    isTabletDown,
    onOpenDocumentViewer: (documentId) => setViewerDocumentId(documentId),
    onEditNote: (noteId) => {
      const note = notesQuery.data?.find((item) => item.id === noteId);
      if (note) {
        openEditNote(note);
      }
    },
    onDeleteNote: (noteId) => {
      setEditingNoteId(noteId);
      setDeleteDialogOpen(true);
    },
  });

  const mobileItems = buildMobileItems({
    activeTab,
    rows: sortedRows,
    onOpenDocumentViewer: (documentId) => setViewerDocumentId(documentId),
    onOpenNote: (noteId) => {
      const note = notesQuery.data?.find((item) => item.id === noteId);
      if (note) {
        openEditNote(note);
      }
    },
    onDeleteNote: (noteId) => {
      setEditingNoteId(noteId);
      setDeleteDialogOpen(true);
    },
  });

  return (
    <Stack spacing={1.25}>
      <CustomerSummaryPanel
        client={client}
        lastActivity={timelineRows[0]?.occurredAt ? String(timelineRows[0].occurredAt) : "No activity recorded"}
      />

      <Box
        sx={{
          border: (appTheme) => `1px solid ${appTheme.palette.divider}`,
          borderRadius: 1,
          backgroundColor: "background.paper",
          overflow: "hidden",
        }}
      >
        <Tabs
          value={activeTab}
          onChange={handleTabChange}
          variant={isTabletDown ? "scrollable" : "standard"}
          scrollButtons="auto"
          allowScrollButtonsMobile
          aria-label="Customer360 tabs"
          sx={{ px: 1.25, pt: 0.25, minHeight: 44, borderBottom: (appTheme) => `1px solid ${appTheme.palette.divider}` }}
        >
          {tabs.map((tab) => (
            <Tab key={tab.key} value={tab.key} label={isMobile ? tab.mobileLabel : tab.label} />
          ))}
        </Tabs>

        <Stack spacing={0.875} sx={{ p: 1.25 }}>
          {reviewQueueCount > 0 ? (
            <Alert
              severity="warning"
              variant="outlined"
              sx={{
                py: 0,
                "& .MuiAlert-message": {
                  py: 0.5,
                },
                "& .MuiAlert-action": {
                  alignItems: "center",
                  py: 0,
                  pr: 0,
                },
              }}
              action={
                <Link
                  component="button"
                  type="button"
                  underline="hover"
                  color="inherit"
                  onClick={() => navigate("/review-queue")}
                  sx={{ typography: "body2", fontWeight: 500 }}
                >
                  Open Review Queue
                </Link>
              }
            >
              {reviewQueueCount} {reviewQueueCount === 1 ? "review item is" : "review items are"} linked to this customer.
            </Alert>
          ) : null}

          <WorkspaceToolbar
            searchPlaceholder={tabMeta.searchPlaceholder}
            searchValue={toolbarQuery}
            onSearchChange={setToolbarQuery}
            onSearchKeyDown={handleToolbarKeyDown}
            filters={(
              <Stack
                direction={{ xs: "column", sm: "row" }}
                spacing={0.75}
                sx={{ width: { xs: "100%", lg: "auto" } }}
              >
                {currentTabQuery ? (
                  <Button
                    variant="text"
                    size="small"
                    color="inherit"
                    onClick={clearToolbarSearch}
                    sx={{ width: { xs: "100%", sm: "auto" } }}
                  >
                    Clear
                  </Button>
                ) : null}
              </Stack>
            )}
            activeFilters={activeFilters}
            primaryAction={(
              <Button
                variant="contained"
                size="small"
                onClick={applyToolbarSearch}
                startIcon={<SearchOutlinedIcon fontSize="small" />}
              >
                Search
              </Button>
            )}
            onRefresh={() => {
              void clientQuery.refetch();
              void notesQuery.refetch();
              void documentsQuery.refetch();
              void emailsQuery.refetch();
              void timelineQuery.refetch();
              void relatedKnowledgeQuery.refetch();
              if (isDemoDataEnabled) {
                void workspaceQuery.refetch();
              }
            }}
          />

          {sortedRows.length === 0 ? (
            <Box sx={{ pt: 0.25 }}>
              <EmptyState
                title={tabMeta.emptyTitle}
                message={tabMeta.emptyMessage}
                compact
              />
            </Box>
          ) : isMobile ? (
            <MobileCollectionList
              items={mobileItems}
              selectedId={selectedRowId}
              onSelect={handleRowSelect}
            />
          ) : (
            <EntityGrid<Record<string, unknown>>
              rows={sortedRows}
              columns={activeColumns}
              getRowId={(row) => String(row.id)}
              onRowClick={({ row }) => handleRowSelect(String(row.id))}
              sortModel={sortModel}
              onSortModelChange={handleSortModelChange}
              disableRowSelectionOnClick
              getRowClassName={({ row }) => (String(row.id) === selectedRowId ? "ikms-selected-row" : "")}
              sx={{
                minHeight: Math.min(Math.max(236, 112 + Math.max(2, sortedRows.length) * 44), 680),
                height: Math.min(Math.max(236, 112 + Math.max(2, sortedRows.length) * 44), 680),
                "& .ikms-selected-row": {
                  backgroundColor: theme.palette.action.selected,
                },
                "& .MuiDataGrid-cell:focus, & .MuiDataGrid-columnHeader:focus": {
                  outline: `2px solid ${theme.palette.primary.main}`,
                  outlineOffset: -2,
                },
              }}
            />
          )}

          {!isMobile && isTabletDown && selectedRow ? (
            <InlineSelectionPanel activeTab={activeTab} row={selectedRow} />
          ) : null}
        </Stack>
      </Box>

      {hasAiAssistant ? (
        <Accordion disableGutters sx={{ border: (appTheme) => `1px solid ${appTheme.palette.divider}` }}>
          <AccordionSummary
            expandIcon={<ExpandMoreOutlinedIcon fontSize="small" />}
            aria-controls="customer360-ai-content"
            id="customer360-ai-header"
            sx={{ minHeight: 44, "& .MuiAccordionSummary-content": { my: 0.75 } }}
          >
            <Stack spacing={0.25}>
              <Typography variant="subtitle2">Evidence Assistant</Typography>
              <Typography variant="body2" color="text.secondary">
                Search linked evidence and ask evidence-based questions when you need more context.
              </Typography>
            </Stack>
          </AccordionSummary>
          <AccordionDetails sx={{ pt: 0 }}>
            <ClientSearchPanel
              clientId={clientId}
              compact
              suggestedPrompts={assistantSuggestedPrompts}
              sourceReferencesOverride={assistantSourceReferences}
              evidenceReferencesOverride={assistantEvidenceReferences}
            />
          </AccordionDetails>
        </Accordion>
      ) : null}

      <Drawer
        anchor="right"
        open={isMobile && Boolean(selectedRow)}
        onClose={() => handleRowSelect("")}
        PaperProps={{ sx: { width: "100%", maxWidth: "100%" } }}
      >
        <Stack spacing={0} sx={{ height: "100%" }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ px: 2, py: 1.25, borderBottom: (theme) => `1px solid ${theme.palette.divider}` }}>
            <Typography variant="subtitle2">Selected record</Typography>
            <IconButton aria-label="Close detail" onClick={() => handleRowSelect("")}>
              <CloseOutlinedIcon fontSize="small" />
            </IconButton>
          </Stack>
          <Box sx={{ overflowY: "auto", px: 2, py: 1.5 }}>
            {selectedRow ? <SelectionDetail activeTab={activeTab} row={selectedRow} /> : null}
          </Box>
        </Stack>
      </Drawer>

      <Drawer
        anchor="right"
        open={noteDrawerOpen}
        onClose={closeNoteDrawer}
        PaperProps={{
          sx: {
            width: { xs: "100%", sm: 440 },
            maxWidth: "100%",
            display: "grid",
            gridTemplateRows: "auto minmax(0, 1fr) auto",
          },
        }}
      >
        <Stack spacing={0.5} sx={{ px: 2, py: 1.25, borderBottom: (theme) => `1px solid ${theme.palette.divider}` }}>
          <Stack spacing={0.5}>
            <Typography variant="h3">{editingNoteId ? "Edit note" : "Add note"}</Typography>
            <Typography variant="body2" color="text.secondary">
              Capture concise customer context for operational follow-up.
            </Typography>
          </Stack>
        </Stack>
        <Box sx={{ overflowY: "auto", px: 2, py: 2 }}>
          <Stack spacing={2}>
            <TextField
              label="Note"
              multiline
              minRows={8}
              value={noteDraft}
              onChange={(event) => setNoteDraft(event.target.value)}
              placeholder="Add a customer note"
              fullWidth
            />
          </Stack>
        </Box>
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ px: 2, py: 1.25, borderTop: (theme) => `1px solid ${theme.palette.divider}` }}>
          <Typography variant="caption" color="text.secondary">
            {noteDraft.trim() ? "Ready to save" : "No note content yet"}
          </Typography>
          <Stack direction="row" spacing={1}>
            <Button variant="text" color="inherit" onClick={closeNoteDrawer}>
              Cancel
            </Button>
            <Button
              variant="contained"
              onClick={handleNoteSubmit}
              disabled={createNoteMutation.isPending || updateNoteMutation.isPending || !noteDraft.trim()}
            >
              {editingNoteId ? "Save note" : "Create note"}
            </Button>
          </Stack>
        </Stack>
      </Drawer>

      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle>Delete note</DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" color="text.secondary">
            This note will be removed from the customer record.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>Cancel</Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => {
              if (editingNoteId) {
                deleteNoteMutation.mutate(editingNoteId);
              }
            }}
            disabled={deleteNoteMutation.isPending}
          >
            Delete
          </Button>
        </DialogActions>
      </Dialog>

      {viewerDocumentConfig ? (
        <DocumentViewerDialog
          open={Boolean(viewerDocumentConfig)}
          onClose={() => setViewerDocumentId(null)}
          document={viewerDocumentConfig}
          state="ready"
          evidenceSections={viewerEvidenceSections}
          layers={documentViewerPlaceholderLayers()}
        />
      ) : null}
    </Stack>
  );
}

function CustomerSummaryPanel({
  client,
  lastActivity,
}: {
  client: Awaited<ReturnType<typeof getClient>>;
  lastActivity: string;
}) {
  const items = [
    { label: "Customer ID", value: `${client.clientId}${client.clientIdTemporary ? " (Temporary)" : ""}` },
    { label: "Customer Type", value: client.clientType },
    { label: "Status", value: client.status },
    { label: "Primary Contact", value: client.contactPerson ?? "Not available" },
    { label: "Email", value: client.primaryEmail ?? "Not available" },
    { label: "Phone", value: client.primaryPhone ?? "Not available" },
    { label: "Last Activity", value: lastActivity },
    { label: "Last Updated", value: formatDateTime(client.updatedAt) },
  ];

  return (
    <Box
      sx={{
        border: (theme) => `1px solid ${theme.palette.divider}`,
        borderRadius: 1,
        backgroundColor: "background.paper",
        px: 1.5,
        py: 0.875,
      }}
    >
      <Stack spacing={0.75}>
        <Typography variant="subtitle2" sx={{ lineHeight: 1.2 }}>
          Customer Summary
        </Typography>
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: {
              xs: "repeat(2, minmax(0, 1fr))",
              md: "repeat(4, minmax(0, 1fr))",
            },
            columnGap: 1,
            rowGap: 0.75,
          }}
        >
          {items.map((item) => (
            <Box key={item.label} sx={{ minWidth: 0 }}>
              <Typography variant="caption" color="text.secondary" sx={{ display: "block", lineHeight: 1.1 }}>
                {item.label}
              </Typography>
              <Typography variant="body2" sx={{ mt: 0.125, lineHeight: 1.25 }} noWrap>
                {item.value}
              </Typography>
            </Box>
          ))}
        </Box>
      </Stack>
    </Box>
  );
}

function MobileCollectionList({
  items,
  selectedId,
  onSelect,
}: {
  items: MobileListItem[];
  selectedId: string | null;
  onSelect: (rowId: string) => void;
}) {
  return (
    <List disablePadding sx={{ display: "grid", gap: 1 }}>
      {items.map((item) => (
        <Box
          key={item.id}
          sx={{
            border: (theme) => `1px solid ${theme.palette.divider}`,
            borderRadius: 1,
            backgroundColor: item.id === selectedId ? "action.selected" : "background.paper",
            overflow: "hidden",
          }}
        >
          <ListItemButton onClick={() => onSelect(item.id)} alignItems="flex-start">
            <ListItemText
              primary={(
                <Stack spacing={1}>
                  <Stack direction="row" justifyContent="space-between" spacing={1} alignItems="flex-start">
                    <Typography variant="body2" fontWeight={600}>
                      {item.title}
                    </Typography>
                    {item.status ? <StatusBadge label={item.status.label} tone={item.status.tone} /> : null}
                  </Stack>
                  <Typography variant="body2" color="text.secondary">
                    {item.subtitle}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {item.meta}
                  </Typography>
                </Stack>
              )}
            />
          </ListItemButton>
          {item.actions && item.actions.length > 0 ? (
            <>
              <Divider />
              <Stack direction="row" spacing={1} sx={{ px: 2, py: 1 }} flexWrap="wrap">
                {item.actions.map((action) => (
                  <Button
                    key={action.key}
                    size="small"
                    startIcon={action.icon}
                    component={action.href ? "a" : "button"}
                    href={action.href}
                    target={action.href ? "_blank" : undefined}
                    rel={action.href ? "noreferrer" : undefined}
                    onClick={action.onClick}
                  >
                    {action.label}
                  </Button>
                ))}
              </Stack>
            </>
          ) : null}
        </Box>
      ))}
    </List>
  );
}

function InlineSelectionPanel({ activeTab, row }: { activeTab: CustomerTab; row: Record<string, unknown> }) {
  return (
    <Box
      sx={{
        border: (theme) => `1px solid ${theme.palette.divider}`,
        borderRadius: 1,
        backgroundColor: "background.paper",
        p: 1.5,
      }}
    >
      <Typography variant="subtitle2" gutterBottom>
        Selected record
      </Typography>
      <SelectionDetail activeTab={activeTab} row={row} />
    </Box>
  );
}

function SelectionDetail({ activeTab, row }: { activeTab: CustomerTab; row: Record<string, unknown> }) {
  const details = describeSelection(activeTab, row);

  return (
    <Stack spacing={1}>
      <Typography variant="body2" fontWeight={600}>
        {details.title}
      </Typography>
      {details.lines.map((line) => (
        <Typography key={line} variant="body2" color="text.secondary">
          {line}
        </Typography>
      ))}
    </Stack>
  );
}

function buildColumns({
  activeTab,
  isTabletDown,
  onOpenDocumentViewer,
  onEditNote,
  onDeleteNote,
}: {
  activeTab: CustomerTab;
  isTabletDown: boolean;
  onOpenDocumentViewer: (documentId: string) => void;
  onEditNote: (noteId: string) => void;
  onDeleteNote: (noteId: string) => void;
}): GridColDef<Record<string, unknown>>[] {
  switch (activeTab) {
    case "documents":
      return [
        {
          field: "title",
          headerName: "Document",
          minWidth: 260,
          flex: 1.2,
          renderCell: ({ row }) => (
            <Stack spacing={0.25} sx={{ py: 0.5 }}>
              <Typography variant="body2" fontWeight={600} noWrap>
                {String(row.title)}
              </Typography>
              <Typography variant="caption" color="text.secondary" noWrap>
                {String(row.category)} · {String(row.version)}
              </Typography>
            </Stack>
          ),
        },
        { field: "category", headerName: "Category", minWidth: 150, flex: isTabletDown ? 0 : 0.75 },
        { field: "version", headerName: "Version", width: 100 },
        { field: "modified", headerName: "Modified", minWidth: 150, flex: 0.75 },
        { field: "owner", headerName: "Owner", minWidth: 140, flex: isTabletDown ? 0 : 0.7 },
        {
          field: "status",
          headerName: "Status",
          width: 140,
          renderCell: ({ row }) => <StatusBadge label={String(row.status)} tone={row.statusTone as StatusTone} />,
        },
        {
          field: "actions",
          headerName: "Actions",
          width: 120,
          sortable: false,
          filterable: false,
          renderCell: ({ row }) => (
            <Stack direction="row" spacing={0.5}>
              <Tooltip title={`Preview ${String(row.permissionLabel).toLowerCase()}`}>
                <IconButton
                  size="small"
                  aria-label={`Preview ${String(row.title)}`}
                  onClick={() => onOpenDocumentViewer(String(row.id))}
                >
                  <VisibilityOutlinedIcon fontSize="small" />
                </IconButton>
              </Tooltip>
              <Tooltip title={`Download ${String(row.permissionLabel).toLowerCase()}`}>
                <IconButton
                  size="small"
                  aria-label={`Download ${String(row.title)}`}
                  component="a"
                  href={String(row.downloadHref)}
                  target="_blank"
                  rel="noreferrer"
                >
                  <DownloadOutlinedIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </Stack>
          ),
        },
      ];
    case "emails":
      return [
        {
          field: "subject",
          headerName: "Subject",
          minWidth: 280,
          flex: 1.3,
          renderCell: ({ row }) => (
            <Stack spacing={0.25} sx={{ py: 0.5 }}>
              <Typography variant="body2" fontWeight={600} noWrap>
                {String(row.subject)}
              </Typography>
              <Typography variant="caption" color="text.secondary" noWrap>
                {String(row.recipients)}
              </Typography>
            </Stack>
          ),
        },
        { field: "sender", headerName: "Sender", minWidth: 220, flex: 1 },
        { field: "date", headerName: "Date", minWidth: 150, flex: 0.8 },
        { field: "attachment", headerName: "Attachment", width: 120 },
        {
          field: "status",
          headerName: "Status",
          width: 140,
          renderCell: ({ row }) => <StatusBadge label={String(row.status)} tone={row.statusTone as StatusTone} />,
        },
      ];
    case "notes":
      return [
        {
          field: "title",
          headerName: "Title",
          minWidth: 260,
          flex: 1.2,
          renderCell: ({ row }) => (
            <Stack spacing={0.25} sx={{ py: 0.5 }}>
              <Typography variant="body2" fontWeight={600} noWrap>
                {String(row.title)}
              </Typography>
              <Typography variant="caption" color="text.secondary" noWrap>
                {String(row.text)}
              </Typography>
            </Stack>
          ),
        },
        { field: "author", headerName: "Author", minWidth: 160, flex: 0.8 },
        { field: "created", headerName: "Created", minWidth: 150, flex: 0.8 },
        { field: "updated", headerName: "Updated", minWidth: 150, flex: 0.8 },
        {
          field: "actions",
          headerName: "Actions",
          width: 110,
          sortable: false,
          filterable: false,
          renderCell: ({ row }) => (
            <Stack direction="row" spacing={0.5}>
              <Tooltip title="Edit note">
                <IconButton size="small" aria-label="Edit note" onClick={() => onEditNote(String(row.id))}>
                  <EditOutlinedIcon fontSize="small" />
                </IconButton>
              </Tooltip>
              <Tooltip title="Delete note">
                <IconButton size="small" aria-label="Delete note" onClick={() => onDeleteNote(String(row.id))}>
                  <DeleteOutlineOutlinedIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </Stack>
          ),
        },
      ];
    case "relationships":
      return [
        { field: "relatedCustomer", headerName: "Related Customer", minWidth: 240, flex: 1.1 },
        { field: "relationship", headerName: "Relationship", minWidth: 180, flex: 0.8 },
        { field: "source", headerName: "Source", minWidth: 160, flex: 0.7 },
        { field: "status", headerName: "Status", minWidth: 120, flex: 0.5 },
      ];
    case "policies":
      return [
        { field: "policyNumber", headerName: "Policy Number", minWidth: 180, flex: 0.9 },
        { field: "product", headerName: "Product", minWidth: 200, flex: 1 },
        { field: "insurer", headerName: "Insurer", minWidth: 200, flex: 1 },
        { field: "status", headerName: "Status", minWidth: 130, flex: 0.7 },
        { field: "expiry", headerName: "Expiry", minWidth: 140, flex: 0.7 },
      ];
    case "claims":
      return [
        { field: "claimNumber", headerName: "Claim Number", minWidth: 180, flex: 0.9 },
        { field: "carrier", headerName: "Carrier", minWidth: 180, flex: 0.9 },
        { field: "status", headerName: "Status", minWidth: 130, flex: 0.6 },
        { field: "opened", headerName: "Opened", minWidth: 140, flex: 0.7 },
        { field: "updated", headerName: "Updated", minWidth: 140, flex: 0.7 },
      ];
    case "timeline":
      return [
        {
          field: "event",
          headerName: "Event",
          minWidth: 260,
          flex: 1.1,
          renderCell: ({ row }) => (
            <Stack spacing={0.25} sx={{ py: 0.5 }}>
              <Typography variant="body2" fontWeight={600} noWrap>
                {String(row.event)}
              </Typography>
              <Typography variant="caption" color="text.secondary" noWrap>
                {String(row.detail)}
              </Typography>
            </Stack>
          ),
        },
        { field: "type", headerName: "Type", minWidth: 130, flex: 0.6 },
        { field: "occurredAt", headerName: "Occurred", minWidth: 160, flex: 0.8 },
        { field: "actor", headerName: "Actor", minWidth: 140, flex: 0.7 },
        {
          field: "status",
          headerName: "Status",
          width: 130,
          renderCell: ({ row }) => <StatusBadge label={String(row.status)} tone={row.statusTone as StatusTone} />,
        },
      ];
  }
}

function buildMobileItems({
  activeTab,
  rows,
  onOpenDocumentViewer,
  onOpenNote,
  onDeleteNote,
}: {
  activeTab: CustomerTab;
  rows: Record<string, unknown>[];
  onOpenDocumentViewer: (documentId: string) => void;
  onOpenNote: (noteId: string) => void;
  onDeleteNote: (noteId: string) => void;
}): MobileListItem[] {
  return rows.map((row) => {
    switch (activeTab) {
      case "documents":
        return {
          id: String(row.id),
          title: String(row.title),
          subtitle: `${String(row.category)} · ${String(row.version)}`,
          meta: String(row.modified),
          status: { label: String(row.status), tone: row.statusTone as StatusTone },
          actions: [
            { key: "preview", label: "Preview", icon: <VisibilityOutlinedIcon fontSize="small" />, onClick: () => onOpenDocumentViewer(String(row.id)) },
            { key: "download", label: "Download", icon: <DownloadOutlinedIcon fontSize="small" />, href: String(row.downloadHref) },
          ],
        };
      case "emails":
        return {
          id: String(row.id),
          title: String(row.subject),
          subtitle: String(row.sender),
          meta: `${String(row.date)} · ${String(row.attachment)}`,
          status: { label: String(row.status), tone: row.statusTone as StatusTone },
        };
      case "notes":
        return {
          id: String(row.id),
          title: String(row.title),
          subtitle: String(row.text),
          meta: `${String(row.updated)} · ${String(row.author)}`,
          actions: [
            { key: "edit", label: "Edit note", icon: <EditOutlinedIcon fontSize="small" />, onClick: () => onOpenNote(String(row.id)) },
            { key: "delete", label: "Delete note", icon: <DeleteOutlineOutlinedIcon fontSize="small" />, onClick: () => onDeleteNote(String(row.id)) },
          ],
        };
      case "relationships":
        return {
          id: String(row.id),
          title: String(row.relatedCustomer),
          subtitle: String(row.relationship),
          meta: `${String(row.source)} · ${String(row.status)} · ${String(row.explanation ?? "")}`,
        };
      case "policies":
        return {
          id: String(row.id),
          title: String(row.policyNumber),
          subtitle: String(row.product),
          meta: `${String(row.insurer)} · ${String(row.expiry)}`,
          status: { label: String(row.status), tone: "info" },
        };
      case "claims":
        return {
          id: String(row.id),
          title: String(row.claimNumber),
          subtitle: String(row.carrier),
          meta: `${String(row.opened)} · ${String(row.updated)}`,
          status: { label: String(row.status), tone: "warning" },
        };
      case "timeline":
        return {
          id: String(row.id),
          title: String(row.event),
          subtitle: String(row.detail),
          meta: `${String(row.occurredAt)} · ${String(row.actor)}${row.businessReferences ? ` · ${String(row.businessReferences)}` : ""}`,
          status: { label: String(row.status), tone: row.statusTone as StatusTone },
        };
    }
  });
}

function buildContextSections({
  workspace,
  activeTab,
  selectedRow,
  reviewQueueCount,
  onOpenCustomerList,
}: {
  workspace: DemoClientWorkspace | undefined;
  activeTab: CustomerTab;
  selectedRow: Record<string, unknown> | null;
  reviewQueueCount: number;
  onOpenCustomerList: () => void;
}): ContextSection[] {
  const sections: ContextSection[] = [];

  if (selectedRow) {
    const details = describeSelection(activeTab, selectedRow);
    sections.push({
      key: "selected-record",
      title: `${getTabMeta(activeTab).label} Record`,
      content: (
        <Stack spacing={0.5}>
          <Typography variant="body2" fontWeight={600}>
            {details.title}
          </Typography>
          {details.lines.map((line) => (
            <Typography key={line} variant="body2" color="text.secondary">
              {line}
            </Typography>
          ))}
        </Stack>
      ),
    });
  }

  if (workspace?.aiSummaries.length) {
    sections.push({
      key: "ai-brief",
      title: "AI Customer Brief",
      content: (
        <Stack spacing={0.5}>
          <Typography variant="body2" color="text.secondary">
            {workspace.aiSummaries[0].summary}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Confidence: {workspace.aiSummaries[0].confidence}
          </Typography>
        </Stack>
      ),
    });
  }

  if (reviewQueueCount > 0) {
    sections.push({
      key: "alerts",
      title: "Alerts",
      content: (
        <Stack spacing={0.75}>
          <Typography variant="body2" color="text.secondary">
            {reviewQueueCount} {reviewQueueCount === 1 ? "review item is" : "review items are"} linked to this customer.
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Open the Review workspace to process linked queue items.
          </Typography>
        </Stack>
      ),
    });
  }

  sections.push({
    key: "quick-actions",
    title: "Quick Actions",
    content: (
      <Stack spacing={0.75}>
        <Button variant="text" color="inherit" size="small" onClick={onOpenCustomerList}>
          Customer list
        </Button>
      </Stack>
    ),
  });

  return sections;
}

function parseTab(value: string | null): CustomerTab {
  return tabs.some((tab) => tab.key === value) ? (value as CustomerTab) : "documents";
}

function shortcutToTab(key: string): CustomerTab | null {
  switch (key) {
    case "1":
      return "documents";
    case "2":
      return "emails";
    case "3":
      return "notes";
    case "4":
      return "relationships";
    case "5":
      return "policies";
    case "6":
      return "claims";
    case "7":
      return "timeline";
    default:
      return null;
  }
}

function tabParam(tab: CustomerTab, suffix: string) {
  return `${tab}-${suffix}`;
}

function setScopedSearchParams(
  setSearchParams: ReturnType<typeof useSearchParams>[1],
  currentParams: URLSearchParams,
  nextValues: Record<string, string | null>,
) {
  const next = new URLSearchParams(currentParams);
  Object.entries(nextValues).forEach(([key, value]) => {
    if (!value) {
      next.delete(key);
    } else {
      next.set(key, value);
    }
  });
  setSearchParams(next, { replace: true });
}

function getTabMeta(tab: CustomerTab) {
  switch (tab) {
    case "documents":
      return {
        label: "Documents",
        searchPlaceholder: "Filter documents by title, source, status, or date",
        emptyTitle: "No documents linked",
        emptyMessage: "No client documents are currently available.",
      };
    case "emails":
      return {
        label: "Emails",
        searchPlaceholder: "Filter emails by subject, sender, recipient, or status",
        emptyTitle: "No emails linked",
        emptyMessage: "No client emails are currently available.",
      };
    case "notes":
      return {
        label: "Notes",
        searchPlaceholder: "Filter notes by title, text, or date",
        emptyTitle: "No notes recorded",
        emptyMessage: "Use Add note to capture operational context.",
      };
    case "relationships":
      return {
        label: "Relationships",
        searchPlaceholder: "Filter relationships",
        emptyTitle: "No relationships available",
        emptyMessage: "No related customers or linked parties are currently available for this customer.",
      };
    case "policies":
      return {
        label: "Policy References",
        searchPlaceholder: "Filter policies by number, insurer, product, or status",
        emptyTitle: "No policies linked",
        emptyMessage: "No policy references are available for this customer.",
      };
    case "claims":
      return {
        label: "Claim References",
        searchPlaceholder: "Filter claim references by number, policy reference, or status",
        emptyTitle: "No claim references linked",
        emptyMessage: "No claim references are available for this customer.",
      };
    case "timeline":
      return {
        label: "Timeline",
        searchPlaceholder: "Filter timeline by event, actor, or detail",
        emptyTitle: "No timeline activity",
        emptyMessage: "Timeline events will appear when operational activity is available.",
      };
  }
}

function mapDocumentRows(documents: ClientDocumentSummary[], permissions: string[]): DocumentRow[] {
  const canViewOriginal = permissions.includes("VIEW_ORIGINAL_DOCUMENTS") && permissions.includes("VIEW_PII");
  const canViewRedacted = permissions.includes("VIEW_REDACTED_DOCUMENTS");

  return documents.map((document) => {
    const permissionLabel = document.containsPii && !canViewOriginal ? "redacted copy" : "document";

    return {
      id: document.id,
      title: document.title,
      category: document.source,
      version: document.currentVersionId ?? "Current",
      modified: formatDateTime(document.createdAt),
      owner: document.parentEmailId ? "Email import" : "Customer intake",
      status: document.reviewStatus,
      statusTone: mapOperationalTone(document.reviewStatus),
      previewHref: `/api/documents/${document.id}/preview`,
      downloadHref: `/api/documents/${document.id}/download`,
      permissionLabel:
        document.containsPii && canViewOriginal
          ? "original document"
          : document.containsPii && canViewRedacted
            ? "redacted document"
            : permissionLabel,
    };
  });
}

function mapEmailRows(emails: ClientEmailSummary[]): EmailRow[] {
  return emails.map((email) => ({
    id: email.id,
    subject: email.subject,
    sender: email.sender,
    date: formatDateTime(email.receivedAt),
    attachment: email.recipients.includes("@") ? "Available" : "None",
    status: email.reviewStatus,
    statusTone: mapOperationalTone(email.reviewStatus),
    recipients: email.recipients,
  }));
}

function mapNoteRows(notes: Note[], currentUserName: string): NoteRow[] {
  return notes.map((note, index) => ({
    id: note.id,
    title: `Note ${index + 1}`,
    author: currentUserName,
    created: formatDateTime(note.createdAt),
    updated: formatDateTime(note.updatedAt),
    text: note.noteText,
  }));
}

function mapPolicyRows(workspace: DemoClientWorkspace | undefined): PolicyReferenceRow[] {
  return (workspace?.policyReferences ?? []).map((policy) => ({
    id: policy.id,
    policyNumber: policy.policyNumber,
    product: policy.lineOfBusiness,
    insurer: policy.carrier,
    status: policy.status,
    expiry: policy.expirationDate,
    summary: policy.summary,
  }));
}

function mapClaimRows(workspace: DemoClientWorkspace | undefined): ClaimReferenceRow[] {
  return (workspace?.claimReferences ?? []).map((claim) => ({
    id: claim.id,
    claimNumber: claim.claimNumber,
    carrier: claim.carrier,
    status: claim.status,
    opened: claim.reportedDate,
    updated: claim.lossDate,
    summary: claim.summary,
  }));
}

function mapRelationshipRows(links: RelatedKnowledgeLink[]): RelationshipRow[] {
  return links.map((link) => ({
    id: link.relationshipId,
    relatedCustomer: link.relatedTitle,
    relationship: humanizeRelationshipType(link.relationshipType),
    source: link.sourceTitle,
    status: link.inferred ? "Inferred" : "Deterministic",
    explanation: link.explanation,
    score: link.score == null ? "N/A" : link.score.toFixed(2),
    derivationType: link.derivationType,
  }));
}

function mapKnowledgeTimelineRows(events: CustomerKnowledgeTimelineEvent[]): TimelineRow[] {
  return events.map((event) => ({
    id: event.eventId,
    event: event.title,
    type: humanizeTimelineEventType(event.eventType),
    occurredAt: formatDateTime(event.occurredAt ?? event.recordedAt),
    actor: event.actor ?? "System",
    detail: event.summary,
    status: event.status,
    statusTone: mapOperationalTone(event.status),
    sourceType: event.sourceType,
    businessReferences: event.businessReferenceFields.map((field) => `${field.label}: ${field.value}`).join(" · "),
  }));
}

function mapTimelineRows({
  notes,
  documents,
  emails,
  workspace,
}: {
  notes: Note[];
  documents: ClientDocumentSummary[];
  emails: ClientEmailSummary[];
  workspace: DemoClientWorkspace | undefined;
}): TimelineRow[] {
  const derived: TimelineRow[] = [
    ...documents.map((document) => ({
      id: `document-${document.id}`,
      event: document.title,
      type: "Document Added",
      occurredAt: formatDateTime(document.createdAt),
      actor: "System",
      detail: `${document.source} · ${document.reviewStatus}`,
      status: document.reviewStatus,
      statusTone: mapOperationalTone(document.reviewStatus),
    })),
    ...emails.map((email) => ({
      id: `email-${email.id}`,
      event: email.subject,
      type: "Email Imported",
      occurredAt: formatDateTime(email.receivedAt),
      actor: email.sender,
      detail: `${email.reviewStatus} · ${email.processingStatus}`,
      status: email.reviewStatus,
      statusTone: mapOperationalTone(email.reviewStatus),
    })),
    ...notes.map((note) => ({
      id: `note-${note.id}`,
      event: "Customer note updated",
      type: "Note Created",
      occurredAt: formatDateTime(note.updatedAt),
      actor: "Current user",
      detail: note.noteText,
      status: note.status,
      statusTone: (note.status === "ACTIVE" ? "success" : "neutral") as StatusTone,
    })),
  ];

  const demoEvents = [
    ...(workspace?.recentActivity ?? []).map((activity) => ({
      id: `activity-${activity.id}`,
      event: activity.title,
      type: activity.type,
      occurredAt: formatDateTime(activity.occurredAt),
      actor: activity.actor,
      detail: activity.description,
      status: "Recorded",
      statusTone: "info" as StatusTone,
    })),
    ...(workspace?.auditEvents ?? []).map((event) => ({
      id: `audit-${event.id}`,
      event: event.action,
      type: event.category,
      occurredAt: formatDateTime(event.occurredAt),
      actor: event.actorUsername ?? "System",
      detail: event.outcome,
      status: event.outcome,
      statusTone: mapAuditOutcomeTone(event.outcome),
    })),
  ];

  return [...derived, ...demoEvents].sort((left, right) => right.occurredAt.localeCompare(left.occurredAt));
}

function filterRows(tab: CustomerTab, rows: Record<string, unknown>[], query: string) {
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return rows;
  }

  return rows.filter((row) =>
    Object.values(selectSearchableFields(tab, row))
      .join(" ")
      .toLowerCase()
      .includes(normalized),
  );
}

function selectSearchableFields(tab: CustomerTab, row: Record<string, unknown>) {
  switch (tab) {
    case "documents":
      return { title: row.title, category: row.category, status: row.status, modified: row.modified };
    case "emails":
      return { subject: row.subject, sender: row.sender, recipients: row.recipients, status: row.status };
    case "notes":
      return { title: row.title, text: row.text, updated: row.updated };
    case "relationships":
      return {
        relatedCustomer: row.relatedCustomer,
        relationship: row.relationship,
        source: row.source,
        explanation: row.explanation,
        derivationType: row.derivationType,
      };
    case "policies":
      return { policyNumber: row.policyNumber, product: row.product, insurer: row.insurer, status: row.status };
    case "claims":
      return { claimNumber: row.claimNumber, carrier: row.carrier, status: row.status, summary: row.summary };
    case "timeline":
      return { event: row.event, detail: row.detail, actor: row.actor, type: row.type, references: row.businessReferences };
  }
}

function sortRows(rows: Record<string, unknown>[], sortModel: GridSortModel) {
  const sort = sortModel[0];
  if (!sort?.field || !sort.sort) {
    return rows;
  }

  return [...rows].sort((left, right) => {
    const leftValue = String(left[sort.field] ?? "").toLowerCase();
    const rightValue = String(right[sort.field] ?? "").toLowerCase();
    const result = leftValue.localeCompare(rightValue);
    return sort.sort === "asc" ? result : -result;
  });
}

function describeSelection(activeTab: CustomerTab, row: Record<string, unknown>) {
  switch (activeTab) {
    case "documents":
      return {
        title: String(row.title),
        lines: [
          `${String(row.category)} · ${String(row.status)}`,
          `Modified ${String(row.modified)} · Owner ${String(row.owner)}`,
        ],
      };
    case "emails":
      return {
        title: String(row.subject),
        lines: [
          `${String(row.sender)} · ${String(row.status)}`,
          `${String(row.recipients)} · ${String(row.date)}`,
        ],
      };
    case "notes":
      return {
        title: String(row.title),
        lines: [String(row.text), `${String(row.updated)} · ${String(row.author)}`],
      };
    case "relationships":
      return {
        title: String(row.relatedCustomer),
        lines: [
          `${String(row.relationship)} · ${String(row.source)}`,
          `${String(row.status)} · ${String(row.derivationType ?? "")}`,
          String(row.explanation ?? ""),
        ],
      };
    case "policies":
      return {
        title: String(row.policyNumber),
        lines: [`${String(row.product)} · ${String(row.insurer)}`, `${String(row.status)} · Expires ${String(row.expiry)}`],
      };
    case "claims":
      return {
        title: String(row.claimNumber),
        lines: [`${String(row.carrier)} · ${String(row.status)}`, `Opened ${String(row.opened)} · Updated ${String(row.updated)}`],
      };
    case "timeline":
      return {
        title: String(row.event),
        lines: [
          `${String(row.type)} · ${String(row.actor)}`,
          `${String(row.occurredAt)} · ${String(row.detail)}`,
          String(row.businessReferences ?? ""),
        ],
      };
  }
}

function buildCustomerViewerDocument({
  client,
  document,
  workspace,
  permissions,
}: {
  client: Awaited<ReturnType<typeof getClient>>;
  document: ClientDocumentSummary;
  workspace: DemoClientWorkspace | undefined;
  permissions: string[];
}) {
  const canViewOriginal = permissions.includes("VIEW_ORIGINAL_DOCUMENTS") && permissions.includes("VIEW_PII");

  return {
    id: document.id,
    title: document.title,
    subtitle: `${client.displayName} · ${document.source}`,
    fileKind: "pdf" as const,
    previewUrl: `/api/documents/${document.id}/preview`,
    downloadUrl: `/api/documents/${document.id}/download`,
    originalUrl: `/api/documents/${document.id}/preview`,
    originalActionLabel: canViewOriginal ? "Open original" : "Open document",
    pages: [{ id: `${document.id}-page-1`, label: "Page 1", pageNumber: 1 }],
    isLargeFile: workspace ? workspace.aiSummaries.length + workspace.policyReferences.length > 6 : false,
  };
}

function buildCustomerViewerEvidenceSections({
  client,
  document,
  workspace,
  relatedLinks,
  versions,
}: {
  client: Awaited<ReturnType<typeof getClient>>;
  document: ClientDocumentSummary;
  workspace: DemoClientWorkspace | undefined;
  relatedLinks: RelatedKnowledgeLink[];
  versions: DocumentVersionSummary[];
}): EvidenceWorkspaceSection[] {
  const groupedReferences = collectBusinessReferenceFields(relatedLinks);
  const versionSummary = versions.length > 0
    ? versions.map((version) => `v${version.versionNumber}${version.current ? " (current)" : ""} · ${formatDateTime(version.createdAt)}`)
    : ["Version history is not available from the current API."];
  const relatedDocuments = relatedLinks.filter((link) => link.relatedSourceType === "DOCUMENT" || link.relatedSourceType === "DOCUMENT_VERSION");
  const relatedEmails = relatedLinks.filter((link) => link.relatedSourceType === "EMAIL" || link.sourceType === "EMAIL");

  return [
    {
      key: "ai-summary",
      title: "AI Summary",
      defaultExpanded: true,
      summary: workspace?.aiSummaries[0] ? "Customer AI summary available" : "No AI summary payload",
      searchText: workspace?.aiSummaries[0]?.summary,
      content: (
        <AISummary
          state={workspace?.aiSummaries[0] ? "ready" : "unavailable"}
          summary={workspace?.aiSummaries[0]?.summary}
          confidence={normalizeConfidence(workspace?.aiSummaries[0]?.confidence)}
          supportingNotes={workspace?.aiSummaries[0]?.evidence ?? []}
        />
      ),
    },
    {
      key: "evidence",
      title: "Evidence",
      defaultExpanded: true,
      summary: "Customer-linked document evidence",
      countLabel: "1 item",
      searchText: `${document.title} ${document.source} ${document.processingStatus} ${document.reviewStatus}`,
      content: (
        <EvidenceCard
          title={document.title}
          excerpt={`${document.source} document linked to ${client.displayName}.`}
          citation={`${document.title} · ${formatDateTime(document.createdAt)}`}
          source={document.source}
          type="Document"
          confidence={document.reviewStatus === "APPROVED" ? "HIGH" : "MEDIUM"}
          metadata={[
            `Processing: ${document.processingStatus}`,
            `Review: ${document.reviewStatus}`,
            `Redaction: ${document.redactionStatus}`,
          ]}
          navigationActions={[
            { key: "doc-page", label: "Jump to page", kind: "page", disabled: true },
            { key: "doc-highlight", label: "Jump to highlight", kind: "highlight", disabled: true },
          ]}
        />
      ),
    },
    {
      key: "fields",
      title: "Extracted Fields",
      summary: "Current APIs do not expose document field extraction here",
      content: (
        <EvidenceMetadataGroup
          title="Field availability"
          fields={[
            {
              key: "field-availability",
              label: "Extracted fields",
              value: "Not exposed in the current Customer360 document API.",
              state: "READ_ONLY",
            },
            {
              key: "field-review",
              label: "Manual review",
              value: "Use Review workspace for extracted field validation.",
              state: "READ_ONLY",
            },
          ]}
        />
      ),
    },
    {
      key: "business-context",
      title: "Related Business Context",
      summary: groupedReferences.length > 0 ? "Customer context with searchable external references" : "Customer context and related knowledge",
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
                helperText: "Current Customer360 record",
                confidence: "HIGH",
              },
              {
                key: "customer-id",
                label: "Customer ID",
                value: `${client.clientId} · ${client.clientType}`,
                state: "READ_ONLY",
              },
            ]}
          />
          {groupedReferences.length > 0 ? (
            <EvidenceMetadataGroup
              title="Business Reference Fields"
              fields={groupedReferences.map((reference, index) => ({
                key: `${reference.label}-${index}`,
                label: reference.label,
                value: reference.value,
                state: "VERIFIED" as const,
                confidence: "HIGH" as const,
              }))}
            />
          ) : null}
          <EvidenceMetadataGroup
            title="Related documents"
            fields={relatedDocuments.length > 0
              ? relatedDocuments.slice(0, 4).map((link, index) => ({
                  key: `related-doc-${index}`,
                  label: humanizeRelationshipType(link.relationshipType),
                  value: `${link.relatedTitle} · ${link.explanation}`,
                  state: link.inferred ? "NEEDS_REVIEW" : "READ_ONLY",
                }))
              : [
                  {
                    key: "related-doc",
                    label: "Document",
                    value: document.title,
                    state: "READ_ONLY" as const,
                  },
                ]}
          />
          {relatedEmails.length > 0 ? (
            <EvidenceMetadataGroup
              title="Related emails"
              fields={relatedEmails.slice(0, 3).map((link, index) => ({
                key: `related-email-${index}`,
                label: humanizeRelationshipType(link.relationshipType),
                value: link.sourceType === "EMAIL" ? link.sourceTitle : link.relatedTitle,
                state: link.inferred ? "NEEDS_REVIEW" : "READ_ONLY",
              }))}
            />
          ) : null}
        </Stack>
      ),
    },
    {
      key: "document-information",
      title: "Document Information",
      summary: "Version and lifecycle information",
      content: (
        <EvidenceMetadataGroup
          title="Document information"
          fields={[
            {
              key: "document-version",
              label: "Version",
              value: document.currentVersionId ?? "Current",
              state: "READ_ONLY",
            },
            {
              key: "document-created",
              label: "Created",
              value: formatDateTime(document.createdAt),
              state: "READ_ONLY",
            },
            {
              key: "document-type",
              label: "Document type",
              value: document.source,
              state: "READ_ONLY",
            },
            {
              key: "redaction-status",
              label: "Redaction",
              value: document.redactionStatus,
              state: "READ_ONLY",
            },
            {
              key: "version-history",
              label: "Version history",
              value: versionSummary.join(" | "),
              state: "READ_ONLY",
            },
          ]}
        />
      ),
    },
    {
      key: "activity",
      title: "Activity",
      summary: "History and comments",
      content: (
        <EvidenceMetadataGroup
          title="Activity"
          fields={[
            {
              key: "document-history",
              label: "History",
              value: `Added ${formatDateTime(document.createdAt)}`,
              state: "READ_ONLY",
            },
            {
              key: "related-knowledge",
              label: "Related knowledge",
              value: relatedLinks.length > 0 ? `${relatedLinks.length} related sources available` : "No related sources returned by the current API.",
              state: "READ_ONLY",
            },
            {
              key: "comments",
              label: "Comments",
              value: "No read-only comments are available from the current API.",
              state: "READ_ONLY",
            },
          ]}
        />
      ),
    },
  ];
}

function humanizeRelationshipType(value: string) {
  return value
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (character) => character.toUpperCase());
}

function humanizeTimelineEventType(value: string) {
  return value
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (character) => character.toUpperCase());
}

function collectBusinessReferenceFields(links: RelatedKnowledgeLink[]) {
  const seen = new Set<string>();
  const references: Array<{ label: string; value: string }> = [];
  for (const link of links) {
    Object.entries(link.supportingFields ?? {}).forEach(([key, value]) => {
      if (!value || seen.has(`${key}:${value}`)) {
        return;
      }
      if (!["policy_number", "claim_number", "insurer", "broker_reference", "external_reference"].includes(key)) {
        return;
      }
      seen.add(`${key}:${value}`);
      references.push({
        label: key
          .replace(/_/g, " ")
          .replace(/\b\w/g, (character) => character.toUpperCase()),
        value,
      });
    });
  }
  return references;
}

function normalizeAssistantSourceKind(value: string) {
  switch (value.toUpperCase()) {
    case "DOCUMENT":
    case "DOCUMENT_VERSION":
      return "DOCUMENT" as const;
    case "EMAIL":
      return "EMAIL" as const;
    case "NOTE":
      return "NOTE" as const;
    case "CUSTOMER":
      return "CUSTOMER" as const;
    default:
      return "METADATA" as const;
  }
}

function mapOperationalTone(status: string): StatusTone {
  switch (status.toUpperCase()) {
    case "APPROVED":
    case "ACTIVE":
    case "LINKED":
    case "CLASSIFIED":
    case "AVAILABLE":
      return "success";
    case "PENDING":
    case "IN_PROGRESS":
    case "RESERVED":
      return "warning";
    case "FAILED":
    case "REJECTED":
    case "BLOCKED":
      return "error";
    default:
      return "info";
  }
}

function mapAuditOutcomeTone(outcome: string): StatusTone {
  switch (outcome.toUpperCase()) {
    case "SUCCESS":
    case "APPROVED":
      return "success";
    case "DENIED":
    case "FAILED":
      return "error";
    default:
      return "info";
  }
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString();
}
