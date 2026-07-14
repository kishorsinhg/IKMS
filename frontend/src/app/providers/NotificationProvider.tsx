import { Alert, AlertColor, Snackbar } from "@mui/material";
import { ReactNode, useCallback, useMemo, useState } from "react";
import { NotificationContext } from "./NotificationContext";

export interface NotificationInput {
  message: string;
  severity?: AlertColor;
  autoHideDuration?: number | null;
}

export interface NotificationContextValue {
  notify: (notification: NotificationInput) => void;
}

export function NotificationProvider({ children }: { children: ReactNode }) {
  const [notification, setNotification] = useState<NotificationInput | null>(null);
  const [open, setOpen] = useState(false);

  const notify = useCallback((next: NotificationInput) => {
    setNotification(next);
    setOpen(true);
  }, []);

  const value = useMemo(() => ({ notify }), [notify]);

  return (
    <NotificationContext.Provider value={value}>
      {children}
      <Snackbar
        open={open}
        onClose={(_, reason) => {
          if (reason !== "clickaway") {
            setOpen(false);
          }
        }}
        autoHideDuration={
          notification?.autoHideDuration === null
            ? undefined
            : notification?.autoHideDuration ?? 4000
        }
        anchorOrigin={{ vertical: "top", horizontal: "right" }}
      >
        <Alert
          onClose={notification?.autoHideDuration === null ? undefined : () => setOpen(false)}
          severity={notification?.severity ?? "info"}
          variant="filled"
          sx={{ width: "100%" }}
        >
          {notification?.message}
        </Alert>
      </Snackbar>
    </NotificationContext.Provider>
  );
}
