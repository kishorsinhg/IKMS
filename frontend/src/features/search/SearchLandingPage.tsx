import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import OpenInNewOutlinedIcon from "@mui/icons-material/OpenInNewOutlined";
import MoreHorizOutlinedIcon from "@mui/icons-material/MoreHorizOutlined";
import FactCheckOutlinedIcon from "@mui/icons-material/FactCheckOutlined";
import PersonOutlineOutlinedIcon from "@mui/icons-material/PersonOutlineOutlined";
import DescriptionOutlinedIcon from "@mui/icons-material/DescriptionOutlined";
import MailOutlineOutlinedIcon from "@mui/icons-material/MailOutlineOutlined";
import StickyNote2OutlinedIcon from "@mui/icons-material/StickyNote2Outlined";
import PolicyOutlinedIcon from "@mui/icons-material/PolicyOutlined";
import AssignmentOutlinedIcon from "@mui/icons-material/AssignmentOutlined";
import FilterAltOffOutlinedIcon from "@mui/icons-material/FilterAltOffOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import {
  Alert,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Drawer,
  FormControl,
  FormControlLabel,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Menu,
  MenuItem,
  Select,
  Stack,
  TextField,
  Tooltip,
  Typography,
  Checkbox,
  useMediaQuery,
} from "@mui/material";
import type { SelectChangeEvent } from "@mui/material";
import { useTheme } from "@mui/material/styles";
import {
  GridColDef,
  GridPaginationModel,
  GridSortModel,
  GridColumnVisibilityModel,
} from "@mui/x-data-grid";
import { useQuery } from "@tanstack/react-query";
import { KeyboardEvent, ReactElement, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useOutletContext, useSearchParams } from "react-router-dom";
import { ClientSummary, listClients } from "../../api/clients";
import {
  DemoSearchGroup,
  DemoWorkspaceSearchItem,
  getDemoClientWorkspace,
  isDemoDataEnabled,
  searchDemoWorkspace,
} from "../../api/demo";
import {
  getClientKnowledgeTimeline,
  getClientRelatedKnowledge,
  getDocumentVersions,
  getSourceRelatedKnowledge,
  knowledgeQueryKeys,
  type CustomerKnowledgeTimelineEvent,
  type DocumentVersionSummary,
  type RelatedKnowledgeLink,
} from "../../api/knowledge";
import {
  ClientDocumentSummary,
  ClientEmailSummary,
  listClientDocuments,
  listClientEmails,
  listReviewQueue,
} from "../../api/intake";
import { useCurrentUser } from "../../app/auth/useCurrentUser";
import { AssistantPanel } from "../../app/components/assistant-panel/AssistantPanel";
import type {
  AssistantEvidenceReference,
  AssistantSourceReference,
  SuggestedQuestion,
} from "../../app/components/assistant-panel/assistantTypes";
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
import {
  buildKnowledgeAssistantEvidenceReferences,
  buildKnowledgeAssistantSourceReferences,
  collectKnowledgeBusinessReferenceFields,
  humanizeKnowledgeRelationshipType,
  summarizeKnowledgeVersions,
} from "../../app/knowledge/knowledgeContext";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  NoResultsState,
  RetryAction,
} from "../../app/WorkspaceStates";
import type { ShellWorkspaceChrome, IkmsShellOutletContext } from "../../app/shell/IkmsAppShell";
import type { ContextSection } from "../../app/components/RightContextPanel";

type ResultTypeFilter = "ALL" | DemoSearchGroup;
type SortableField = "title" | "customer" | "resultType" | "reference" | "status";

interface SearchRow {
  [key: string]: unknown;
  id: string;
  title: string;
  customer: string;
  resultType: DemoSearchGroup;
  reference: string;
  status: string;
  statusTone: StatusTone;
  clientId: string;
  summary: string;
  detail: string;
  updatedAt?: string;
  sourceId?: string;
}

const liveResultType: DemoSearchGroup = "Customers";
const resultTypeOptions: Array<{ value: ResultTypeFilter; label: string }> = [
  { value: "ALL", label: "All result types" },
  { value: "Customers", label: "Customers" },
  { value: "Documents", label: "Documents" },
  { value: "Emails", label: "Emails" },
  { value: "Notes", label: "Notes" },
  { value: "Knowledge", label: "Knowledge" },
  { value: "Policy References", label: "Policy References" },
  { value: "Claim References", label: "Claim References" },
];

const sortableColumns = new Set<SortableField>(["title", "customer", "resultType", "reference", "status"]);

export function SearchLandingPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { setWorkspaceChrome, clearWorkspaceChrome } = useOutletContext<IkmsShellOutletContext>();
  const currentUserQuery = useCurrentUser();
  const theme = useTheme();
  const searchInputRef = useRef<HTMLInputElement | null>(null);
  const isTabletDown = useMediaQuery(theme.breakpoints.down("lg"));
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));
  const [localQuery, setLocalQuery] = useState(searchParams.get("q") ?? "");
  const [columnDialogOpen, setColumnDialogOpen] = useState(false);
  const [actionMenuAnchor, setActionMenuAnchor] = useState<HTMLElement | null>(null);
  const [actionMenuRowId, setActionMenuRowId] = useState<string | null>(null);
  const [mobileDetailOpen, setMobileDetailOpen] = useState(false);
  const [viewerTargetId, setViewerTargetId] = useState<string | null>(null);
  const [assistantOpen, setAssistantOpen] = useState(false);
  const [assistantDraft, setAssistantDraft] = useState("");

  const query = searchParams.get("q") ?? "";
  const selectedType = (searchParams.get("type") as ResultTypeFilter | null) ?? "ALL";
  const selectedId = searchParams.get("selected");
  const page = parsePositiveNumber(searchParams.get("page"), 0);
  const pageSize = parsePositiveNumber(searchParams.get("pageSize"), 10);
  const sortField = (searchParams.get("sort") as SortableField | null) ?? "title";
  const sortDirection = searchParams.get("dir") === "desc" ? "desc" : "asc";
  const queryActive = query.trim().length > 0;
  const reviewQueueEnabled = currentUserQuery.data?.permissions.includes("REVIEW_QUEUE_ACCESS") ?? false;

  const recentClientsQuery = useQuery({
    queryKey: ["search", "recent-clients"],
    queryFn: () => listClients(""),
  });

  const clientSearchQuery = useQuery({
    queryKey: ["search", "clients", query],
    queryFn: () => listClients(query),
    enabled: !isDemoDataEnabled && queryActive,
  });

  const demoSearchQuery = useQuery({
    queryKey: ["search", "workspace", query],
    queryFn: () => searchDemoWorkspace(query),
    enabled: isDemoDataEnabled && queryActive,
  });

  const reviewQueueQuery = useQuery({
    queryKey: ["search", "review-alert"],
    queryFn: () => listReviewQueue("OPEN", ""),
    enabled: reviewQueueEnabled,
  });

  const clientLookup = useMemo(
    () => new Map((recentClientsQuery.data ?? []).map((client) => [client.id, client])),
    [recentClientsQuery.data],
  );

  const sourceRows = useMemo<SearchRow[]>(() => {
    if (queryActive) {
      if (isDemoDataEnabled) {
        return (demoSearchQuery.data ?? []).map((item) => mapDemoSearchRow(item, clientLookup));
      }
      return (clientSearchQuery.data ?? []).map(mapClientRow);
    }

    return (recentClientsQuery.data ?? []).map(mapClientRow);
  }, [
    clientLookup,
    clientSearchQuery.data,
    demoSearchQuery.data,
    queryActive,
    recentClientsQuery.data,
  ]);

  const filteredRows = useMemo(() => {
    if (selectedType === "ALL") {
      return sourceRows;
    }
    return sourceRows.filter((row) => row.resultType === selectedType);
  }, [selectedType, sourceRows]);

  const sortModel = useMemo<GridSortModel>(
    () =>
      sortableColumns.has(sortField)
        ? [{ field: sortField as string, sort: sortDirection }]
        : [{ field: "title", sort: "asc" }],
    [sortDirection, sortField],
  );

  const sortedRows = useMemo(() => {
    const [{ field, sort }] = sortModel;
    if (!field || !sort) {
      return filteredRows;
    }

    return [...filteredRows].sort((left, right) => compareRows(left, right, field as SortableField, sort));
  }, [filteredRows, sortModel]);

  const activePageSize = pageSize > 0 ? pageSize : 10;
  const pagedMobileRows = useMemo(() => {
    const start = page * activePageSize;
    return sortedRows.slice(start, start + activePageSize);
  }, [activePageSize, page, sortedRows]);

  const activeRow = useMemo(
    () => sortedRows.find((row) => row.id === selectedId) ?? null,
    [selectedId, sortedRows],
  );

  const selectedWorkspaceContextQuery = useQuery({
    queryKey: ["search", "workspace-context", activeRow?.clientId],
    queryFn: () => getDemoClientWorkspace(activeRow!.clientId),
    enabled: isDemoDataEnabled && Boolean(activeRow?.clientId),
  });
  const activeRowSourceType = useMemo(
    () => (activeRow ? mapSearchRowSourceType(activeRow) : null),
    [activeRow],
  );
  const activeRowSourceId = useMemo(
    () => (activeRow ? resolveSearchSourceId(activeRow) : null),
    [activeRow],
  );
  const selectedTimelineQuery = useQuery({
    queryKey: activeRow?.clientId
      ? knowledgeQueryKeys.clientTimeline(activeRow.clientId, { limit: 5 })
      : ["search", "selected-timeline", "empty"],
    queryFn: ({ signal }) => getClientKnowledgeTimeline(activeRow!.clientId, { limit: 5 }, signal),
    enabled: Boolean(activeRow?.clientId),
  });
  const selectedRelatedQuery = useQuery({
    queryKey: activeRow?.clientId
      ? knowledgeQueryKeys.clientRelated(activeRow.clientId, 8)
      : ["search", "selected-related", "empty"],
    queryFn: ({ signal }) => getClientRelatedKnowledge(activeRow!.clientId, 8, signal),
    enabled: Boolean(activeRow?.clientId),
  });
  const selectedSourceRelatedQuery = useQuery({
    queryKey: activeRowSourceType && activeRowSourceId
      ? knowledgeQueryKeys.sourceRelated(activeRowSourceType, activeRowSourceId, 8)
      : ["search", "selected-source-related", "empty"],
    queryFn: ({ signal }) => getSourceRelatedKnowledge(activeRowSourceType!, activeRowSourceId!, 8, signal),
    enabled: Boolean(activeRowSourceType && activeRowSourceId && activeRowSourceType !== "CUSTOMER"),
  });
  const selectedVersionsQuery = useQuery({
    queryKey: activeRowSourceType === "DOCUMENT" && activeRowSourceId
      ? knowledgeQueryKeys.documentVersions(activeRowSourceId)
      : ["search", "selected-versions", "empty"],
    queryFn: ({ signal }) => getDocumentVersions(activeRowSourceId!, signal),
    enabled: Boolean(activeRowSourceType === "DOCUMENT" && activeRowSourceId),
  });
  const selectedRelatedLinks = useMemo(
    () =>
      selectedSourceRelatedQuery.data?.links?.length
        ? selectedSourceRelatedQuery.data.links
        : selectedRelatedQuery.data?.links ?? [],
    [selectedRelatedQuery.data?.links, selectedSourceRelatedQuery.data?.links],
  );
  const viewerTargetRow = useMemo(
    () => sortedRows.find((row) => row.id === viewerTargetId) ?? null,
    [sortedRows, viewerTargetId],
  );
  const viewerWorkspaceQuery = useQuery({
    queryKey: ["search", "viewer-workspace", viewerTargetRow?.clientId],
    queryFn: () => getDemoClientWorkspace(viewerTargetRow!.clientId),
    enabled: isDemoDataEnabled && Boolean(viewerTargetRow?.clientId),
  });
  const viewerSourceType = useMemo(
    () => (viewerTargetRow ? mapSearchRowSourceType(viewerTargetRow) : null),
    [viewerTargetRow],
  );
  const viewerSourceId = useMemo(
    () => (viewerTargetRow ? resolveSearchSourceId(viewerTargetRow) : null),
    [viewerTargetRow],
  );
  const viewerDocumentsQuery = useQuery({
    queryKey: ["search", "viewer-documents", viewerTargetRow?.clientId],
    queryFn: () => listClientDocuments(viewerTargetRow!.clientId),
    enabled: Boolean(viewerTargetRow?.clientId) && viewerTargetRow?.resultType === "Documents",
  });
  const viewerEmailsQuery = useQuery({
    queryKey: ["search", "viewer-emails", viewerTargetRow?.clientId],
    queryFn: () => listClientEmails(viewerTargetRow!.clientId),
    enabled: Boolean(viewerTargetRow?.clientId) && viewerTargetRow?.resultType === "Emails",
  });
  const viewerSourceRelatedQuery = useQuery({
    queryKey: viewerSourceType && viewerSourceId
      ? knowledgeQueryKeys.sourceRelated(viewerSourceType, viewerSourceId, 12)
      : ["search", "viewer-source-related", "empty"],
    queryFn: ({ signal }) => getSourceRelatedKnowledge(viewerSourceType!, viewerSourceId!, 12, signal),
    enabled: Boolean(viewerSourceType && viewerSourceId && viewerSourceType !== "CUSTOMER"),
  });
  const viewerVersionsQuery = useQuery({
    queryKey: viewerSourceType === "DOCUMENT" && viewerSourceId
      ? knowledgeQueryKeys.documentVersions(viewerSourceId)
      : ["search", "viewer-versions", "empty"],
    queryFn: ({ signal }) => getDocumentVersions(viewerSourceId!, signal),
    enabled: Boolean(viewerSourceType === "DOCUMENT" && viewerSourceId),
  });
  const viewerDocumentRecord = useMemo(
    () =>
      viewerTargetRow?.resultType === "Documents"
        ? matchSearchDocument(viewerTargetRow, viewerDocumentsQuery.data ?? [])
        : null,
    [viewerDocumentsQuery.data, viewerTargetRow],
  );
  const viewerEmailRecord = useMemo(
    () =>
      viewerTargetRow?.resultType === "Emails"
        ? matchSearchEmail(viewerTargetRow, viewerEmailsQuery.data ?? [])
        : null,
    [viewerEmailsQuery.data, viewerTargetRow],
  );
  const viewerDocumentConfig = useMemo(
    () =>
      viewerTargetRow
        ? buildSearchViewerDocument(viewerTargetRow, viewerDocumentRecord, viewerEmailRecord)
        : null,
    [viewerDocumentRecord, viewerEmailRecord, viewerTargetRow],
  );
  const viewerEvidenceSections = useMemo<EnterpriseDocumentViewerProps["evidenceSections"]>(
    () =>
      viewerTargetRow
        ? buildSearchViewerEvidenceSections(
            viewerTargetRow,
            viewerDocumentRecord,
            viewerEmailRecord,
            viewerWorkspaceQuery.data,
            viewerSourceRelatedQuery.data?.links ?? [],
            viewerVersionsQuery.data ?? [],
          )
        : [],
    [viewerDocumentRecord, viewerEmailRecord, viewerTargetRow, viewerVersionsQuery.data, viewerWorkspaceQuery.data, viewerSourceRelatedQuery.data?.links],
  );

  const resultCountLabel = queryActive ? `${sortedRows.length} matching results` : `${sortedRows.length} recent customer records`;
  const reviewQueueCount = reviewQueueQuery.data?.length ?? 0;
  const loading = queryActive
    ? (isDemoDataEnabled ? demoSearchQuery.isLoading : clientSearchQuery.isLoading)
    : recentClientsQuery.isLoading;
  const error = queryActive
    ? (isDemoDataEnabled ? demoSearchQuery.error : clientSearchQuery.error)
    : recentClientsQuery.error;

  const baseVisibilityModel = useMemo<GridColumnVisibilityModel>(
    () => ({
      reference: !isTabletDown,
      updatedAt: false,
      customer: true,
      resultType: true,
      status: true,
      actions: true,
    }),
    [isTabletDown],
  );
  const [columnVisibilityModel, setColumnVisibilityModel] = useState<GridColumnVisibilityModel>(baseVisibilityModel);

  useEffect(() => {
    setLocalQuery(query);
  }, [query]);

  useEffect(() => {
    setColumnVisibilityModel((current) => ({
      ...baseVisibilityModel,
      ...current,
      reference: current.reference ?? baseVisibilityModel.reference,
    }));
  }, [baseVisibilityModel]);

  useEffect(() => {
    if (selectedId && !sortedRows.some((row) => row.id === selectedId)) {
      updateSearchParams(setSearchParams, searchParams, { selected: null, page: "0" });
    }
  }, [searchParams, selectedId, setSearchParams, sortedRows]);

  useEffect(() => {
    if (!isMobile) {
      setMobileDetailOpen(false);
    }
  }, [isMobile]);

  useEffect(() => {
    function handleShortcut(event: globalThis.KeyboardEvent) {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        searchInputRef.current?.focus();
        searchInputRef.current?.select();
      }
    }

    window.addEventListener("keydown", handleShortcut);
    return () => window.removeEventListener("keydown", handleShortcut);
  }, []);

  const chrome = useMemo<ShellWorkspaceChrome>(() => {
    const contextSections = activeRow
      ? [
          ...buildSelectedContextSections(
            activeRow,
            selectedWorkspaceContextQuery.data,
            () => openCustomer(activeRow.clientId, navigate),
            canOpenSearchViewer(activeRow) ? () => setViewerTargetId(activeRow.id) : undefined,
            () => setAssistantOpen(true),
          ),
          ...buildSearchKnowledgeContextSections(
            activeRow,
            selectedRelatedLinks,
            selectedTimelineQuery.data?.events ?? [],
            selectedVersionsQuery.data ?? [],
          ),
          {
            key: "assistant",
            title: "Assistant",
            content: (
              <AssistantPanel
                title="Enterprise AI Assistant"
                subtitle="Search context only. Conversation placeholders are shown where no assistant API is currently available."
                conversationState="empty"
                messages={[]}
                suggestedQuestions={buildSearchAssistantQuestions(activeRow, setAssistantDraft, selectedRelatedLinks, selectedVersionsQuery.data ?? [])}
                evidenceReferences={buildSearchAssistantEvidenceReferences(activeRow, selectedRelatedLinks, selectedTimelineQuery.data?.events ?? [])}
                sourceReferences={buildSearchAssistantSourceReferences(activeRow, selectedRelatedLinks)}
                emptyTitle="Conversation unavailable"
                emptyMessage="Search does not expose an assistant conversation endpoint in the current frontend contract."
                composerContent={(
                  <Stack spacing={1}>
                    <Typography variant="body2" color="text.secondary">
                      Suggested questions are based on the selected result and available timeline or related-knowledge context. Submission remains unavailable until a Search assistant API is exposed.
                    </Typography>
                    <Select
                      size="small"
                      displayEmpty
                      value={assistantDraft}
                      onChange={(event) => setAssistantDraft(String(event.target.value))}
                      inputProps={{ "aria-label": "Assistant question draft" }}
                    >
                      <MenuItem value="">
                        <em>Select or type a question</em>
                      </MenuItem>
                      {buildSearchAssistantQuestions(activeRow, setAssistantDraft, selectedRelatedLinks, selectedVersionsQuery.data ?? []).map((question) => (
                        <MenuItem key={question.key} value={question.label}>
                          {question.label}
                        </MenuItem>
                      ))}
                    </Select>
                    <Button variant="contained" disabled>
                      AI unavailable in Search
                    </Button>
                  </Stack>
                )}
              />
            ),
          },
        ]
      : buildSearchContextSections({
          query,
          selectedType,
          resultCount: sortedRows.length,
          reviewQueueCount,
        });

    return {
      title: "Search",
      subtitle: "Locate customers and operational records with compact, keyboard-friendly controls.",
      globalSearchValue: query,
      globalSearchPlaceholder: "Focus Search workspace input",
      onGlobalSearchSubmit: () => {
        searchInputRef.current?.focus();
        searchInputRef.current?.select();
      },
      contextTitle: activeRow ? "Selected Result" : "Search Context",
      contextSections,
    };
  }, [
    activeRow,
    assistantDraft,
    navigate,
    query,
    reviewQueueCount,
    selectedRelatedLinks,
    selectedTimelineQuery.data?.events,
    selectedType,
    selectedVersionsQuery.data,
    selectedWorkspaceContextQuery.data,
    sortedRows.length,
  ]);

  useEffect(() => {
    setWorkspaceChrome(chrome);
    return () => clearWorkspaceChrome();
  }, [chrome, clearWorkspaceChrome, setWorkspaceChrome]);

  const activeFilters = useMemo(() => {
    const filters = [];

    if (query.trim()) {
      filters.push({
        key: "query",
        label: `Query: ${query}`,
        onDelete: () => {
          setLocalQuery("");
          updateSearchParams(setSearchParams, searchParams, {
            q: null,
            selected: null,
            page: "0",
          });
        },
      });
    }

    if (selectedType !== "ALL") {
      filters.push({
        key: "type",
        label: resultTypeOptions.find((option) => option.value === selectedType)?.label ?? selectedType,
        onDelete: () => updateSearchParams(setSearchParams, searchParams, { type: null, page: "0" }),
      });
    }

    return filters;
  }, [query, searchParams, selectedType, setSearchParams]);

  const hasActiveFilters = activeFilters.length > 0;
  const columns = useMemo<GridColDef<SearchRow>[]>(
    () => [
      {
        field: "title",
        headerName: "Title",
        flex: 1.4,
        minWidth: 280,
        sortable: true,
        renderCell: ({ row }) => (
          <Stack spacing={0.25} sx={{ minWidth: 0, py: 0.5 }}>
            <Tooltip title={row.title}>
              <Typography variant="body2" fontWeight={600} noWrap>
                {row.title}
              </Typography>
            </Tooltip>
            <Typography variant="caption" color="text.secondary" noWrap>
              {row.summary}
            </Typography>
          </Stack>
        ),
      },
      {
        field: "customer",
        headerName: "Customer",
        flex: 1,
        minWidth: 220,
        sortable: true,
        renderCell: ({ row }) => (
          <Stack spacing={0.25} sx={{ minWidth: 0, py: 0.5 }}>
            <Tooltip title={row.customer}>
              <Typography variant="body2" noWrap>
                {row.customer}
              </Typography>
            </Tooltip>
            <Typography variant="caption" color="text.secondary" noWrap>
              {row.reference}
            </Typography>
          </Stack>
        ),
      },
      {
        field: "resultType",
        headerName: "Type",
        width: 148,
        sortable: true,
        renderCell: ({ row }) => (
          <Chip
            size="small"
            variant="outlined"
            icon={getResultTypeIcon(row.resultType)}
            label={row.resultType}
            sx={{ maxWidth: "100%" }}
          />
        ),
      },
      {
        field: "reference",
        headerName: "Reference",
        flex: 0.85,
        minWidth: 200,
        sortable: true,
        renderCell: ({ value }) => (
          <Tooltip title={String(value)}>
            <Typography variant="body2" color="text.secondary" noWrap>
              {String(value)}
            </Typography>
          </Tooltip>
        ),
      },
      {
        field: "status",
        headerName: "Status",
        width: 138,
        sortable: true,
        renderCell: ({ row }) => <StatusBadge label={row.status} tone={row.statusTone} />,
      },
      {
        field: "actions",
        headerName: "Actions",
        width: 72,
        sortable: false,
        filterable: false,
        disableColumnMenu: true,
        renderCell: ({ row }) => (
          <Tooltip title="Row actions">
            <IconButton
              aria-label={`Open actions for ${row.title}`}
              onClick={(event) => {
                event.stopPropagation();
                setActionMenuAnchor(event.currentTarget);
                setActionMenuRowId(row.id);
              }}
            >
              <MoreHorizOutlinedIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        ),
      },
      {
        field: "updatedAt",
        headerName: "Updated",
        width: 140,
        sortable: false,
        renderCell: ({ value }) => (
          <Typography variant="body2" color="text.secondary">
            {value ? formatDate(String(value)) : "Not available"}
          </Typography>
        ),
      },
    ],
    [],
  );

  const paginationModel = useMemo<GridPaginationModel>(
    () => ({
      page,
      pageSize: activePageSize,
    }),
    [activePageSize, page],
  );

  const gridHeight = useMemo(() => {
    const visibleRows = Math.min(sortedRows.length, activePageSize);
    const rows = Math.max(visibleRows, 4);
    const baseHeight = 56 + 52 + rows * 44 + 16;
    return Math.min(Math.max(baseHeight, 320), isTabletDown ? 540 : 680);
  }, [activePageSize, isTabletDown, sortedRows.length]);

  const actionRow = sortedRows.find((row) => row.id === actionMenuRowId) ?? null;

  function applySearch() {
    const trimmed = localQuery.trim();
    updateSearchParams(setSearchParams, searchParams, {
      q: trimmed || null,
      page: "0",
      selected: null,
    });
  }

  function clearSearch() {
    setLocalQuery("");
    updateSearchParams(setSearchParams, searchParams, {
      q: null,
      type: null,
      selected: null,
      page: "0",
    });
  }

  function handleToolbarKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Enter") {
      event.preventDefault();
      applySearch();
    }
  }

  function handleTypeChange(event: SelectChangeEvent<ResultTypeFilter>) {
    updateSearchParams(setSearchParams, searchParams, {
      type: event.target.value === "ALL" ? null : event.target.value,
      page: "0",
      selected: null,
    });
  }

  function handlePaginationModelChange(model: GridPaginationModel) {
    updateSearchParams(setSearchParams, searchParams, {
      page: String(model.page),
      pageSize: String(model.pageSize),
    });
  }

  function handleSortModelChange(model: GridSortModel) {
    const next = model[0];
    if (!next?.field || !next.sort) {
      updateSearchParams(setSearchParams, searchParams, {
        sort: null,
        dir: null,
      });
      return;
    }

    updateSearchParams(setSearchParams, searchParams, {
      sort: next.field,
      dir: next.sort,
    });
  }

  function selectRow(row: SearchRow) {
    updateSearchParams(setSearchParams, searchParams, {
      selected: row.id,
    });

    if (isMobile) {
      setMobileDetailOpen(true);
    }
  }

  if (currentUserQuery.isLoading) {
    return (
      <LoadingState
        title="Loading Search"
        message="Preparing operational search controls and current user permissions."
      />
    );
  }

  if (currentUserQuery.isError || !currentUserQuery.data) {
    return (
      <ErrorState
        title="Search unavailable"
        message="The current user context could not be loaded."
      />
    );
  }

  return (
    <Stack spacing={1.5}>
      <WorkspaceToolbar
        searchPlaceholder="Search by customer, policy reference, claim reference, email, metadata, or note"
        searchValue={localQuery}
        onSearchChange={setLocalQuery}
        onSearchKeyDown={handleToolbarKeyDown}
        searchInputRef={searchInputRef}
        searchAriaLabel="Search workspace query"
        filters={(
          <Stack direction={{ xs: "column", sm: "row" }} spacing={1} sx={{ width: { xs: "100%", lg: "auto" } }}>
            <FormControl size="small" sx={{ minWidth: { xs: "100%", sm: 190 } }}>
              <Select<ResultTypeFilter>
                value={selectedType}
                displayEmpty
                inputProps={{ "aria-label": "Result type" }}
                onChange={handleTypeChange}
              >
                {resultTypeOptions
                  .filter((option) => isDemoDataEnabled || option.value === "ALL" || option.value === "Customers")
                  .map((option) => (
                    <MenuItem key={option.value} value={option.value}>
                      {option.label}
                    </MenuItem>
                  ))}
              </Select>
            </FormControl>
            {hasActiveFilters ? (
              <Button variant="text" color="inherit" onClick={clearSearch} startIcon={<FilterAltOffOutlinedIcon fontSize="small" />}>
                Clear
              </Button>
            ) : null}
          </Stack>
        )}
        activeFilters={activeFilters}
        primaryAction={(
          <Button variant="contained" onClick={applySearch} startIcon={<SearchOutlinedIcon fontSize="small" />}>
            Search
          </Button>
        )}
        onRefresh={() => {
          if (queryActive) {
            if (isDemoDataEnabled) {
              void demoSearchQuery.refetch();
            } else {
              void clientSearchQuery.refetch();
            }
            return;
          }

          void recentClientsQuery.refetch();
        }}
        onExport={() => downloadCsv(sortedRows)}
        onColumns={() => setColumnDialogOpen(true)}
      />

      {reviewQueueEnabled && reviewQueueCount > 0 ? (
        <Alert
          severity="warning"
          variant="outlined"
          action={
            <Button
              color="inherit"
              size="small"
              onClick={() => navigate("/review-queue")}
            >
              Open Review Queue
            </Button>
          }
        >
          {reviewQueueCount} {reviewQueueCount === 1 ? "item requires" : "items require"} human review.
        </Alert>
      ) : null}

      <Box
        sx={{
          border: (appTheme) => `1px solid ${appTheme.palette.divider}`,
          borderRadius: 1,
          backgroundColor: "background.paper",
          overflow: "hidden",
        }}
      >
        <Stack spacing={1} sx={{ px: 2, py: 1.5 }}>
          <Stack
            direction={{ xs: "column", sm: "row" }}
            justifyContent="space-between"
            alignItems={{ xs: "flex-start", sm: "center" }}
            spacing={1}
          >
            <Stack spacing={0.25}>
              <Typography variant="subtitle2">
                {queryActive ? "Search results" : "Continue working"}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {resultCountLabel}
              </Typography>
            </Stack>
            <Stack direction="row" spacing={0.75} alignItems="center" flexWrap="wrap">
              {!queryActive ? <Chip size="small" label="Recent customers" variant="outlined" /> : null}
              {isDemoDataEnabled ? <Chip size="small" label="Demo search index" color="success" /> : null}
            </Stack>
          </Stack>

          {loading ? (
            <LoadingState
              title="Loading results"
              message="Search results are being prepared."
            />
          ) : error ? (
            <ErrorState
              title="Unable to load search results"
              message="The current search could not be completed."
              action={<RetryAction onClick={() => void (queryActive ? (isDemoDataEnabled ? demoSearchQuery.refetch() : clientSearchQuery.refetch()) : recentClientsQuery.refetch())} />}
            />
          ) : sortedRows.length === 0 ? (
            queryActive ? (
              <NoResultsState
                title="No results for this search"
                message="Try a broader customer, policy reference, claim reference, email, or note query."
                action={<RetryAction label="Clear filters" onClick={clearSearch} />}
              />
            ) : (
              <EmptyState
                title="No recent customer records"
                message="Recent work will appear here once customer records are opened."
                compact
              />
            )
          ) : isMobile ? (
            <>
              <SearchResultsList
                rows={pagedMobileRows}
                selectedId={selectedId}
                onSelect={selectRow}
                onOpen={openCustomer}
              />
              <Box
                sx={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  px: 0.5,
                  pb: 0.5,
                }}
              >
                <Typography variant="caption" color="text.secondary">
                  Page {page + 1} of {Math.max(1, Math.ceil(sortedRows.length / activePageSize))}
                </Typography>
                <Stack direction="row" spacing={1}>
                  <Button
                    size="small"
                    disabled={page === 0}
                    onClick={() => handlePaginationModelChange({ page: Math.max(0, page - 1), pageSize: activePageSize })}
                  >
                    Previous
                  </Button>
                  <Button
                    size="small"
                    disabled={(page + 1) * activePageSize >= sortedRows.length}
                    onClick={() => handlePaginationModelChange({ page: page + 1, pageSize: activePageSize })}
                  >
                    Next
                  </Button>
                </Stack>
              </Box>
            </>
          ) : (
            <EntityGrid<SearchRow>
              rows={sortedRows}
              columns={columns}
              getRowId={(row) => row.id}
              pagination
              paginationMode="client"
              paginationModel={paginationModel}
              onPaginationModelChange={handlePaginationModelChange}
              sortModel={sortModel}
              onSortModelChange={handleSortModelChange}
              onRowClick={({ row }) => selectRow(row)}
              onRowDoubleClick={({ row }) => openCustomer(row.clientId, navigate)}
              onCellKeyDown={(params, event) => {
                if (event.key === "Enter") {
                  openCustomer(params.row.clientId, navigate);
                }
              }}
              getRowClassName={({ row }) => (row.id === selectedId ? "ikms-selected-row" : "")}
              columnVisibilityModel={columnVisibilityModel}
              onColumnVisibilityModelChange={setColumnVisibilityModel}
              emptyTitle="No search results"
              emptyMessage="There are no results for the current query and filters."
              sx={{
                minHeight: gridHeight,
                height: gridHeight,
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
        </Stack>
      </Box>

      <Menu
        anchorEl={actionMenuAnchor}
        open={Boolean(actionMenuAnchor)}
        onClose={() => {
          setActionMenuAnchor(null);
          setActionMenuRowId(null);
        }}
      >
        <MenuItem
          onClick={() => {
            if (actionRow) {
              openCustomer(actionRow.clientId, navigate);
            }
            setActionMenuAnchor(null);
            setActionMenuRowId(null);
          }}
        >
          Open customer
        </MenuItem>
        {actionRow && canOpenSearchViewer(actionRow) ? (
          <MenuItem
            onClick={() => {
              setViewerTargetId(actionRow.id);
              setActionMenuAnchor(null);
              setActionMenuRowId(null);
            }}
          >
            Open evidence workspace
          </MenuItem>
        ) : null}
      </Menu>

      <Dialog open={columnDialogOpen} onClose={() => setColumnDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Visible columns</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={0.5}>
            {columns
              .filter((column) => ["title", "customer", "resultType", "reference", "status", "updatedAt"].includes(String(column.field)))
              .map((column) => (
                <FormControlLabel
                  key={String(column.field)}
                  control={(
                    <Checkbox
                      checked={columnVisibilityModel[String(column.field)] !== false}
                      onChange={(event) =>
                        setColumnVisibilityModel((current) => ({
                          ...current,
                          [String(column.field)]: event.target.checked,
                        }))
                      }
                    />
                  )}
                  label={column.headerName ?? String(column.field)}
                />
              ))}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setColumnDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      <Drawer
        anchor="right"
        open={isMobile && mobileDetailOpen && Boolean(activeRow)}
        onClose={() => setMobileDetailOpen(false)}
        PaperProps={{ sx: { width: "100%", maxWidth: "100%" } }}
      >
        <Stack spacing={0} sx={{ height: "100%" }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ px: 2, py: 1.25, borderBottom: (theme) => `1px solid ${theme.palette.divider}` }}>
            <Typography variant="subtitle2">Selected result</Typography>
            <IconButton aria-label="Close detail" onClick={() => setMobileDetailOpen(false)}>
              <CloseOutlinedIcon fontSize="small" />
            </IconButton>
          </Stack>
          <Box sx={{ overflowY: "auto", px: 2, py: 1.5 }}>
            {activeRow ? (
              <ResultDetailContent
                row={activeRow}
                workspace={selectedWorkspaceContextQuery.data}
                onOpen={() => openCustomer(activeRow.clientId, navigate)}
                onOpenEvidenceWorkspace={canOpenSearchViewer(activeRow) ? () => setViewerTargetId(activeRow.id) : undefined}
                onOpenAssistant={() => setAssistantOpen(true)}
              />
            ) : null}
          </Box>
        </Stack>
      </Drawer>

      {viewerDocumentConfig ? (
        <DocumentViewerDialog
          open={Boolean(viewerDocumentConfig)}
          onClose={() => setViewerTargetId(null)}
          document={viewerDocumentConfig}
          state={viewerDocumentsQuery.isLoading || viewerEmailsQuery.isLoading ? "loading" : viewerDocumentConfig.fileKind === "email" || viewerDocumentConfig.previewUrl ? "ready" : "unsupported"}
          evidenceSections={viewerEvidenceSections}
          layers={documentViewerPlaceholderLayers()}
        />
      ) : null}

      {activeRow ? (
        <AssistantPanel
          title="Enterprise AI Assistant"
          subtitle="Search context only. Conversation placeholders are shown where no assistant API is currently available."
          variant={isMobile ? "sheet" : "drawer"}
          open={assistantOpen}
          onClose={() => setAssistantOpen(false)}
          conversationState="empty"
          messages={[]}
          suggestedQuestions={buildSearchAssistantQuestions(activeRow, setAssistantDraft, selectedRelatedLinks, selectedVersionsQuery.data ?? [])}
          evidenceReferences={buildSearchAssistantEvidenceReferences(activeRow, selectedRelatedLinks, selectedTimelineQuery.data?.events ?? [])}
          sourceReferences={buildSearchAssistantSourceReferences(activeRow, selectedRelatedLinks)}
          emptyTitle="Conversation unavailable"
          emptyMessage="Search does not expose an assistant conversation endpoint in the current frontend contract."
          composerContent={(
            <Stack spacing={1}>
              <TextField
                size="small"
                multiline
                minRows={2}
                value={assistantDraft}
                onChange={(event) => setAssistantDraft(event.target.value)}
                placeholder="Capture a Search-specific assistant prompt"
                aria-label="Search assistant prompt"
              />
              <Button variant="contained" disabled>
                AI unavailable in Search
              </Button>
            </Stack>
          )}
        />
      ) : null}
    </Stack>
  );
}

function SearchResultsList({
  rows,
  selectedId,
  onSelect,
  onOpen,
}: {
  rows: SearchRow[];
  selectedId: string | null;
  onSelect: (row: SearchRow) => void;
  onOpen: (clientId: string, navigate: ReturnType<typeof useNavigate>) => void;
}) {
  const navigate = useNavigate();

  return (
    <List disablePadding sx={{ display: "grid", gap: 1 }}>
      {rows.map((row) => (
        <Box
          key={row.id}
          sx={{
            border: (theme) => `1px solid ${theme.palette.divider}`,
            borderRadius: 1,
            overflow: "hidden",
            backgroundColor: selectedId === row.id ? "action.selected" : "background.paper",
          }}
        >
          <ListItemButton onClick={() => onSelect(row)} alignItems="flex-start">
            <ListItemText
              primary={(
                <Stack spacing={1}>
                  <Stack direction="row" spacing={1} justifyContent="space-between" alignItems="flex-start">
                    <Typography variant="body2" fontWeight={600}>
                      {row.title}
                    </Typography>
                    <StatusBadge label={row.status} tone={row.statusTone} />
                  </Stack>
                  <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap alignItems="center">
                    <Chip size="small" variant="outlined" icon={getResultTypeIcon(row.resultType)} label={row.resultType} />
                    <Typography variant="caption" color="text.secondary">
                      {row.customer}
                    </Typography>
                  </Stack>
                  <Typography variant="body2" color="text.secondary">
                    {row.reference}
                  </Typography>
                </Stack>
              )}
            />
          </ListItemButton>
          <Divider />
          <Box sx={{ px: 2, py: 1 }}>
            <Button
              size="small"
              startIcon={<OpenInNewOutlinedIcon fontSize="small" />}
              onClick={() => onOpen(row.clientId, navigate)}
            >
              Open customer
            </Button>
          </Box>
        </Box>
      ))}
    </List>
  );
}

function ResultDetailContent({
  row,
  workspace,
  onOpen,
  onOpenEvidenceWorkspace,
  onOpenAssistant,
}: {
  row: SearchRow;
  workspace?: Awaited<ReturnType<typeof getDemoClientWorkspace>>;
  onOpen: () => void;
  onOpenEvidenceWorkspace?: () => void;
  onOpenAssistant?: () => void;
}) {
  return (
    <Stack spacing={1.5}>
      <Stack spacing={0.75}>
        <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap alignItems="center">
          <Chip size="small" variant="outlined" icon={getResultTypeIcon(row.resultType)} label={row.resultType} />
          <StatusBadge label={row.status} tone={row.statusTone} />
        </Stack>
        <Typography variant="h3">{row.title}</Typography>
        <Typography variant="body2" color="text.secondary">
          {row.customer}
        </Typography>
      </Stack>

      <Box>
        <Typography variant="subtitle2" gutterBottom>
          Matched evidence
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {row.detail}
        </Typography>
      </Box>

      <Box>
        <Typography variant="subtitle2" gutterBottom>
          Reference
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {row.reference}
        </Typography>
      </Box>

      {workspace ? (
        <Box>
          <Typography variant="subtitle2" gutterBottom>
            External references
          </Typography>
          <Stack spacing={0.75}>
            {workspace.policyReferences.slice(0, 2).map((policy) => (
              <Typography key={policy.id} variant="body2" color="text.secondary">
                Policy reference {policy.policyNumber} · {policy.carrier}
              </Typography>
            ))}
            {workspace.claimReferences.slice(0, 2).map((claim) => (
              <Typography key={claim.id} variant="body2" color="text.secondary">
                Claim reference {claim.claimNumber} · {claim.status}
              </Typography>
            ))}
            {workspace.policyReferences.length === 0 && workspace.claimReferences.length === 0 ? (
              <Typography variant="body2" color="text.secondary">
                No related external policy or claim references are available for this result.
              </Typography>
            ) : null}
          </Stack>
        </Box>
      ) : null}

      <Box>
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          <Button variant="contained" startIcon={<OpenInNewOutlinedIcon fontSize="small" />} onClick={onOpen}>
            Open customer
          </Button>
          {onOpenEvidenceWorkspace ? (
            <Button variant="outlined" color="inherit" onClick={onOpenEvidenceWorkspace}>
              Open evidence workspace
            </Button>
          ) : null}
          {onOpenAssistant ? (
            <Button variant="outlined" color="inherit" onClick={onOpenAssistant}>
              Open assistant
            </Button>
          ) : null}
        </Stack>
      </Box>
    </Stack>
  );
}

function buildSearchContextSections({
  query,
  selectedType,
  resultCount,
  reviewQueueCount,
}: {
  query: string;
  selectedType: ResultTypeFilter;
  resultCount: number;
  reviewQueueCount: number;
}) {
  const items = [
    query ? `Query: ${query}` : "Query: recent customer records",
    `Result type: ${selectedType === "ALL" ? "All result types" : selectedType}`,
    `Results: ${resultCount}`,
    reviewQueueCount > 0 ? `Open review items: ${reviewQueueCount}` : null,
  ].filter(Boolean) as string[];

  return [
    {
      key: "search-context",
      title: "Search Context",
      content: (
        <Stack spacing={0.75}>
          {items.map((item) => (
            <Typography key={item} variant="body2" color="text.secondary">
              {item}
            </Typography>
          ))}
          <Typography variant="caption" color="text.secondary">
            Press Ctrl+K to focus the workspace search field.
          </Typography>
        </Stack>
      ),
    },
  ];
}

function buildSelectedContextSections(
  row: SearchRow,
  workspace: Awaited<ReturnType<typeof getDemoClientWorkspace>> | undefined,
  onOpen: () => void,
  onOpenEvidenceWorkspace?: () => void,
  onOpenAssistant?: () => void,
) {
  return [
    {
      key: "selected-summary",
      title: "Selected Result",
      content: <ResultDetailContent row={row} workspace={workspace} onOpen={onOpen} onOpenEvidenceWorkspace={onOpenEvidenceWorkspace} onOpenAssistant={onOpenAssistant} />,
    },
  ];
}

function buildSearchAssistantQuestions(
  row: SearchRow,
  setDraft: (value: string) => void,
  relatedLinks: RelatedKnowledgeLink[],
  versions: DocumentVersionSummary[],
): SuggestedQuestion[] {
  const labels = [
    `Explain ${row.resultType.toLowerCase()} relevance`,
    relatedLinks.length > 0 ? "Show related documents" : "Summarize customer context",
    versions.length > 1 ? "Find earlier versions" : "What other sources mention this reference?",
    "Why did this result match?",
  ];

  return labels.map((label) => ({
    key: `${row.id}-${label}`,
    label,
    onSelect: () => setDraft(label),
  }));
}

function buildSearchAssistantEvidenceReferences(
  row: SearchRow,
  relatedLinks: RelatedKnowledgeLink[],
  timelineEvents: CustomerKnowledgeTimelineEvent[],
): AssistantEvidenceReference[] {
  return buildKnowledgeAssistantEvidenceReferences(relatedLinks, timelineEvents, {
    key: `${row.id}-match`,
    label: row.title,
    target: row.resultType === "Documents" ? "page" : "metadata",
    detail: row.reference,
    disabled: true,
  });
}

function buildSearchAssistantSourceReferences(
  row: SearchRow,
  relatedLinks: RelatedKnowledgeLink[],
): AssistantSourceReference[] {
  return buildKnowledgeAssistantSourceReferences({
    customerLabel: row.customer,
    primarySource: {
      key: `${row.id}-result`,
      label: row.title,
      sourceType: mapSearchRowSourceType(row) ?? "METADATA",
      detail: row.reference,
    },
    relatedLinks,
  }).map((source) =>
    source.kind === normalizeSearchSourceKind(row.resultType)
      ? { ...source, referenceType: source.referenceType ?? normalizeSearchReferenceType(row.resultType) }
      : source,
  );
}

function normalizeSearchSourceKind(resultType: DemoSearchGroup): AssistantSourceReference["kind"] {
  switch (resultType) {
    case "Documents":
      return "DOCUMENT";
    case "Emails":
      return "EMAIL";
    case "Notes":
      return "NOTE";
    case "Policy References":
      return "METADATA";
    case "Claim References":
      return "METADATA";
    case "Customers":
      return "CUSTOMER";
    default:
      return "UNKNOWN";
  }
}

function normalizeSearchReferenceType(resultType: DemoSearchGroup): AssistantSourceReference["referenceType"] | undefined {
  switch (resultType) {
    case "Policy References":
      return "policy";
    case "Claim References":
      return "claim";
    default:
      return undefined;
  }
}

function mapClientRow(client: ClientSummary): SearchRow {
  return {
    id: `customer-${client.id}`,
    sourceId: client.id,
    title: client.displayName,
    customer: client.displayName,
    resultType: liveResultType,
    reference: `${client.clientId}${client.clientIdTemporary ? " (Temporary)" : ""}`,
    status: client.status,
    statusTone: mapClientStatusTone(client.status),
    clientId: client.id,
    summary: `${client.clientType} customer`,
    detail: `${client.displayName} is available for direct Customer360 access.`,
  };
}

function mapDemoSearchRow(item: DemoWorkspaceSearchItem, lookup: Map<string, ClientSummary>): SearchRow {
  const customer = lookup.get(item.clientId)?.displayName ?? "Linked customer";
  const status = item.group === "Customers" ? extractCustomerStatus(item.summary) : "Matched";

  return {
    id: item.id,
    sourceId: extractDemoSearchSourceId(item),
    title: item.title,
    customer,
    resultType: item.group,
    reference: item.meta,
    status,
    statusTone: status === "Matched" ? "info" : mapClientStatusTone(status),
    clientId: item.clientId,
    summary: item.summary,
    detail: item.meta,
  };
}

function mapClientStatusTone(status: string): StatusTone {
  switch (status) {
    case "ACTIVE":
      return "success";
    case "INACTIVE":
      return "warning";
    case "ARCHIVED":
      return "neutral";
    default:
      return "info";
  }
}

function extractCustomerStatus(summary: string) {
  if (summary.includes("ACTIVE")) {
    return "ACTIVE";
  }
  if (summary.includes("INACTIVE")) {
    return "INACTIVE";
  }
  if (summary.includes("ARCHIVED")) {
    return "ARCHIVED";
  }
  return "Matched";
}

function compareRows(left: SearchRow, right: SearchRow, field: SortableField, direction: "asc" | "desc") {
  const leftValue = String(left[field] ?? "").toLowerCase();
  const rightValue = String(right[field] ?? "").toLowerCase();
  const result = leftValue.localeCompare(rightValue);
  return direction === "asc" ? result : -result;
}

function updateSearchParams(
  setSearchParams: ReturnType<typeof useSearchParams>[1],
  searchParams: URLSearchParams,
  nextValues: Record<string, string | null>,
) {
  const next = new URLSearchParams(searchParams);

  Object.entries(nextValues).forEach(([key, value]) => {
    if (!value) {
      next.delete(key);
      return;
    }
    next.set(key, value);
  });

  setSearchParams(next, { replace: true });
}

function parsePositiveNumber(value: string | null, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback;
}

function openCustomer(clientId: string, navigate: ReturnType<typeof useNavigate>) {
  navigate(`/clients/${clientId}`);
}

function canOpenSearchViewer(row: SearchRow | null) {
  return row?.resultType === "Documents" || row?.resultType === "Emails";
}

function matchSearchDocument(row: SearchRow, documents: ClientDocumentSummary[]) {
  return documents.find((document) => document.id === row.sourceId || document.title === row.title) ?? null;
}

function matchSearchEmail(row: SearchRow, emails: ClientEmailSummary[]) {
  return emails.find((email) => email.id === row.sourceId || email.subject === row.title) ?? null;
}

function buildSearchViewerDocument(
  row: SearchRow,
  document: ClientDocumentSummary | null,
  email: ClientEmailSummary | null,
) {
  if (row.resultType === "Emails") {
    return {
      id: email?.id ?? row.id,
      title: email?.subject ?? row.title,
      subtitle: row.customer,
      fileKind: "email" as const,
      previewUrl: null,
      downloadUrl: null,
      originalUrl: null,
      unsupportedReason: "Inline email preview is not available in the current API.",
      pages: [{ id: `${row.id}-message`, label: "Message 1", pageNumber: 1 }],
    };
  }

  return {
    id: document?.id ?? row.id,
    title: document?.title ?? row.title,
    subtitle: `${row.customer} · ${row.reference}`,
    fileKind: "pdf" as const,
    previewUrl: document ? `/api/documents/${document.id}/preview` : null,
    downloadUrl: document ? `/api/documents/${document.id}/download` : null,
    originalUrl: document ? `/api/documents/${document.id}/preview` : null,
    originalActionLabel: "Open document",
    pages: [{ id: `${row.id}-page-1`, label: "Page 1", pageNumber: 1 }],
    unsupportedReason: "A document result was found, but the preview asset is not available from the current API response.",
  };
}

function buildSearchViewerEvidenceSections(
  row: SearchRow,
  document: ClientDocumentSummary | null,
  email: ClientEmailSummary | null,
  workspace: Awaited<ReturnType<typeof getDemoClientWorkspace>> | undefined,
  relatedLinks: RelatedKnowledgeLink[],
  versions: DocumentVersionSummary[],
): EvidenceWorkspaceSection[] {
  const businessReferences = collectKnowledgeBusinessReferenceFields(relatedLinks);
  const relatedDocuments = relatedLinks.filter((link) => link.relatedSourceType === "DOCUMENT" || link.relatedSourceType === "DOCUMENT_VERSION");
  const relatedEmails = relatedLinks.filter((link) => link.relatedSourceType === "EMAIL" || link.sourceType === "EMAIL");

  return [
    {
      key: "ai-summary",
      title: "AI Summary",
      defaultExpanded: true,
      summary: workspace?.aiSummaries[0] ? "Search AI summary available" : "No AI summary payload",
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
      summary: "Matched search evidence",
      countLabel: "1 item",
      searchText: `${row.title} ${row.detail} ${row.reference}`,
      content: (
        <EvidenceCard
          title={row.title}
          excerpt={row.detail}
          citation={row.reference}
          source={document?.source ?? email?.sender ?? row.resultType}
          type={row.resultType}
          confidence={row.status === "Matched" ? "MEDIUM" : "HIGH"}
          metadata={[
            `Status: ${row.status}`,
            document ? `Processing: ${document.processingStatus}` : email ? `Sender: ${email.sender}` : `Customer: ${row.customer}`,
          ]}
          navigationActions={[
            { key: "search-page", label: "Jump to page", kind: "page", disabled: true },
            { key: "search-region", label: "Jump to OCR region", kind: "ocr-region", disabled: true },
            { key: "search-highlight", label: "Jump to highlight", kind: "highlight", disabled: true },
          ]}
        />
      ),
    },
    {
      key: "fields",
      title: "Extracted Fields",
      summary: "Search results do not expose extracted field payloads",
      content: (
        <EvidenceMetadataGroup
          title="Field availability"
          fields={[
            {
              key: "search-fields",
              label: "Extracted fields",
              value: "Not exposed by the current Search API result payload.",
              state: "READ_ONLY",
            },
            {
              key: "search-review",
              label: "Manual review",
              value: "Open Review or Customer360 for deeper document context.",
              state: "READ_ONLY",
            },
          ]}
        />
      ),
    },
    {
      key: "business-context",
      title: "Related Business Context",
      summary: businessReferences.length > 0 ? "Customer context and related business reference fields" : "Customer and related knowledge context",
      content: (
        <Stack spacing={1}>
          <EvidenceMetadataGroup
            title="Customer"
            fields={[
              {
                key: "search-customer",
                label: "Customer",
                value: row.customer,
                state: "VERIFIED",
                confidence: "HIGH",
              },
            ]}
          />
          {businessReferences.length > 0 ? (
            <EvidenceMetadataGroup
              title="Business Reference Fields"
              fields={businessReferences.map((reference, index) => ({
                key: `search-reference-${index}`,
                label: reference.label,
                value: reference.value,
                state: "VERIFIED" as const,
                confidence: "HIGH" as const,
              }))}
            />
          ) : null}
          {relatedDocuments.length > 0 ? (
            <EvidenceMetadataGroup
              title="Related documents"
              fields={relatedDocuments.slice(0, 4).map((link, index) => ({
                key: `search-related-document-${index}`,
                label: humanizeKnowledgeRelationshipType(link.relationshipType),
                value: `${link.relatedTitle} · ${link.explanation}`,
                state: link.inferred ? "NEEDS_REVIEW" : "READ_ONLY",
              }))}
            />
          ) : null}
          {relatedEmails.length > 0 ? (
            <EvidenceMetadataGroup
              title="Related emails"
              fields={relatedEmails.slice(0, 3).map((link, index) => ({
                key: `search-related-email-${index}`,
                label: humanizeKnowledgeRelationshipType(link.relationshipType),
                value: link.sourceType === "EMAIL" ? link.sourceTitle : link.relatedTitle,
                state: link.inferred ? "NEEDS_REVIEW" : "READ_ONLY",
              }))}
            />
          ) : (
            <EvidenceMetadataGroup
              title="Related knowledge"
              fields={[
                {
                  key: "search-related",
                  label: "Result record",
                  value: row.title,
                  state: "READ_ONLY",
                },
              ]}
            />
          )}
        </Stack>
      ),
    },
    {
      key: "document-information",
      title: "Document Information",
      summary: "Result metadata and timing",
      content: (
        <EvidenceMetadataGroup
          title="Document information"
          fields={[
            {
              key: "result-type",
              label: "Result type",
              value: row.resultType,
              state: "READ_ONLY",
            },
            {
              key: "result-reference",
              label: "Reference",
              value: row.reference,
              state: "READ_ONLY",
            },
            {
              key: "result-status",
              label: "Status",
              value: row.status,
              state: "READ_ONLY",
            },
            {
              key: "source-info",
              label: "Source",
              value: document?.source ?? email?.sender ?? "Not available",
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
    },
    {
      key: "activity",
      title: "Activity",
      summary: "Available history and comments",
      content: (
        <EvidenceMetadataGroup
          title="Activity"
          fields={[
            {
              key: "occurred-at",
              label: "Occurred",
              value: row.updatedAt ? formatDate(String(row.updatedAt)) : "Not available",
              state: row.updatedAt ? "READ_ONLY" : "MISSING",
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

function buildSearchKnowledgeContextSections(
  _row: SearchRow,
  relatedLinks: RelatedKnowledgeLink[],
  timelineEvents: CustomerKnowledgeTimelineEvent[],
  versions: DocumentVersionSummary[],
): ContextSection[] {
  const businessReferences = collectKnowledgeBusinessReferenceFields(relatedLinks);

  if (relatedLinks.length === 0 && timelineEvents.length === 0 && versions.length === 0 && businessReferences.length === 0) {
    return [];
  }

  return [
    {
      key: "knowledge-context",
      title: "Related Knowledge",
      content: (
        <Stack spacing={0.75}>
          {relatedLinks.slice(0, 3).map((link) => (
            <Typography key={link.relationshipId} variant="body2" color="text.secondary">
              {link.inferred ? "Possible related source" : "Related source"}: {link.relatedTitle} · {humanizeKnowledgeRelationshipType(link.relationshipType)}
            </Typography>
          ))}
          {versions.length > 0 ? (
            <Typography variant="body2" color="text.secondary">
              Version history: {summarizeKnowledgeVersions(versions)}
            </Typography>
          ) : null}
          {timelineEvents.slice(0, 2).map((event) => (
            <Typography key={event.eventId} variant="body2" color="text.secondary">
              Recent customer knowledge: {event.title}
            </Typography>
          ))}
          {businessReferences.slice(0, 3).map((reference) => (
            <Typography key={`${reference.key}-${reference.value}`} variant="body2" color="text.secondary">
              {reference.label}: {reference.value}
            </Typography>
          ))}
        </Stack>
      ),
    },
  ];
}

function mapSearchRowSourceType(row: SearchRow) {
  switch (row.resultType) {
    case "Customers":
      return "CUSTOMER";
    case "Documents":
      return "DOCUMENT";
    case "Emails":
      return "EMAIL";
    case "Notes":
      return "NOTE";
    default:
      return null;
  }
}

function resolveSearchSourceId(row: SearchRow) {
  return row.sourceId ?? row.clientId;
}

function extractDemoSearchSourceId(item: DemoWorkspaceSearchItem) {
  switch (item.group) {
    case "Customers":
      return item.clientId;
    case "Documents":
      return item.id.replace(/^document-/, "");
    case "Emails":
      return item.id.replace(/^email-/, "");
    case "Notes":
      return item.id.replace(/^note-/, "");
    case "Knowledge":
      return item.id.replace(/^knowledge-/, "");
    case "Policy References":
      return item.id.replace(/^policy-/, "");
    case "Claim References":
      return item.id.replace(/^claim-/, "");
    default:
      return item.id;
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

function downloadCsv(rows: SearchRow[]) {
      const header = ["Title", "Customer", "Type", "Reference", "Status"];
  const lines = rows.map((row) =>
    [row.title, row.customer, row.resultType, row.reference, row.status]
      .map((value) => `"${String(value).split('"').join('""')}"`)
      .join(","),
  );
  const csv = [header.join(","), ...lines].join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const href = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = href;
  link.download = "ikms-search-results.csv";
  link.click();
  URL.revokeObjectURL(href);
}

function getResultTypeIcon(resultType: DemoSearchGroup): ReactElement {
  switch (resultType) {
    case "Customers":
      return <PersonOutlineOutlinedIcon fontSize="small" />;
    case "Documents":
      return <DescriptionOutlinedIcon fontSize="small" />;
    case "Emails":
      return <MailOutlineOutlinedIcon fontSize="small" />;
    case "Notes":
      return <StickyNote2OutlinedIcon fontSize="small" />;
    case "Knowledge":
      return <SearchOutlinedIcon fontSize="small" />;
    case "Policy References":
      return <PolicyOutlinedIcon fontSize="small" />;
    case "Claim References":
      return <AssignmentOutlinedIcon fontSize="small" />;
    default:
      return <FactCheckOutlinedIcon fontSize="small" />;
  }
}
