import ChevronRightOutlinedIcon from "@mui/icons-material/ChevronRightOutlined";
import { Breadcrumbs, Box, Stack, Typography } from "@mui/material";
import { ReactNode } from "react";

export interface WorkspaceHeaderBreadcrumb {
  label: string;
  href?: string;
}

export interface WorkspaceHeaderProps {
  breadcrumbs?: WorkspaceHeaderBreadcrumb[];
  title: string;
  subtitle?: string;
  primaryActions?: ReactNode;
  secondaryActions?: ReactNode;
}

export function WorkspaceHeader({
  breadcrumbs,
  title,
  subtitle,
  primaryActions,
  secondaryActions,
}: WorkspaceHeaderProps) {
  return (
    <Box
      component="header"
      sx={{
        display: "grid",
        gap: 1.5,
        px: 2,
        py: 1.5,
        border: (theme) => `1px solid ${theme.palette.divider}`,
        borderRadius: 1,
        backgroundColor: "background.paper",
      }}
    >
      <Stack
        direction={{ xs: "column", lg: "row" }}
        spacing={1.5}
        justifyContent="space-between"
        alignItems={{ xs: "flex-start", lg: "flex-start" }}
      >
        <Stack spacing={0.75}>
          {breadcrumbs && breadcrumbs.length > 0 ? (
            <Breadcrumbs separator={<ChevronRightOutlinedIcon fontSize="small" />} aria-label="Breadcrumb">
              {breadcrumbs.map((item) => (
                <Typography
                  key={`${item.label}-${item.href ?? "current"}`}
                  variant="caption"
                  color={item.href ? "text.secondary" : "text.primary"}
                  component={item.href ? "a" : "span"}
                  href={item.href}
                  sx={{ textDecoration: "none" }}
                >
                  {item.label}
                </Typography>
              ))}
            </Breadcrumbs>
          ) : null}

          <Stack spacing={0.5}>
            <Typography variant="h1">{title}</Typography>
            {subtitle ? (
              <Typography variant="body2" color="text.secondary">
                {subtitle}
              </Typography>
            ) : null}
          </Stack>
        </Stack>

        {(primaryActions || secondaryActions) && (
          <Stack
            direction="row"
            spacing={1}
            alignItems="center"
            justifyContent={{ xs: "flex-start", lg: "flex-end" }}
            flexWrap="wrap"
          >
            {secondaryActions}
            {primaryActions}
          </Stack>
        )}
      </Stack>
    </Box>
  );
}
