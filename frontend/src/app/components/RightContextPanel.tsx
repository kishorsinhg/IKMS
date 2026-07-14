import ChevronRightOutlinedIcon from "@mui/icons-material/ChevronRightOutlined";
import ChevronLeftOutlinedIcon from "@mui/icons-material/ChevronLeftOutlined";
import { Box, Divider, IconButton, Stack, Typography } from "@mui/material";
import { ReactNode } from "react";

export interface ContextSection {
  key: string;
  title: string;
  content: ReactNode;
}

export interface RightContextPanelProps {
  title?: string;
  sections: ContextSection[];
  collapsed: boolean;
  onToggle: () => void;
  width?: number;
}

export function RightContextPanel({
  title = "Context",
  sections,
  collapsed,
  onToggle,
  width = 340,
}: RightContextPanelProps) {
  return (
    <Box
      sx={{
        display: { xs: "none", lg: "block" },
        width: collapsed ? 52 : width,
        minWidth: collapsed ? 52 : 320,
        maxWidth: collapsed ? 52 : 380,
        transition: "width 160ms ease",
      }}
    >
      <Box
        sx={{
          position: "sticky",
          top: 16,
          display: "grid",
          gridTemplateRows: "auto minmax(0, 1fr)",
          maxHeight: "calc(100vh - 32px)",
          border: (theme) => `1px solid ${theme.palette.divider}`,
          borderRadius: 1,
          backgroundColor: "background.paper",
          overflow: "hidden",
        }}
      >
        <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ px: 1, py: 0.75 }}>
          {!collapsed ? (
            <Typography variant="subtitle2" component="h2">
              {title}
            </Typography>
          ) : null}
          <IconButton aria-label={collapsed ? "Expand context panel" : "Collapse context panel"} onClick={onToggle}>
            {collapsed ? <ChevronLeftOutlinedIcon fontSize="small" /> : <ChevronRightOutlinedIcon fontSize="small" />}
          </IconButton>
        </Stack>

        {!collapsed ? (
          <Box sx={{ overflowY: "auto", px: 1.5, pb: 1.5 }}>
            <Stack spacing={1.25} divider={<Divider flexItem />}>
              {sections.map((section) => (
                <Box key={section.key} sx={{ pt: 0.5 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    {section.title}
                  </Typography>
                  <Box>{section.content}</Box>
                </Box>
              ))}
            </Stack>
          </Box>
        ) : null}
      </Box>
    </Box>
  );
}
