import DescriptionOutlinedIcon from "@mui/icons-material/DescriptionOutlined";
import WarningAmberOutlinedIcon from "@mui/icons-material/WarningAmberOutlined";
import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  Drawer,
  List,
  ListItemButton,
  ListItemText,
  Paper,
  Stack,
  Typography,
  useMediaQuery,
} from "@mui/material";
import { useTheme } from "@mui/material/styles";
import { ReactNode, useEffect, useMemo, useState } from "react";
import { DocumentViewerToolbar, type DocumentViewerToolbarAction } from "./DocumentViewerToolbar";
import { EvidencePanel } from "./EvidencePanel";
import type { EvidenceWorkspaceSection } from "./evidenceWorkspaceTypes";

export type DocumentViewerFileKind = "pdf" | "tiff" | "image" | "office" | "email" | "unknown";
export type DocumentViewerState = "loading" | "ready" | "empty" | "error" | "unsupported";
export type DocumentViewerLayerKind = "ocr" | "highlight" | "annotation";
export type DocumentViewerLayerStatus = "available" | "placeholder" | "disabled";

export interface DocumentViewerPage {
  id: string;
  label: string;
  pageNumber?: number | null;
  description?: string | null;
}

export interface DocumentViewerLayerContext {
  documentId: string;
  selectedPage: DocumentViewerPage | null;
  zoomPercent: number;
  rotationDegrees: number;
}

export interface DocumentViewerLayer {
  id: string;
  kind: DocumentViewerLayerKind;
  label: string;
  status: DocumentViewerLayerStatus;
  render?: (context: DocumentViewerLayerContext) => ReactNode;
}

export interface EnterpriseDocumentDescriptor {
  id: string;
  title: string;
  subtitle?: string;
  fileKind: DocumentViewerFileKind;
  previewUrl?: string | null;
  downloadUrl?: string | null;
  originalUrl?: string | null;
  originalActionLabel?: string;
  pages?: DocumentViewerPage[];
  isLargeFile?: boolean;
  unsupportedReason?: string;
  emptyMessage?: string;
  errorMessage?: string;
}

export interface EnterpriseDocumentViewerProps {
  document: EnterpriseDocumentDescriptor;
  state?: DocumentViewerState;
  evidenceSections?: EvidenceWorkspaceSection[];
  layers?: DocumentViewerLayer[];
  onBack?: () => void;
  embedded?: boolean;
  overflowActions?: DocumentViewerToolbarAction[];
}

interface PersistedViewerState {
  zoomPercent: number;
  fitMode: "width" | "page";
  rotationDegrees: number;
}

const defaultPersistedState: PersistedViewerState = {
  zoomPercent: 100,
  fitMode: "width",
  rotationDegrees: 0,
};

export function EnterpriseDocumentViewer({
  document,
  state = "ready",
  evidenceSections = [],
  layers = [],
  onBack,
  embedded = false,
  overflowActions = [],
}: EnterpriseDocumentViewerProps) {
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up("lg"));
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));
  const [selectedPageIndex, setSelectedPageIndex] = useState(0);
  const [searchValue, setSearchValue] = useState("");
  const [evidenceOpen, setEvidenceOpen] = useState(false);
  const [viewerState, setViewerState] = useState<PersistedViewerState>(defaultPersistedState);

  const pages = useMemo<DocumentViewerPage[]>(
    () =>
      document.pages && document.pages.length > 0
        ? document.pages
        : [{ id: `${document.id}-page-1`, label: "Page 1", pageNumber: 1 }],
    [document.id, document.pages],
  );

  useEffect(() => {
    const persisted = readViewerState(document.id);
    setViewerState(persisted);
    setSelectedPageIndex(0);
    setSearchValue("");
    setEvidenceOpen(false);
  }, [document.id]);

  useEffect(() => {
    writeViewerState(document.id, viewerState);
  }, [document.id, viewerState]);

  const safeSelectedPageIndex = Math.min(selectedPageIndex, Math.max(pages.length - 1, 0));
  const selectedPage = pages[safeSelectedPageIndex] ?? null;
  const filteredEvidenceSections = filterEvidenceSections(evidenceSections, searchValue);
  const surfaceHeight = embedded ? { xs: 420, md: 560, lg: 660 } : { xs: "60vh", md: "72vh", lg: "76vh" };
  const effectiveState = state === "ready" && !document.previewUrl && document.fileKind !== "email" ? "unsupported" : state;
  const layerContext: DocumentViewerLayerContext = {
    documentId: document.id,
    selectedPage,
    zoomPercent: viewerState.zoomPercent,
    rotationDegrees: viewerState.rotationDegrees,
  };

  const mergedOverflowActions = [
    ...overflowActions,
    {
      key: "reset-view",
      label: "Reset view",
      onClick: () => setViewerState(defaultPersistedState),
    },
  ];

  return (
    <Stack spacing={1}>
      <DocumentViewerToolbar
        title={document.title}
        subtitle={document.subtitle}
        canGoBack={Boolean(onBack)}
        onBack={onBack}
        zoomPercent={viewerState.zoomPercent}
        onZoomIn={() =>
          setViewerState((current) => ({
            ...current,
            zoomPercent: Math.min(current.zoomPercent + 10, 200),
          }))
        }
        onZoomOut={() =>
          setViewerState((current) => ({
            ...current,
            zoomPercent: Math.max(current.zoomPercent - 10, 50),
          }))
        }
        onFitWidth={() => setViewerState((current) => ({ ...current, zoomPercent: 100, fitMode: "width" }))}
        onFitPage={() => setViewerState((current) => ({ ...current, zoomPercent: 90, fitMode: "page" }))}
        onRotate={() =>
          setViewerState((current) => ({
            ...current,
            rotationDegrees: (current.rotationDegrees + 90) % 360,
          }))
        }
        pageIndex={safeSelectedPageIndex}
        pageCount={pages.length}
        onPreviousPage={() => setSelectedPageIndex((current) => Math.max(current - 1, 0))}
        onNextPage={() => setSelectedPageIndex((current) => Math.min(current + 1, pages.length - 1))}
        searchValue={searchValue}
        onSearchChange={setSearchValue}
        onDownload={
          document.downloadUrl
            ? () => window.open(document.downloadUrl!, "_blank", "noopener,noreferrer")
            : undefined
        }
        onOpenOriginal={
          document.originalUrl
            ? () => window.open(document.originalUrl!, "_blank", "noopener,noreferrer")
            : undefined
        }
        originalActionLabel={document.originalActionLabel}
        onToggleEvidence={!isDesktop && evidenceSections.length > 0 ? () => setEvidenceOpen(true) : undefined}
        evidenceToggleLabel={isMobile ? "Evidence" : "Open evidence"}
        overflowActions={mergedOverflowActions}
      />

      {document.isLargeFile ? (
        <Alert severity="warning" variant="outlined" icon={<WarningAmberOutlinedIcon fontSize="inherit" />}>
          Large file mode is active. Rendering stays lightweight to protect the current workspace.
        </Alert>
      ) : null}

      <Box
        sx={{
          display: "grid",
          gap: 1,
          gridTemplateColumns: isDesktop
            ? `${pages.length > 1 ? "88px " : ""}minmax(0, 1fr) ${filteredEvidenceSections.length > 0 ? "320px" : ""}`
            : "1fr",
          alignItems: "stretch",
        }}
      >
        {isDesktop && pages.length > 1 ? (
          <ThumbnailRail
            pages={pages}
            selectedPageIndex={safeSelectedPageIndex}
            onSelectPage={setSelectedPageIndex}
            searchValue={searchValue}
          />
        ) : null}

        <Paper variant="outlined" sx={{ minWidth: 0, overflow: "hidden" }}>
          {renderViewerSurface({
            state: effectiveState,
            document,
            selectedPage,
            zoomPercent: viewerState.zoomPercent,
            rotationDegrees: viewerState.rotationDegrees,
            surfaceHeight,
            layers,
            layerContext,
          })}
        </Paper>

        {isDesktop && filteredEvidenceSections.length > 0 ? (
          <EvidencePanel
            title="Evidence Workspace"
            subtitle={selectedPage?.pageNumber ? `Page ${selectedPage.pageNumber}` : undefined}
            sections={filteredEvidenceSections}
          />
        ) : null}
      </Box>

      {!isDesktop && filteredEvidenceSections.length > 0 ? (
        <Drawer
          anchor={isMobile ? "bottom" : "right"}
          open={evidenceOpen}
          onClose={() => setEvidenceOpen(false)}
          PaperProps={{
            sx: isMobile
              ? {
                  borderTopLeftRadius: 16,
                  borderTopRightRadius: 16,
                  maxHeight: "72vh",
                }
              : {
                  width: 380,
                  maxWidth: "100%",
                },
          }}
        >
          <EvidencePanel
            title="Evidence Workspace"
            subtitle={selectedPage?.pageNumber ? `Page ${selectedPage.pageNumber}` : undefined}
            sections={filteredEvidenceSections}
            mobile
          />
        </Drawer>
      ) : null}
    </Stack>
  );
}

function ThumbnailRail({
  pages,
  selectedPageIndex,
  onSelectPage,
  searchValue,
}: {
  pages: DocumentViewerPage[];
  selectedPageIndex: number;
  onSelectPage: (index: number) => void;
  searchValue: string;
}) {
  const normalizedSearch = searchValue.trim().toLowerCase();

  return (
    <Paper variant="outlined" sx={{ overflow: "hidden" }}>
      <List disablePadding sx={{ display: "grid", gap: 0, p: 0.5, backgroundColor: "background.default" }}>
        {pages.map((page, index) => {
          const matched = normalizedSearch
            ? [page.label, page.description ?? "", String(page.pageNumber ?? "")]
                .join(" ")
                .toLowerCase()
                .includes(normalizedSearch)
            : false;

          return (
            <ListItemButton
              key={page.id}
              selected={index === selectedPageIndex}
              onClick={() => onSelectPage(index)}
              sx={{
                mb: 0.5,
                borderRadius: 1,
                alignItems: "stretch",
                px: 0.75,
                py: 0.75,
              }}
            >
              <ListItemText
                primary={
                  <Stack spacing={0.5}>
                    <Box
                      sx={{
                        height: 72,
                        borderRadius: 1,
                        border: (theme) => `1px solid ${theme.palette.divider}`,
                        background: "linear-gradient(180deg, rgba(162,171,188,0.15) 0%, rgba(162,171,188,0.04) 100%)",
                        display: "grid",
                        placeItems: "center",
                      }}
                    >
                      <DescriptionOutlinedIcon fontSize="small" color="action" />
                    </Box>
                    <Typography variant="caption" fontWeight={600}>
                      {page.pageNumber ? `Page ${page.pageNumber}` : page.label}
                    </Typography>
                    {matched ? <Chip size="small" label="Match" color="warning" sx={{ maxWidth: "100%" }} /> : null}
                  </Stack>
                }
              />
            </ListItemButton>
          );
        })}
      </List>
    </Paper>
  );
}

function renderViewerSurface({
  state,
  document,
  selectedPage,
  zoomPercent,
  rotationDegrees,
  surfaceHeight,
  layers,
  layerContext,
}: {
  state: DocumentViewerState;
  document: EnterpriseDocumentDescriptor;
  selectedPage: DocumentViewerPage | null;
  zoomPercent: number;
  rotationDegrees: number;
  surfaceHeight: { xs: number | string; md: number | string; lg: number | string };
  layers: DocumentViewerLayer[];
  layerContext: DocumentViewerLayerContext;
}) {
  if (state === "loading") {
    return (
      <ViewerPlaceholder
        icon={<CircularProgress size={24} />}
        title="Loading document"
        message="Preparing the current document workspace."
        minHeight={surfaceHeight}
      />
    );
  }

  if (state === "empty") {
    return (
      <ViewerPlaceholder
        icon={<DescriptionOutlinedIcon fontSize="small" color="action" />}
        title="No document selected"
        message={document.emptyMessage ?? "Choose a document to open the evidence workspace."}
        minHeight={surfaceHeight}
      />
    );
  }

  if (state === "error") {
    return (
      <ViewerPlaceholder
        icon={<WarningAmberOutlinedIcon fontSize="small" color="warning" />}
        title="Unable to load preview"
        message={document.errorMessage ?? "The current preview could not be loaded with the available API response."}
        minHeight={surfaceHeight}
      />
    );
  }

  if (state === "unsupported") {
    return (
      <ViewerPlaceholder
        icon={<DescriptionOutlinedIcon fontSize="small" color="action" />}
        title="Preview not supported"
        message={document.unsupportedReason ?? buildUnsupportedMessage(document.fileKind)}
        minHeight={surfaceHeight}
      />
    );
  }

  if (document.fileKind === "email") {
    return (
      <ViewerPlaceholder
        icon={<DescriptionOutlinedIcon fontSize="small" color="action" />}
        title="Email preview pending"
        message="Inline email rendering is not available from the current API. Use the evidence panel and linked record context instead."
        minHeight={surfaceHeight}
      />
    );
  }

  return (
    <Box
      sx={{
        position: "relative",
        minHeight: surfaceHeight,
        backgroundColor: "background.default",
        overflow: "auto",
        p: { xs: 1, md: 1.5 },
      }}
    >
      <Box
        sx={{
          minHeight: "100%",
          display: "grid",
          placeItems: "start center",
        }}
      >
        <Box
          sx={{
            width: "100%",
            maxWidth: 1040,
            transform: `scale(${zoomPercent / 100}) rotate(${rotationDegrees}deg)`,
            transformOrigin: "top center",
            transition: "transform 120ms ease",
          }}
        >
          <Box
            component="iframe"
            title={selectedPage?.pageNumber ? `${document.title} page ${selectedPage.pageNumber}` : document.title}
            src={document.previewUrl ?? undefined}
            sx={{
              display: "block",
              width: "100%",
              height: { xs: 420, md: 620, lg: 720 },
              border: 0,
              backgroundColor: "common.white",
              borderRadius: 1,
              boxShadow: 1,
            }}
          />
        </Box>
      </Box>
      <DocumentLayerHost layers={layers} context={layerContext} />
    </Box>
  );
}

function DocumentLayerHost({
  layers,
  context,
}: {
  layers: DocumentViewerLayer[];
  context: DocumentViewerLayerContext;
}) {
  const availableLayers = layers.filter((layer) => layer.status === "available" && layer.render);
  if (availableLayers.length === 0) {
    return null;
  }

  return (
    <Box sx={{ position: "absolute", inset: 0, pointerEvents: "none" }}>
      {availableLayers.map((layer) => (
        <Box key={layer.id} sx={{ position: "absolute", inset: 0 }}>
          {layer.render?.(context)}
        </Box>
      ))}
    </Box>
  );
}

function ViewerPlaceholder({
  icon,
  title,
  message,
  minHeight,
}: {
  icon: ReactNode;
  title: string;
  message: string;
  minHeight: { xs: number | string; md: number | string; lg: number | string };
}) {
  return (
    <Box
      sx={{
        minHeight,
        display: "grid",
        placeItems: "center",
        p: 3,
        backgroundColor: "background.default",
      }}
    >
      <Stack spacing={1.25} alignItems="center" sx={{ maxWidth: 420, textAlign: "center" }}>
        <Box
          sx={{
            width: 48,
            height: 48,
            borderRadius: "50%",
            backgroundColor: "background.paper",
            border: (theme) => `1px solid ${theme.palette.divider}`,
            display: "grid",
            placeItems: "center",
          }}
        >
          {icon}
        </Box>
        <Stack spacing={0.5}>
          <Typography variant="subtitle2">{title}</Typography>
          <Typography variant="body2" color="text.secondary">
            {message}
          </Typography>
        </Stack>
      </Stack>
    </Box>
  );
}

function filterEvidenceSections(
  sections: EvidenceWorkspaceSection[],
  searchValue: string,
) {
  const normalizedSearch = searchValue.trim().toLowerCase();
  if (!normalizedSearch) {
    return sections;
  }

  return sections.filter((section) => {
    const text = `${section.searchText ?? ""} ${flattenReactNode(section.content)}`.toLowerCase();
    return section.title.toLowerCase().includes(normalizedSearch) || text.includes(normalizedSearch);
  });
}

function flattenReactNode(node: ReactNode): string {
  if (typeof node === "string" || typeof node === "number") {
    return String(node);
  }
  if (Array.isArray(node)) {
    return node.map((item) => flattenReactNode(item)).join(" ");
  }
  if (!node || typeof node !== "object") {
    return "";
  }
  if ("props" in node) {
    return flattenReactNode((node as { props?: { children?: ReactNode } }).props?.children ?? "");
  }
  return "";
}

function buildUnsupportedMessage(fileKind: DocumentViewerFileKind) {
  switch (fileKind) {
    case "office":
      return "Office document preview architecture is ready, but inline rendering is not exposed by the current API yet.";
    case "tiff":
      return "TIFF preview architecture is ready, but the current API does not provide an inline renderable asset yet.";
    case "image":
      return "Image preview is unavailable with the current API payload.";
    default:
      return "The current file cannot be previewed with the available API response.";
  }
}

function readViewerState(documentId: string): PersistedViewerState {
  if (typeof window === "undefined") {
    return defaultPersistedState;
  }

  try {
    const raw = window.localStorage.getItem(storageKey(documentId));
    if (!raw) {
      return defaultPersistedState;
    }
    const parsed = JSON.parse(raw) as Partial<PersistedViewerState>;
    return {
      zoomPercent: typeof parsed.zoomPercent === "number" ? parsed.zoomPercent : defaultPersistedState.zoomPercent,
      fitMode: parsed.fitMode === "page" ? "page" : "width",
      rotationDegrees: typeof parsed.rotationDegrees === "number" ? parsed.rotationDegrees : defaultPersistedState.rotationDegrees,
    };
  } catch {
    return defaultPersistedState;
  }
}

function writeViewerState(documentId: string, value: PersistedViewerState) {
  if (typeof window === "undefined") {
    return;
  }

  try {
    window.localStorage.setItem(storageKey(documentId), JSON.stringify(value));
  } catch {
    // Ignore storage failures; viewer state remains session-local.
  }
}

function storageKey(documentId: string) {
  return `ikms-document-viewer:${documentId}`;
}
