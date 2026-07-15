import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import AdminPanelSettingsOutlinedIcon from "@mui/icons-material/AdminPanelSettingsOutlined";
import ArrowForwardOutlinedIcon from "@mui/icons-material/ArrowForwardOutlined";
import AutoAwesomeOutlinedIcon from "@mui/icons-material/AutoAwesomeOutlined";
import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import ContentCopyOutlinedIcon from "@mui/icons-material/ContentCopyOutlined";
import DescriptionOutlinedIcon from "@mui/icons-material/DescriptionOutlined";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import FolderOutlinedIcon from "@mui/icons-material/FolderOutlined";
import MailOutlineOutlinedIcon from "@mui/icons-material/MailOutlineOutlined";
import PersonOutlineOutlinedIcon from "@mui/icons-material/PersonOutlineOutlined";
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import ViewListOutlinedIcon from "@mui/icons-material/ViewListOutlined";
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
  FormControlLabel,
  InputLabel,
  List,
  ListItemButton,
  ListItemText,
  MenuItem,
  Select,
  Stack,
  Switch,
  TextField,
  Tooltip,
  Typography,
  useMediaQuery,
} from "@mui/material";
import { useTheme } from "@mui/material/styles";
import { SimpleTreeView } from "@mui/x-tree-view/SimpleTreeView";
import { TreeItem } from "@mui/x-tree-view/TreeItem";
import { GridColDef, GridPaginationModel, GridSortModel } from "@mui/x-data-grid";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { KeyboardEvent as ReactKeyboardEvent, SyntheticEvent, useEffect, useMemo, useState } from "react";
import { useOutletContext, useSearchParams } from "react-router-dom";
import {
  AiProviderSettingConfig,
  AiProviderValidationResult,
  AdminUser,
  createDocumentType,
  createMailbox,
  createMetadataField,
  createSharedFolder,
  DocumentTypeConfig,
  getAiSetting,
  getReviewSetting,
  listAdminUsers,
  listDocumentTypes,
  listMailboxes,
  listMetadataFields,
  listSharedFolders,
  MailboxConfig,
  MetadataFieldConfig,
  ReviewSettingConfig,
  SharedFolderConfig,
  updateAiSetting,
  updateReviewSetting,
  validateAiSetting,
} from "../../api/admin";
import {
  AiGovernancePolicyConfig,
  ClassificationPolicyConfig,
  ComplianceReport,
  getAiGovernancePolicy,
  getClassificationPolicy,
  getComplianceReport,
  getRetentionPolicy,
  getSecurityPolicy,
  LegalHoldConfig,
  listLegalHolds,
  RetentionPolicyConfig,
  SecurityPolicyConfig,
  updateAiGovernancePolicy,
  updateSecurityPolicy,
} from "../../api/governance";
import {
  cancelOperationsJob,
  clearOperationsCache,
  disableOperationsScheduler,
  enableOperationsScheduler,
  getOperationsDiagnostics,
  getOperationsHealth,
  listOperationsCaches,
  listOperationsJobs,
  listOperationsQueues,
  listOperationsSchedulers,
  OperationsCache,
  OperationsDiagnostics,
  OperationsHealthComponent,
  OperationsJob,
  OperationsQueue,
  OperationsScheduler,
  refreshOperationsCache,
  resumeOperationsQueue,
  retryOperationsJob,
  pauseOperationsQueue,
  runOperationsScheduler,
  invalidateOperationsCache,
} from "../../api/operations";
import { EntityGrid } from "../../app/components/EntityGrid";
import type { ContextSection } from "../../app/components/RightContextPanel";
import { StatusBadge, StatusTone } from "../../app/components/StatusBadge";
import { WorkspaceToolbar } from "../../app/components/WorkspaceToolbar";
import { useNotification } from "../../app/providers/useNotification";
import type { IkmsShellOutletContext, ShellWorkspaceChrome } from "../../app/shell/IkmsAppShell";
import { EmptyState, ErrorState, LoadingState, NoResultsState, RetryAction } from "../../app/WorkspaceStates";

type AdminModuleKey =
  | "users"
  | "document-types"
  | "metadata-fields"
  | "shared-folders"
  | "mailboxes"
  | "review-settings"
  | "ai-settings"
  | "background-jobs"
  | "queues"
  | "scheduler"
  | "embeddings"
  | "ocr"
  | "ai-operations"
  | "cache"
  | "health"
  | "diagnostics"
  | "classification-policies"
  | "retention-policies"
  | "legal-holds"
  | "ai-governance"
  | "security-policies";

type StatusFilter = "ALL" | "ACTIVE" | "INACTIVE" | "DISABLED" | "ARCHIVED" | "CONFIGURED";
type AdminSortField = "configuration" | "type" | "status" | "lastUpdated" | "updatedBy";
type EditorMode = "view" | "create" | "edit";

interface AdminGridRow extends Record<string, unknown> {
  id: string;
  configuration: string;
  configurationDetail: string;
  type: AdminModuleKey;
  typeLabel: string;
  status: StatusFilter | "ACTIVE";
  statusLabel: string;
  statusTone: StatusTone;
  lastUpdated: string;
  lastUpdatedLabel: string;
  updatedBy: string;
  description: string;
  relatedDependencies: string[];
  raw:
    | AdminUser
    | DocumentTypeConfig
    | MetadataFieldConfig
    | SharedFolderConfig
    | MailboxConfig
    | ReviewSettingConfig
    | AiProviderSettingConfig
    | OperationsJob
    | OperationsQueue
    | OperationsScheduler
    | OperationsCache
    | OperationsHealthComponent
    | OperationsDiagnostics
    | ClassificationPolicyConfig
    | RetentionPolicyConfig
    | LegalHoldConfig
    | AiGovernancePolicyConfig
    | SecurityPolicyConfig
    | ComplianceReport;
}

interface AdminEditorState {
  module: AdminModuleKey;
  rowId?: string | null;
  mode: EditorMode;
}

interface ModuleOption {
  key: AdminModuleKey;
  label: string;
  description: string;
  editable: boolean;
  creatable: boolean;
}

const adminQueryKey = ["admin"] as const;
const defaultPageSize = 10;
const sortFields = new Set<AdminSortField>(["configuration", "type", "status", "lastUpdated", "updatedBy"]);

function explorerLabel(icon: React.ReactNode, text: string) {
  return (
    <Stack direction="row" spacing={0.75} alignItems="center">
      <Box sx={{ color: "text.secondary", display: "grid", placeItems: "center" }}>{icon}</Box>
      <Typography variant="body2">{text}</Typography>
    </Stack>
  );
}

const moduleOptions: ModuleOption[] = [
  {
    key: "users",
    label: "Users",
    description: "Provisioned identities and assigned roles.",
    editable: false,
    creatable: false,
  },
  {
    key: "document-types",
    label: "Document Types",
    description: "Supported inbound and reviewable document categories.",
    editable: false,
    creatable: true,
  },
  {
    key: "metadata-fields",
    label: "Metadata Templates",
    description: "Structured extraction fields and PII tagging.",
    editable: false,
    creatable: true,
  },
  {
    key: "shared-folders",
    label: "System Settings",
    description: "Shared intake locations and file-system configuration.",
    editable: false,
    creatable: true,
  },
  {
    key: "mailboxes",
    label: "Notification Rules",
    description: "Mailbox integrations used for intake and operational routing.",
    editable: false,
    creatable: true,
  },
  {
    key: "review-settings",
    label: "Permission Groups",
    description: "Human review thresholds and queue behavior.",
    editable: true,
    creatable: false,
  },
  {
    key: "ai-settings",
    label: "AI Configuration",
    description: "Provider, model, embedding, and OCR configuration.",
    editable: true,
    creatable: false,
  },
  {
    key: "background-jobs",
    label: "Background Jobs",
    description: "Operational jobs with progress, retry, cancellation, and failure visibility.",
    editable: false,
    creatable: false,
  },
  {
    key: "queues",
    label: "Queues",
    description: "Operational queue depth, pause state, and queue-level controls.",
    editable: false,
    creatable: false,
  },
  {
    key: "scheduler",
    label: "Scheduler",
    description: "Scheduled operational jobs, enablement state, and execution history.",
    editable: false,
    creatable: false,
  },
  {
    key: "embeddings",
    label: "Embeddings",
    description: "Embedding rebuild and vector-maintenance operations.",
    editable: false,
    creatable: false,
  },
  {
    key: "ocr",
    label: "OCR",
    description: "OCR retries, OCR-backed failures, and document-processing visibility.",
    editable: false,
    creatable: false,
  },
  {
    key: "ai-operations",
    label: "AI Operations",
    description: "AI retries, provider-backed operational jobs, and degraded-request visibility.",
    editable: false,
    creatable: false,
  },
  {
    key: "cache",
    label: "Cache",
    description: "Cache clear, invalidate, and refresh actions across operational caches.",
    editable: false,
    creatable: false,
  },
  {
    key: "health",
    label: "Health",
    description: "Platform health status by dependency and subsystem.",
    editable: false,
    creatable: false,
  },
  {
    key: "diagnostics",
    label: "Diagnostics",
    description: "System information, bottlenecks, queue depth, failed jobs, and validation notes.",
    editable: false,
    creatable: false,
  },
  {
    key: "classification-policies",
    label: "Classification Policies",
    description: "Information-classification levels and governance thresholds.",
    editable: false,
    creatable: false,
  },
  {
    key: "retention-policies",
    label: "Retention Policies",
    description: "Retention, review, archive, and disposal eligibility windows.",
    editable: false,
    creatable: false,
  },
  {
    key: "legal-holds",
    label: "Legal Hold",
    description: "Active legal hold records and protected retention states.",
    editable: false,
    creatable: false,
  },
  {
    key: "ai-governance",
    label: "AI Governance",
    description: "Approved models, prompt policy, response policy, and grounding controls.",
    editable: true,
    creatable: false,
  },
  {
    key: "security-policies",
    label: "Security Policies",
    description: "Encryption posture, secret handling, and export governance defaults.",
    editable: true,
    creatable: false,
  },
];

const statusOptions: Array<{ value: StatusFilter; label: string }> = [
  { value: "ALL", label: "All statuses" },
  { value: "ACTIVE", label: "Active" },
  { value: "INACTIVE", label: "Inactive" },
  { value: "DISABLED", label: "Disabled" },
  { value: "ARCHIVED", label: "Archived" },
  { value: "CONFIGURED", label: "Configured" },
];

export function AdminConfigurationPage() {
  const queryClient = useQueryClient();
  const theme = useTheme();
  const { notify } = useNotification();
  const { setWorkspaceChrome, clearWorkspaceChrome } = useOutletContext<IkmsShellOutletContext>();
  const [searchParams, setSearchParams] = useSearchParams();
  const isTabletDown = useMediaQuery(theme.breakpoints.down("lg"));
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));
  const [editorState, setEditorState] = useState<AdminEditorState | null>(null);
  const [discardDialogOpen, setDiscardDialogOpen] = useState(false);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [localQuery, setLocalQuery] = useState(searchParams.get("q") ?? "");

  const selectedModule = parseModule(searchParams.get("type"));
  const selectedStatus = parseStatus(searchParams.get("status"));
  const selectedId = searchParams.get("selected");
  const query = searchParams.get("q") ?? "";
  const page = parsePageIndex(searchParams.get("page"), 0);
  const pageSize = parsePageSize(searchParams.get("pageSize"), defaultPageSize);
  const sortField = parseSortField(searchParams.get("sort"));
  const sortDirection = searchParams.get("dir") === "desc" ? "desc" : "asc";

  const usersQuery = useQuery({ queryKey: [...adminQueryKey, "users"], queryFn: listAdminUsers });
  const documentTypesQuery = useQuery({ queryKey: [...adminQueryKey, "document-types"], queryFn: listDocumentTypes });
  const metadataFieldsQuery = useQuery({ queryKey: [...adminQueryKey, "metadata-fields"], queryFn: listMetadataFields });
  const sharedFoldersQuery = useQuery({ queryKey: [...adminQueryKey, "shared-folders"], queryFn: listSharedFolders });
  const mailboxesQuery = useQuery({ queryKey: [...adminQueryKey, "mailboxes"], queryFn: listMailboxes });
  const reviewSettingQuery = useQuery({ queryKey: [...adminQueryKey, "review-setting"], queryFn: getReviewSetting });
  const aiSettingQuery = useQuery({ queryKey: [...adminQueryKey, "ai-setting"], queryFn: getAiSetting });
  const classificationPolicyQuery = useQuery({ queryKey: [...adminQueryKey, "classification-policy"], queryFn: getClassificationPolicy });
  const retentionPolicyQuery = useQuery({ queryKey: [...adminQueryKey, "retention-policy"], queryFn: getRetentionPolicy });
  const legalHoldsQuery = useQuery({ queryKey: [...adminQueryKey, "legal-holds"], queryFn: listLegalHolds });
  const aiGovernanceQuery = useQuery({ queryKey: [...adminQueryKey, "ai-governance"], queryFn: getAiGovernancePolicy });
  const securityPolicyQuery = useQuery({ queryKey: [...adminQueryKey, "security-policy"], queryFn: getSecurityPolicy });
  const complianceReportQuery = useQuery({ queryKey: [...adminQueryKey, "compliance-report"], queryFn: getComplianceReport });
  const operationsJobsQuery = useQuery({ queryKey: [...adminQueryKey, "operations-jobs"], queryFn: listOperationsJobs });
  const operationsQueuesQuery = useQuery({ queryKey: [...adminQueryKey, "operations-queues"], queryFn: listOperationsQueues });
  const operationsSchedulersQuery = useQuery({ queryKey: [...adminQueryKey, "operations-schedulers"], queryFn: listOperationsSchedulers });
  const operationsCachesQuery = useQuery({ queryKey: [...adminQueryKey, "operations-caches"], queryFn: listOperationsCaches });
  const operationsHealthQuery = useQuery({ queryKey: [...adminQueryKey, "operations-health"], queryFn: getOperationsHealth });
  const operationsDiagnosticsQuery = useQuery({ queryKey: [...adminQueryKey, "operations-diagnostics"], queryFn: getOperationsDiagnostics });

  useEffect(() => {
    setLocalQuery(query);
  }, [query]);

  const createDocumentTypeMutation = useMutation({
    mutationFn: createDocumentType,
    onSuccess: async () => {
      notify({ severity: "success", message: "Document type created." });
      await queryClient.invalidateQueries({ queryKey: [...adminQueryKey, "document-types"] });
    },
    onError: () => notify({ severity: "error", message: "Unable to create document type." }),
  });

  const createMetadataFieldMutation = useMutation({
    mutationFn: createMetadataField,
    onSuccess: async () => {
      notify({ severity: "success", message: "Metadata template created." });
      await queryClient.invalidateQueries({ queryKey: [...adminQueryKey, "metadata-fields"] });
    },
    onError: () => notify({ severity: "error", message: "Unable to create metadata template." }),
  });

  const createSharedFolderMutation = useMutation({
    mutationFn: createSharedFolder,
    onSuccess: async () => {
      notify({ severity: "success", message: "Shared folder created." });
      await queryClient.invalidateQueries({ queryKey: [...adminQueryKey, "shared-folders"] });
    },
    onError: () => notify({ severity: "error", message: "Unable to create shared folder." }),
  });

  const createMailboxMutation = useMutation({
    mutationFn: createMailbox,
    onSuccess: async () => {
      notify({ severity: "success", message: "Mailbox created." });
      await queryClient.invalidateQueries({ queryKey: [...adminQueryKey, "mailboxes"] });
    },
    onError: () => notify({ severity: "error", message: "Unable to create mailbox." }),
  });

  const updateReviewMutation = useMutation({
    mutationFn: updateReviewSetting,
    onSuccess: async () => {
      notify({ severity: "success", message: "Review settings updated." });
      await queryClient.invalidateQueries({ queryKey: [...adminQueryKey, "review-setting"] });
    },
    onError: () => notify({ severity: "error", message: "Unable to update review settings." }),
  });

  const updateAiMutation = useMutation({
    mutationFn: updateAiSetting,
    onSuccess: async () => {
      notify({ severity: "success", message: "AI configuration updated." });
      await queryClient.invalidateQueries({ queryKey: [...adminQueryKey, "ai-setting"] });
    },
    onError: () => notify({ severity: "error", message: "Unable to update AI configuration." }),
  });

  const validateAiMutation = useMutation({
    mutationFn: validateAiSetting,
    onSuccess: (result) => {
      notify({
        severity: result.valid ? "success" : "warning",
        message: `${result.status}: ${result.message}`,
      });
    },
    onError: () => notify({ severity: "error", message: "Unable to validate AI configuration." }),
  });

  const updateAiGovernanceMutation = useMutation({
    mutationFn: updateAiGovernancePolicy,
    onSuccess: async () => {
      notify({ severity: "success", message: "AI governance updated." });
      await queryClient.invalidateQueries({ queryKey: [...adminQueryKey, "ai-governance"] });
    },
    onError: () => notify({ severity: "error", message: "Unable to update AI governance." }),
  });

  const updateSecurityPolicyMutation = useMutation({
    mutationFn: updateSecurityPolicy,
    onSuccess: async () => {
      notify({ severity: "success", message: "Security policy updated." });
      await queryClient.invalidateQueries({ queryKey: [...adminQueryKey, "security-policy"] });
    },
    onError: () => notify({ severity: "error", message: "Unable to update security policy." }),
  });

  const retryOperationsJobMutation = useMutation({
    mutationFn: retryOperationsJob,
    onSuccess: async () => {
      notify({ severity: "success", message: "Background job retried." });
      await invalidateOperationsQueries(queryClient);
    },
    onError: () => notify({ severity: "error", message: "Unable to retry background job." }),
  });

  const cancelOperationsJobMutation = useMutation({
    mutationFn: cancelOperationsJob,
    onSuccess: async () => {
      notify({ severity: "success", message: "Background job cancelled." });
      await invalidateOperationsQueries(queryClient);
    },
    onError: () => notify({ severity: "error", message: "Unable to cancel background job." }),
  });

  const pauseQueueMutation = useMutation({
    mutationFn: pauseOperationsQueue,
    onSuccess: async () => {
      notify({ severity: "success", message: "Queue paused." });
      await invalidateOperationsQueries(queryClient);
    },
    onError: () => notify({ severity: "error", message: "Unable to pause queue." }),
  });

  const resumeQueueMutation = useMutation({
    mutationFn: resumeOperationsQueue,
    onSuccess: async () => {
      notify({ severity: "success", message: "Queue resumed." });
      await invalidateOperationsQueries(queryClient);
    },
    onError: () => notify({ severity: "error", message: "Unable to resume queue." }),
  });

  const runSchedulerMutation = useMutation({
    mutationFn: runOperationsScheduler,
    onSuccess: async () => {
      notify({ severity: "success", message: "Scheduler run requested." });
      await invalidateOperationsQueries(queryClient);
    },
    onError: () => notify({ severity: "error", message: "Unable to run scheduler." }),
  });

  const enableSchedulerMutation = useMutation({
    mutationFn: enableOperationsScheduler,
    onSuccess: async () => {
      notify({ severity: "success", message: "Scheduler enabled." });
      await invalidateOperationsQueries(queryClient);
    },
    onError: () => notify({ severity: "error", message: "Unable to enable scheduler." }),
  });

  const disableSchedulerMutation = useMutation({
    mutationFn: disableOperationsScheduler,
    onSuccess: async () => {
      notify({ severity: "success", message: "Scheduler disabled." });
      await invalidateOperationsQueries(queryClient);
    },
    onError: () => notify({ severity: "error", message: "Unable to disable scheduler." }),
  });

  const clearCacheMutation = useMutation({
    mutationFn: clearOperationsCache,
    onSuccess: async () => {
      notify({ severity: "success", message: "Cache cleared." });
      await invalidateOperationsQueries(queryClient);
    },
    onError: () => notify({ severity: "error", message: "Unable to clear cache." }),
  });

  const invalidateCacheMutation = useMutation({
    mutationFn: invalidateOperationsCache,
    onSuccess: async () => {
      notify({ severity: "success", message: "Cache invalidated." });
      await invalidateOperationsQueries(queryClient);
    },
    onError: () => notify({ severity: "error", message: "Unable to invalidate cache." }),
  });

  const refreshCacheMutation = useMutation({
    mutationFn: refreshOperationsCache,
    onSuccess: async () => {
      notify({ severity: "success", message: "Cache refreshed." });
      await invalidateOperationsQueries(queryClient);
    },
    onError: () => notify({ severity: "error", message: "Unable to refresh cache." }),
  });

  const rows = useMemo<AdminGridRow[]>(() => {
    const nextRows: AdminGridRow[] = [];

    for (const user of usersQuery.data ?? []) {
      nextRows.push(mapUserRow(user));
    }
    for (const item of documentTypesQuery.data ?? []) {
      nextRows.push(mapDocumentTypeRow(item));
    }
    for (const item of metadataFieldsQuery.data ?? []) {
      nextRows.push(mapMetadataFieldRow(item));
    }
    for (const item of sharedFoldersQuery.data ?? []) {
      nextRows.push(mapSharedFolderRow(item));
    }
    for (const item of mailboxesQuery.data ?? []) {
      nextRows.push(mapMailboxRow(item));
    }
    if (reviewSettingQuery.data) {
      nextRows.push(mapReviewSettingRow(reviewSettingQuery.data));
    }
    if (aiSettingQuery.data) {
      nextRows.push(mapAiSettingRow(aiSettingQuery.data));
    }
    for (const item of operationsJobsQuery.data ?? []) {
      nextRows.push(mapOperationsJobRow(item, "background-jobs"));
      if (isEmbeddingJob(item)) {
        nextRows.push(mapOperationsJobRow(item, "embeddings"));
      }
      if (isOcrJob(item)) {
        nextRows.push(mapOperationsJobRow(item, "ocr"));
      }
      if (isAiOperationsJob(item)) {
        nextRows.push(mapOperationsJobRow(item, "ai-operations"));
      }
    }
    for (const item of operationsQueuesQuery.data ?? []) {
      nextRows.push(mapOperationsQueueRow(item));
    }
    for (const item of operationsSchedulersQuery.data ?? []) {
      nextRows.push(mapOperationsSchedulerRow(item));
    }
    for (const item of operationsCachesQuery.data ?? []) {
      nextRows.push(mapOperationsCacheRow(item));
    }
    for (const item of operationsHealthQuery.data?.components ?? []) {
      nextRows.push(mapOperationsHealthRow(item));
    }
    if (operationsDiagnosticsQuery.data) {
      nextRows.push(...mapOperationsDiagnosticsRows(operationsDiagnosticsQuery.data));
    }
    if (classificationPolicyQuery.data) {
      nextRows.push(mapClassificationPolicyRow(classificationPolicyQuery.data, complianceReportQuery.data ?? null));
    }
    if (retentionPolicyQuery.data) {
      nextRows.push(mapRetentionPolicyRow(retentionPolicyQuery.data));
    }
    for (const item of legalHoldsQuery.data ?? []) {
      nextRows.push(mapLegalHoldRow(item));
    }
    if (aiGovernanceQuery.data) {
      nextRows.push(mapAiGovernanceRow(aiGovernanceQuery.data));
    }
    if (securityPolicyQuery.data) {
      nextRows.push(mapSecurityPolicyRow(securityPolicyQuery.data));
    }

    return nextRows;
  }, [
    aiSettingQuery.data,
    aiGovernanceQuery.data,
    classificationPolicyQuery.data,
    complianceReportQuery.data,
    documentTypesQuery.data,
    legalHoldsQuery.data,
    mailboxesQuery.data,
    metadataFieldsQuery.data,
    operationsCachesQuery.data,
    operationsDiagnosticsQuery.data,
    operationsHealthQuery.data,
    operationsJobsQuery.data,
    operationsQueuesQuery.data,
    operationsSchedulersQuery.data,
    reviewSettingQuery.data,
    retentionPolicyQuery.data,
    securityPolicyQuery.data,
    sharedFoldersQuery.data,
    usersQuery.data,
  ]);

  const filteredRows = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    return rows.filter((row) => {
      if (row.type !== selectedModule) {
        return false;
      }

      if (selectedStatus !== "ALL" && row.status !== selectedStatus) {
        return false;
      }

      if (!normalizedQuery) {
        return true;
      }

      return [
        row.configuration,
        row.configurationDetail,
        row.description,
        row.updatedBy,
        row.lastUpdatedLabel,
        row.typeLabel,
        ...row.relatedDependencies,
      ].some((value) => value.toLowerCase().includes(normalizedQuery));
    });
  }, [query, rows, selectedModule, selectedStatus]);

  const sortModel = useMemo<GridSortModel>(
    () => [{ field: sortField, sort: sortDirection }],
    [sortDirection, sortField],
  );

  const sortedRows = useMemo(() => sortRows(filteredRows, sortModel), [filteredRows, sortModel]);
  const selectedRow = useMemo(
    () => sortedRows.find((row) => row.id === selectedId) ?? null,
    [selectedId, sortedRows],
  );
  const pagedMobileRows = useMemo(
    () => sortedRows.slice(page * pageSize, page * pageSize + pageSize),
    [page, pageSize, sortedRows],
  );
  const selectedModuleOption = moduleOptions.find((option) => option.key === selectedModule) ?? moduleOptions[0];

  useEffect(() => {
    if (selectedId && !sortedRows.some((row) => row.id === selectedId)) {
      updateSearchParams(setSearchParams, searchParams, { selected: null });
    }
  }, [searchParams, selectedId, setSearchParams, sortedRows]);

  useEffect(() => {
    const maxPage = Math.max(0, Math.ceil(sortedRows.length / pageSize) - 1);
    if (page > maxPage) {
      updateSearchParams(setSearchParams, searchParams, { page: String(maxPage) });
    }
  }, [page, pageSize, searchParams, setSearchParams, sortedRows.length]);

  const hasActiveFilters = Boolean(query || selectedStatus !== "ALL");

  const activeFilters = [
    ...(query
      ? [
          {
            key: "q",
            label: `Search: ${query}`,
            onDelete: () => {
              setLocalQuery("");
              updateSearchParams(setSearchParams, searchParams, { q: null, page: "0", selected: null });
            },
          },
        ]
      : []),
    ...(selectedStatus !== "ALL"
      ? [
          {
            key: "status",
            label: `Status: ${statusOptions.find((option) => option.value === selectedStatus)?.label ?? selectedStatus}`,
            onDelete: () => updateSearchParams(setSearchParams, searchParams, { status: null, page: "0", selected: null }),
          },
        ]
      : []),
  ];

  const chrome = useMemo<ShellWorkspaceChrome>(() => ({
    title: "Administration",
    subtitle: "Manage operational configuration entities, open the selected record, and keep edits within the shared workspace model.",
    primaryActions: selectedModuleOption.creatable ? (
      <Button
        variant="contained"
        startIcon={<AddOutlinedIcon fontSize="small" />}
        onClick={() => openEditor({ module: selectedModule, mode: "create" })}
      >
        New {singularize(selectedModuleOption.label)}
      </Button>
    ) : undefined,
    contextTitle: selectedRow ? "Selected Configuration" : "Administration Context",
    contextWidth: 328,
    contextSections: buildContextSections({
      selectedRow,
      selectedModuleOption,
      totalVisible: sortedRows.length,
      hasActiveFilters,
      onOpenSelected:
        selectedRow
          ? () => openEditor({ module: selectedRow.type, rowId: selectedRow.id, mode: isEditableModule(selectedRow.type) ? "edit" : "view" })
          : undefined,
      onCreateSelectedModule:
        selectedModuleOption.creatable
          ? () => openEditor({ module: selectedModule, mode: "create" })
          : undefined,
    }),
  }), [hasActiveFilters, selectedModule, selectedModuleOption, selectedRow, sortedRows.length]);

  useEffect(() => {
    setWorkspaceChrome(chrome);
    return () => clearWorkspaceChrome();
  }, [chrome, clearWorkspaceChrome, setWorkspaceChrome]);

  function openEditor(next: AdminEditorState) {
    setEditorState(next);
    setHasUnsavedChanges(false);
  }

  function requestCloseEditor() {
    if (hasUnsavedChanges) {
      setDiscardDialogOpen(true);
      return;
    }
    setEditorState(null);
  }

  function discardEditorChanges() {
    setDiscardDialogOpen(false);
    setHasUnsavedChanges(false);
    setEditorState(null);
  }

  function applySearch() {
    updateSearchParams(setSearchParams, searchParams, {
      q: localQuery.trim() || null,
      page: "0",
      selected: null,
    });
  }

  function clearFilters() {
    setLocalQuery("");
    updateSearchParams(setSearchParams, searchParams, {
      q: null,
      status: null,
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

  function handlePaginationModelChange(model: GridPaginationModel) {
    updateSearchParams(setSearchParams, searchParams, {
      page: String(model.page),
      pageSize: String(model.pageSize),
    });
  }

  function handleSortModelChange(model: GridSortModel) {
    const next = model[0];
    updateSearchParams(setSearchParams, searchParams, {
      sort: next?.field ?? "configuration",
      dir: next?.sort ?? "asc",
    });
  }

  function handleSelectModule(module: AdminModuleKey) {
    updateSearchParams(setSearchParams, searchParams, {
      type: module,
      page: "0",
      selected: null,
    });
  }

  function handleSelectRow(rowId: string) {
    updateSearchParams(setSearchParams, searchParams, { selected: rowId });
  }

  async function handleSaveEditor(values: EditorFormValues) {
    switch (values.module) {
      case "document-types":
        await createDocumentTypeMutation.mutateAsync({
          name: values.name.trim(),
          description: values.description.trim() || undefined,
          active: values.active,
        });
        break;
      case "metadata-fields":
        await createMetadataFieldMutation.mutateAsync({
          fieldKey: values.fieldKey.trim(),
          label: values.label.trim(),
          pii: values.pii,
          active: values.active,
        });
        break;
      case "shared-folders":
        await createSharedFolderMutation.mutateAsync({
          path: values.path.trim(),
          active: values.active,
        });
        break;
      case "mailboxes":
        await createMailboxMutation.mutateAsync({
          name: values.name.trim(),
          host: values.host.trim(),
          username: values.username.trim(),
          active: values.active,
        });
        break;
      case "review-settings":
        await updateReviewMutation.mutateAsync({
          mode: values.mode.trim(),
          lowConfidenceThreshold: Number(values.lowConfidenceThreshold),
        });
        break;
      case "ai-settings":
        await updateAiMutation.mutateAsync({
          providerName: values.providerName.trim(),
          modelName: values.modelName.trim(),
          embeddingModelName: values.embeddingModelName.trim(),
          apiBaseUrl: values.apiBaseUrl.trim(),
          apiKey: values.apiKey,
          ocrProvider: values.ocrProvider.trim(),
          active: values.active,
        });
        break;
      case "ai-governance":
        await updateAiGovernanceMutation.mutateAsync({
          approvedModels: values.approvedModels.split(",").map((value) => value.trim()).filter(Boolean),
          promptPolicyVersion: values.promptPolicyVersion.trim(),
          responsePolicyVersion: values.responsePolicyVersion.trim(),
          citationRequired: values.citationRequired,
          groundingValidationRequired: values.groundingValidationRequired,
        });
        break;
      case "security-policies":
        await updateSecurityPolicyMutation.mutateAsync({
          encryptionAtRest: values.encryptionAtRest.trim(),
          encryptionInTransit: values.encryptionInTransit.trim(),
          keyManagement: values.keyManagement.trim(),
          secretManagement: values.secretManagement.trim(),
          exportApprovalRequired: values.exportApprovalRequired,
          watermarkByDefault: values.watermarkByDefault,
        });
        break;
      default:
        return;
    }

    setHasUnsavedChanges(false);
    setEditorState(null);
  }

  async function handleValidateAi(values: EditorFormValues) {
    if (values.module !== "ai-settings") {
      return;
    }

    await validateAiMutation.mutateAsync({
      providerName: values.providerName.trim(),
      modelName: values.modelName.trim(),
      embeddingModelName: values.embeddingModelName.trim(),
      apiBaseUrl: values.apiBaseUrl.trim(),
      apiKey: values.apiKey,
      ocrProvider: values.ocrProvider.trim(),
      active: values.active,
    });
  }

  async function handleOperationsAction(row: AdminGridRow, action: "retry" | "cancel" | "pause" | "resume" | "run" | "enable" | "disable" | "clear" | "invalidate" | "refresh") {
    switch (action) {
      case "retry":
        if ("jobId" in row.raw) {
          await retryOperationsJobMutation.mutateAsync(row.raw.jobId);
        }
        break;
      case "cancel":
        if ("jobId" in row.raw) {
          await cancelOperationsJobMutation.mutateAsync(row.raw.jobId);
        }
        break;
      case "pause":
        if ("queueKey" in row.raw && "queueName" in row.raw) {
          await pauseQueueMutation.mutateAsync(row.raw.queueKey);
        }
        break;
      case "resume":
        if ("queueKey" in row.raw && "queueName" in row.raw) {
          await resumeQueueMutation.mutateAsync(row.raw.queueKey);
        }
        break;
      case "run":
        if ("schedulerKey" in row.raw) {
          await runSchedulerMutation.mutateAsync(row.raw.schedulerKey);
        }
        break;
      case "enable":
        if ("schedulerKey" in row.raw) {
          await enableSchedulerMutation.mutateAsync(row.raw.schedulerKey);
        }
        break;
      case "disable":
        if ("schedulerKey" in row.raw) {
          await disableSchedulerMutation.mutateAsync(row.raw.schedulerKey);
        }
        break;
      case "clear":
        if ("cacheKey" in row.raw) {
          await clearCacheMutation.mutateAsync(row.raw.cacheKey);
        }
        break;
      case "invalidate":
        if ("cacheKey" in row.raw) {
          await invalidateCacheMutation.mutateAsync(row.raw.cacheKey);
        }
        break;
      case "refresh":
        if ("cacheKey" in row.raw) {
          await refreshCacheMutation.mutateAsync(row.raw.cacheKey);
        }
        break;
    }
  }

  const columns = useMemo<GridColDef<AdminGridRow>[]>(() => {
    const nextColumns: GridColDef<AdminGridRow>[] = [
      {
        field: "configuration",
        headerName: "Configuration",
        minWidth: 280,
        flex: 1.35,
        renderCell: ({ row }) => (
          <Stack spacing={0.25} sx={{ py: 0.45, minWidth: 0 }}>
            <Typography variant="body2" fontWeight={600} noWrap title={row.configuration}>
              {row.configuration}
            </Typography>
            <Typography variant="caption" color="text.secondary" noWrap title={row.configurationDetail}>
              {row.configurationDetail}
            </Typography>
          </Stack>
        ),
      },
      {
        field: "type",
        headerName: "Type",
        minWidth: 180,
        width: 184,
        valueGetter: (_value, row) => row.typeLabel,
      },
      {
        field: "status",
        headerName: "Status",
        minWidth: 132,
        width: 140,
        valueGetter: (_value, row) => row.statusLabel,
        renderCell: ({ row }) => <StatusBadge label={row.statusLabel} tone={row.statusTone} />,
      },
      {
        field: "lastUpdated",
        headerName: "Last Updated",
        minWidth: 168,
        width: 176,
        valueGetter: (_value, row) => row.lastUpdatedLabel,
      },
      {
        field: "updatedBy",
        headerName: "Updated By",
        minWidth: 164,
        width: 176,
      },
      {
        field: "actions",
        headerName: "Actions",
        sortable: false,
        filterable: false,
        width: 96,
        renderCell: ({ row }) => (
          <Tooltip title="Open configuration">
            <Button
              size="small"
              color="inherit"
              variant="text"
              aria-label={`Open configuration ${row.configuration}`}
              onClick={(event) => {
                event.stopPropagation();
                openEditor({
                  module: row.type,
                  rowId: row.id,
                  mode: isEditableModule(row.type) ? "edit" : "view",
                });
              }}
            >
              Open
            </Button>
          </Tooltip>
        ),
      },
    ];

    if (isTabletDown) {
      return nextColumns.filter((column) => !["updatedBy"].includes(String(column.field)));
    }

    return nextColumns;
  }, [isTabletDown]);

  if (
    usersQuery.isLoading ||
    documentTypesQuery.isLoading ||
    metadataFieldsQuery.isLoading ||
    sharedFoldersQuery.isLoading ||
    mailboxesQuery.isLoading ||
    reviewSettingQuery.isLoading ||
    aiSettingQuery.isLoading ||
    classificationPolicyQuery.isLoading ||
    retentionPolicyQuery.isLoading ||
    legalHoldsQuery.isLoading ||
    aiGovernanceQuery.isLoading ||
    securityPolicyQuery.isLoading ||
    complianceReportQuery.isLoading ||
    operationsJobsQuery.isLoading ||
    operationsQueuesQuery.isLoading ||
    operationsSchedulersQuery.isLoading ||
    operationsCachesQuery.isLoading ||
    operationsHealthQuery.isLoading ||
    operationsDiagnosticsQuery.isLoading
  ) {
    return (
      <LoadingState
        title="Loading administration workspace"
        message="Retrieving configuration entities, operational settings, and editor context."
      />
    );
  }

  if (
    usersQuery.isError ||
    documentTypesQuery.isError ||
    metadataFieldsQuery.isError ||
    sharedFoldersQuery.isError ||
    mailboxesQuery.isError ||
    reviewSettingQuery.isError ||
    aiSettingQuery.isError ||
    classificationPolicyQuery.isError ||
    retentionPolicyQuery.isError ||
    legalHoldsQuery.isError ||
    aiGovernanceQuery.isError ||
    securityPolicyQuery.isError ||
    complianceReportQuery.isError ||
    operationsJobsQuery.isError ||
    operationsQueuesQuery.isError ||
    operationsSchedulersQuery.isError ||
    operationsCachesQuery.isError ||
    operationsHealthQuery.isError ||
    operationsDiagnosticsQuery.isError
  ) {
    return (
      <ErrorState
        title="Unable to load administration workspace"
        message="The operational configuration data could not be retrieved."
        action={(
          <RetryAction
            onClick={() => {
              void usersQuery.refetch();
              void documentTypesQuery.refetch();
              void metadataFieldsQuery.refetch();
              void sharedFoldersQuery.refetch();
              void mailboxesQuery.refetch();
              void reviewSettingQuery.refetch();
              void aiSettingQuery.refetch();
              void classificationPolicyQuery.refetch();
              void retentionPolicyQuery.refetch();
              void legalHoldsQuery.refetch();
              void aiGovernanceQuery.refetch();
              void securityPolicyQuery.refetch();
              void complianceReportQuery.refetch();
              void operationsJobsQuery.refetch();
              void operationsQueuesQuery.refetch();
              void operationsSchedulersQuery.refetch();
              void operationsCachesQuery.refetch();
              void operationsHealthQuery.refetch();
              void operationsDiagnosticsQuery.refetch();
            }}
          />
        )}
      />
    );
  }

  return (
    <Stack spacing={1.25}>
      <WorkspaceToolbar
        searchPlaceholder={`Search ${selectedModuleOption.label.toLowerCase()}`}
        searchValue={localQuery}
        onSearchChange={setLocalQuery}
        onSearchKeyDown={handleSearchKeyDown}
        searchAriaLabel="Administration search"
        filters={(
          <Stack
            direction={{ xs: "column", xl: "row" }}
            spacing={1}
            alignItems={{ xs: "stretch", xl: "center" }}
            sx={{ width: { xs: "100%", xl: "auto" }, minWidth: 0 }}
          >
            <FormControl size="small" sx={{ minWidth: { xs: "100%", sm: 220 } }}>
              <InputLabel id="administration-type-label">Configuration Type</InputLabel>
              <Select
                labelId="administration-type-label"
                label="Configuration Type"
                value={selectedModule}
                onChange={(event) => handleSelectModule(event.target.value as AdminModuleKey)}
              >
                {moduleOptions.map((option) => (
                  <MenuItem key={option.key} value={option.key}>
                    {option.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: { xs: "100%", sm: 170 } }}>
              <InputLabel id="administration-status-label">Status</InputLabel>
              <Select
                labelId="administration-status-label"
                label="Status"
                value={selectedStatus}
                onChange={(event) =>
                  updateSearchParams(setSearchParams, searchParams, {
                    status: event.target.value === "ALL" ? null : String(event.target.value),
                    page: "0",
                    selected: null,
                  })
                }
              >
                {statusOptions.map((option) => (
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
        )}
        activeFilters={activeFilters}
        primaryAction={(
          <Button variant="contained" onClick={applySearch}>
            Search
          </Button>
        )}
        onRefresh={() => {
          void usersQuery.refetch();
          void documentTypesQuery.refetch();
          void metadataFieldsQuery.refetch();
          void sharedFoldersQuery.refetch();
          void mailboxesQuery.refetch();
          void reviewSettingQuery.refetch();
          void aiSettingQuery.refetch();
          void operationsJobsQuery.refetch();
          void operationsQueuesQuery.refetch();
          void operationsSchedulersQuery.refetch();
          void operationsCachesQuery.refetch();
          void operationsHealthQuery.refetch();
          void operationsDiagnosticsQuery.refetch();
        }}
        secondaryActions={[
          ...(selectedRow
            ? [
                {
                  key: "open-selected",
                  label: "Open selected configuration",
                  onClick: () =>
                    openEditor({
                      module: selectedRow.type,
                      rowId: selectedRow.id,
                      mode: isEditableModule(selectedRow.type) ? "edit" : "view",
                    }),
                },
              ]
            : []),
          ...(selectedModuleOption.creatable
            ? [
                {
                  key: "new-configuration",
                  label: `New ${singularize(selectedModuleOption.label)}`,
                  onClick: () => openEditor({ module: selectedModule, mode: "create" }),
                },
              ]
            : []),
          ...(hasActiveFilters ? [{ key: "clear-filters", label: "Clear filters", onClick: clearFilters }] : []),
        ]}
      />

      <Stack
        direction={{ xs: "column", md: "row" }}
        spacing={1.25}
        alignItems="stretch"
        sx={{ minHeight: 0 }}
      >
        <Box
          sx={{
            display: { xs: "none", md: "block" },
            width: { md: 248, xl: 272 },
            flexShrink: 0,
            border: (appTheme) => `1px solid ${appTheme.palette.divider}`,
            borderRadius: 1,
            backgroundColor: "background.paper",
            p: 1,
          }}
        >
          <Typography variant="subtitle2" sx={{ px: 1, py: 0.75 }}>
            Configuration Explorer
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ px: 1, pb: 1 }}>
            Switch modules without leaving the shared administration workspace.
          </Typography>
          <Divider sx={{ mb: 1 }} />
          <SimpleTreeView
            selectedItems={selectedModule}
            sx={{
              "--TreeView-itemChildrenIndentation": "12px",
              "& .MuiTreeItem-content": {
                minHeight: 30,
                borderRadius: 1,
                px: 0.75,
                py: 0.25,
              },
              "& .MuiTreeItem-content.Mui-selected, & .MuiTreeItem-content.Mui-selected:hover": {
                backgroundColor: "action.selected",
                color: "text.primary",
              },
              "& .MuiTreeItem-content:hover": {
                backgroundColor: "action.hover",
              },
              "& .MuiTreeItem-label": {
                py: 0.125,
              },
              "& .MuiTreeItem-iconContainer": {
                width: 18,
                color: "text.secondary",
              },
              "& .MuiTreeItem-groupTransition": {
                ml: 0.5,
              },
            }}
            onSelectedItemsChange={(_event: SyntheticEvent | null, itemIds: string | null) => {
              const next = Array.isArray(itemIds) ? itemIds[0] : itemIds;
              if (next) {
                handleSelectModule(next as AdminModuleKey);
              }
            }}
          >
            <TreeItem itemId="access" label={explorerLabel(<AdminPanelSettingsOutlinedIcon fontSize="small" />, "Access")}>
              <TreeItem itemId="users" label={explorerLabel(<PersonOutlineOutlinedIcon fontSize="small" />, "Users")} />
            </TreeItem>
            <TreeItem itemId="configuration" label={explorerLabel(<DescriptionOutlinedIcon fontSize="small" />, "Configuration")}>
              <TreeItem itemId="document-types" label={explorerLabel(<DescriptionOutlinedIcon fontSize="small" />, "Document Types")} />
              <TreeItem itemId="metadata-fields" label={explorerLabel(<ViewListOutlinedIcon fontSize="small" />, "Metadata Templates")} />
            </TreeItem>
            <TreeItem itemId="operations" label={explorerLabel(<FolderOutlinedIcon fontSize="small" />, "Operations")}>
              <TreeItem itemId="shared-folders" label={explorerLabel(<FolderOutlinedIcon fontSize="small" />, "System Settings")} />
              <TreeItem itemId="mailboxes" label={explorerLabel(<MailOutlineOutlinedIcon fontSize="small" />, "Notification Rules")} />
              <TreeItem itemId="background-jobs" label={explorerLabel(<ViewListOutlinedIcon fontSize="small" />, "Background Jobs")} />
              <TreeItem itemId="queues" label={explorerLabel(<FolderOutlinedIcon fontSize="small" />, "Queues")} />
              <TreeItem itemId="scheduler" label={explorerLabel(<AdminPanelSettingsOutlinedIcon fontSize="small" />, "Scheduler")} />
              <TreeItem itemId="embeddings" label={explorerLabel(<AutoAwesomeOutlinedIcon fontSize="small" />, "Embeddings")} />
              <TreeItem itemId="ocr" label={explorerLabel(<DescriptionOutlinedIcon fontSize="small" />, "OCR")} />
              <TreeItem itemId="ai-operations" label={explorerLabel(<AutoAwesomeOutlinedIcon fontSize="small" />, "AI Operations")} />
              <TreeItem itemId="cache" label={explorerLabel(<ContentCopyOutlinedIcon fontSize="small" />, "Cache")} />
              <TreeItem itemId="health" label={explorerLabel(<AdminPanelSettingsOutlinedIcon fontSize="small" />, "Health")} />
              <TreeItem itemId="diagnostics" label={explorerLabel(<DescriptionOutlinedIcon fontSize="small" />, "Diagnostics")} />
            </TreeItem>
            <TreeItem itemId="governance" label={explorerLabel(<AdminPanelSettingsOutlinedIcon fontSize="small" />, "Governance")}>
              <TreeItem itemId="review-settings" label={explorerLabel(<AdminPanelSettingsOutlinedIcon fontSize="small" />, "Permission Groups")} />
              <TreeItem itemId="ai-settings" label={explorerLabel(<AutoAwesomeOutlinedIcon fontSize="small" />, "AI Configuration")} />
              <TreeItem itemId="classification-policies" label={explorerLabel(<DescriptionOutlinedIcon fontSize="small" />, "Classification Policies")} />
              <TreeItem itemId="retention-policies" label={explorerLabel(<FolderOutlinedIcon fontSize="small" />, "Retention Policies")} />
              <TreeItem itemId="legal-holds" label={explorerLabel(<AdminPanelSettingsOutlinedIcon fontSize="small" />, "Legal Hold")} />
              <TreeItem itemId="ai-governance" label={explorerLabel(<AutoAwesomeOutlinedIcon fontSize="small" />, "AI Governance")} />
              <TreeItem itemId="security-policies" label={explorerLabel(<AdminPanelSettingsOutlinedIcon fontSize="small" />, "Security Policies")} />
            </TreeItem>
          </SimpleTreeView>
        </Box>

        <Box
          sx={{
            flex: 1,
            minWidth: 0,
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
                  title="No configuration records match these filters"
                  message="Broaden the search or remove one of the active filters."
                  action={<RetryAction label="Clear filters" onClick={clearFilters} />}
                  compact
                />
              ) : (
                <EmptyState
                  title={`No ${selectedModuleOption.label.toLowerCase()} available`}
                  message={selectedModuleOption.description}
                  action={
                    selectedModuleOption.creatable ? (
                      <RetryAction label={`Create ${singularize(selectedModuleOption.label)}`} onClick={() => openEditor({ module: selectedModule, mode: "create" })} />
                    ) : undefined
                  }
                  compact
                />
              )
            ) : isMobile ? (
              <MobileAdministrationList
                rows={pagedMobileRows}
                selectedId={selectedId}
                onSelect={(row) => {
                  handleSelectRow(row.id);
                  openEditor({
                    module: row.type,
                    rowId: row.id,
                    mode: isEditableModule(row.type) ? "edit" : "view",
                  });
                }}
              />
            ) : (
              <EntityGrid<AdminGridRow>
                rows={sortedRows}
                columns={columns}
                getRowId={(row) => row.id}
                pagination
                paginationMode="client"
                paginationModel={{ page, pageSize }}
                onPaginationModelChange={handlePaginationModelChange}
                sortModel={sortModel}
                onSortModelChange={handleSortModelChange}
                onRowClick={({ row }) => handleSelectRow(row.id)}
                onRowDoubleClick={({ row }) =>
                  openEditor({
                    module: row.type,
                    rowId: row.id,
                    mode: isEditableModule(row.type) ? "edit" : "view",
                  })
                }
                onCellKeyDown={(params, event) => {
                  if (event.key === "Enter") {
                    openEditor({
                      module: params.row.type,
                      rowId: params.row.id,
                      mode: isEditableModule(params.row.type) ? "edit" : "view",
                    });
                  }
                }}
                getRowClassName={({ row }) => (row.id === selectedId ? "ikms-selected-row" : "")}
                emptyTitle="No configuration records"
                emptyMessage="There are no operational configuration records in the current module."
                sx={{
                  minHeight: Math.min(Math.max(320, 108 + Math.max(3, sortedRows.length) * 44), 720),
                  height: Math.min(Math.max(320, 108 + Math.max(3, sortedRows.length) * 44), 720),
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
      </Stack>

      <AdministrationEditorDrawer
        open={Boolean(editorState)}
        state={editorState}
        row={editorState?.rowId ? rows.find((row) => row.id === editorState.rowId) ?? null : null}
        validationResult={validateAiMutation.data ?? null}
        validationPending={validateAiMutation.isPending}
        saving={
          createDocumentTypeMutation.isPending ||
          createMetadataFieldMutation.isPending ||
          createSharedFolderMutation.isPending ||
          createMailboxMutation.isPending ||
          updateReviewMutation.isPending ||
          updateAiMutation.isPending
        }
        onClose={requestCloseEditor}
        onDirtyChange={setHasUnsavedChanges}
        onSave={handleSaveEditor}
        onValidateAi={handleValidateAi}
        onOperationsAction={handleOperationsAction}
      />

      <Dialog open={discardDialogOpen} onClose={() => setDiscardDialogOpen(false)}>
        <DialogTitle>Discard unsaved changes?</DialogTitle>
        <DialogContent dividers>
          <Typography variant="body2" color="text.secondary">
            The configuration editor has unsaved changes. Close it only if you want to lose the current edits.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDiscardDialogOpen(false)}>Keep editing</Button>
          <Button color="error" onClick={discardEditorChanges}>
            Discard changes
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

interface EditorFormValues {
  module: AdminModuleKey;
  name: string;
  description: string;
  fieldKey: string;
  label: string;
  pii: boolean;
  path: string;
  host: string;
  username: string;
  mode: string;
  lowConfidenceThreshold: string;
  providerName: string;
  modelName: string;
  embeddingModelName: string;
  apiBaseUrl: string;
  apiKey: string;
  ocrProvider: string;
  active: boolean;
  approvedModels: string;
  promptPolicyVersion: string;
  responsePolicyVersion: string;
  citationRequired: boolean;
  groundingValidationRequired: boolean;
  encryptionAtRest: string;
  encryptionInTransit: string;
  keyManagement: string;
  secretManagement: string;
  exportApprovalRequired: boolean;
  watermarkByDefault: boolean;
}

function AdministrationEditorDrawer({
  open,
  state,
  row,
  validationResult,
  validationPending,
  saving,
  onClose,
  onDirtyChange,
  onSave,
  onValidateAi,
  onOperationsAction,
}: {
  open: boolean;
  state: AdminEditorState | null;
  row: AdminGridRow | null;
  validationResult: AiProviderValidationResult | null;
  validationPending: boolean;
  saving: boolean;
  onClose: () => void;
  onDirtyChange: (dirty: boolean) => void;
  onSave: (values: EditorFormValues) => Promise<void>;
  onValidateAi: (values: EditorFormValues) => Promise<void>;
  onOperationsAction: (row: AdminGridRow, action: "retry" | "cancel" | "pause" | "resume" | "run" | "enable" | "disable" | "clear" | "invalidate" | "refresh") => Promise<void>;
}) {
  const isMobile = useMediaQuery(useTheme().breakpoints.down("md"));
  const [values, setValues] = useState<EditorFormValues>(defaultEditorValues("users"));
  const [dirty, setDirty] = useState(false);

  useEffect(() => {
    if (!state) {
      return;
    }
    setValues(buildEditorValues(state, row));
    setDirty(false);
    onDirtyChange(false);
  }, [onDirtyChange, row, state]);

  function updateValues(next: Partial<EditorFormValues>) {
    setValues((current) => {
      const updated = { ...current, ...next };
      if (!dirty) {
        setDirty(true);
        onDirtyChange(true);
      }
      return updated;
    });
  }

  if (!state) {
    return null;
  }

  const title = state.mode === "create"
    ? `New ${singularize(moduleOptions.find((option) => option.key === state.module)?.label ?? "Configuration")}`
    : state.mode === "edit"
      ? `Edit ${row?.configuration ?? "Configuration"}`
      : row?.configuration ?? "Configuration";

  const canSave = state.mode !== "view" && isSaveSupported(state.module);
  const operationsActions = row ? buildOperationsActions(row) : [];

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      PaperProps={{
        sx: {
          width: { xs: "100%", md: 520 },
          maxWidth: "100%",
          display: "grid",
          gridTemplateRows: "auto minmax(0, 1fr) auto",
        },
      }}
    >
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ px: 2, py: 1.25 }}>
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="subtitle2" noWrap>
            {title}
          </Typography>
          <Typography variant="body2" color="text.secondary" noWrap>
            {moduleOptions.find((option) => option.key === state.module)?.description}
          </Typography>
        </Box>
        <Button color="inherit" onClick={onClose} startIcon={<CloseOutlinedIcon fontSize="small" />}>
          Close
        </Button>
      </Stack>

      <Box sx={{ overflowY: "auto", px: 2, pb: 2 }}>
        <Stack spacing={2}>
          {state.mode === "view" && row ? (
            <ReadOnlyConfigurationView row={row} />
          ) : (
            <EditableConfigurationView
              values={values}
              module={state.module}
              onChange={updateValues}
              validationResult={validationResult}
              validationPending={validationPending}
              onValidateAi={() => void onValidateAi(values)}
            />
          )}
        </Stack>
      </Box>

      <Stack
        direction="row"
        spacing={1}
        justifyContent="space-between"
        alignItems="center"
        sx={{ px: 2, py: 1.25, borderTop: (theme) => `1px solid ${theme.palette.divider}` }}
      >
        <Typography variant="caption" color="text.secondary">
          {dirty ? "Unsaved changes" : isMobile ? "Ready" : "No pending changes"}
        </Typography>
        <Stack direction="row" spacing={1}>
          {state.mode === "view" && row
            ? operationsActions.map((action) => (
                <Button
                  key={action.key}
                  variant="text"
                  color="inherit"
                  onClick={() => void onOperationsAction(row, action.key)}
                >
                  {action.label}
                </Button>
              ))
            : null}
          <Button variant="text" color="inherit" onClick={onClose}>
            Cancel
          </Button>
          {canSave ? (
            <Button
              variant="contained"
              startIcon={<SaveOutlinedIcon fontSize="small" />}
              onClick={() => void onSave(values)}
              disabled={saving || !isEditorValid(values)}
            >
              {state.module === "review-settings"
                ? "Save review settings"
                : state.module === "ai-settings"
                  ? "Save AI settings"
                  : state.module === "ai-governance"
                    ? "Save AI governance"
                    : state.module === "security-policies"
                      ? "Save security policy"
                      : "Save"}
            </Button>
          ) : null}
        </Stack>
      </Stack>
    </Drawer>
  );
}

function EditableConfigurationView({
  values,
  module,
  onChange,
  validationResult,
  validationPending,
  onValidateAi,
}: {
  values: EditorFormValues;
  module: AdminModuleKey;
  onChange: (next: Partial<EditorFormValues>) => void;
  validationResult: AiProviderValidationResult | null;
  validationPending: boolean;
  onValidateAi: () => void;
}) {
  switch (module) {
    case "document-types":
      return (
        <Stack spacing={1.5}>
          <FormSection title="General">
            <TextField
              label="Document Type Name"
              value={values.name}
              onChange={(event) => onChange({ name: event.target.value })}
              helperText="Visible label used throughout Search, Review, and intake classification."
              fullWidth
              size="small"
            />
            <TextField
              label="Description"
              value={values.description}
              onChange={(event) => onChange({ description: event.target.value })}
              helperText="Short operational description for administrators and reviewers."
              fullWidth
              size="small"
              multiline
              minRows={3}
            />
            <FormControlLabel
              control={<Switch checked={values.active} onChange={(event) => onChange({ active: event.target.checked })} />}
              label="Active"
            />
          </FormSection>
        </Stack>
      );
    case "metadata-fields":
      return (
        <Stack spacing={1.5}>
          <FormSection title="General">
            <TextField
              label="Field Key"
              value={values.fieldKey}
              onChange={(event) => onChange({ fieldKey: event.target.value })}
              helperText="Stable backend field identifier used during extraction and review."
              fullWidth
              size="small"
            />
            <TextField
              label="Field Label"
              value={values.label}
              onChange={(event) => onChange({ label: event.target.value })}
              helperText="Operational label shown in review and document detail screens."
              fullWidth
              size="small"
            />
            <FormControlLabel
              control={<Switch checked={values.pii} onChange={(event) => onChange({ pii: event.target.checked })} />}
              label="Contains PII"
            />
            <FormControlLabel
              control={<Switch checked={values.active} onChange={(event) => onChange({ active: event.target.checked })} />}
              label="Active"
            />
          </FormSection>
        </Stack>
      );
    case "shared-folders":
      return (
        <Stack spacing={1.5}>
          <FormSection title="General">
            <TextField
              label="Shared Folder Path"
              value={values.path}
              onChange={(event) => onChange({ path: event.target.value })}
              helperText="Network or mounted path used by intake workers."
              fullWidth
              size="small"
            />
            <FormControlLabel
              control={<Switch checked={values.active} onChange={(event) => onChange({ active: event.target.checked })} />}
              label="Active"
            />
          </FormSection>
        </Stack>
      );
    case "mailboxes":
      return (
        <Stack spacing={1.5}>
          <FormSection title="General">
            <TextField
              label="Mailbox Name"
              value={values.name}
              onChange={(event) => onChange({ name: event.target.value })}
              helperText="Operational mailbox label used in intake routing."
              fullWidth
              size="small"
            />
            <TextField
              label="Host"
              value={values.host}
              onChange={(event) => onChange({ host: event.target.value })}
              helperText="IMAP or mail host used to connect the mailbox."
              fullWidth
              size="small"
            />
            <TextField
              label="Username"
              value={values.username}
              onChange={(event) => onChange({ username: event.target.value })}
              helperText="Mailbox login identity used by the intake worker."
              fullWidth
              size="small"
            />
            <FormControlLabel
              control={<Switch checked={values.active} onChange={(event) => onChange({ active: event.target.checked })} />}
              label="Active"
            />
          </FormSection>
        </Stack>
      );
    case "review-settings":
      return (
        <Stack spacing={1.5}>
          <FormSection title="General">
            <TextField
              label="Review Mode"
              value={values.mode}
              onChange={(event) => onChange({ mode: event.target.value })}
              helperText="Current human review operating mode."
              fullWidth
              size="small"
            />
            <TextField
              label="Low Confidence Threshold"
              value={values.lowConfidenceThreshold}
              onChange={(event) => onChange({ lowConfidenceThreshold: event.target.value })}
              helperText="Numeric threshold used to determine whether a document enters manual review."
              fullWidth
              size="small"
            />
          </FormSection>
        </Stack>
      );
    case "ai-settings":
      return (
        <Stack spacing={1.5}>
          <FormSection title="General">
            <TextField
              label="AI Provider"
              value={values.providerName}
              onChange={(event) => onChange({ providerName: event.target.value })}
              helperText="Primary provider used for classification and extraction."
              fullWidth
              size="small"
            />
            <TextField
              label="Chat / Classification Model"
              value={values.modelName}
              onChange={(event) => onChange({ modelName: event.target.value })}
              helperText="Operational model used for classification and assistant workflows."
              fullWidth
              size="small"
            />
            <TextField
              label="Embedding Model"
              value={values.embeddingModelName}
              onChange={(event) => onChange({ embeddingModelName: event.target.value })}
              helperText="Model used for retrieval, similarity, and contextual search features."
              fullWidth
              size="small"
            />
            <TextField
              label="API Base URL"
              value={values.apiBaseUrl}
              onChange={(event) => onChange({ apiBaseUrl: event.target.value })}
              helperText="Provider base URL used by the backend integration."
              fullWidth
              size="small"
            />
            <TextField
              label="API Key"
              type="password"
              value={values.apiKey}
              onChange={(event) => onChange({ apiKey: event.target.value })}
              helperText="Leave blank to preserve the configured key when only validating or editing other fields."
              fullWidth
              size="small"
            />
            <TextField
              label="OCR Provider"
              value={values.ocrProvider}
              onChange={(event) => onChange({ ocrProvider: event.target.value })}
              helperText="OCR engine paired with the provider configuration."
              fullWidth
              size="small"
            />
            <FormControlLabel
              control={<Switch checked={values.active} onChange={(event) => onChange({ active: event.target.checked })} />}
              label="Active"
            />
          </FormSection>

          <FormSection title="Validation">
            <Stack direction={{ xs: "column", sm: "row" }} spacing={1}>
              <Button
                variant="outlined"
                startIcon={<ArrowForwardOutlinedIcon fontSize="small" />}
                onClick={onValidateAi}
                disabled={validationPending || !isEditorValid(values)}
              >
                Validate AI settings
              </Button>
            </Stack>
            {validationResult ? (
              <Alert severity={validationResult.valid ? "success" : "warning"} variant="outlined">
                {`${validationResult.status}: ${validationResult.message}`}
              </Alert>
            ) : null}
          </FormSection>
        </Stack>
      );
    case "ai-governance":
      return (
        <Stack spacing={1.5}>
          <FormSection title="Approved Models">
            <TextField
              label="Approved Models"
              value={values.approvedModels}
              onChange={(event) => onChange({ approvedModels: event.target.value })}
              helperText="Comma-separated `provider:model` entries allowed for provider-generated responses."
              fullWidth
              size="small"
              multiline
              minRows={2}
            />
            <TextField
              label="Prompt Policy Version"
              value={values.promptPolicyVersion}
              onChange={(event) => onChange({ promptPolicyVersion: event.target.value })}
              fullWidth
              size="small"
            />
            <TextField
              label="Response Policy Version"
              value={values.responsePolicyVersion}
              onChange={(event) => onChange({ responsePolicyVersion: event.target.value })}
              fullWidth
              size="small"
            />
            <FormControlLabel
              control={<Switch checked={values.citationRequired} onChange={(event) => onChange({ citationRequired: event.target.checked })} />}
              label="Citation required"
            />
            <FormControlLabel
              control={<Switch checked={values.groundingValidationRequired} onChange={(event) => onChange({ groundingValidationRequired: event.target.checked })} />}
              label="Grounding validation required"
            />
          </FormSection>
        </Stack>
      );
    case "security-policies":
      return (
        <Stack spacing={1.5}>
          <FormSection title="Encryption">
            <TextField label="Encryption At Rest" value={values.encryptionAtRest} onChange={(event) => onChange({ encryptionAtRest: event.target.value })} fullWidth size="small" multiline minRows={2} />
            <TextField label="Encryption In Transit" value={values.encryptionInTransit} onChange={(event) => onChange({ encryptionInTransit: event.target.value })} fullWidth size="small" multiline minRows={2} />
            <TextField label="Key Management" value={values.keyManagement} onChange={(event) => onChange({ keyManagement: event.target.value })} fullWidth size="small" multiline minRows={2} />
            <TextField label="Secret Management" value={values.secretManagement} onChange={(event) => onChange({ secretManagement: event.target.value })} fullWidth size="small" multiline minRows={2} />
            <FormControlLabel
              control={<Switch checked={values.exportApprovalRequired} onChange={(event) => onChange({ exportApprovalRequired: event.target.checked })} />}
              label="Export approval required"
            />
            <FormControlLabel
              control={<Switch checked={values.watermarkByDefault} onChange={(event) => onChange({ watermarkByDefault: event.target.checked })} />}
              label="Watermark by default"
            />
          </FormSection>
        </Stack>
      );
    default:
      return null;
  }
}

function ReadOnlyConfigurationView({ row }: { row: AdminGridRow }) {
  return (
    <Stack spacing={1.5}>
      <FormSection title="General">
        <ReadOnlyField label="Configuration" value={row.configuration} />
        <ReadOnlyField label="Type" value={row.typeLabel} />
        <ReadOnlyField label="Status" value={row.statusLabel} />
        <ReadOnlyField label="Last Updated" value={row.lastUpdatedLabel} />
        <ReadOnlyField label="Updated By" value={row.updatedBy} />
      </FormSection>
      <FormSection title="Description">
        <Typography variant="body2" color="text.secondary">
          {row.description}
        </Typography>
      </FormSection>
      <FormSection title="Related Dependencies">
        <Stack spacing={0.5}>
          {row.relatedDependencies.map((dependency) => (
            <Typography key={dependency} variant="body2" color="text.secondary">
              {dependency}
            </Typography>
          ))}
        </Stack>
      </FormSection>
    </Stack>
  );
}

function MobileAdministrationList({
  rows,
  selectedId,
  onSelect,
}: {
  rows: AdminGridRow[];
  selectedId: string | null;
  onSelect: (row: AdminGridRow) => void;
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
          <ListItemButton alignItems="flex-start" onClick={() => onSelect(row)} sx={{ px: 1.5, py: 1.25 }}>
            <ListItemText
              primary={(
                <Stack spacing={0.75}>
                  <Stack direction="row" spacing={1} justifyContent="space-between" alignItems="flex-start">
                    <Typography variant="body2" fontWeight={600} sx={{ pr: 1 }}>
                      {row.configuration}
                    </Typography>
                    <StatusBadge label={row.statusLabel} tone={row.statusTone} />
                  </Stack>
                  <Typography variant="body2" color="text.secondary">
                    {row.typeLabel}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {row.configurationDetail}
                  </Typography>
                </Stack>
              )}
            />
          </ListItemButton>
        </Box>
      ))}
    </List>
  );
}

function FormSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Stack spacing={1}>
      <Typography variant="subtitle2">{title}</Typography>
      <Stack spacing={1}>{children}</Stack>
    </Stack>
  );
}

function ReadOnlyField({ label, value }: { label: string; value: string }) {
  return (
    <Stack spacing={0.35}>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="body2">{value}</Typography>
    </Stack>
  );
}

function buildContextSections({
  selectedRow,
  selectedModuleOption,
  totalVisible,
  hasActiveFilters,
  onOpenSelected,
  onCreateSelectedModule,
}: {
  selectedRow: AdminGridRow | null;
  selectedModuleOption: ModuleOption;
  totalVisible: number;
  hasActiveFilters: boolean;
  onOpenSelected?: () => void;
  onCreateSelectedModule?: () => void;
}): ContextSection[] {
  if (!selectedRow) {
    return [
      {
        key: "module-summary",
        title: "Configuration Summary",
        content: (
          <Stack spacing={0.75}>
            <Typography variant="body2" color="text.secondary">
              {selectedModuleOption.label} contains {totalVisible} visible {totalVisible === 1 ? "record" : "records"}.
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {hasActiveFilters ? "Filters are active for the current configuration collection." : selectedModuleOption.description}
            </Typography>
          </Stack>
        ),
      },
      {
        key: "quick-actions",
        title: "Quick Actions",
        content: (
          <Stack spacing={0.75}>
            {onCreateSelectedModule ? (
              <Button size="small" variant="outlined" startIcon={<AddOutlinedIcon fontSize="small" />} onClick={onCreateSelectedModule}>
                New {singularize(selectedModuleOption.label)}
              </Button>
            ) : null}
            <Typography variant="body2" color="text.secondary">
              Select a configuration row to inspect details or open the editor.
            </Typography>
          </Stack>
        ),
      },
    ];
  }

  return [
    {
      key: "summary",
      title: "Configuration Summary",
      content: (
        <Stack spacing={0.75}>
          <Typography variant="body2" fontWeight={600}>
            {selectedRow.configuration}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {selectedRow.configurationDetail}
          </Typography>
        </Stack>
      ),
    },
    {
      key: "type",
      title: "Configuration Type",
      content: <Typography variant="body2">{selectedRow.typeLabel}</Typography>,
    },
    {
      key: "status",
      title: "Current Status",
      content: <StatusBadge label={selectedRow.statusLabel} tone={selectedRow.statusTone} />,
    },
    {
      key: "updated-by",
      title: "Updated By",
      content: <Typography variant="body2">{selectedRow.updatedBy}</Typography>,
    },
    {
      key: "last-updated",
      title: "Last Updated",
      content: <Typography variant="body2">{selectedRow.lastUpdatedLabel}</Typography>,
    },
    {
      key: "description",
      title: "Description",
      content: <Typography variant="body2" color="text.secondary">{selectedRow.description}</Typography>,
    },
    {
      key: "dependencies",
      title: "Related Dependencies",
      content: (
        <Stack spacing={0.4}>
          {selectedRow.relatedDependencies.map((dependency) => (
            <Typography key={dependency} variant="body2" color="text.secondary">
              {dependency}
            </Typography>
          ))}
        </Stack>
      ),
    },
    {
      key: "actions",
      title: "Quick Actions",
      content: (
        <Stack spacing={0.75}>
          {onOpenSelected ? (
            <Button size="small" variant="outlined" startIcon={<EditOutlinedIcon fontSize="small" />} onClick={onOpenSelected}>
              {isEditableModule(selectedRow.type) ? "Edit" : "Open"}
            </Button>
          ) : null}
          {selectedRow.type === "document-types" || selectedRow.type === "metadata-fields" || selectedRow.type === "mailboxes" || selectedRow.type === "shared-folders" ? (
            <Button size="small" variant="text" color="inherit" startIcon={<ContentCopyOutlinedIcon fontSize="small" />} disabled>
              Duplicate unsupported
            </Button>
          ) : null}
        </Stack>
      ),
    },
  ];
}

function buildEditorValues(state: AdminEditorState, row: AdminGridRow | null): EditorFormValues {
  const defaults = defaultEditorValues(state.module);

  if (state.mode === "create" || !row) {
    return defaults;
  }

  switch (state.module) {
    case "document-types": {
      const item = row.raw as DocumentTypeConfig;
      return {
        ...defaults,
        name: item.name,
        description: item.description ?? "",
        active: item.active,
      };
    }
    case "metadata-fields": {
      const item = row.raw as MetadataFieldConfig;
      return {
        ...defaults,
        fieldKey: item.fieldKey,
        label: item.label,
        pii: item.pii,
        active: item.active,
      };
    }
    case "shared-folders": {
      const item = row.raw as SharedFolderConfig;
      return {
        ...defaults,
        path: item.path,
        active: item.active,
      };
    }
    case "mailboxes": {
      const item = row.raw as MailboxConfig;
      return {
        ...defaults,
        name: item.name,
        host: item.host,
        username: item.username,
        active: item.active,
      };
    }
    case "review-settings": {
      const item = row.raw as ReviewSettingConfig;
      return {
        ...defaults,
        mode: item.mode,
        lowConfidenceThreshold: String(item.lowConfidenceThreshold),
      };
    }
    case "ai-settings": {
      const item = row.raw as AiProviderSettingConfig;
      return {
        ...defaults,
        providerName: item.providerName,
        modelName: item.modelName,
        embeddingModelName: item.embeddingModelName,
        apiBaseUrl: item.apiBaseUrl ?? "",
        apiKey: "",
        ocrProvider: item.ocrProvider,
        active: item.active,
      };
    }
    case "ai-governance": {
      const item = row.raw as AiGovernancePolicyConfig;
      return {
        ...defaults,
        approvedModels: item.approvedModels.join(", "),
        promptPolicyVersion: item.promptPolicyVersion,
        responsePolicyVersion: item.responsePolicyVersion,
        citationRequired: item.citationRequired,
        groundingValidationRequired: item.groundingValidationRequired,
      };
    }
    case "security-policies": {
      const item = row.raw as SecurityPolicyConfig;
      return {
        ...defaults,
        encryptionAtRest: item.encryptionAtRest,
        encryptionInTransit: item.encryptionInTransit,
        keyManagement: item.keyManagement,
        secretManagement: item.secretManagement,
        exportApprovalRequired: item.exportApprovalRequired,
        watermarkByDefault: item.watermarkByDefault,
      };
    }
    default:
      return defaults;
  }
}

function defaultEditorValues(module: AdminModuleKey): EditorFormValues {
  return {
    module,
    name: "",
    description: "",
    fieldKey: "",
    label: "",
    pii: false,
    path: "",
    host: "",
    username: "",
    mode: "confidence",
    lowConfidenceThreshold: "0.75",
    providerName: "mistral",
    modelName: "mistral-small",
    embeddingModelName: "mistral-embed",
    apiBaseUrl: "",
    apiKey: "",
    ocrProvider: "tesseract",
    active: true,
    approvedModels: "mistral:mistral-small, openai:gpt-5-mini",
    promptPolicyVersion: "prompt-policy-v1",
    responsePolicyVersion: "response-policy-v1",
    citationRequired: true,
    groundingValidationRequired: true,
    encryptionAtRest: "",
    encryptionInTransit: "",
    keyManagement: "",
    secretManagement: "",
    exportApprovalRequired: true,
    watermarkByDefault: true,
  };
}

function isEditorValid(values: EditorFormValues) {
  switch (values.module) {
    case "document-types":
      return values.name.trim().length > 0;
    case "metadata-fields":
      return values.fieldKey.trim().length > 0 && values.label.trim().length > 0;
    case "shared-folders":
      return values.path.trim().length > 0;
    case "mailboxes":
      return values.name.trim().length > 0 && values.host.trim().length > 0 && values.username.trim().length > 0;
    case "review-settings":
      return values.mode.trim().length > 0 && Number.isFinite(Number(values.lowConfidenceThreshold));
    case "ai-settings":
      return (
        values.providerName.trim().length > 0 &&
        values.modelName.trim().length > 0 &&
        values.embeddingModelName.trim().length > 0 &&
        values.apiBaseUrl.trim().length > 0 &&
        values.ocrProvider.trim().length > 0
      );
    case "ai-governance":
      return values.approvedModels.trim().length > 0 && values.promptPolicyVersion.trim().length > 0 && values.responsePolicyVersion.trim().length > 0;
    case "security-policies":
      return values.encryptionAtRest.trim().length > 0
        && values.encryptionInTransit.trim().length > 0
        && values.keyManagement.trim().length > 0
        && values.secretManagement.trim().length > 0;
    default:
      return true;
  }
}

function isSaveSupported(module: AdminModuleKey) {
  return ![
    "users",
    "background-jobs",
    "queues",
    "scheduler",
    "embeddings",
    "ocr",
    "ai-operations",
    "cache",
    "health",
    "diagnostics",
  ].includes(module);
}

function isEditableModule(module: AdminModuleKey) {
  return module === "review-settings" || module === "ai-settings" || module === "ai-governance" || module === "security-policies";
}

function parseModule(value: string | null): AdminModuleKey {
  const next = moduleOptions.find((option) => option.key === value);
  return next?.key ?? "users";
}

function parseStatus(value: string | null): StatusFilter {
  return statusOptions.some((option) => option.value === value) ? (value as StatusFilter) : "ALL";
}

function parseSortField(value: string | null): AdminSortField {
  return value && sortFields.has(value as AdminSortField) ? (value as AdminSortField) : "configuration";
}

function parsePageIndex(value: string | null, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback;
}

function parsePageSize(value: string | null, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
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

function sortRows(rows: AdminGridRow[], sortModel: GridSortModel) {
  const next = sortModel[0];
  if (!next?.field || !next.sort) {
    return rows;
  }

  return [...rows].sort((left, right) => {
    const leftValue = String(getSortValue(left, next.field as AdminSortField));
    const rightValue = String(getSortValue(right, next.field as AdminSortField));
    const comparison = leftValue.localeCompare(rightValue, undefined, { numeric: true, sensitivity: "base" });
    return next.sort === "asc" ? comparison : comparison * -1;
  });
}

function getSortValue(row: AdminGridRow, field: AdminSortField) {
  switch (field) {
    case "configuration":
      return row.configuration;
    case "type":
      return row.typeLabel;
    case "status":
      return row.statusLabel;
    case "lastUpdated":
      return row.lastUpdated;
    case "updatedBy":
      return row.updatedBy;
  }
}

function mapUserRow(user: AdminUser): AdminGridRow {
  return {
    id: user.id,
    configuration: user.displayName,
    configurationDetail: `${user.username}${user.email ? ` · ${user.email}` : ""}`,
    type: "users",
    typeLabel: "Users",
    status: normalizeUserStatus(user.status),
    statusLabel: titleCase(user.status),
    statusTone: mapStatusTone(normalizeUserStatus(user.status)),
    lastUpdated: "",
    lastUpdatedLabel: "Unavailable",
    updatedBy: "Unavailable",
    description: user.roles.length > 0 ? `Assigned roles: ${user.roles.join(", ")}` : "No roles assigned.",
    relatedDependencies: user.roles.map((role) => `Role: ${titleCase(role)}`),
    raw: user,
  };
}

function mapDocumentTypeRow(item: DocumentTypeConfig): AdminGridRow {
  return {
    id: item.id,
    configuration: item.name,
    configurationDetail: item.description ?? "No description",
    type: "document-types",
    typeLabel: "Document Types",
    status: item.active ? "ACTIVE" : "DISABLED",
    statusLabel: item.active ? "Active" : "Disabled",
    statusTone: item.active ? "success" : "neutral",
    lastUpdated: item.createdAt,
    lastUpdatedLabel: formatDateTime(item.createdAt),
    updatedBy: "Unavailable",
    description: item.description ?? "No description available for this document type.",
    relatedDependencies: ["Search", "Review Queue", "Review Detail"],
    raw: item,
  };
}

function mapMetadataFieldRow(item: MetadataFieldConfig): AdminGridRow {
  return {
    id: item.id,
    configuration: item.label,
    configurationDetail: `${item.fieldKey}${item.pii ? " · PII" : " · Standard field"}`,
    type: "metadata-fields",
    typeLabel: "Metadata Templates",
    status: item.active ? "ACTIVE" : "DISABLED",
    statusLabel: item.active ? "Active" : "Disabled",
    statusTone: item.active ? "success" : "neutral",
    lastUpdated: item.createdAt,
    lastUpdatedLabel: formatDateTime(item.createdAt),
    updatedBy: "Unavailable",
    description: item.pii ? "Structured extraction field marked as PII." : "Structured extraction field.",
    relatedDependencies: [item.fieldKey, item.pii ? "PII handling" : "Standard handling"],
    raw: item,
  };
}

function mapSharedFolderRow(item: SharedFolderConfig): AdminGridRow {
  return {
    id: item.id,
    configuration: item.path,
    configurationDetail: "Inbound shared folder",
    type: "shared-folders",
    typeLabel: "System Settings",
    status: item.active ? "ACTIVE" : "DISABLED",
    statusLabel: item.active ? "Active" : "Disabled",
    statusTone: item.active ? "success" : "neutral",
    lastUpdated: item.createdAt,
    lastUpdatedLabel: formatDateTime(item.createdAt),
    updatedBy: "Unavailable",
    description: "Shared intake location monitored by operational ingestion workflows.",
    relatedDependencies: ["Intake", "File polling"],
    raw: item,
  };
}

function mapMailboxRow(item: MailboxConfig): AdminGridRow {
  return {
    id: item.id,
    configuration: item.name,
    configurationDetail: `${item.host} · ${item.username}`,
    type: "mailboxes",
    typeLabel: "Notification Rules",
    status: item.active ? "ACTIVE" : "DISABLED",
    statusLabel: item.active ? "Active" : "Disabled",
    statusTone: item.active ? "success" : "neutral",
    lastUpdated: item.createdAt,
    lastUpdatedLabel: formatDateTime(item.createdAt),
    updatedBy: "Unavailable",
    description: "Mailbox integration used by intake and notification routing.",
    relatedDependencies: [item.host, item.username],
    raw: item,
  };
}

function mapReviewSettingRow(item: ReviewSettingConfig): AdminGridRow {
  return {
    id: item.id,
    configuration: "Review Policy",
    configurationDetail: `${titleCase(item.mode)} mode · ${item.lowConfidenceThreshold} threshold`,
    type: "review-settings",
    typeLabel: "Permission Groups",
    status: "CONFIGURED",
    statusLabel: "Configured",
    statusTone: "info",
    lastUpdated: item.updatedAt,
    lastUpdatedLabel: formatDateTime(item.updatedAt),
    updatedBy: "Unavailable",
    description: "Human review operating mode and threshold used for queue admission.",
    relatedDependencies: ["Review Queue", "Review Detail"],
    raw: item,
  };
}

function mapAiSettingRow(item: AiProviderSettingConfig): AdminGridRow {
  return {
    id: item.id,
    configuration: item.providerName,
    configurationDetail: `${item.modelName} · ${item.embeddingModelName}`,
    type: "ai-settings",
    typeLabel: "AI Configuration",
    status: item.active ? "ACTIVE" : "DISABLED",
    statusLabel: item.active ? "Active" : "Disabled",
    statusTone: item.active ? "success" : "neutral",
    lastUpdated: item.updatedAt,
    lastUpdatedLabel: formatDateTime(item.updatedAt),
    updatedBy: "Unavailable",
    description: `${item.apiKeyConfigured ? "API key configured." : "API key not configured."} OCR provider: ${item.ocrProvider}.`,
    relatedDependencies: [item.apiBaseUrl ?? "No API base URL", item.ocrProvider],
    raw: item,
  };
}

function mapOperationsJobRow(item: OperationsJob, module: AdminModuleKey): AdminGridRow {
  return {
    id: `${module}:${item.jobId}`,
    configuration: readableJobType(item.jobType),
    configurationDetail: `${item.status} · ${item.progress}%`,
    type: module,
    typeLabel: titleForOperationsModule(module),
    status: normalizeOperationsStatus(item.status),
    statusLabel: titleCase(item.status),
    statusTone: mapStatusTone(normalizeOperationsStatus(item.status)),
    lastUpdated: item.submittedAt,
    lastUpdatedLabel: formatDateTime(item.submittedAt),
    updatedBy: item.submittedBy ?? "System",
    description: item.errorSummary ?? `${item.targetType ?? "Operation"} ${item.targetId ?? ""}`.trim(),
    relatedDependencies: [item.queueKey ?? "No queue", `Retry count: ${item.retryCount}`],
    raw: item,
  };
}

function mapOperationsQueueRow(item: OperationsQueue): AdminGridRow {
  return {
    id: item.queueKey,
    configuration: item.queueName,
    configurationDetail: `${item.depth} items · ${item.runningItems} running · ${item.failedItems} failed`,
    type: "queues",
    typeLabel: "Queues",
    status: item.paused ? "DISABLED" : "ACTIVE",
    statusLabel: item.paused ? "Paused" : "Running",
    statusTone: item.paused ? "warning" : "success",
    lastUpdated: item.updatedAt,
    lastUpdatedLabel: formatDateTime(item.updatedAt),
    updatedBy: "Operations",
    description: item.explanation,
    relatedDependencies: [`Depth: ${item.depth}`, `Running: ${item.runningItems}`, `Failed: ${item.failedItems}`],
    raw: item,
  };
}

function mapOperationsSchedulerRow(item: OperationsScheduler): AdminGridRow {
  return {
    id: item.schedulerKey,
    configuration: item.displayName,
    configurationDetail: item.nextExecution ? `Next ${formatDateTime(item.nextExecution)}` : "Next execution unavailable",
    type: "scheduler",
    typeLabel: "Scheduler",
    status: item.enabled ? "ACTIVE" : "DISABLED",
    statusLabel: item.enabled ? "Enabled" : "Disabled",
    statusTone: item.enabled ? "success" : "neutral",
    lastUpdated: item.lastExecution ?? item.nextExecution ?? "",
    lastUpdatedLabel: item.lastExecution ? formatDateTime(item.lastExecution) : "Not yet executed",
    updatedBy: "Operations",
    description: item.description,
    relatedDependencies: item.history.slice(0, 2).map((history) => `${history.status} via ${history.triggerSource}`),
    raw: item,
  };
}

function mapOperationsCacheRow(item: OperationsCache): AdminGridRow {
  return {
    id: item.cacheKey,
    configuration: item.displayName,
    configurationDetail: `${item.entryCount} entries · ${titleCase(item.lastAction)}`,
    type: "cache",
    typeLabel: "Cache",
    status: "CONFIGURED",
    statusLabel: titleCase(item.lastAction),
    statusTone: "info",
    lastUpdated: item.lastActionAt,
    lastUpdatedLabel: formatDateTime(item.lastActionAt),
    updatedBy: "Operations",
    description: "Administrative cache action surface for operational recovery and refresh flows.",
    relatedDependencies: [item.cacheKey, `Entries: ${item.entryCount}`],
    raw: item,
  };
}

function mapOperationsHealthRow(item: OperationsHealthComponent): AdminGridRow {
  return {
    id: item.component,
    configuration: item.component,
    configurationDetail: item.explanation,
    type: "health",
    typeLabel: "Health",
    status: normalizeOperationsStatus(item.status),
    statusLabel: titleCase(item.status),
    statusTone: mapStatusTone(normalizeOperationsStatus(item.status)),
    lastUpdated: "",
    lastUpdatedLabel: "Live status",
    updatedBy: "Health service",
    description: item.explanation,
    relatedDependencies: [item.status],
    raw: item,
  };
}

function mapOperationsDiagnosticsRows(item: OperationsDiagnostics): AdminGridRow[] {
  const rows: AdminGridRow[] = [
    {
      id: "diagnostics-summary",
      configuration: "System Diagnostics",
      configurationDetail: `${item.failedJobs} failed jobs · ${item.bottlenecks.length} bottlenecks`,
      type: "diagnostics",
      typeLabel: "Diagnostics",
      status: item.failedJobs > 0 ? "INACTIVE" : "CONFIGURED",
      statusLabel: item.failedJobs > 0 ? "Attention Needed" : "Ready",
      statusTone: item.failedJobs > 0 ? "warning" : "success",
      lastUpdated: item.metrics[0]?.recordedAt ?? "",
      lastUpdatedLabel: item.metrics[0]?.recordedAt ? formatDateTime(item.metrics[0].recordedAt) : "No metrics yet",
      updatedBy: "Diagnostics",
      description: item.bottlenecks[0] ?? "No active bottlenecks reported.",
      relatedDependencies: [...item.configurationValidation, ...item.dependencyValidation].slice(0, 4),
      raw: item,
    },
  ];

  for (const failure of item.recentFailures) {
    rows.push({
      ...mapOperationsJobRow(failure, "diagnostics"),
      id: `diagnostics:${failure.jobId}`,
      typeLabel: "Diagnostics",
    });
  }
  return rows;
}

function buildOperationsActions(row: AdminGridRow) {
  if ("jobId" in row.raw) {
    return [
      { key: "retry" as const, label: "Retry job" },
      { key: "cancel" as const, label: "Cancel job" },
    ];
  }
  if ("queueKey" in row.raw && "queueName" in row.raw) {
    return row.raw.paused
      ? [{ key: "resume" as const, label: "Resume queue" }]
      : [{ key: "pause" as const, label: "Pause queue" }];
  }
  if ("schedulerKey" in row.raw) {
    return [
      { key: "run" as const, label: "Run now" },
      ...(row.raw.enabled
        ? [{ key: "disable" as const, label: "Disable" }]
        : [{ key: "enable" as const, label: "Enable" }]),
    ];
  }
  if ("cacheKey" in row.raw) {
    return [
      { key: "clear" as const, label: "Clear" },
      { key: "invalidate" as const, label: "Invalidate" },
      { key: "refresh" as const, label: "Refresh" },
    ];
  }
  return [];
}

function normalizeOperationsStatus(value: string): StatusFilter {
  const normalized = value.toUpperCase();
  if (normalized === "HEALTHY" || normalized === "RUNNING" || normalized === "COMPLETED" || normalized === "READY" || normalized === "ENABLED") {
    return "ACTIVE";
  }
  if (normalized === "WARNING" || normalized === "PAUSED" || normalized === "RETRYING") {
    return "INACTIVE";
  }
  if (normalized === "FAILED" || normalized === "CRITICAL" || normalized === "CANCELLED" || normalized === "DISABLED") {
    return "DISABLED";
  }
  return "CONFIGURED";
}

function isEmbeddingJob(item: OperationsJob) {
  return item.jobType.includes("EMBEDDING") || item.jobType.includes("VECTOR");
}

function isOcrJob(item: OperationsJob) {
  return item.jobType.includes("OCR");
}

function isAiOperationsJob(item: OperationsJob) {
  return item.jobType.includes("AI");
}

function titleForOperationsModule(module: AdminModuleKey) {
  switch (module) {
    case "background-jobs":
      return "Background Jobs";
    case "embeddings":
      return "Embeddings";
    case "ocr":
      return "OCR";
    case "ai-operations":
      return "AI Operations";
    case "diagnostics":
      return "Diagnostics";
    default:
      return "Operations";
  }
}

function readableJobType(jobType: string) {
  return titleCase(jobType.replace(/_/g, " "));
}

async function invalidateOperationsQueries(queryClient: ReturnType<typeof useQueryClient>) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: [...adminQueryKey, "operations-jobs"] }),
    queryClient.invalidateQueries({ queryKey: [...adminQueryKey, "operations-queues"] }),
    queryClient.invalidateQueries({ queryKey: [...adminQueryKey, "operations-schedulers"] }),
    queryClient.invalidateQueries({ queryKey: [...adminQueryKey, "operations-caches"] }),
    queryClient.invalidateQueries({ queryKey: [...adminQueryKey, "operations-health"] }),
    queryClient.invalidateQueries({ queryKey: [...adminQueryKey, "operations-diagnostics"] }),
  ]);
}

function mapClassificationPolicyRow(item: ClassificationPolicyConfig, report: ComplianceReport | null): AdminGridRow {
  return {
    id: "classification-policy",
    configuration: "Enterprise Classification Policy",
    configurationDetail: `${item.defaultClassification} default · AI restricted from ${item.aiRestrictionThreshold}`,
    type: "classification-policies",
    typeLabel: "Classification Policies",
    status: "CONFIGURED",
    statusLabel: "Configured",
    statusTone: "info",
    lastUpdated: item.updatedAt,
    lastUpdatedLabel: formatDateTime(item.updatedAt),
    updatedBy: "Unavailable",
    description: `${item.levels.join(", ")}. Restricted documents: ${report?.restrictedDocuments ?? 0}.`,
    relatedDependencies: [item.exportRestrictionThreshold, item.aiRestrictionThreshold],
    raw: item,
  };
}

function mapRetentionPolicyRow(item: RetentionPolicyConfig): AdminGridRow {
  return {
    id: "retention-policy",
    configuration: "Enterprise Retention Policy",
    configurationDetail: `${item.policies.length} retention classes configured`,
    type: "retention-policies",
    typeLabel: "Retention Policies",
    status: "CONFIGURED",
    statusLabel: "Configured",
    statusTone: "info",
    lastUpdated: item.updatedAt,
    lastUpdatedLabel: formatDateTime(item.updatedAt),
    updatedBy: "Unavailable",
    description: "Retention windows for documents, emails, notes, AI conversations, review records, and audit events.",
    relatedDependencies: item.policies.map((policy) => `${policy.contentType}: ${policy.retentionDays} days`),
    raw: item,
  };
}

function mapLegalHoldRow(item: LegalHoldConfig): AdminGridRow {
  return {
    id: item.id,
    configuration: `${titleCase(item.holdType)} Hold`,
    configurationDetail: `${item.targetType} · ${item.targetId}`,
    type: "legal-holds",
    typeLabel: "Legal Hold",
    status: item.legalHold ? "ACTIVE" : "ARCHIVED",
    statusLabel: item.legalHold ? "Active" : "Archived",
    statusTone: item.legalHold ? "warning" : "neutral",
    lastUpdated: item.executedAt ?? item.reviewAt ?? new Date(0).toISOString(),
    lastUpdatedLabel: formatDateTime(item.executedAt ?? item.reviewAt ?? new Date(0).toISOString()),
    updatedBy: "Unavailable",
    description: item.reason ?? "Legal hold applied.",
    relatedDependencies: [item.retentionPolicyKey ?? "legal-hold", item.clientId ?? "No client scope"],
    raw: item,
  };
}

function mapAiGovernanceRow(item: AiGovernancePolicyConfig): AdminGridRow {
  return {
    id: "ai-governance",
    configuration: "Approved Model Registry",
    configurationDetail: `${item.approvedModels.length} approved models · ${item.promptPolicyVersion}`,
    type: "ai-governance",
    typeLabel: "AI Governance",
    status: "CONFIGURED",
    statusLabel: "Configured",
    statusTone: "info",
    lastUpdated: item.updatedAt,
    lastUpdatedLabel: formatDateTime(item.updatedAt),
    updatedBy: "Unavailable",
    description: `${item.citationRequired ? "Citations required." : "Citations optional."} ${item.groundingValidationRequired ? "Grounding validation required." : "Grounding validation optional."}`,
    relatedDependencies: item.approvedModels,
    raw: item,
  };
}

function mapSecurityPolicyRow(item: SecurityPolicyConfig): AdminGridRow {
  return {
    id: "security-policy",
    configuration: "Enterprise Security Policy",
    configurationDetail: `${item.exportApprovalRequired ? "Approval required" : "Approval optional"} · ${item.watermarkByDefault ? "Watermarked" : "No default watermark"}`,
    type: "security-policies",
    typeLabel: "Security Policies",
    status: "CONFIGURED",
    statusLabel: "Configured",
    statusTone: "info",
    lastUpdated: item.updatedAt,
    lastUpdatedLabel: formatDateTime(item.updatedAt),
    updatedBy: "Unavailable",
    description: item.encryptionAtRest,
    relatedDependencies: [item.encryptionInTransit, item.keyManagement],
    raw: item,
  };
}

function formatDateTime(value: string) {
  if (!value) {
    return "Unavailable";
  }
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(value));
}

function singularize(value: string) {
  return value.endsWith("s") ? value.slice(0, -1) : value;
}

function titleCase(value: string) {
  return value
    .toLowerCase()
    .split(/[_\s-]+/)
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(" ");
}

function normalizeUserStatus(value: string): StatusFilter {
  switch (value) {
    case "ACTIVE":
      return "ACTIVE";
    case "ARCHIVED":
      return "ARCHIVED";
    default:
      return "INACTIVE";
  }
}

function mapStatusTone(value: StatusFilter | "ACTIVE"): StatusTone {
  switch (value) {
    case "ACTIVE":
      return "success";
    case "CONFIGURED":
      return "info";
    case "ARCHIVED":
      return "neutral";
    default:
      return "warning";
  }
}
