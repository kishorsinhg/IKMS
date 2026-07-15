import AutoFixHighOutlinedIcon from "@mui/icons-material/AutoFixHighOutlined";
import PublishedWithChangesOutlinedIcon from "@mui/icons-material/PublishedWithChangesOutlined";
import RestartAltOutlinedIcon from "@mui/icons-material/RestartAltOutlined";
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  List,
  ListItem,
  ListItemText,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { GridColDef } from "@mui/x-data-grid";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { KeyboardEvent, useEffect, useMemo, useState } from "react";
import { useOutletContext, useSearchParams } from "react-router-dom";
import {
  bulkCorrectKnowledgeQuality,
  getKnowledgeQualityCustomer,
  knowledgeQualityQueryKeys,
  listKnowledgeQualityCustomers,
  reindexKnowledgeQuality,
  revalidateKnowledgeQuality,
  type BulkQualityCorrectionRequest,
  type CustomerKnowledgeQualitySummary,
  type KnowledgeQualityIssue,
} from "../../api/knowledgeQuality";
import { EntityGrid } from "../../app/components/EntityGrid";
import type { ContextSection } from "../../app/components/RightContextPanel";
import { StatusBadge, type StatusTone } from "../../app/components/StatusBadge";
import { WorkspaceToolbar } from "../../app/components/WorkspaceToolbar";
import { useNotification } from "../../app/providers/useNotification";
import type { IkmsShellOutletContext, ShellWorkspaceChrome } from "../../app/shell/IkmsAppShell";
import { EmptyState, ErrorState, LoadingState } from "../../app/WorkspaceStates";

type QualityGridRow = CustomerKnowledgeQualitySummary & Record<string, unknown>;
type CorrectionOperation =
  | "METADATA_CORRECTION"
  | "BUSINESS_REFERENCE_CORRECTION"
  | "CUSTOMER_REASSIGNMENT"
  | "PUBLISH";

const qualityColumns: GridColDef<QualityGridRow>[] = [
  {
    field: "customerName",
    headerName: "Customer",
    minWidth: 260,
    flex: 1.4,
    renderCell: ({ row }) => (
      <Stack spacing={0.25} sx={{ py: 0.5, minWidth: 0 }}>
        <Typography variant="body2" fontWeight={600} noWrap>
          {row.customerName}
        </Typography>
        <Typography variant="caption" color="text.secondary" noWrap>
          {row.customerExternalId}
        </Typography>
      </Stack>
    ),
  },
  {
    field: "overallScore",
    headerName: "Quality Score",
    minWidth: 140,
    flex: 0.8,
    valueFormatter: ({ value }) => formatScore(Number(value)),
  },
  {
    field: "readinessState",
    headerName: "Readiness",
    minWidth: 170,
    flex: 0.9,
    renderCell: ({ row }) => <StatusBadge label={humanizeReadiness(row.readinessState)} tone={readinessTone(row.readinessState)} />,
  },
  {
    field: "openIssueCount",
    headerName: "Open Issues",
    minWidth: 120,
    flex: 0.7,
  },
  {
    field: "evaluatedAt",
    headerName: "Evaluated",
    minWidth: 190,
    flex: 1,
    valueFormatter: ({ value }) => formatDateTime(String(value)),
  },
];

export function KnowledgeQualityPage() {
  const queryClient = useQueryClient();
  const { notify } = useNotification();
  const { setWorkspaceChrome, clearWorkspaceChrome } = useOutletContext<IkmsShellOutletContext>();
  const [searchParams, setSearchParams] = useSearchParams();
  const [localQuery, setLocalQuery] = useState(searchParams.get("q") ?? "");
  const [revalidateOpen, setRevalidateOpen] = useState(false);
  const [reindexOpen, setReindexOpen] = useState(false);
  const [correctionOpen, setCorrectionOpen] = useState(false);
  const [correctionOperation, setCorrectionOperation] = useState<CorrectionOperation>("BUSINESS_REFERENCE_CORRECTION");
  const [correctionFieldKey, setCorrectionFieldKey] = useState("");
  const [correctionValue, setCorrectionValue] = useState("");
  const [correctionTargetClientId, setCorrectionTargetClientId] = useState("");
  const [correctionSourceType, setCorrectionSourceType] = useState("DOCUMENT");
  const [correctionSourceId, setCorrectionSourceId] = useState("");

  const query = searchParams.get("q") ?? "";
  const selectedId = searchParams.get("selected");

  const customersQuery = useQuery({
    queryKey: knowledgeQualityQueryKeys.customers(query),
    queryFn: ({ signal }) => listKnowledgeQualityCustomers(query, false, signal),
  });
  const selectedCustomerQuery = useQuery({
    queryKey: selectedId ? knowledgeQualityQueryKeys.customer(selectedId) : ["knowledge-quality", "customer", "empty"],
    queryFn: ({ signal }) => getKnowledgeQualityCustomer(selectedId!, false, signal),
    enabled: Boolean(selectedId),
  });

  useEffect(() => {
    setLocalQuery(query);
  }, [query]);

  const rows = useMemo<QualityGridRow[]>(
    () => (customersQuery.data?.customers ?? []).map((customer) => ({ ...customer })),
    [customersQuery.data?.customers],
  );
  const selectedSummary = rows.find((row) => row.clientId === selectedId) ?? null;
  const selectedDetail = selectedCustomerQuery.data ?? null;
  const selectedIssues = selectedDetail?.issues ?? [];
  const selectedIssue = selectedIssues[0] ?? null;

  useEffect(() => {
    if (!selectedId && rows.length > 0) {
      setSearchParams((current) => {
        const next = new URLSearchParams(current);
        next.set("selected", rows[0].clientId);
        return next;
      });
    }
  }, [rows, selectedId, setSearchParams]);

  useEffect(() => {
    if (!selectedIssue) {
      return;
    }
    setCorrectionSourceType(selectedIssue.sourceType ?? "DOCUMENT");
    setCorrectionSourceId(selectedIssue.sourceId ?? "");
    setCorrectionFieldKey(selectedIssue.businessReferenceKey ?? "");
    setCorrectionValue("");
  }, [selectedIssue]);

  const revalidateMutation = useMutation({
    mutationFn: () =>
      revalidateKnowledgeQuality({
        clientIds: selectedId ? [selectedId] : [],
        confirmed: true,
      }),
    onSuccess: async () => {
      setRevalidateOpen(false);
      notify({ severity: "success", message: "Knowledge quality revalidated." });
      await invalidateQualityQueries(queryClient, selectedId);
    },
    onError: () => notify({ severity: "error", message: "Unable to revalidate knowledge quality." }),
  });

  const reindexMutation = useMutation({
    mutationFn: () =>
      reindexKnowledgeQuality({
        clientIds: selectedId ? [selectedId] : [],
        confirmed: true,
      }),
    onSuccess: async () => {
      setReindexOpen(false);
      notify({ severity: "success", message: "Knowledge quality reindex triggered." });
      await invalidateQualityQueries(queryClient, selectedId);
    },
    onError: () => notify({ severity: "error", message: "Unable to reindex customer knowledge." }),
  });

  const correctionMutation = useMutation({
    mutationFn: () => {
      if (!selectedId) {
        throw new Error("A customer must be selected.");
      }
      const request: BulkQualityCorrectionRequest = {
        operationType: correctionOperation,
        confirmed: true,
        items: [
          {
            clientId: selectedId,
            sourceType: correctionSourceType || undefined,
            sourceId: correctionSourceId || undefined,
            fieldKey: correctionFieldKey || undefined,
            value: correctionValue || undefined,
            targetClientId: correctionTargetClientId || undefined,
          },
        ],
      };
      return bulkCorrectKnowledgeQuality(request);
    },
    onSuccess: async () => {
      setCorrectionOpen(false);
      notify({ severity: "success", message: "Knowledge quality correction submitted." });
      await invalidateQualityQueries(queryClient, selectedId);
    },
    onError: () => notify({ severity: "error", message: "Unable to submit the quality correction." }),
  });

  const chrome = useMemo<ShellWorkspaceChrome>(() => {
    const contextSections = buildContextSections(selectedSummary, selectedDetail);

    return {
      title: "Knowledge Quality",
      subtitle: "Evaluate customer knowledge health, steward corrections, and trigger controlled revalidation or reindexing.",
      contextTitle: selectedSummary ? "Quality Context" : "Steward Context",
      contextSections,
      primaryActions: (
        <Button
          variant="contained"
          startIcon={<RestartAltOutlinedIcon fontSize="small" />}
          onClick={() => setRevalidateOpen(true)}
          disabled={!selectedId}
        >
          Revalidate
        </Button>
      ),
      secondaryActions: (
        <Button
          variant="text"
          color="inherit"
          startIcon={<PublishedWithChangesOutlinedIcon fontSize="small" />}
          onClick={() => setReindexOpen(true)}
          disabled={!selectedId}
        >
          Reindex
        </Button>
      ),
    };
  }, [selectedDetail, selectedId, selectedSummary]);

  useEffect(() => {
    setWorkspaceChrome(chrome);
    return () => clearWorkspaceChrome();
  }, [chrome, clearWorkspaceChrome, setWorkspaceChrome]);

  function applySearch() {
    setSearchParams((current) => {
      const next = new URLSearchParams(current);
      if (localQuery.trim()) {
        next.set("q", localQuery.trim());
      } else {
        next.delete("q");
      }
      next.delete("selected");
      return next;
    });
  }

  function handleSearchKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Enter") {
      event.preventDefault();
      applySearch();
    }
  }

  if (customersQuery.isLoading) {
    return (
      <LoadingState
        title="Loading knowledge quality workspace"
        message="Preparing customer quality scores, issue counts, and stewardship actions."
      />
    );
  }

  if (customersQuery.isError) {
    return (
      <ErrorState
        title="Unable to load knowledge quality"
        message="The stewardship workspace could not retrieve customer quality summaries."
      />
    );
  }

  if (rows.length === 0) {
    return (
      <EmptyState
        title="No customer knowledge quality results"
        message="Quality snapshots will appear here after customer knowledge has been evaluated."
      />
    );
  }

  return (
    <Stack spacing={2}>
      <WorkspaceToolbar
        searchPlaceholder="Search by customer name or external reference"
        searchValue={localQuery}
        onSearchChange={setLocalQuery}
        onSearchKeyDown={handleSearchKeyDown}
        onRefresh={() => void invalidateQualityQueries(queryClient, selectedId)}
        primaryAction={
          <Button
            variant="outlined"
            startIcon={<AutoFixHighOutlinedIcon fontSize="small" />}
            onClick={() => setCorrectionOpen(true)}
            disabled={!selectedId}
          >
            Prepare correction
          </Button>
        }
      />

      <EntityGrid<QualityGridRow>
        rows={rows}
        columns={qualityColumns}
        getRowId={(row) => row.clientId}
        onRowClick={(params) => {
          setSearchParams((current) => {
            const next = new URLSearchParams(current);
            next.set("selected", String(params.id));
            return next;
          });
        }}
        loading={customersQuery.isFetching}
        emptyTitle="No customer quality results"
        emptyMessage="Adjust the search query or wait for customer quality evaluations to be generated."
      />

      {selectedDetail ? (
        <Box
          sx={{
            display: "grid",
            gap: 2,
            gridTemplateColumns: { xs: "1fr", xl: "minmax(0, 1.2fr) minmax(320px, 0.8fr)" },
          }}
        >
          <Box
            sx={{
              border: (theme) => `1px solid ${theme.palette.divider}`,
              borderRadius: 1,
              bgcolor: "background.paper",
              p: 2,
            }}
          >
            <Stack spacing={1.5}>
              <Stack direction={{ xs: "column", md: "row" }} spacing={1} justifyContent="space-between">
                <Stack spacing={0.5}>
                  <Typography variant="h2" sx={{ fontSize: "1rem" }}>
                    Steward Queue
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Open issues are grouped under the selected customer and remain customer-centric knowledge remediation work.
                  </Typography>
                </Stack>
                <StatusBadge
                  label={`${selectedIssues.length} open issue${selectedIssues.length === 1 ? "" : "s"}`}
                  tone={selectedIssues.length > 0 ? "warning" : "success"}
                />
              </Stack>

              {selectedIssues.length === 0 ? (
                <Alert severity="success">This customer currently has no open knowledge quality issues.</Alert>
              ) : (
                <List disablePadding sx={{ display: "grid", gap: 1 }}>
                  {selectedIssues.map((issue) => (
                    <ListItem
                      key={issue.id}
                      disablePadding
                      sx={{
                        border: (theme) => `1px solid ${theme.palette.divider}`,
                        borderRadius: 1,
                        px: 1.5,
                        py: 1.25,
                        alignItems: "flex-start",
                      }}
                    >
                      <ListItemText
                        secondaryTypographyProps={{ component: "div" }}
                        primary={
                          <Stack
                            direction={{ xs: "column", md: "row" }}
                            spacing={1}
                            alignItems={{ xs: "flex-start", md: "center" }}
                            justifyContent="space-between"
                          >
                            <Typography variant="subtitle2">{issue.title}</Typography>
                            <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
                              <StatusBadge label={humanizeSeverity(issue.severity)} tone={severityTone(issue.severity)} />
                              <StatusBadge label={humanizeCategory(issue.category)} tone="info" />
                            </Stack>
                          </Stack>
                        }
                        secondary={
                          <Stack spacing={0.75} sx={{ mt: 0.75 }}>
                            {issue.detail ? (
                              <Typography variant="body2" color="text.secondary">
                                {issue.detail}
                              </Typography>
                            ) : null}
                            <Typography variant="caption" color="text.secondary">
                              {issue.recommendationDetail ?? fallbackRecommendation(issue)}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              {issue.sourceType ? `${issue.sourceType} source` : "Customer-level issue"} · Updated {formatDateTime(issue.updatedAt)}
                            </Typography>
                          </Stack>
                        }
                      />
                    </ListItem>
                  ))}
                </List>
              )}
            </Stack>
          </Box>

          <Box
            sx={{
              border: (theme) => `1px solid ${theme.palette.divider}`,
              borderRadius: 1,
              bgcolor: "background.paper",
              p: 2,
            }}
          >
            <Stack spacing={1.5}>
              <Typography variant="h2" sx={{ fontSize: "1rem" }}>
                Quality Score Breakdown
              </Typography>
              {selectedDetail.summary.dimensions.map((dimension) => (
                <Stack key={dimension.key} spacing={0.4}>
                  <Stack direction="row" justifyContent="space-between" spacing={1}>
                    <Typography variant="body2" fontWeight={600}>
                      {dimension.label}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {formatScore(dimension.score)}
                    </Typography>
                  </Stack>
                  <Typography variant="caption" color="text.secondary">
                    {dimension.summary}
                  </Typography>
                </Stack>
              ))}
            </Stack>
          </Box>
        </Box>
      ) : selectedCustomerQuery.isLoading ? (
        <LoadingState
          title="Loading customer quality detail"
          message="Retrieving the selected customer quality dimensions and open issues."
        />
      ) : null}

      <ConfirmationDialog
        open={revalidateOpen}
        title="Revalidate knowledge quality"
        description="This recomputes customer quality scores and issue detection without changing policy or claim ownership semantics."
        confirmLabel="Run revalidation"
        busy={revalidateMutation.isPending}
        onClose={() => setRevalidateOpen(false)}
        onConfirm={() => revalidateMutation.mutate()}
      />

      <ConfirmationDialog
        open={reindexOpen}
        title="Reindex customer knowledge"
        description="This republishes customer knowledge projections and rebuilds retrieval-ready artifacts for the selected customer."
        confirmLabel="Run reindex"
        busy={reindexMutation.isPending}
        onClose={() => setReindexOpen(false)}
        onConfirm={() => reindexMutation.mutate()}
      />

      <CorrectionDialog
        open={correctionOpen}
        busy={correctionMutation.isPending}
        operation={correctionOperation}
        fieldKey={correctionFieldKey}
        sourceId={correctionSourceId}
        sourceType={correctionSourceType}
        targetClientId={correctionTargetClientId}
        value={correctionValue}
        onClose={() => setCorrectionOpen(false)}
        onConfirm={() => correctionMutation.mutate()}
        onFieldKeyChange={setCorrectionFieldKey}
        onOperationChange={setCorrectionOperation}
        onSourceIdChange={setCorrectionSourceId}
        onSourceTypeChange={setCorrectionSourceType}
        onTargetClientIdChange={setCorrectionTargetClientId}
        onValueChange={setCorrectionValue}
      />
    </Stack>
  );
}

function ConfirmationDialog({
  open,
  title,
  description,
  confirmLabel,
  busy,
  onClose,
  onConfirm,
}: {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  busy: boolean;
  onClose: () => void;
  onConfirm: () => void;
}) {
  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary">
          {description}
        </Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={onConfirm} disabled={busy}>
          {confirmLabel}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function CorrectionDialog(props: {
  open: boolean;
  busy: boolean;
  operation: CorrectionOperation;
  sourceType: string;
  sourceId: string;
  fieldKey: string;
  value: string;
  targetClientId: string;
  onClose: () => void;
  onConfirm: () => void;
  onOperationChange: (value: CorrectionOperation) => void;
  onSourceTypeChange: (value: string) => void;
  onSourceIdChange: (value: string) => void;
  onFieldKeyChange: (value: string) => void;
  onValueChange: (value: string) => void;
  onTargetClientIdChange: (value: string) => void;
}) {
  const {
    open,
    busy,
    operation,
    sourceType,
    sourceId,
    fieldKey,
    value,
    targetClientId,
    onClose,
    onConfirm,
    onOperationChange,
    onSourceTypeChange,
    onSourceIdChange,
    onFieldKeyChange,
    onValueChange,
    onTargetClientIdChange,
  } = props;

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Prepare correction</DialogTitle>
      <DialogContent sx={{ display: "grid", gap: 2, pt: 1.5 }}>
        <Typography variant="body2" color="text.secondary">
          Steward actions remain human-reviewed. Policy Number and Claim Number stay structured Business Reference Fields, not IKMS-owned entities.
        </Typography>
        <FormControl fullWidth size="small">
          <InputLabel id="quality-operation-label">Operation</InputLabel>
          <Select
            labelId="quality-operation-label"
            label="Operation"
            value={operation}
            onChange={(event) => onOperationChange(event.target.value as CorrectionOperation)}
          >
            <MenuItem value="BUSINESS_REFERENCE_CORRECTION">Business reference correction</MenuItem>
            <MenuItem value="METADATA_CORRECTION">Metadata correction</MenuItem>
            <MenuItem value="CUSTOMER_REASSIGNMENT">Customer reassignment</MenuItem>
            <MenuItem value="PUBLISH">Publish selected source</MenuItem>
          </Select>
        </FormControl>
        <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
          <TextField
            label="Source Type"
            value={sourceType}
            onChange={(event) => onSourceTypeChange(event.target.value)}
            size="small"
            fullWidth
          />
          <TextField
            label="Source ID"
            value={sourceId}
            onChange={(event) => onSourceIdChange(event.target.value)}
            size="small"
            fullWidth
          />
        </Stack>
        {operation !== "PUBLISH" ? (
          <TextField
            label="Field Key"
            value={fieldKey}
            onChange={(event) => onFieldKeyChange(event.target.value)}
            size="small"
            fullWidth
          />
        ) : null}
        {operation === "CUSTOMER_REASSIGNMENT" ? (
          <TextField
            label="Target Customer ID"
            value={targetClientId}
            onChange={(event) => onTargetClientIdChange(event.target.value)}
            size="small"
            fullWidth
          />
        ) : operation !== "PUBLISH" ? (
          <TextField
            label="Corrected Value"
            value={value}
            onChange={(event) => onValueChange(event.target.value)}
            size="small"
            fullWidth
          />
        ) : null}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={onConfirm} disabled={busy}>
          Submit
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function buildContextSections(
  summary: CustomerKnowledgeQualitySummary | null,
  detail: { summary: CustomerKnowledgeQualitySummary; issues: KnowledgeQualityIssue[] } | null,
): ContextSection[] {
  if (!summary || !detail) {
    return [
      {
        key: "quality-overview",
        title: "Quality Overview",
        content: (
          <Typography variant="body2" color="text.secondary">
            Select a customer to inspect quality dimensions, steward actions, and retrieval-readiness issues.
          </Typography>
        ),
      },
    ];
  }

  return [
    {
      key: "quality-snapshot",
      title: "Quality Snapshot",
      content: (
        <Stack spacing={1}>
          <StatusBadge label={humanizeReadiness(summary.readinessState)} tone={readinessTone(summary.readinessState)} />
          <Typography variant="body2">
            Overall score {formatScore(summary.overallScore)}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Evaluated {formatDateTime(summary.evaluatedAt)}
          </Typography>
        </Stack>
      ),
    },
    {
      key: "quality-recommendations",
      title: "Recommendations",
      content: summary.recommendationHighlights.length > 0 ? (
        <Stack spacing={0.75}>
          {summary.recommendationHighlights.slice(0, 4).map((item) => (
            <Typography key={item} variant="body2" color="text.secondary">
              {item}
            </Typography>
          ))}
        </Stack>
      ) : (
        <Typography variant="body2" color="text.secondary">
          No recommendation highlights are currently open for this customer.
        </Typography>
      ),
    },
    {
      key: "quality-issues",
      title: "Issue Focus",
      content: detail.issues.length > 0 ? (
        <Stack spacing={0.75}>
          {detail.issues.slice(0, 3).map((issue) => (
            <Typography key={issue.id} variant="body2" color="text.secondary">
              {issue.title}
            </Typography>
          ))}
        </Stack>
      ) : (
        <Typography variant="body2" color="text.secondary">
          No open stewardship issues for this customer.
        </Typography>
      ),
    },
  ];
}

async function invalidateQualityQueries(queryClient: ReturnType<typeof useQueryClient>, selectedId: string | null) {
  await queryClient.invalidateQueries({ queryKey: ["knowledge-quality"] });
  if (selectedId) {
    await queryClient.invalidateQueries({ queryKey: knowledgeQualityQueryKeys.customer(selectedId) });
  }
}

function formatScore(value: number) {
  return `${Math.round(value * 100)}%`;
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString();
}

function readinessTone(state: CustomerKnowledgeQualitySummary["readinessState"]): StatusTone {
  switch (state) {
    case "READY":
      return "success";
    case "BLOCKED":
      return "error";
    default:
      return "warning";
  }
}

function severityTone(severity: KnowledgeQualityIssue["severity"]): StatusTone {
  switch (severity) {
    case "CRITICAL":
      return "error";
    case "HIGH":
      return "warning";
    case "MEDIUM":
      return "info";
    default:
      return "neutral";
  }
}

function humanizeReadiness(state: CustomerKnowledgeQualitySummary["readinessState"]) {
  return state === "NEEDS_ATTENTION" ? "Needs attention" : state === "BLOCKED" ? "Blocked" : "Ready";
}

function humanizeSeverity(severity: KnowledgeQualityIssue["severity"]) {
  return severity.charAt(0) + severity.slice(1).toLowerCase();
}

function humanizeCategory(category: string) {
  return category
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function fallbackRecommendation(issue: KnowledgeQualityIssue) {
  if (issue.recommendationType) {
    return `Recommended action: ${issue.recommendationType.toLowerCase().replace(/_/g, " ")}.`;
  }
  return "Recommended action: review the source evidence and republish the corrected customer knowledge if needed.";
}
