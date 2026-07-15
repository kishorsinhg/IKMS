import type { DocumentViewerLayer } from "./EnterpriseDocumentViewer";

export function documentViewerPlaceholderLayers(): DocumentViewerLayer[] {
  return [
    { id: "ocr-layer", kind: "ocr", label: "OCR overlay", status: "placeholder" },
    { id: "highlight-layer", kind: "highlight", label: "Evidence highlights", status: "placeholder" },
    { id: "annotation-layer", kind: "annotation", label: "Annotations", status: "placeholder" },
  ];
}
