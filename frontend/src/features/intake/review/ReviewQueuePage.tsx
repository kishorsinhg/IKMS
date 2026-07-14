import AssignmentTurnedInOutlinedIcon from "@mui/icons-material/AssignmentTurnedInOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import LinkOutlinedIcon from "@mui/icons-material/LinkOutlined";
import OpenInFullOutlinedIcon from "@mui/icons-material/OpenInFullOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import TaskAltOutlinedIcon from "@mui/icons-material/TaskAltOutlined";
import {
  Alert,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Drawer,
  FormControl,
  IconButton,
  InputLabel,
  List,
  ListItemButton,
  ListItemText,
  MenuItem,
  Select,
  Stack,
  TextField,
  Tooltip,
  Typography,
  useMediaQuery,
} from "@mui/material";
import { useTheme } from "@mui/material/styles";
import { GridColDef, GridPaginationModel, GridSortModel } from "@mui/x-data-grid";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { KeyboardEvent as ReactKeyboardEvent, useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useOutletContext, useSearchParams } from "react-router-dom";
import { listDocumentTypes, listMetadataFields } from "../../../api/admin";
import { ClientSummary, listClients } from "../../../api/clients";
import {
  approveReviewItem,
  correctReviewItemMetadata,
  getReviewQueueItem,
  linkReviewItemClient,
  listReviewQueue,
  rejectReviewItem,
  ReviewQueueItem,
  ReviewQueueReason,
  ReviewQueueStatus,
} from "../../../api/intake";
import { EntityGrid } from "../../../app/components/EntityGrid";
import type { ContextSection } from "../../../app/components/RightContextPanel";
import { StatusBadge, StatusTone } from "../../../app/components/StatusBadge";
import { WorkspaceToolbar } from "../../../app/components/WorkspaceToolbar";
import { useNotification } from "../../../app/providers/useNotification";
import type { IkmsShellOutletContext, ShellWorkspaceChrome } from "../../../app/shell/IkmsAppShell";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  NoResultsState,
  RetryAction,
} from "../../../app/WorkspaceStates";

type ReviewSortField = "title" | "customer" | "itemType" | "reason" | "status" | "assignedTo";
type ReviewGridRow = ReviewQueueItem & Record<string, unknown> & {
  customerName: string;
  reviewTypeLabel: string;
  reasonLabel: string;
  reasonTone: StatusTone;
  statusTone: StatusTone;
  assignmentLabel: string;
};

interface MetadataDraft {
  title: string;
  documentTypeId: string;
  metadataValues: Record<string, string>;
}

const reviewQueueKey = ["review-queue"] as const;
const defaultPageSize = 10;

const statusOptions: Array<{ value: ReviewQueueStatus | "ALL"; label: string }> = [
  { value: "ALL", label: "All statuses" },
  { value: "OPEN", label: "Open" },
  { value: "IN_PROGRESS", label: "In progress" },
  { value: "RESOLVED", label: "Resolved" },
  { value: "REJECTED", label: "Rejected" },
];

const reasonOptions: Array<{ value: ReviewQueueReason | "ALL"; label: string }> = [
  { value: "ALL", label: "All reasons" },
  { value: "UNLINKED", label: "Unlinked" },
  { value: "LOW_CLIENT_CONFIDENCE", label: "Low client confidence" },
  { value: "LOW_CLASSIFICATION_CONFIDENCE", label: "Low classification confidence" },
  { value: "LOW_EXTRACTION_CONFIDENCE", label: "Low extraction confidence" },
  { value: "DUPLICATE_UNCERTAINTY", label: "Duplicate uncertainty" },
  { value: "REDACTION_FAILED", label: "Redaction failed" },
  { value: "PROMPT_INJECTION_RISK", label: "Prompt injection risk" },
  { value: "PROCESSING_FAILED", label: "Processing failed" },
];

export function ReviewQueuePage() {
  const theme = useTheme();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { notify } = useNotification();
  const { setWorkspaceChrome, clearWorkspaceChrome } = useOutletContext<IkmsShellOutletContext>();
  const [searchParams, setSearchParams] = useSearchParams();
  const isTabletDown = useMediaQuery(theme.breakpoints.down("lg"));
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));
  const [localQuery, setLocalQuery] = useState(searchParams.get("q") ?? "");
  const [linkDialogOpen, setLinkDialogOpen] = useState(false);
  const [metadataDialogOpen, setMetadataDialogOpen] = useState(false);
  const [approveDialogOpen, setApproveDialogOpen] = useState(false);
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [shortcutsDialogOpen, setShortcutsDialogOpen] = useState(false);
  const [mobileDetailOpen, setMobileDetailOpen] = useState(false);
  const [selectedClientId, setSelectedClientId] = useState("");
  const [rejectReason, setRejectReason] = useState("");
  const [metadataDraft, setMetadataDraft] = useState<MetadataDraft>({
    title: "",
    documentTypeId: "",
    metadataValues: {},
  });

  const query = searchParams.get("q") ?? "";
  const selectedStatus = parseStatusFilter(searchParams.get("status"));
  const selectedReason = parseReasonFilter(searchParams.get("reason"));
  const selectedId = searchParams.get("selected");
  const sortField = parseSortField(searchParams.get("sort"));
  const sortDirection = searchParams.get("dir") === "desc" ? "desc" : "asc";
  const page = parsePositiveInt(searchParams.get("page"), 0);
  const pageSize = parsePositiveInt(searchParams.get("pageSize"), defaultPageSize);

  const queueQuery = useQuery({
    queryKey: [...reviewQueueKey, selectedStatus, selectedReason],
    queryFn: () => listReviewQueue(selectedStatus === "ALL" ? "" : selectedStatus, selectedReason === "ALL" ? "" : selectedReason),
  });
  const detailQuery = useQuery({
    queryKey: [...reviewQueueKey, "detail", selectedId],
    queryFn: () => getReviewQueueItem(selectedId!),
    enabled: Boolean(selectedId),
  });
  const clientsQuery = useQuery({
    queryKey: ["clients", "review-lookup"],
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

  const clientLookup = useMemo(
    () => new Map((clientsQuery.data ?? []).map((client) => [client.id, client])),
    [clientsQuery.data],
  );

  useEffect(() => {
    setLocalQuery(query);
  }, [query]);

  const queueRows = useMemo<ReviewGridRow[]>(() => {
    return (queueQuery.data ?? []).map((item) => {
      const linkedClient = item.clientId ? clientLookup.get(item.clientId) : null;
      return {
        ...item,
        customerName: linkedClient?.displayName ?? (item.clientId ? item.clientId : "Unlinked"),
        reviewTypeLabel: formatReviewType(item.itemType),
        reasonLabel: formatReason(item.reason),
        reasonTone: mapReasonTone(item.reason),
        statusTone: mapStatusTone(item.status),
        assignmentLabel: item.assignedTo ?? "Unassigned",
      };
    });
  }, [clientLookup, queueQuery.data]);

  const filteredRows = useMemo(() => {
    const trimmed = query.trim().toLowerCase();
    if (!trimmed) {
      return queueRows;
    }

    return queueRows.filter((row) =>
      [
        row.title ?? "",
        row.customerName,
        row.reviewTypeLabel,
        row.reasonLabel,
        row.status,
        row.assignmentLabel,
        row.itemId,
      ].some((value) => String(value).toLowerCase().includes(trimmed)),
    );
  }, [query, queueRows]);

  const sortModel = useMemo<GridSortModel>(() => [{ field: sortField, sort: sortDirection }], [sortDirection, sortField]);
  const sortedRows = useMemo(() => sortRows(filteredRows, sortModel), [filteredRows, sortModel]);
  const pagedRows = useMemo(
    () => sortedRows.slice(page * pageSize, page * pageSize + pageSize),
    [page, pageSize, sortedRows],
  );
  const selectedRow = useMemo(
    () => sortedRows.find((row) => row.id === selectedId) ?? null,
    [selectedId, sortedRows],
  );
  const selectedItem = detailQuery.data ?? selectedRow;
  const selectedClient = selectedItem?.clientId ? clientLookup.get(selectedItem.clientId) ?? null : null;

  useEffect(() => {
    if (!selectedId && sortedRows[0]) {
      setScopedSearchParams(setSearchParams, searchParams, { selected: sortedRows[0].id });
      return;
    }

    if (selectedId && !sortedRows.some((row) => row.id === selectedId)) {
      setScopedSearchParams(setSearchParams, searchParams, { selected: sortedRows[0]?.id ?? null, page: "0" });
    }
  }, [searchParams, selectedId, setSearchParams, sortedRows]);

  useEffect(() => {
    const maxPage = Math.max(0, Math.ceil(sortedRows.length / pageSize) - 1);
    if (page > maxPage) {
      setScopedSearchParams(setSearchParams, searchParams, { page: String(maxPage) });
    }
  }, [page, pageSize, searchParams, setSearchParams, sortedRows.length]);

  useEffect(() => {
    if (!selectedItem) {
      setSelectedClientId("");
      setMetadataDraft({ title: "", documentTypeId: "", metadataValues: {} });
      return;
    }

    setSelectedClientId(selectedItem.clientId ?? "");
    setMetadataDraft({
      title: selectedItem.title ?? "",
      documentTypeId: selectedItem.documentTypeId ?? "",
      metadataValues: selectedItem.metadataValues ?? {},
    });
  }, [selectedItem]);

  useEffect(() => {
    if (!isMobile) {
      setMobileDetailOpen(false);
    }
  }, [isMobile]);

  useEffect(() => {
    function handleKeyboardShortcuts(event: globalThis.KeyboardEvent) {
      if (!selectedItem) {
        return;
      }

      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "s") {
        event.preventDefault();
        setMetadataDialogOpen(true);
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
  }, [selectedItem]);

  const refreshQueue = async () => {
    await queryClient.invalidateQueries({ queryKey: reviewQueueKey });
  };

  const linkMutation = useMutation({
    mutationFn: () => linkReviewItemClient(selectedItem!.id, selectedClientId),
    onSuccess: async () => {
      notify({ severity: "success", message: "Review item linked to customer." });
      setLinkDialogOpen(false);
      await refreshQueue();
    },
    onError: () => {
      notify({ severity: "error", message: "Unable to link the review item." });
    },
  });

  const metadataMutation = useMutation({
    mutationFn: () =>
      correctReviewItemMetadata(selectedItem!.id, {
        title: metadataDraft.title.trim(),
        documentTypeId: metadataDraft.documentTypeId || undefined,
        metadataValues: trimMetadataValues(metadataDraft.metadataValues),
      }),
    onSuccess: async () => {
      notify({ severity: "success", message: "Review metadata updated." });
      setMetadataDialogOpen(false);
      await refreshQueue();
    },
    onError: () => {
      notify({ severity: "error", message: "Unable to update review metadata." });
    },
  });

  const approveMutation = useMutation({
    mutationFn: () => approveReviewItem(selectedItem!.id),
    onSuccess: async () => {
      notify({ severity: "success", message: "Review item approved." });
      setApproveDialogOpen(false);
      await refreshQueue();
    },
    onError: () => {
      notify({ severity: "error", message: "Unable to approve the review item." });
    },
  });

  const rejectMutation = useMutation({
    mutationFn: () => rejectReviewItem(selectedItem!.id, rejectReason.trim()),
    onSuccess: async () => {
      notify({ severity: "warning", message: "Review item rejected." });
      setRejectDialogOpen(false);
      setRejectReason("");
      await refreshQueue();
    },
    onError: () => {
      notify({ severity: "error", message: "Unable to reject the review item." });
    },
  });

  function applySearch() {
    setScopedSearchParams(setSearchParams, searchParams, {
      q: localQuery.trim() || null,
      page: "0",
      selected: null,
    });
  }

  function clearFilters() {
    setLocalQuery("");
    setScopedSearchParams(setSearchParams, searchParams, {
      q: null,
      status: null,
      reason: null,
      page: "0",
      selected: null,
    });
  }

  function handleSearchKeyDown(event: ReactKeyboardEvent<HTMLInputElement>) {
    if (event.key === "Enter") {
      event.preventDefault();
      applySearch();
    }
  }

  const handleRowSelect = useCallback((itemId: string) => {
    setScopedSearchParams(setSearchParams, searchParams, { selected: itemId });
    if (isMobile) {
      setMobileDetailOpen(true);
    }
  }, [isMobile, searchParams, setSearchParams]);

  const openSelectedItem = useCallback((itemId: string) => {
    handleRowSelect(itemId);
    requestAnimationFrame(() => {
      const row = document.querySelector(`[data-id="${itemId}"]`) as HTMLElement | null;
      row?.scrollIntoView({ block: "nearest" });
      row?.focus();
    });
  }, [handleRowSelect]);

  const navigateToReviewItem = useCallback((itemId: string) => {
    openSelectedItem(itemId);
    const query = searchParams.toString();
    navigate(`/review-queue/${itemId}${query ? `?${query}` : ""}`, {
      state: { from: `/review-queue${query ? `?${query}` : ""}` },
    });
  }, [navigate, openSelectedItem, searchParams]);

  function handleSortModelChange(model: GridSortModel) {
    const next = model[0];
    setScopedSearchParams(setSearchParams, searchParams, {
      sort: next?.field ?? "title",
      dir: next?.sort ?? "asc",
    });
  }

  function handlePaginationModelChange(model: GridPaginationModel) {
    setScopedSearchParams(setSearchParams, searchParams, {
      page: String(model.page),
      pageSize: String(model.pageSize),
    });
  }

  const activeFilters = buildActiveFilters({
    query,
    selectedStatus,
    selectedReason,
    onClearQuery: () => {
      setLocalQuery("");
      setScopedSearchParams(setSearchParams, searchParams, { q: null, page: "0", selected: null });
    },
    onClearStatus: () => setScopedSearchParams(setSearchParams, searchParams, { status: null, page: "0", selected: null }),
    onClearReason: () => setScopedSearchParams(setSearchParams, searchParams, { reason: null, page: "0", selected: null }),
  });

  const toolbarSecondaryActions = selectedItem
    ? [
        { key: "open-review-item", label: "Open review item", onClick: () => navigateToReviewItem(selectedItem.id) },
        { key: "link-customer", label: "Link customer", onClick: () => setLinkDialogOpen(true) },
        { key: "edit-metadata", label: "Edit metadata", onClick: () => setMetadataDialogOpen(true) },
        { key: "reject-item", label: "Reject item", onClick: () => setRejectDialogOpen(true) },
        { key: "keyboard-shortcuts", label: "Keyboard shortcuts", onClick: () => setShortcutsDialogOpen(true) },
      ]
    : [
        { key: "keyboard-shortcuts", label: "Keyboard shortcuts", onClick: () => setShortcutsDialogOpen(true) },
      ];

  const selectedActionStrip = !isMobile && selectedItem ? (
    <Stack direction="row" spacing={0.75} alignItems="center" useFlexGap flexWrap="wrap">
      <Chip
        size="small"
        variant="outlined"
        label={`${sortedRows.length} ${sortedRows.length === 1 ? "item" : "items"}`}
      />
      <Chip size="small" variant="outlined" color="info" label="Review item selected" />
      <Button
        size="small"
        variant="outlined"
        startIcon={<OpenInFullOutlinedIcon fontSize="small" />}
        onClick={() => navigateToReviewItem(selectedItem.id)}
      >
        Open review item
      </Button>
      <Button
        size="small"
        variant="contained"
        startIcon={<TaskAltOutlinedIcon fontSize="small" />}
        onClick={() => setApproveDialogOpen(true)}
      >
        Approve
      </Button>
      <Button size="small" variant="text" color="inherit" onClick={() => setLinkDialogOpen(true)}>
        Link customer
      </Button>
      <Button size="small" variant="text" color="inherit" onClick={() => setMetadataDialogOpen(true)}>
        Edit metadata
      </Button>
      <Button size="small" color="error" variant="text" onClick={() => setRejectDialogOpen(true)}>
        Reject
      </Button>
    </Stack>
  ) : null;

  const chrome = useMemo<ShellWorkspaceChrome>(() => ({
    title: "Review",
    subtitle: "Resolve intake exceptions, validate extracted data, and complete human review without leaving the queue.",
    contextTitle: selectedItem ? "Selected Review Item" : "Review Context",
    contextWidth: 336,
    contextSections: buildContextSections({
      selectedItem,
      selectedClient,
      metadataFields: metadataFieldsQuery.data ?? [],
      onLink: () => setLinkDialogOpen(true),
      onEditMetadata: () => setMetadataDialogOpen(true),
      onOpen: () => {
        if (selectedItem) {
          navigateToReviewItem(selectedItem.id);
        }
      },
      onApprove: () => setApproveDialogOpen(true),
      onReject: () => setRejectDialogOpen(true),
      totalVisible: sortedRows.length,
      query,
      selectedStatus,
      selectedReason,
    }),
  }), [
    metadataFieldsQuery.data,
    navigateToReviewItem,
    query,
    selectedClient,
    selectedItem,
    selectedReason,
    selectedStatus,
    sortedRows.length,
  ]);

  useEffect(() => {
    setWorkspaceChrome(chrome);
    return () => clearWorkspaceChrome();
  }, [chrome, clearWorkspaceChrome, setWorkspaceChrome]);

  const columns = useMemo<GridColDef<ReviewGridRow>[]>(() => {
    const desktopColumns: GridColDef<ReviewGridRow>[] = [
      {
        field: "title",
        headerName: "Review item",
        flex: 1.4,
        minWidth: 260,
        renderCell: ({ row }) => (
          <Stack spacing={0.25} sx={{ py: 0.5, minWidth: 0 }}>
            <Typography variant="body2" fontWeight={600} noWrap title={row.title ?? row.itemId}>
              {row.title ?? row.itemId}
            </Typography>
            <Typography variant="caption" color="text.secondary" noWrap title={row.itemId}>
              {row.itemId}
            </Typography>
          </Stack>
        ),
      },
      {
        field: "customerName",
        headerName: "Customer",
        flex: 1,
        minWidth: 180,
        renderCell: ({ row }) => (
          <Typography variant="body2" noWrap title={row.customerName}>
            {row.customerName}
          </Typography>
        ),
      },
      {
        field: "itemType",
        headerName: "Review type",
        flex: 0.8,
        minWidth: 150,
        valueGetter: (_, row) => row.reviewTypeLabel,
      },
      {
        field: "reason",
        headerName: "Reason",
        flex: 0.95,
        minWidth: 200,
        renderCell: ({ row }) => <StatusBadge label={row.reasonLabel} tone={row.reasonTone} />,
      },
      {
        field: "status",
        headerName: "Status",
        flex: 0.7,
        minWidth: 140,
        renderCell: ({ row }) => <StatusBadge label={formatStatus(row.status)} tone={row.statusTone} />,
      },
      {
        field: "assignedTo",
        headerName: "Assigned",
        flex: 0.75,
        minWidth: 140,
        valueGetter: (_, row) => row.assignmentLabel,
      },
      {
        field: "actions",
        headerName: "Actions",
        width: 96,
        sortable: false,
        filterable: false,
        renderCell: ({ row }) => (
          <Tooltip title="Open review item">
            <IconButton
              size="small"
              aria-label={`Open review item ${row.title ?? row.itemId}`}
              onClick={(event) => {
                event.stopPropagation();
                navigateToReviewItem(row.id);
              }}
            >
              <OpenInFullOutlinedIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        ),
      },
    ];

    if (isTabletDown) {
      return desktopColumns.filter((column) => !["assignedTo"].includes(String(column.field)));
    }

    return desktopColumns;
  }, [isTabletDown, navigateToReviewItem]);

  const gridHeight = Math.min(Math.max(276, 108 + Math.max(2, sortedRows.length) * 44), 700);
  const hasActiveFilters = activeFilters.length > 0;
  const paginationModel = { page, pageSize };

  if (queueQuery.isLoading && !queueQuery.data) {
    return (
      <LoadingState
        title="Loading review queue"
        message="Preparing the operational review queue."
      />
    );
  }

  if (queueQuery.isError) {
    return (
      <ErrorState
        title="Unable to load the review queue"
        message="The current review items could not be retrieved."
        action={<RetryAction onClick={() => void queueQuery.refetch()} />}
      />
    );
  }

  return (
    <Stack spacing={1.5}>
      <WorkspaceToolbar
        searchPlaceholder="Search by title, customer, queue item, or assignment"
        searchValue={localQuery}
        onSearchChange={setLocalQuery}
        onSearchKeyDown={handleSearchKeyDown}
        searchAriaLabel="Review queue query"
        filters={(
          <Stack spacing={1} sx={{ width: { xs: "100%", xl: "auto" } }}>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ width: "100%" }}>
              <FormControl size="small" sx={{ minWidth: 0, flex: 1 }}>
                <Select<ReviewQueueStatus | "ALL">
                  value={selectedStatus}
                  inputProps={{ "aria-label": "Review status" }}
                  onChange={(event) => {
                    setScopedSearchParams(setSearchParams, searchParams, {
                      status: event.target.value === "ALL" ? null : String(event.target.value),
                      page: "0",
                      selected: null,
                    });
                  }}
                >
                  {statusOptions.map((option) => (
                    <MenuItem key={option.value} value={option.value}>
                      {option.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 0, flex: 1.25 }}>
                <Select<ReviewQueueReason | "ALL">
                  value={selectedReason}
                  inputProps={{ "aria-label": "Review reason" }}
                  onChange={(event) => {
                    setScopedSearchParams(setSearchParams, searchParams, {
                      reason: event.target.value === "ALL" ? null : String(event.target.value),
                      page: "0",
                      selected: null,
                    });
                  }}
                >
                  {reasonOptions.map((option) => (
                    <MenuItem key={option.value} value={option.value}>
                      {option.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              {hasActiveFilters ? (
                <Button variant="text" color="inherit" onClick={clearFilters}>
                  Clear
                </Button>
              ) : null}
            </Stack>
          </Stack>
        )}
        activeFilters={activeFilters}
        bulkActions={selectedActionStrip}
        primaryAction={(
          <Button variant="contained" startIcon={<SearchOutlinedIcon fontSize="small" />} onClick={applySearch}>
            Search
          </Button>
        )}
        onRefresh={() => void queueQuery.refetch()}
        secondaryActions={toolbarSecondaryActions}
      />

      <Dialog open={shortcutsDialogOpen} onClose={() => setShortcutsDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Keyboard shortcuts</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={1}>
            <Typography variant="body2"><strong>Ctrl/Cmd + S</strong> Edit metadata</Typography>
            <Typography variant="body2"><strong>Ctrl/Cmd + Enter</strong> Approve</Typography>
            <Typography variant="body2"><strong>Ctrl/Cmd + R</strong> Reject</Typography>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShortcutsDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      <Box
        sx={{
          border: (appTheme) => `1px solid ${appTheme.palette.divider}`,
          borderRadius: 1,
          backgroundColor: "background.paper",
          overflow: "hidden",
        }}
      >
        <Stack spacing={1} sx={{ px: 2, py: 1.25 }}>
          {sortedRows.length === 0 ? (
            hasActiveFilters || query.trim() ? (
              <NoResultsState
                title="No review items match these filters"
                message="Try a broader search or remove one of the current queue filters."
                action={<RetryAction label="Clear filters" onClick={clearFilters} />}
                compact
              />
            ) : (
              <EmptyState
                title="No review items are waiting"
                message="The review queue is currently empty for the selected status and reason."
                action={<RetryAction label="Refresh queue" onClick={() => void queueQuery.refetch()} />}
                compact
              />
            )
          ) : isMobile ? (
            <MobileReviewList rows={pagedRows} selectedId={selectedId} onSelect={handleRowSelect} />
          ) : (
            <EntityGrid<ReviewGridRow>
              rows={sortedRows}
              columns={columns}
              getRowId={(row) => row.id}
              pagination
              paginationMode="client"
              paginationModel={paginationModel}
              onPaginationModelChange={handlePaginationModelChange}
              sortModel={sortModel}
              onSortModelChange={handleSortModelChange}
              onRowClick={({ row }) => handleRowSelect(row.id)}
              onRowDoubleClick={({ row }) => navigateToReviewItem(row.id)}
              onCellKeyDown={(params, event) => {
                if (event.key === "Enter") {
                  navigateToReviewItem(params.row.id);
                }
              }}
              getRowClassName={({ row }) => (row.id === selectedId ? "ikms-selected-row" : "")}
              emptyTitle="No review items"
              emptyMessage="There are no review items for the current queue view."
              sx={{
                minHeight: gridHeight,
                height: gridHeight,
                "& .ikms-selected-row": {
                  backgroundColor: theme.palette.action.selected,
                  boxShadow: `inset 3px 0 0 ${theme.palette.primary.main}`,
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

      <Drawer
        anchor="right"
        open={isMobile && mobileDetailOpen && Boolean(selectedItem)}
        onClose={() => setMobileDetailOpen(false)}
        PaperProps={{ sx: { width: "100%", maxWidth: "100%" } }}
      >
        <Stack spacing={0} sx={{ height: "100%" }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ px: 2, py: 1.25, borderBottom: (theme) => `1px solid ${theme.palette.divider}` }}>
            <Typography variant="subtitle2">Selected review item</Typography>
            <IconButton aria-label="Close detail" onClick={() => setMobileDetailOpen(false)}>
              <CloseOutlinedIcon fontSize="small" />
            </IconButton>
          </Stack>
          <Box sx={{ overflowY: "auto", px: 2, py: 1.5 }}>
            {selectedItem ? (
              <SelectedReviewDetail
                item={selectedItem}
                client={selectedClient}
                metadataFields={metadataFieldsQuery.data?.map((field) => field.fieldKey) ?? []}
                onOpen={() => navigateToReviewItem(selectedItem.id)}
                onLink={() => setLinkDialogOpen(true)}
                onEditMetadata={() => setMetadataDialogOpen(true)}
                onApprove={() => setApproveDialogOpen(true)}
                onReject={() => setRejectDialogOpen(true)}
              />
            ) : null}
          </Box>
        </Stack>
      </Drawer>

      <Dialog open={linkDialogOpen} onClose={() => setLinkDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Link review item to customer</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={1.5} sx={{ pt: 0.5 }}>
            <Typography variant="body2" color="text.secondary">
              Select the customer that should own this review item.
            </Typography>
            <FormControl size="small" fullWidth>
              <InputLabel id="review-link-customer-label">Customer</InputLabel>
              <Select
                labelId="review-link-customer-label"
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
            disabled={!selectedItem || !selectedClientId || linkMutation.isPending}
          >
            {linkMutation.isPending ? "Linking..." : "Link customer"}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={metadataDialogOpen} onClose={() => setMetadataDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Edit review metadata</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={1.5} sx={{ pt: 0.5 }}>
            <TextField
              label="Title"
              value={metadataDraft.title}
              onChange={(event) => setMetadataDraft((current) => ({ ...current, title: event.target.value }))}
              autoFocus
            />
            <FormControl size="small" fullWidth>
              <InputLabel id="review-document-type-label">Document Type</InputLabel>
              <Select
                labelId="review-document-type-label"
                label="Document Type"
                value={metadataDraft.documentTypeId}
                onChange={(event) => setMetadataDraft((current) => ({ ...current, documentTypeId: String(event.target.value) }))}
              >
                <MenuItem value="">None</MenuItem>
                {(documentTypesQuery.data ?? []).map((item) => (
                  <MenuItem key={item.id} value={item.id}>
                    {item.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            {(metadataFieldsQuery.data ?? []).map((field) => (
              <TextField
                key={field.id}
                label={field.label}
                value={metadataDraft.metadataValues[field.fieldKey] ?? ""}
                onChange={(event) =>
                  setMetadataDraft((current) => ({
                    ...current,
                    metadataValues: {
                      ...current.metadataValues,
                      [field.fieldKey]: event.target.value,
                    },
                  }))
                }
              />
            ))}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setMetadataDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={() => metadataMutation.mutate()}
            disabled={!selectedItem || !metadataDraft.title.trim() || metadataMutation.isPending}
          >
            {metadataMutation.isPending ? "Saving..." : "Save metadata"}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={approveDialogOpen} onClose={() => setApproveDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Approve review item</DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" color="text.secondary">
            Approve this item after confirming the customer link, title, document type, and extracted metadata.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setApproveDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={() => approveMutation.mutate()}
            disabled={!selectedItem || approveMutation.isPending}
          >
            {approveMutation.isPending ? "Approving..." : "Approve"}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={rejectDialogOpen} onClose={() => setRejectDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Reject review item</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={1.5} sx={{ pt: 0.5 }}>
            <Alert severity="warning" variant="outlined">
              Rejection keeps the decision human-controlled and records an explicit reason for follow-up.
            </Alert>
            <TextField
              label="Rejection reason"
              value={rejectReason}
              onChange={(event) => setRejectReason(event.target.value)}
              multiline
              minRows={3}
              autoFocus
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectDialogOpen(false)}>Cancel</Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => rejectMutation.mutate()}
            disabled={!selectedItem || !rejectReason.trim() || rejectMutation.isPending}
          >
            {rejectMutation.isPending ? "Rejecting..." : "Reject"}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

function MobileReviewList({
  rows,
  selectedId,
  onSelect,
}: {
  rows: ReviewGridRow[];
  selectedId: string | null;
  onSelect: (itemId: string) => void;
}) {
  return (
    <List disablePadding sx={{ display: "grid", gap: 0.75 }}>
      {rows.map((row) => (
        <Box
          key={row.id}
          sx={{
            border: (theme) => `1px solid ${theme.palette.divider}`,
            borderRadius: 1,
            borderColor: row.id === selectedId ? "primary.main" : "divider",
            backgroundColor: row.id === selectedId ? "action.selected" : "background.paper",
            overflow: "hidden",
          }}
        >
          <ListItemButton onClick={() => onSelect(row.id)} alignItems="flex-start" sx={{ px: 1.5, py: 1.25 }}>
            <ListItemText
              primary={(
                <Stack spacing={0.75}>
                  <Stack direction="row" spacing={1} justifyContent="space-between" alignItems="flex-start">
                    <Typography variant="body2" fontWeight={600} sx={{ pr: 1 }}>
                      {row.title ?? row.itemId}
                    </Typography>
                    <StatusBadge label={formatStatus(row.status)} tone={row.statusTone} />
                  </Stack>
                  <Typography variant="body2" color="text.secondary">
                    {row.customerName}
                  </Typography>
                  <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
                    <StatusBadge label={row.reasonLabel} tone={row.reasonTone} />
                    <Typography variant="caption" color="text.secondary">
                      {row.reviewTypeLabel}
                    </Typography>
                  </Stack>
                </Stack>
              )}
            />
          </ListItemButton>
        </Box>
      ))}
    </List>
  );
}

function SelectedReviewDetail({
  item,
  client,
  metadataFields,
  onOpen,
  onLink,
  onEditMetadata,
  onApprove,
  onReject,
}: {
  item: ReviewQueueItem;
  client: ClientSummary | null;
  metadataFields: string[];
  onOpen: () => void;
  onLink: () => void;
  onEditMetadata: () => void;
  onApprove: () => void;
  onReject: () => void;
}) {
  return (
    <Stack spacing={1.5}>
      <Stack spacing={0.75}>
        <Typography variant="body2" fontWeight={600}>
          {item.title ?? item.itemId}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {formatReviewType(item.itemType)} · {item.itemId}
        </Typography>
        <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
          <StatusBadge label={formatReason(item.reason)} tone={mapReasonTone(item.reason)} variant="outlined" />
          <StatusBadge label={formatStatus(item.status)} tone={mapStatusTone(item.status)} />
        </Stack>
      </Stack>

      <Stack spacing={0.5}>
        <Typography variant="subtitle2">Customer context</Typography>
        <Typography variant="body2" color="text.secondary">
          {client ? `${client.displayName} (${client.clientId})` : "No customer linked"}
        </Typography>
      </Stack>

      <Stack spacing={0.5}>
        <Typography variant="subtitle2">Extracted metadata</Typography>
        <Typography variant="body2" color="text.secondary">
          {metadataFields.length > 0
            ? metadataFields
              .filter((field) => item.metadataValues[field])
              .map((field) => `${field}: ${item.metadataValues[field]}`)
              .join(" · ") || "No extracted metadata available"
            : "No configured metadata fields available"}
        </Typography>
      </Stack>

      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
        <Button size="small" variant="outlined" startIcon={<OpenInFullOutlinedIcon fontSize="small" />} onClick={onOpen}>
          Open review item
        </Button>
        <Button size="small" variant="contained" startIcon={<TaskAltOutlinedIcon fontSize="small" />} onClick={onApprove}>
          Approve
        </Button>
        <Button size="small" variant="outlined" startIcon={<LinkOutlinedIcon fontSize="small" />} onClick={onLink}>
          Link customer
        </Button>
        <Button size="small" variant="outlined" startIcon={<EditOutlinedIcon fontSize="small" />} onClick={onEditMetadata}>
          Edit metadata
        </Button>
        <Button size="small" color="error" variant="text" onClick={onReject}>
          Reject
        </Button>
      </Stack>
    </Stack>
  );
}

function buildContextSections({
  selectedItem,
  selectedClient,
  metadataFields,
  onLink,
  onEditMetadata,
  onOpen,
  onApprove,
  onReject,
  totalVisible,
  query,
  selectedStatus,
  selectedReason,
}: {
  selectedItem: ReviewQueueItem | null;
  selectedClient: ClientSummary | null;
  metadataFields: Array<{ fieldKey: string; label: string }>;
  onLink: () => void;
  onEditMetadata: () => void;
  onOpen: () => void;
  onApprove: () => void;
  onReject: () => void;
  totalVisible: number;
  query: string;
  selectedStatus: ReviewQueueStatus | "ALL";
  selectedReason: ReviewQueueReason | "ALL";
}): ContextSection[] {
  if (!selectedItem) {
    return [
      {
        key: "queue-summary",
        title: "Queue Summary",
        content: (
          <Stack spacing={0.75}>
            <Typography variant="body2" color="text.secondary">
              {totalVisible} visible {totalVisible === 1 ? "item" : "items"} in the current queue view.
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Status: {selectedStatus === "ALL" ? "All statuses" : formatStatus(selectedStatus)}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Reason: {selectedReason === "ALL" ? "All reasons" : formatReason(selectedReason)}
            </Typography>
            {query.trim() ? (
              <Typography variant="body2" color="text.secondary">
                Search: {query}
              </Typography>
            ) : null}
          </Stack>
        ),
      },
      {
        key: "queue-guidance",
        title: "Queue Guidance",
        content: (
          <Stack spacing={0.75}>
            <Typography variant="body2" color="text.secondary">
              Select a queue item to review extracted metadata, confirm customer linkage, and complete the next permitted action.
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Use the overflow menu for keyboard shortcuts.
            </Typography>
          </Stack>
        ),
      },
    ];
  }

  const metadataSummary = metadataFields
    .filter((field) => selectedItem.metadataValues[field.fieldKey])
    .map((field) => `${field.label}: ${selectedItem.metadataValues[field.fieldKey]}`);

  return [
    {
      key: "selected-summary",
      title: "Selected Item",
      content: (
        <Stack spacing={0.75}>
          <Typography variant="body2" fontWeight={600}>
            {selectedItem.title ?? selectedItem.itemId}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {formatReviewType(selectedItem.itemType)} · {selectedItem.itemId}
          </Typography>
          <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
            <StatusBadge label={formatReason(selectedItem.reason)} tone={mapReasonTone(selectedItem.reason)} variant="outlined" />
            <StatusBadge label={formatStatus(selectedItem.status)} tone={mapStatusTone(selectedItem.status)} />
          </Stack>
        </Stack>
      ),
    },
    {
      key: "customer-context",
      title: "Customer Context",
      content: (
        <Stack spacing={0.5}>
          <Typography variant="body2" color="text.secondary">
            {selectedClient ? selectedClient.displayName : "No customer linked"}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {selectedClient ? `${selectedClient.clientId} · ${selectedClient.clientType}` : "Link the item to continue customer-specific handling."}
          </Typography>
        </Stack>
      ),
    },
    {
      key: "metadata-summary",
      title: "Extracted Metadata",
      content: (
        <Stack spacing={0.5}>
          <Typography variant="body2" color="text.secondary">
            {metadataSummary.length > 0 ? metadataSummary.join(" · ") : "No extracted metadata is currently available."}
          </Typography>
        </Stack>
      ),
    },
    {
      key: "quick-actions",
      title: "Quick Actions",
      content: (
        <Stack spacing={0.75}>
          <Button size="small" variant="contained" startIcon={<AssignmentTurnedInOutlinedIcon fontSize="small" />} onClick={onApprove}>
            Approve item
          </Button>
          <Button size="small" variant="outlined" startIcon={<OpenInFullOutlinedIcon fontSize="small" />} onClick={onOpen}>
            Open review item
          </Button>
          <Button size="small" variant="text" color="inherit" startIcon={<LinkOutlinedIcon fontSize="small" />} onClick={onLink}>
            Link customer
          </Button>
          <Button size="small" variant="text" color="inherit" startIcon={<EditOutlinedIcon fontSize="small" />} onClick={onEditMetadata}>
            Edit metadata
          </Button>
          <Button size="small" color="error" variant="text" onClick={onReject}>
            Reject item
          </Button>
        </Stack>
      ),
    },
  ];
}

function buildActiveFilters({
  query,
  selectedStatus,
  selectedReason,
  onClearQuery,
  onClearStatus,
  onClearReason,
}: {
  query: string;
  selectedStatus: ReviewQueueStatus | "ALL";
  selectedReason: ReviewQueueReason | "ALL";
  onClearQuery: () => void;
  onClearStatus: () => void;
  onClearReason: () => void;
}) {
  return [
    ...(query.trim() ? [{ key: "query", label: `Query: ${query.trim()}`, onDelete: onClearQuery }] : []),
    ...(selectedStatus !== "ALL" ? [{ key: "status", label: `Status: ${formatStatus(selectedStatus)}`, onDelete: onClearStatus }] : []),
    ...(selectedReason !== "ALL" ? [{ key: "reason", label: `Reason: ${formatReason(selectedReason)}`, onDelete: onClearReason }] : []),
  ];
}

function sortRows(rows: ReviewGridRow[], sortModel: GridSortModel) {
  const next = sortModel[0];
  if (!next?.field || !next.sort) {
    return rows;
  }

  return [...rows].sort((left, right) => {
    const leftValue = sortValue(left, next.field as ReviewSortField);
    const rightValue = sortValue(right, next.field as ReviewSortField);
    const comparison = leftValue.localeCompare(rightValue, undefined, { numeric: true, sensitivity: "base" });
    return next.sort === "asc" ? comparison : comparison * -1;
  });
}

function sortValue(row: ReviewGridRow, field: ReviewSortField) {
  switch (field) {
    case "customer":
      return row.customerName;
    case "itemType":
      return row.reviewTypeLabel;
    case "reason":
      return row.reasonLabel;
    case "status":
      return formatStatus(row.status);
    case "assignedTo":
      return row.assignmentLabel;
    case "title":
    default:
      return row.title ?? row.itemId;
  }
}

function trimMetadataValues(metadataValues: Record<string, string>) {
  return Object.fromEntries(
    Object.entries(metadataValues)
      .map(([key, value]) => [key, value.trim()])
      .filter(([, value]) => value.length > 0),
  );
}

function formatReviewType(type: ReviewQueueItem["itemType"]) {
  switch (type) {
    case "DOCUMENT_VERSION":
      return "Document Version";
    case "EMAIL":
      return "Email";
    case "DOCUMENT":
    default:
      return "Document";
  }
}

function formatReason(reason: ReviewQueueReason) {
  return reason
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatStatus(status: ReviewQueueStatus) {
  return status === "IN_PROGRESS"
    ? "In progress"
    : status.charAt(0) + status.slice(1).toLowerCase();
}

function mapStatusTone(status: ReviewQueueStatus): StatusTone {
  switch (status) {
    case "RESOLVED":
      return "success";
    case "REJECTED":
      return "error";
    case "IN_PROGRESS":
      return "info";
    case "OPEN":
    default:
      return "warning";
  }
}

function mapReasonTone(reason: ReviewQueueReason): StatusTone {
  switch (reason) {
    case "PROCESSING_FAILED":
    case "PROMPT_INJECTION_RISK":
    case "REDACTION_FAILED":
      return "error";
    case "LOW_CLIENT_CONFIDENCE":
    case "LOW_CLASSIFICATION_CONFIDENCE":
    case "LOW_EXTRACTION_CONFIDENCE":
    case "DUPLICATE_UNCERTAINTY":
      return "warning";
    case "UNLINKED":
    default:
      return "info";
  }
}

function parseStatusFilter(value: string | null): ReviewQueueStatus | "ALL" {
  return statusOptions.some((option) => option.value === value)
    ? (value as ReviewQueueStatus | "ALL")
    : "OPEN";
}

function parseReasonFilter(value: string | null): ReviewQueueReason | "ALL" {
  return reasonOptions.some((option) => option.value === value)
    ? (value as ReviewQueueReason | "ALL")
    : "ALL";
}

function parseSortField(value: string | null): ReviewSortField {
  const allowed: ReviewSortField[] = ["title", "customer", "itemType", "reason", "status", "assignedTo"];
  return allowed.includes(value as ReviewSortField) ? (value as ReviewSortField) : "title";
}

function parsePositiveInt(value: string | null, fallback: number) {
  if (value === null || value === "") {
    return fallback;
  }
  const next = Number(value);
  return Number.isFinite(next) && next >= 0 ? next : fallback;
}

function setScopedSearchParams(
  setSearchParams: ReturnType<typeof useSearchParams>[1],
  current: URLSearchParams,
  updates: Record<string, string | null>,
) {
  const next = new URLSearchParams(current);
  Object.entries(updates).forEach(([key, value]) => {
    if (value === null || value === "") {
      next.delete(key);
      return;
    }
    next.set(key, value);
  });
  setSearchParams(next, { replace: true });
}
