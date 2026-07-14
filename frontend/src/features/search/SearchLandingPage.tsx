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
import { listReviewQueue } from "../../api/intake";
import { useCurrentUser } from "../../app/auth/useCurrentUser";
import { EntityGrid } from "../../app/components/EntityGrid";
import { StatusBadge, StatusTone } from "../../app/components/StatusBadge";
import { WorkspaceToolbar } from "../../app/components/WorkspaceToolbar";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  NoResultsState,
  RetryAction,
} from "../../app/WorkspaceStates";
import type { ShellWorkspaceChrome, IkmsShellOutletContext } from "../../app/shell/IkmsAppShell";

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
      ? buildSelectedContextSections(activeRow, selectedWorkspaceContextQuery.data, () => openCustomer(activeRow.clientId, navigate))
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
  }, [activeRow, navigate, query, reviewQueueCount, selectedType, selectedWorkspaceContextQuery.data, sortedRows.length]);

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
        searchPlaceholder="Search by customer, policy, claim, email, metadata, or note"
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
                message="Try a broader customer, policy, claim, email, or note query."
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
              />
            ) : null}
          </Box>
        </Stack>
      </Drawer>
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
}: {
  row: SearchRow;
  workspace?: Awaited<ReturnType<typeof getDemoClientWorkspace>>;
  onOpen: () => void;
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
            Related records
          </Typography>
          <Stack spacing={0.75}>
            {workspace.policyReferences.slice(0, 2).map((policy) => (
              <Typography key={policy.id} variant="body2" color="text.secondary">
                Policy {policy.policyNumber} · {policy.carrier}
              </Typography>
            ))}
            {workspace.claimReferences.slice(0, 2).map((claim) => (
              <Typography key={claim.id} variant="body2" color="text.secondary">
                Claim {claim.claimNumber} · {claim.status}
              </Typography>
            ))}
            {workspace.policyReferences.length === 0 && workspace.claimReferences.length === 0 ? (
              <Typography variant="body2" color="text.secondary">
                No related policy or claim references are available for this result.
              </Typography>
            ) : null}
          </Stack>
        </Box>
      ) : null}

      <Box>
        <Button variant="contained" startIcon={<OpenInNewOutlinedIcon fontSize="small" />} onClick={onOpen}>
          Open customer
        </Button>
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
) {
  return [
    {
      key: "selected-summary",
      title: "Selected Result",
      content: <ResultDetailContent row={row} workspace={workspace} onOpen={onOpen} />,
    },
  ];
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
    sourceId: item.clientId,
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
