import DownloadOutlinedIcon from "@mui/icons-material/DownloadOutlined";
import OpenInFullOutlinedIcon from "@mui/icons-material/OpenInFullOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Drawer,
  FormControl,
  IconButton,
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
import { useMutation, useQuery } from "@tanstack/react-query";
import { KeyboardEvent as ReactKeyboardEvent, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useOutletContext, useSearchParams } from "react-router-dom";
import { AuditFilters, AuditLogEntry, exportAuditLogs, searchAuditLogs } from "../../api/audit";
import { useCurrentUser } from "../../app/auth/useCurrentUser";
import { EntityGrid } from "../../app/components/EntityGrid";
import type { ContextSection } from "../../app/components/RightContextPanel";
import { StatusBadge, StatusTone } from "../../app/components/StatusBadge";
import { WorkspaceToolbar } from "../../app/components/WorkspaceToolbar";
import { useNotification } from "../../app/providers/useNotification";
import type { IkmsShellOutletContext, ShellWorkspaceChrome } from "../../app/shell/IkmsAppShell";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  NoResultsState,
  RetryAction,
} from "../../app/WorkspaceStates";

type AuditSortField = "occurredAt" | "event" | "entity" | "user" | "outcome" | "source";

interface AuditRow extends Record<string, unknown> {
  id: string;
  occurredAt: string;
  timestampLabel: string;
  eventLabel: string;
  categoryLabel: string;
  entityLabel: string;
  userLabel: string;
  outcomeLabel: string;
  outcomeTone: StatusTone;
  sourceLabel: string;
  clientLabel: string;
  detailSummary: string;
  relatedPolicy: string | null;
  relatedClaim: string | null;
  ipAddress: string | null;
  correlationId: string | null;
  raw: AuditLogEntry;
}

const defaultPageSize = 10;
const sortFields = new Set<AuditSortField>(["occurredAt", "event", "entity", "user", "outcome", "source"]);

export function AuditPage() {
  const theme = useTheme();
  const navigate = useNavigate();
  const { notify } = useNotification();
  const { setWorkspaceChrome, clearWorkspaceChrome } = useOutletContext<IkmsShellOutletContext>();
  const currentUserQuery = useCurrentUser();
  const searchInputRef = useRef<HTMLInputElement | null>(null);
  const [searchParams, setSearchParams] = useSearchParams();
  const isTabletDown = useMediaQuery(theme.breakpoints.down("lg"));
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));
  const [mobileDetailOpen, setMobileDetailOpen] = useState(false);
  const [exportDialogOpen, setExportDialogOpen] = useState(false);
  const [draftQuery, setDraftQuery] = useState(searchParams.get("q") ?? "");
  const [draftActor, setDraftActor] = useState(searchParams.get("actor") ?? "");
  const [draftFrom, setDraftFrom] = useState(searchParams.get("from") ?? "");
  const [draftTo, setDraftTo] = useState(searchParams.get("to") ?? "");

  const query = searchParams.get("q") ?? "";
  const eventType = searchParams.get("eventType") ?? "ALL";
  const actor = searchParams.get("actor") ?? "";
  const from = searchParams.get("from") ?? "";
  const to = searchParams.get("to") ?? "";
  const selectedId = searchParams.get("selected");
  const page = parsePositiveInt(searchParams.get("page"), 0);
  const pageSize = parsePositiveInt(searchParams.get("pageSize"), defaultPageSize);
  const sortField = parseSortField(searchParams.get("sort"));
  const sortDirection = searchParams.get("dir") === "asc" ? "asc" : "desc";

  const apiFilters = useMemo<AuditFilters>(
    () => ({
      actor: actor.trim() || undefined,
      from: from || undefined,
      to: to || undefined,
    }),
    [actor, from, to],
  );

  const auditQuery = useQuery({
    queryKey: ["audit", apiFilters],
    queryFn: () => searchAuditLogs(apiFilters),
  });

  const exportMutation = useMutation({
    mutationFn: () => exportAuditLogs(apiFilters),
    onSuccess: () => {
      setExportDialogOpen(true);
      notify({ severity: "success", message: "Audit CSV export prepared." });
    },
    onError: () => {
      notify({ severity: "error", message: "Unable to export audit records." });
    },
  });

  useEffect(() => {
    setDraftQuery(query);
    setDraftActor(actor);
    setDraftFrom(from);
    setDraftTo(to);
  }, [actor, from, query, to]);

  const rows = useMemo<AuditRow[]>(
    () => (auditQuery.data ?? []).map(mapAuditRow),
    [auditQuery.data],
  );

  const eventTypeOptions = useMemo(
    () => ["ALL", ...new Set(rows.map((row) => row.raw.category))],
    [rows],
  );

  const filteredRows = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    return rows.filter((row) => {
      const matchesType = eventType === "ALL" ? true : row.raw.category === eventType;
      const matchesQuery = normalizedQuery
        ? [
            row.eventLabel,
            row.entityLabel,
            row.userLabel,
            row.sourceLabel,
            row.clientLabel,
            row.detailSummary,
            row.relatedPolicy ?? "",
            row.relatedClaim ?? "",
            row.correlationId ?? "",
            row.ipAddress ?? "",
          ].some((value) => value.toLowerCase().includes(normalizedQuery))
        : true;

      return matchesType && matchesQuery;
    });
  }, [eventType, query, rows]);

  const sortModel = useMemo<GridSortModel>(() => [{ field: sortField, sort: sortDirection }], [sortDirection, sortField]);
  const sortedRows = useMemo(() => sortRows(filteredRows, sortModel), [filteredRows, sortModel]);
  const selectedRow = useMemo(
    () => sortedRows.find((row) => row.id === selectedId) ?? null,
    [selectedId, sortedRows],
  );

  const paginationModel = useMemo<GridPaginationModel>(() => ({ page, pageSize }), [page, pageSize]);
  const pagedMobileRows = useMemo(
    () => sortedRows.slice(page * pageSize, page * pageSize + pageSize),
    [page, pageSize, sortedRows],
  );

  useEffect(() => {
    if (!selectedId && sortedRows[0]) {
      updateSearchParams(setSearchParams, searchParams, { selected: sortedRows[0].id });
      return;
    }

    if (selectedId && !sortedRows.some((row) => row.id === selectedId)) {
      updateSearchParams(setSearchParams, searchParams, { selected: sortedRows[0]?.id ?? null, page: "0" });
    }
  }, [searchParams, selectedId, setSearchParams, sortedRows]);

  useEffect(() => {
    if (!isMobile) {
      setMobileDetailOpen(false);
    }
  }, [isMobile]);

  useEffect(() => {
    function handleKeyboardShortcuts(event: KeyboardEvent) {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        searchInputRef.current?.focus();
      }

      if (event.key === "Escape" && selectedId) {
        updateSearchParams(setSearchParams, searchParams, { selected: null });
      }
    }

    window.addEventListener("keydown", handleKeyboardShortcuts);
    return () => window.removeEventListener("keydown", handleKeyboardShortcuts);
  }, [searchParams, selectedId, setSearchParams]);

  const canExport = currentUserQuery.data?.permissions.includes("EXPORT_AUDIT") ?? false;
  const canOpenCustomer = currentUserQuery.data?.permissions.includes("CLIENT_VIEW") ?? false;
  const hasActiveFilters = Boolean(query || actor || from || to || eventType !== "ALL");

  const activeFilters = [
    ...(query ? [{ key: "q", label: `Search: ${query}`, onDelete: () => updateSearchParams(setSearchParams, searchParams, { q: null, page: "0" }) }] : []),
    ...(eventType !== "ALL" ? [{ key: "eventType", label: `Event type: ${eventType}`, onDelete: () => updateSearchParams(setSearchParams, searchParams, { eventType: null, page: "0" }) }] : []),
    ...(actor ? [{ key: "actor", label: `Actor: ${actor}`, onDelete: () => updateSearchParams(setSearchParams, searchParams, { actor: null, page: "0" }) }] : []),
    ...(from ? [{ key: "from", label: `From: ${formatDateTime(from)}`, onDelete: () => updateSearchParams(setSearchParams, searchParams, { from: null, page: "0" }) }] : []),
    ...(to ? [{ key: "to", label: `To: ${formatDateTime(to)}`, onDelete: () => updateSearchParams(setSearchParams, searchParams, { to: null, page: "0" }) }] : []),
  ];

  const handleRowSelect = useCallback((rowId: string, openMobile: boolean) => {
    updateSearchParams(setSearchParams, searchParams, { selected: rowId });
    if (openMobile && isMobile) {
      setMobileDetailOpen(true);
    }
  }, [isMobile, searchParams, setSearchParams]);

  const chrome = useMemo<ShellWorkspaceChrome>(() => ({
    title: "Audit",
    subtitle: "Investigate immutable operational events, filter by actor and date, and inspect the full audit record without leaving the shared shell.",
    contextTitle: selectedRow ? "Selected Audit Event" : "Audit Context",
    contextWidth: 336,
    contextSections: buildContextSections({
      selectedRow,
      hasActiveFilters,
      totalVisible: sortedRows.length,
      canOpenCustomer,
      canExport,
      onOpenCustomer: selectedRow?.raw.clientId ? () => navigate(`/clients/${selectedRow.raw.clientId}`) : undefined,
      onExport: canExport ? () => exportMutation.mutate() : undefined,
      onClearSelection: selectedRow ? () => updateSearchParams(setSearchParams, searchParams, { selected: null }) : undefined,
    }),
  }), [
    canExport,
    canOpenCustomer,
    exportMutation,
    hasActiveFilters,
    navigate,
    searchParams,
    selectedRow,
    setSearchParams,
    sortedRows.length,
  ]);

  useEffect(() => {
    setWorkspaceChrome(chrome);
    return () => clearWorkspaceChrome();
  }, [chrome, clearWorkspaceChrome, setWorkspaceChrome]);

  const columns = useMemo<GridColDef<AuditRow>[]>(() => {
    const desktopColumns: GridColDef<AuditRow>[] = [
      {
        field: "occurredAt",
        headerName: "Timestamp",
        minWidth: 170,
        width: 176,
        renderCell: ({ row }) => (
          <Stack spacing={0.2} sx={{ py: 0.4 }}>
            <Typography variant="body2" noWrap>
              {row.timestampLabel}
            </Typography>
            <Typography variant="caption" color="text.secondary" noWrap>
              {row.raw.retainedUntil ? `Retained until ${formatDate(row.raw.retainedUntil)}` : ""}
            </Typography>
          </Stack>
        ),
      },
      {
        field: "event",
        headerName: "Event",
        flex: 1.1,
        minWidth: 220,
        valueGetter: (_, row) => row.eventLabel,
        renderCell: ({ row }) => (
          <Stack spacing={0.2} sx={{ py: 0.4, minWidth: 0 }}>
            <Typography variant="body2" fontWeight={600} noWrap title={row.eventLabel}>
              {row.eventLabel}
            </Typography>
            <Typography variant="caption" color="text.secondary" noWrap title={row.categoryLabel}>
              {row.categoryLabel}
            </Typography>
          </Stack>
        ),
      },
      {
        field: "entity",
        headerName: "Entity",
        flex: 1,
        minWidth: 210,
        valueGetter: (_, row) => row.entityLabel,
        renderCell: ({ row }) => (
          <Stack spacing={0.2} sx={{ py: 0.4, minWidth: 0 }}>
            <Typography variant="body2" noWrap title={row.entityLabel}>
              {row.entityLabel}
            </Typography>
            <Typography variant="caption" color="text.secondary" noWrap title={row.clientLabel}>
              {row.clientLabel}
            </Typography>
          </Stack>
        ),
      },
      {
        field: "user",
        headerName: "User",
        flex: 0.82,
        minWidth: 160,
        valueGetter: (_, row) => row.userLabel,
      },
      {
        field: "outcome",
        headerName: "Outcome",
        minWidth: 130,
        width: 132,
        valueGetter: (_, row) => row.outcomeLabel,
        renderCell: ({ row }) => <StatusBadge label={row.outcomeLabel} tone={row.outcomeTone} />,
      },
      {
        field: "source",
        headerName: "Source",
        flex: 0.78,
        minWidth: 150,
        valueGetter: (_, row) => row.sourceLabel,
      },
      {
        field: "actions",
        headerName: "Actions",
        width: 92,
        sortable: false,
        filterable: false,
        renderCell: ({ row }) => (
          <Tooltip title="Open audit event">
            <IconButton
              size="small"
              aria-label={`Open audit event ${row.eventLabel}`}
              onClick={(event) => {
                event.stopPropagation();
                handleRowSelect(row.id, false);
              }}
            >
              <OpenInFullOutlinedIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        ),
      },
    ];

    if (isTabletDown) {
      return desktopColumns.filter((column) => !["source"].includes(String(column.field)));
    }

    return desktopColumns;
  }, [handleRowSelect, isTabletDown]);

  function applyFilters() {
    updateSearchParams(setSearchParams, searchParams, {
      q: draftQuery.trim() || null,
      actor: draftActor.trim() || null,
      from: draftFrom || null,
      to: draftTo || null,
      page: "0",
      selected: null,
    });
  }

  function clearFilters() {
    setDraftQuery("");
    setDraftActor("");
    setDraftFrom("");
    setDraftTo("");
    updateSearchParams(setSearchParams, searchParams, {
      q: null,
      eventType: null,
      actor: null,
      from: null,
      to: null,
      page: "0",
      selected: null,
    });
  }

  function handleSearchKeyDown(event: ReactKeyboardEvent<HTMLInputElement>) {
    if (event.key === "Enter") {
      event.preventDefault();
      applyFilters();
    }
  }

  function handleSortModelChange(model: GridSortModel) {
    const next = model[0];
    updateSearchParams(setSearchParams, searchParams, {
      sort: next?.field ?? "occurredAt",
      dir: next?.sort ?? "desc",
    });
  }

  function handlePaginationModelChange(model: GridPaginationModel) {
    updateSearchParams(setSearchParams, searchParams, {
      page: String(model.page),
      pageSize: String(model.pageSize),
    });
  }

  if (currentUserQuery.isLoading || (auditQuery.isLoading && !auditQuery.data)) {
    return (
      <LoadingState
        title="Loading audit workspace"
        message="Retrieving immutable audit events and permission-aware investigation context."
      />
    );
  }

  if (currentUserQuery.isError || auditQuery.isError) {
    return (
      <ErrorState
        title="Unable to load audit events"
        message="The audit workspace could not retrieve the current operational history."
        action={<RetryAction onClick={() => void auditQuery.refetch()} />}
      />
    );
  }

  return (
    <Stack spacing={1.25}>
      <WorkspaceToolbar
        searchPlaceholder="Search events, entities, customers, IPs, or audit details"
        searchValue={draftQuery}
        onSearchChange={setDraftQuery}
        onSearchKeyDown={handleSearchKeyDown}
        searchInputRef={searchInputRef}
        searchAriaLabel="Audit search"
        filters={(
          <Stack
            direction={{ xs: "column", xl: "row" }}
            spacing={1}
            alignItems={{ xs: "stretch", xl: "center" }}
            sx={{ width: { xs: "100%", xl: "auto" }, minWidth: 0 }}
          >
            <FormControl size="small" sx={{ minWidth: { xs: "100%", sm: 180 } }}>
              <Select
                value={eventType}
                inputProps={{ "aria-label": "Event type" }}
                onChange={(event) =>
                  updateSearchParams(setSearchParams, searchParams, {
                    eventType: event.target.value === "ALL" ? null : String(event.target.value),
                    page: "0",
                    selected: null,
                  })
                }
              >
                {eventTypeOptions.map((option) => (
                  <MenuItem key={option} value={option}>
                    {option === "ALL" ? "All event types" : option}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              size="small"
              label="Actor"
              value={draftActor}
              onChange={(event) => setDraftActor(event.target.value)}
              onKeyDown={handleSearchKeyDown}
              sx={{ minWidth: { xs: "100%", sm: 180 } }}
            />
            <TextField
              size="small"
              label="From"
              type="datetime-local"
              value={draftFrom}
              onChange={(event) => setDraftFrom(event.target.value)}
              InputLabelProps={{ shrink: true }}
              sx={{ minWidth: { xs: "100%", sm: 200 } }}
            />
            <TextField
              size="small"
              label="To"
              type="datetime-local"
              value={draftTo}
              onChange={(event) => setDraftTo(event.target.value)}
              InputLabelProps={{ shrink: true }}
              sx={{ minWidth: { xs: "100%", sm: 200 } }}
            />
            {hasActiveFilters ? (
              <Button size="small" color="inherit" variant="text" onClick={clearFilters}>
                Clear
              </Button>
            ) : null}
          </Stack>
        )}
        activeFilters={activeFilters}
        primaryAction={(
          <Button variant="contained" startIcon={<SearchOutlinedIcon fontSize="small" />} onClick={applyFilters}>
            Search
          </Button>
        )}
        onRefresh={() => void auditQuery.refetch()}
        secondaryActions={[
          ...(canExport ? [{ key: "export", label: exportMutation.isPending ? "Exporting..." : "Export CSV", onClick: () => exportMutation.mutate() }] : []),
          ...(isMobile && selectedRow ? [{ key: "open-selected", label: "Open selected event", onClick: () => setMobileDetailOpen(true) }] : []),
          ...(hasActiveFilters ? [{ key: "clear", label: "Clear filters", onClick: clearFilters }] : []),
        ]}
      />

      {!canExport ? (
        <Alert severity="info" variant="outlined" sx={{ py: 0.25 }}>
          CSV export requires the `EXPORT_AUDIT` permission.
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
        <Stack spacing={1} sx={{ px: 2, py: 1.25 }}>
          {sortedRows.length === 0 ? (
            hasActiveFilters ? (
              <NoResultsState
                title="No audit events match these filters"
                message="Try a broader search range or remove one of the current investigation filters."
                action={<RetryAction label="Clear filters" onClick={clearFilters} />}
                compact
              />
            ) : (
              <EmptyState
                title="No audit events available"
                message="Audit records will appear here when operational activity is available."
                action={<RetryAction label="Refresh" onClick={() => void auditQuery.refetch()} />}
                compact
              />
            )
          ) : isMobile ? (
            <MobileAuditList rows={pagedMobileRows} selectedId={selectedId} onSelect={(rowId) => handleRowSelect(rowId, true)} />
          ) : (
            <EntityGrid<AuditRow>
              rows={sortedRows}
              columns={columns}
              getRowId={(row) => row.id}
              pagination
              paginationMode="client"
              paginationModel={paginationModel}
              onPaginationModelChange={handlePaginationModelChange}
              sortModel={sortModel}
              onSortModelChange={handleSortModelChange}
              onRowClick={({ row }) => handleRowSelect(row.id, false)}
              onCellKeyDown={(params, event) => {
                if (event.key === "Enter") {
                  handleRowSelect(params.row.id, false);
                }
              }}
              getRowClassName={({ row }) => (row.id === selectedId ? "ikms-selected-row" : "")}
              emptyTitle="No audit events"
              emptyMessage="There are no audit events for the current operational view."
              sx={{
                minHeight: Math.min(Math.max(300, 108 + Math.max(3, sortedRows.length) * 44), 700),
                height: Math.min(Math.max(300, 108 + Math.max(3, sortedRows.length) * 44), 700),
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
        open={isMobile && mobileDetailOpen && Boolean(selectedRow)}
        onClose={() => setMobileDetailOpen(false)}
        PaperProps={{ sx: { width: "100%", maxWidth: "100%" } }}
      >
        <Stack spacing={0} sx={{ height: "100%" }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ px: 2, py: 1.25, borderBottom: (theme) => `1px solid ${theme.palette.divider}` }}>
            <Typography variant="subtitle2">Selected audit event</Typography>
            <IconButton aria-label="Close detail" onClick={() => setMobileDetailOpen(false)}>
              <CloseOutlinedIcon fontSize="small" />
            </IconButton>
          </Stack>
          <Box sx={{ overflowY: "auto", px: 2, py: 1.5 }}>
            {selectedRow ? (
              <SelectedAuditDetail
                row={selectedRow}
                canOpenCustomer={canOpenCustomer}
                canExport={canExport}
                onOpenCustomer={selectedRow.raw.clientId ? () => navigate(`/clients/${selectedRow.raw.clientId}`) : undefined}
                onExport={canExport ? () => exportMutation.mutate() : undefined}
              />
            ) : null}
          </Box>
        </Stack>
      </Drawer>

      <Dialog open={exportDialogOpen} onClose={() => setExportDialogOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>Audit CSV preview</DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
            Exported using the current actor and date-range API filters.
          </Typography>
          <Box
            component="pre"
            sx={{
              m: 0,
              whiteSpace: "pre-wrap",
              overflowX: "auto",
              fontFamily: "ui-monospace, SFMono-Regular, SF Mono, Menlo, monospace",
              fontSize: 12,
            }}
          >
            {exportMutation.data}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setExportDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

function MobileAuditList({
  rows,
  selectedId,
  onSelect,
}: {
  rows: AuditRow[];
  selectedId: string | null;
  onSelect: (rowId: string) => void;
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
          <ListItemButton alignItems="flex-start" onClick={() => onSelect(row.id)} sx={{ px: 1.5, py: 1.25 }}>
            <ListItemText
              primary={(
                <Stack spacing={0.75}>
                  <Stack direction="row" spacing={1} justifyContent="space-between" alignItems="flex-start">
                    <Typography variant="body2" fontWeight={600} sx={{ pr: 1 }}>
                      {row.eventLabel}
                    </Typography>
                    <StatusBadge label={row.outcomeLabel} tone={row.outcomeTone} />
                  </Stack>
                  <Typography variant="body2" color="text.secondary">
                    {row.entityLabel}
                  </Typography>
                  <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
                    <Typography variant="caption" color="text.secondary">
                      {row.timestampLabel}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {row.userLabel}
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

function SelectedAuditDetail({
  row,
  canOpenCustomer,
  canExport,
  onOpenCustomer,
  onExport,
}: {
  row: AuditRow;
  canOpenCustomer: boolean;
  canExport: boolean;
  onOpenCustomer?: () => void;
  onExport?: () => void;
}) {
  return (
    <Stack spacing={1.25}>
      <Stack spacing={0.5}>
        <Typography variant="body2" fontWeight={600}>
          {row.eventLabel}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {row.entityLabel}
        </Typography>
        <Stack direction="row" spacing={0.75} useFlexGap flexWrap="wrap">
          <StatusBadge label={row.outcomeLabel} tone={row.outcomeTone} />
          <StatusBadge label={row.categoryLabel} tone="neutral" variant="outlined" />
        </Stack>
      </Stack>

      <Stack spacing={0.4}>
        <Typography variant="subtitle2">Audit Details</Typography>
        <Typography variant="body2" color="text.secondary">
          {row.detailSummary}
        </Typography>
      </Stack>

      <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
        {canOpenCustomer && onOpenCustomer ? (
          <Button size="small" variant="outlined" onClick={onOpenCustomer}>
            Open Customer360
          </Button>
        ) : null}
        {canExport && onExport ? (
          <Button size="small" variant="outlined" startIcon={<DownloadOutlinedIcon fontSize="small" />} onClick={onExport}>
            Export CSV
          </Button>
        ) : null}
      </Stack>
    </Stack>
  );
}

function buildContextSections({
  selectedRow,
  hasActiveFilters,
  totalVisible,
  canOpenCustomer,
  canExport,
  onOpenCustomer,
  onExport,
  onClearSelection,
}: {
  selectedRow: AuditRow | null;
  hasActiveFilters: boolean;
  totalVisible: number;
  canOpenCustomer: boolean;
  canExport: boolean;
  onOpenCustomer?: () => void;
  onExport?: () => void;
  onClearSelection?: () => void;
}): ContextSection[] {
  if (!selectedRow) {
    return [
      {
        key: "audit-summary",
        title: "Audit Summary",
        content: (
          <Stack spacing={0.75}>
            <Typography variant="body2" color="text.secondary">
              {totalVisible} visible {totalVisible === 1 ? "event" : "events"} in the current audit view.
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {hasActiveFilters ? "Filters are applied to the current investigation." : "No additional filters are applied."}
            </Typography>
          </Stack>
        ),
      },
      {
        key: "audit-guidance",
        title: "Quick Actions",
        content: (
          <Typography variant="body2" color="text.secondary">
            Select an audit event to inspect immutable details, related business context, and export the filtered result set when authorized.
          </Typography>
        ),
      },
    ];
  }

  return [
    {
      key: "summary",
      title: "Event Summary",
      content: (
        <Stack spacing={0.75}>
          <Typography variant="body2" fontWeight={600}>
            {selectedRow.eventLabel}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {selectedRow.entityLabel}
          </Typography>
          <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
            <StatusBadge label={selectedRow.outcomeLabel} tone={selectedRow.outcomeTone} />
            <StatusBadge label={selectedRow.categoryLabel} tone="neutral" variant="outlined" />
          </Stack>
        </Stack>
      ),
    },
    {
      key: "user",
      title: "User",
      content: (
        <Stack spacing={0.4}>
          <Typography variant="body2">{selectedRow.userLabel}</Typography>
          <Typography variant="body2" color="text.secondary">
            Actor ID: {selectedRow.raw.actorUserId ?? "Unavailable"}
          </Typography>
        </Stack>
      ),
    },
    {
      key: "timestamp",
      title: "Timestamp",
      content: (
        <Stack spacing={0.4}>
          <Typography variant="body2">{selectedRow.timestampLabel}</Typography>
          <Typography variant="body2" color="text.secondary">
            Retained until {formatDate(selectedRow.raw.retainedUntil)}
          </Typography>
        </Stack>
      ),
    },
    {
      key: "entity",
      title: "Entity",
      content: (
        <Stack spacing={0.4}>
          <Typography variant="body2">{selectedRow.entityLabel}</Typography>
          <Typography variant="body2" color="text.secondary">
            Customer: {selectedRow.clientLabel}
          </Typography>
        </Stack>
      ),
    },
    {
      key: "details",
      title: "Audit Details",
      content: (
        <Stack spacing={0.4}>
          {Object.entries(selectedRow.raw.details).length > 0 ? (
            Object.entries(selectedRow.raw.details).map(([key, value]) => (
              <Typography key={key} variant="body2" color="text.secondary">
                {`${humanizeKey(key)}: ${value}`}
              </Typography>
            ))
          ) : (
            <Typography variant="body2" color="text.secondary">
              No structured detail payload is available for this event.
            </Typography>
          )}
        </Stack>
      ),
    },
    ...(selectedRow.raw.clientId || selectedRow.relatedPolicy || selectedRow.relatedClaim
      ? [
          {
            key: "related",
            title: "Related Customer/Policy/Claim",
            content: (
              <Stack spacing={0.4}>
                <Typography variant="body2" color="text.secondary">
                  Customer: {selectedRow.clientLabel}
                </Typography>
                {selectedRow.relatedPolicy ? (
                  <Typography variant="body2" color="text.secondary">
                    Policy: {selectedRow.relatedPolicy}
                  </Typography>
                ) : null}
                {selectedRow.relatedClaim ? (
                  <Typography variant="body2" color="text.secondary">
                    Claim: {selectedRow.relatedClaim}
                  </Typography>
                ) : null}
              </Stack>
            ),
          } satisfies ContextSection,
        ]
      : []),
    {
      key: "actions",
      title: "Quick Actions",
      content: (
        <Stack spacing={0.75}>
          {canOpenCustomer && onOpenCustomer ? (
            <Button size="small" variant="outlined" onClick={onOpenCustomer}>
              Open Customer360
            </Button>
          ) : null}
          {canExport && onExport ? (
            <Button size="small" variant="outlined" startIcon={<DownloadOutlinedIcon fontSize="small" />} onClick={onExport}>
              Export current view
            </Button>
          ) : null}
          {onClearSelection ? (
            <Button size="small" variant="text" color="inherit" onClick={onClearSelection}>
              Clear selection
            </Button>
          ) : null}
        </Stack>
      ),
    },
  ];
}

function mapAuditRow(entry: AuditLogEntry): AuditRow {
  return {
    id: entry.id,
    occurredAt: entry.occurredAt,
    timestampLabel: formatDateTime(entry.occurredAt),
    eventLabel: formatEventLabel(entry.action),
    categoryLabel: humanizeKey(entry.category),
    entityLabel: buildEntityLabel(entry),
    userLabel: entry.actorUsername ?? "System",
    outcomeLabel: formatOutcome(entry.outcome),
    outcomeTone: mapOutcomeTone(entry.outcome),
    sourceLabel: buildSourceLabel(entry),
    clientLabel: entry.clientId ?? "No linked customer",
    detailSummary: Object.entries(entry.details).map(([key, value]) => `${humanizeKey(key)}: ${value}`).join(" · ") || "No structured details",
    relatedPolicy: findRelatedValue(entry.details, ["policyNumber", "policyId"]),
    relatedClaim: findRelatedValue(entry.details, ["claimNumber", "claimId"]),
    ipAddress: findRelatedValue(entry.details, ["ipAddress"]),
    correlationId: findRelatedValue(entry.details, ["correlationId", "interactionId"]),
    raw: entry,
  };
}

function sortRows(rows: AuditRow[], sortModel: GridSortModel) {
  const next = sortModel[0];
  if (!next?.field || !next.sort) {
    return rows;
  }

  return [...rows].sort((left, right) => {
    const leftValue = String(sortValue(left, next.field as AuditSortField));
    const rightValue = String(sortValue(right, next.field as AuditSortField));
    const comparison = leftValue.localeCompare(rightValue, undefined, { numeric: true, sensitivity: "base" });
    return next.sort === "asc" ? comparison : comparison * -1;
  });
}

function sortValue(row: AuditRow, field: AuditSortField) {
  switch (field) {
    case "occurredAt":
      return row.occurredAt;
    case "event":
      return row.eventLabel;
    case "entity":
      return row.entityLabel;
    case "user":
      return row.userLabel;
    case "outcome":
      return row.outcomeLabel;
    case "source":
      return row.sourceLabel;
  }
}

function parsePositiveInt(value: string | null, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function parseSortField(value: string | null): AuditSortField {
  return value && sortFields.has(value as AuditSortField) ? (value as AuditSortField) : "occurredAt";
}

function updateSearchParams(
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

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(value));
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  }).format(new Date(value));
}

function formatEventLabel(value: string) {
  return humanizeKey(value);
}

function formatOutcome(value: string) {
  return humanizeKey(value);
}

function humanizeKey(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(" ");
}

function mapOutcomeTone(value: string): StatusTone {
  switch (value) {
    case "SUCCESS":
      return "success";
    case "FAILED":
    case "DENIED":
      return "error";
    case "WARNING":
    case "NO_EVIDENCE":
      return "warning";
    default:
      return "info";
  }
}

function buildEntityLabel(entry: AuditLogEntry) {
  const primary = entry.targetType ? humanizeKey(entry.targetType) : "Operational event";
  return entry.targetId ? `${primary} · ${entry.targetId}` : primary;
}

function buildSourceLabel(entry: AuditLogEntry) {
  return (
    findRelatedValue(entry.details, ["mailbox", "sourceSystem", "ipAddress", "retrievalMode"]) ??
    humanizeKey(entry.category)
  );
}

function findRelatedValue(details: Record<string, string>, keys: string[]) {
  for (const key of keys) {
    if (details[key]) {
      return details[key];
    }
  }
  return null;
}
