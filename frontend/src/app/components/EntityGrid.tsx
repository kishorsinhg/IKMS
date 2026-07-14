import { ReactNode } from "react";
import {
  DataGrid,
  DataGridProps,
  GridColDef,
  GridOverlay,
} from "@mui/x-data-grid";
import { Box, CircularProgress, Stack, Typography } from "@mui/material";

export interface EntityGridProps<R extends Record<string, unknown> = Record<string, unknown>>
  extends Omit<DataGridProps<R>, "columns"> {
  columns: GridColDef<R>[];
  errorTitle?: string;
  errorMessage?: string;
  emptyTitle?: string;
  emptyMessage?: string;
  error?: string | null;
  footerContent?: ReactNode;
}

function OverlayState({ title, message, loading }: { title: string; message: string; loading?: boolean }) {
  return (
    <GridOverlay>
      <Stack spacing={1} alignItems="center" justifyContent="center" sx={{ py: 3 }}>
        {loading ? <CircularProgress size={20} /> : null}
        <Typography variant="subtitle2">{title}</Typography>
        <Typography variant="body2" color="text.secondary" align="center">
          {message}
        </Typography>
      </Stack>
    </GridOverlay>
  );
}

export function EntityGrid<R extends Record<string, unknown> = Record<string, unknown>>({
  columns,
  errorTitle = "Unable to load records",
  errorMessage = "The operational collection could not be displayed.",
  emptyTitle = "No records available",
  emptyMessage = "There are no matching results for the current view.",
  error,
  loading,
  rows,
  initialState,
  pageSizeOptions = [10, 25, 50],
  disableRowSelectionOnClick = true,
  autosizeOnMount,
  footerContent,
  ...rest
}: EntityGridProps<R>) {
  if (error) {
    return (
      <Box
        sx={{
          display: "grid",
          placeItems: "center",
          minHeight: 220,
          border: (theme) => `1px solid ${theme.palette.divider}`,
          borderRadius: 1,
          backgroundColor: "background.paper",
          p: 2,
        }}
      >
        <Stack spacing={1} alignItems="center">
          <Typography variant="subtitle2">{errorTitle}</Typography>
          <Typography variant="body2" color="text.secondary">
            {errorMessage}
          </Typography>
        </Stack>
      </Box>
    );
  }

  return (
    <Box sx={{ display: "grid", gap: 1 }}>
      <DataGrid
        columns={columns}
        rows={rows}
        loading={loading}
        disableRowSelectionOnClick={disableRowSelectionOnClick}
        pageSizeOptions={pageSizeOptions}
        autosizeOnMount={autosizeOnMount}
        density="compact"
        initialState={{
          pagination: {
            paginationModel: {
              pageSize: 10,
              page: 0,
            },
          },
          ...initialState,
        }}
        slots={{
          noRowsOverlay: () => <OverlayState title={emptyTitle} message={emptyMessage} />,
          noResultsOverlay: () => <OverlayState title="No matching results" message={emptyMessage} />,
          loadingOverlay: () => (
            <OverlayState
              title="Loading records"
              message="The grid is loading operational data."
              loading
            />
          ),
        }}
        sx={{ minHeight: 320 }}
        {...rest}
      />
      {footerContent}
    </Box>
  );
}
