import CloseOutlinedIcon from "@mui/icons-material/CloseOutlined";
import { Dialog, DialogContent, IconButton, Stack, useMediaQuery } from "@mui/material";
import { useTheme } from "@mui/material/styles";
import { EnterpriseDocumentViewer, type EnterpriseDocumentViewerProps } from "./EnterpriseDocumentViewer";

export interface DocumentViewerDialogProps
  extends Omit<EnterpriseDocumentViewerProps, "onBack" | "embedded"> {
  open: boolean;
  onClose: () => void;
}

export function DocumentViewerDialog({
  open,
  onClose,
  ...viewerProps
}: DocumentViewerDialogProps) {
  const theme = useTheme();
  const fullScreen = useMediaQuery(theme.breakpoints.down("md"));

  return (
    <Dialog
      open={open}
      onClose={onClose}
      fullScreen={fullScreen}
      fullWidth
      maxWidth="xl"
      PaperProps={{
        sx: fullScreen
          ? undefined
          : {
              minHeight: "88vh",
            },
      }}
    >
      <DialogContent sx={{ p: { xs: 1, md: 1.25 } }}>
        <Stack spacing={1}>
          {!fullScreen ? (
            <Stack direction="row" justifyContent="flex-end">
              <IconButton aria-label="Close document viewer" onClick={onClose}>
                <CloseOutlinedIcon fontSize="small" />
              </IconButton>
            </Stack>
          ) : null}
          <EnterpriseDocumentViewer
            {...viewerProps}
            embedded={false}
            onBack={onClose}
          />
        </Stack>
      </DialogContent>
    </Dialog>
  );
}
