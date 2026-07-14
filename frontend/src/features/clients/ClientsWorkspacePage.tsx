import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import ArrowForwardOutlinedIcon from "@mui/icons-material/ArrowForwardOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import FileUploadOutlinedIcon from "@mui/icons-material/FileUploadOutlined";
import SearchOutlinedIcon from "@mui/icons-material/SearchOutlined";
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Drawer,
  FormControl,
  InputLabel,
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
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { KeyboardEvent, useEffect, useMemo, useState } from "react";
import { useNavigate, useOutletContext, useSearchParams } from "react-router-dom";
import { ClientStatus, ClientSummary, ClientType, createClient, listClients } from "../../api/clients";
import { EntityGrid } from "../../app/components/EntityGrid";
import { StatusBadge, StatusTone } from "../../app/components/StatusBadge";
import { WorkspaceToolbar } from "../../app/components/WorkspaceToolbar";
import type { ContextSection } from "../../app/components/RightContextPanel";
import { useNotification } from "../../app/providers/useNotification";
import type { IkmsShellOutletContext, ShellWorkspaceChrome } from "../../app/shell/IkmsAppShell";
import { EmptyState, ErrorState, LoadingState, NoResultsState, RetryAction } from "../../app/WorkspaceStates";

const clientsQueryKey = ["clients"] as const;
const defaultPageSize = 10;
type ClientGridRow = ClientSummary & Record<string, unknown>;

type ClientTypeFilter = "ALL" | ClientType;
type ClientStatusFilter = "ALL" | ClientStatus;

const typeOptions: Array<{ value: ClientTypeFilter; label: string }> = [
  { value: "ALL", label: "All types" },
  { value: "BUSINESS", label: "Business" },
  { value: "INDIVIDUAL", label: "Individual" },
];

const statusOptions: Array<{ value: ClientStatusFilter; label: string }> = [
  { value: "ALL", label: "All statuses" },
  { value: "ACTIVE", label: "Active" },
  { value: "INACTIVE", label: "Inactive" },
  { value: "ARCHIVED", label: "Archived" },
];

export function ClientsWorkspacePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { notify } = useNotification();
  const { setWorkspaceChrome, clearWorkspaceChrome } = useOutletContext<IkmsShellOutletContext>();
  const [searchParams, setSearchParams] = useSearchParams();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [displayName, setDisplayName] = useState("");
  const [requestedClientId, setRequestedClientId] = useState("");
  const [clientType, setClientType] = useState<ClientType>("BUSINESS");

  const query = searchParams.get("q") ?? "";
  const selectedType = parseTypeFilter(searchParams.get("type"));
  const selectedStatus = parseStatusFilter(searchParams.get("status"));
  const selectedId = searchParams.get("selected");
  const sortField = searchParams.get("sort") ?? "displayName";
  const sortDirection = searchParams.get("dir") === "desc" ? "desc" : "asc";
  const page = parsePositiveInt(searchParams.get("page"), 0);
  const pageSize = parsePositiveInt(searchParams.get("pageSize"), defaultPageSize);
  const [localQuery, setLocalQuery] = useState(query);

  const clientsQuery = useQuery({
    queryKey: [...clientsQueryKey, query],
    queryFn: () => listClients(query),
  });

  const createClientMutation = useMutation({
    mutationFn: createClient,
    onSuccess: async (client) => {
      setCreateDialogOpen(false);
      setDisplayName("");
      setRequestedClientId("");
      setClientType("BUSINESS");
      notify({ severity: "success", message: "Customer created." });
      await queryClient.invalidateQueries({ queryKey: clientsQueryKey });
      setScopedSearchParams(setSearchParams, searchParams, {
        selected: client.id,
      });
    },
    onError: () => {
      notify({ severity: "error", message: "Unable to create customer." });
    },
  });

  useEffect(() => {
    setLocalQuery(query);
  }, [query]);

  const filteredRows = useMemo(() => {
    return (clientsQuery.data ?? []).filter((client) => {
      if (selectedType !== "ALL" && client.clientType !== selectedType) {
        return false;
      }
      if (selectedStatus !== "ALL" && client.status !== selectedStatus) {
        return false;
      }
      return true;
    });
  }, [clientsQuery.data, selectedStatus, selectedType]);

  const sortModel = useMemo<GridSortModel>(
    () => [{ field: sortField, sort: sortDirection }],
    [sortDirection, sortField],
  );

  const sortedRows = useMemo(() => sortClientRows(filteredRows, sortModel), [filteredRows, sortModel]);
  const selectedRow = useMemo(
    () => sortedRows.find((client) => client.id === selectedId) ?? null,
    [selectedId, sortedRows],
  );

  useEffect(() => {
    if (selectedId && !sortedRows.some((client) => client.id === selectedId)) {
      setScopedSearchParams(setSearchParams, searchParams, { selected: null });
    }
  }, [searchParams, selectedId, setSearchParams, sortedRows]);

  useEffect(() => {
    const maxPage = Math.max(0, Math.ceil(sortedRows.length / pageSize) - 1);
    if (page > maxPage) {
      setScopedSearchParams(setSearchParams, searchParams, { page: String(maxPage) });
    }
  }, [page, pageSize, searchParams, setSearchParams, sortedRows.length]);

  const chrome = useMemo<ShellWorkspaceChrome>(() => {
    const contextSections = selectedRow
      ? buildSelectedContextSections(selectedRow, () => openCustomer(selectedRow.id, navigate))
      : buildWorkspaceContextSections({
          query,
        });

    return {
      title: "Customer Access",
      subtitle: "Search customer records, refine the list, and move directly into Customer360.",
      primaryActions: (
        <Button
          variant="contained"
          startIcon={<AddOutlinedIcon fontSize="small" />}
          onClick={() => setCreateDialogOpen(true)}
        >
          Create customer
        </Button>
      ),
      secondaryActions: (
        <Button
          variant="text"
          color="inherit"
          startIcon={<FileUploadOutlinedIcon fontSize="small" />}
          onClick={() => navigate("/clients/import")}
        >
          CSV import
        </Button>
      ),
      contextTitle: selectedRow ? "Selected Customer" : "Customer Context",
      contextSections,
    };
  }, [navigate, query, selectedRow]);

  useEffect(() => {
    setWorkspaceChrome(chrome);
    return () => clearWorkspaceChrome();
  }, [chrome, clearWorkspaceChrome, setWorkspaceChrome]);

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
      type: null,
      status: null,
      page: "0",
      selected: null,
    });
  }

  function handleSearchKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Enter") {
      event.preventDefault();
      applySearch();
    }
  }

  function handleSortModelChange(model: GridSortModel) {
    const next = model[0];
    setScopedSearchParams(setSearchParams, searchParams, {
      sort: next?.field ?? "displayName",
      dir: next?.sort ?? "asc",
    });
  }

  function handlePaginationModelChange(model: GridPaginationModel) {
    setScopedSearchParams(setSearchParams, searchParams, {
      page: String(model.page),
      pageSize: String(model.pageSize),
    });
  }

  function handleRowSelect(clientId: string) {
    setScopedSearchParams(setSearchParams, searchParams, {
      selected: clientId,
    });
  }

  const activeFilters = buildActiveFilters({
    query,
    selectedType,
    selectedStatus,
    onClearQuery: () => {
      setLocalQuery("");
      setScopedSearchParams(setSearchParams, searchParams, { q: null, page: "0", selected: null });
    },
    onClearType: () => setScopedSearchParams(setSearchParams, searchParams, { type: null, page: "0", selected: null }),
    onClearStatus: () => setScopedSearchParams(setSearchParams, searchParams, { status: null, page: "0", selected: null }),
  });

  const gridRows = useMemo<ClientGridRow[]>(() => sortedRows.map((row) => ({ ...row })), [sortedRows]);

  const columns = useMemo<GridColDef<ClientGridRow>[]>(() => [
    {
      field: "displayName",
      headerName: "Customer",
      minWidth: 280,
      flex: 1.4,
      renderCell: ({ row }) => (
        <Stack spacing={0.25} sx={{ py: 0.5, minWidth: 0 }}>
          <Typography variant="body2" fontWeight={600} noWrap>
            {row.displayName}
          </Typography>
          <Typography variant="caption" color="text.secondary" noWrap>
            {row.clientId}{row.clientIdTemporary ? " · Temporary ID" : ""}
          </Typography>
        </Stack>
      ),
    },
    {
      field: "clientId",
      headerName: "Customer ID",
      minWidth: 150,
      flex: 0.8,
    },
    {
      field: "clientType",
      headerName: "Type",
      minWidth: 130,
      flex: 0.6,
    },
    {
      field: "status",
      headerName: "Status",
      minWidth: 130,
      flex: 0.6,
      renderCell: ({ row }) => <StatusBadge label={row.status} tone={mapClientStatusTone(row.status)} />,
    },
    {
      field: "actions",
      headerName: "Actions",
      width: 110,
      sortable: false,
      filterable: false,
      renderCell: ({ row }) => (
        <Tooltip title="Open Customer360">
          <IconButton
            size="small"
            aria-label={`Open ${row.displayName}`}
            onClick={(event) => {
              event.stopPropagation();
              openCustomer(row.id, navigate);
            }}
          >
            <ArrowForwardOutlinedIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      ),
    },
  ], [navigate]);

  const gridHeight = Math.min(Math.max(264, 108 + Math.max(2, sortedRows.length) * 44), 680);
  const paginationModel = { page, pageSize };
  const pagedRows = sortedRows.slice(page * pageSize, (page + 1) * pageSize);
  const hasActiveFilters = activeFilters.length > 0;

  if (clientsQuery.isLoading) {
    return (
      <LoadingState
        title="Loading customer records"
        message="Preparing the customer access workspace."
      />
    );
  }

  if (clientsQuery.isError) {
    return (
      <ErrorState
        title="Unable to load customer records"
        message="Customer Access could not retrieve the current client list."
        action={<RetryAction onClick={() => void clientsQuery.refetch()} />}
      />
    );
  }

  return (
    <Stack spacing={1.5}>
      <WorkspaceToolbar
        searchPlaceholder="Search by customer name or client ID"
        searchValue={localQuery}
        onSearchChange={setLocalQuery}
        onSearchKeyDown={handleSearchKeyDown}
        searchAriaLabel="Customer list query"
        filters={(
          <Stack direction={{ xs: "column", sm: "row" }} spacing={1} sx={{ width: { xs: "100%", lg: "auto" } }}>
            <FormControl size="small" sx={{ minWidth: { xs: "100%", sm: 160 } }}>
              <Select<ClientTypeFilter>
                value={selectedType}
                inputProps={{ "aria-label": "Customer type" }}
                onChange={(event) => {
                  setScopedSearchParams(setSearchParams, searchParams, {
                    type: event.target.value === "ALL" ? null : String(event.target.value),
                    page: "0",
                    selected: null,
                  });
                }}
              >
                {typeOptions.map((option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: { xs: "100%", sm: 150 } }}>
              <Select<ClientStatusFilter>
                value={selectedStatus}
                inputProps={{ "aria-label": "Customer status" }}
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
            <Button variant="contained" onClick={applySearch} startIcon={<SearchOutlinedIcon fontSize="small" />}>
              Search
            </Button>
            {hasActiveFilters ? (
              <Button variant="text" color="inherit" onClick={clearFilters}>
                Clear
              </Button>
            ) : null}
          </Stack>
        )}
        activeFilters={activeFilters}
        onRefresh={() => void clientsQuery.refetch()}
        secondaryActions={[
          { key: "create-customer", label: "Create customer", onClick: () => setCreateDialogOpen(true) },
          { key: "import-customers", label: "Open CSV import", onClick: () => navigate("/clients/import") },
        ]}
      />

      <Box
        sx={{
          border: (appTheme) => `1px solid ${appTheme.palette.divider}`,
          borderRadius: 1,
          backgroundColor: "background.paper",
          overflow: "hidden",
        }}
      >
        <Stack spacing={0.75} sx={{ px: 2, py: 1.25 }}>
          {sortedRows.length === 0 ? (
            hasActiveFilters || query.trim() ? (
              <NoResultsState
                title="No customers match these filters"
                message="Try a broader customer name, client ID, or filter combination."
                action={<RetryAction label="Clear filters" onClick={clearFilters} />}
                compact
              />
            ) : (
              <EmptyState
                title="No customer records available"
                message="Create a customer or import a CSV file to open Customer360."
                action={(
                  <Stack direction="row" spacing={1}>
                    <Button variant="contained" onClick={() => setCreateDialogOpen(true)}>
                      Create customer
                    </Button>
                    <Button variant="outlined" onClick={() => navigate("/clients/import")}>
                      Open CSV import
                    </Button>
                  </Stack>
                )}
                compact
              />
            )
          ) : isMobile ? (
            <MobileClientList
              rows={pagedRows}
              selectedId={selectedId}
              onSelect={handleRowSelect}
              onOpen={(clientId) => openCustomer(clientId, navigate)}
            />
          ) : (
            <EntityGrid<ClientGridRow>
              rows={gridRows}
              columns={columns}
              getRowId={(row) => row.id}
              pagination
              paginationMode="client"
              paginationModel={paginationModel}
              onPaginationModelChange={handlePaginationModelChange}
              sortModel={sortModel}
              onSortModelChange={handleSortModelChange}
              onRowClick={({ row }) => handleRowSelect(row.id)}
              onRowDoubleClick={({ row }) => openCustomer(row.id, navigate)}
              onCellKeyDown={(params, event) => {
                if (event.key === "Enter") {
                  openCustomer(params.row.id, navigate);
                }
              }}
              getRowClassName={({ row }) => (row.id === selectedId ? "ikms-selected-row" : "")}
              emptyTitle="No customer records"
              emptyMessage="There are no customer records for the current query and filters."
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

      <Drawer
        anchor="right"
        open={isMobile && Boolean(selectedRow)}
        onClose={() => handleRowSelect("")}
        PaperProps={{ sx: { width: "100%", maxWidth: "100%" } }}
      >
        <Stack spacing={2} sx={{ p: 2 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <Typography variant="subtitle2">Selected customer</Typography>
            <IconButton aria-label="Close detail" onClick={() => handleRowSelect("")}>
              <CloseOutlinedIcon fontSize="small" />
            </IconButton>
          </Stack>
          {selectedRow ? <SelectedCustomerDetail client={selectedRow} onOpen={() => openCustomer(selectedRow.id, navigate)} /> : null}
        </Stack>
      </Drawer>

      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Create customer</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={1.5} sx={{ pt: 0.5 }}>
            <TextField
              label="Display name"
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              autoFocus
            />
            <TextField
              label="Client ID"
              helperText="Optional. Leave blank to assign a temporary ID."
              value={requestedClientId}
              onChange={(event) => setRequestedClientId(event.target.value)}
            />
            <FormControl size="small">
              <InputLabel id="create-customer-type-label">Customer Type</InputLabel>
              <Select<ClientType>
                labelId="create-customer-type-label"
                label="Customer Type"
                value={clientType}
                inputProps={{ "aria-label": "Client type" }}
                onChange={(event) => setClientType(event.target.value as ClientType)}
              >
                <MenuItem value="BUSINESS">Business</MenuItem>
                <MenuItem value="INDIVIDUAL">Individual</MenuItem>
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={() => {
              if (!displayName.trim()) {
                return;
              }
              createClientMutation.mutate({
                clientId: requestedClientId.trim() || undefined,
                clientType,
                displayName: displayName.trim(),
              });
            }}
            disabled={createClientMutation.isPending || !displayName.trim()}
          >
            {createClientMutation.isPending ? "Creating..." : "Create customer"}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

function MobileClientList({
  rows,
  selectedId,
  onSelect,
  onOpen,
}: {
  rows: ClientSummary[];
  selectedId: string | null;
  onSelect: (clientId: string) => void;
  onOpen: (clientId: string) => void;
}) {
  return (
    <List disablePadding sx={{ display: "grid", gap: 1 }}>
      {rows.map((client) => (
        <Box
          key={client.id}
          sx={{
            border: (theme) => `1px solid ${theme.palette.divider}`,
            borderRadius: 1,
            backgroundColor: client.id === selectedId ? "action.selected" : "background.paper",
            overflow: "hidden",
          }}
        >
          <ListItemButton onClick={() => onSelect(client.id)} alignItems="flex-start">
            <ListItemText
              primary={(
                <Stack spacing={1}>
                  <Stack direction="row" spacing={1} justifyContent="space-between" alignItems="flex-start">
                    <Typography variant="body2" fontWeight={600}>
                      {client.displayName}
                    </Typography>
                    <StatusBadge label={client.status} tone={mapClientStatusTone(client.status)} />
                  </Stack>
                  <Typography variant="body2" color="text.secondary">
                    {client.clientId}{client.clientIdTemporary ? " · Temporary ID" : ""}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {client.clientType}
                  </Typography>
                </Stack>
              )}
            />
          </ListItemButton>
          <Box sx={{ px: 2, pb: 1.25 }}>
            <Button size="small" onClick={() => onOpen(client.id)}>
              Open Customer360
            </Button>
          </Box>
        </Box>
      ))}
    </List>
  );
}

function SelectedCustomerDetail({
  client,
  onOpen,
}: {
  client: ClientSummary;
  onOpen: () => void;
}) {
  return (
    <Stack spacing={1.25}>
      <Stack spacing={0.25}>
        <Typography variant="body2" fontWeight={600}>
          {client.displayName}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {client.clientId}{client.clientIdTemporary ? " (Temporary ID)" : ""}
        </Typography>
      </Stack>
      <Stack direction="row" spacing={1} flexWrap="wrap">
        <StatusBadge label={client.status} tone={mapClientStatusTone(client.status)} />
        <Typography variant="body2" color="text.secondary">
          {client.clientType}
        </Typography>
      </Stack>
      <Button variant="contained" onClick={onOpen}>
        Open Customer360
      </Button>
    </Stack>
  );
}

function buildWorkspaceContextSections({
  query,
}: {
  query: string;
}): ContextSection[] {
  return [
    {
      key: "guidance",
      title: "Workspace Guidance",
      content: (
        <Stack spacing={0.5}>
          <Typography variant="body2" color="text.secondary">
            Search by customer name or client ID to move directly into Customer360.
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Apply type and status filters to narrow the active list.
          </Typography>
          {query.trim() ? (
            <Typography variant="caption" color="text.secondary">
              Current query: {query}
            </Typography>
          ) : (
            <Typography variant="caption" color="text.secondary">
              Press Enter in the search field to refresh the list quickly.
            </Typography>
          )}
        </Stack>
      ),
    },
  ];
}

function buildSelectedContextSections(client: ClientSummary, onOpen: () => void): ContextSection[] {
  return [
    {
      key: "selected-customer",
      title: "Selected Customer",
      content: (
        <Stack spacing={0.5}>
          <Typography variant="body2" fontWeight={600}>
            {client.displayName}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {client.clientId}{client.clientIdTemporary ? " (Temporary ID)" : ""}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {client.clientType}
          </Typography>
        </Stack>
      ),
    },
    {
      key: "status",
      title: "Status",
      content: (
        <Stack spacing={0.75}>
          <StatusBadge label={client.status} tone={mapClientStatusTone(client.status)} />
          {client.clientIdTemporary ? (
            <Typography variant="caption" color="text.secondary">
              Temporary ID assigned until the system of record confirms the client.
            </Typography>
          ) : null}
        </Stack>
      ),
    },
    {
      key: "quick-actions",
      title: "Quick Actions",
      content: (
        <Button variant="contained" size="small" onClick={onOpen}>
          Open Customer360
        </Button>
      ),
    },
  ];
}

function buildActiveFilters({
  query,
  selectedType,
  selectedStatus,
  onClearQuery,
  onClearType,
  onClearStatus,
}: {
  query: string;
  selectedType: ClientTypeFilter;
  selectedStatus: ClientStatusFilter;
  onClearQuery: () => void;
  onClearType: () => void;
  onClearStatus: () => void;
}) {
  const filters: Array<{ key: string; label: string; onDelete: () => void }> = [];

  if (query.trim()) {
    filters.push({ key: "query", label: `Query: ${query}`, onDelete: onClearQuery });
  }
  if (selectedType !== "ALL") {
    filters.push({ key: "type", label: `Type: ${selectedType}`, onDelete: onClearType });
  }
  if (selectedStatus !== "ALL") {
    filters.push({ key: "status", label: `Status: ${selectedStatus}`, onDelete: onClearStatus });
  }

  return filters;
}

function parseTypeFilter(value: string | null): ClientTypeFilter {
  return value === "BUSINESS" || value === "INDIVIDUAL" ? value : "ALL";
}

function parseStatusFilter(value: string | null): ClientStatusFilter {
  return value === "ACTIVE" || value === "INACTIVE" || value === "ARCHIVED" ? value : "ALL";
}

function parsePositiveInt(value: string | null, fallback: number) {
  if (value === null || value.trim() === "") {
    return fallback;
  }
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback;
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

function sortClientRows(rows: ClientSummary[], sortModel: GridSortModel) {
  const rule = sortModel[0];
  if (!rule?.field || !rule.sort) {
    return rows;
  }

  const direction = rule.sort === "desc" ? -1 : 1;
  return [...rows].sort((left, right) => {
    const leftValue = String(left[rule.field as keyof ClientSummary] ?? "").toLowerCase();
    const rightValue = String(right[rule.field as keyof ClientSummary] ?? "").toLowerCase();
    if (leftValue < rightValue) {
      return -1 * direction;
    }
    if (leftValue > rightValue) {
      return 1 * direction;
    }
    return 0;
  });
}

function mapClientStatusTone(status: ClientStatus): StatusTone {
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

function openCustomer(clientId: string, navigate: ReturnType<typeof useNavigate>) {
  navigate(`/clients/${clientId}`);
}
