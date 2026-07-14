import { ReactNode } from "react";
import { Permission } from "../../api/auth";
import { LoadingState, RestrictedContentState } from "../WorkspaceStates";
import { useCurrentUser } from "./useCurrentUser";

export interface PermissionGuardProps {
  anyOf?: Permission[];
  allOf?: Permission[];
  fallback?: ReactNode;
  loadingFallback?: ReactNode;
  children: ReactNode;
}

function hasRequiredPermissions(
  permissions: Permission[],
  { anyOf, allOf }: Pick<PermissionGuardProps, "anyOf" | "allOf">,
) {
  const matchesAny = !anyOf || anyOf.length === 0 || anyOf.some((permission) => permissions.includes(permission));
  const matchesAll = !allOf || allOf.every((permission) => permissions.includes(permission));

  return matchesAny && matchesAll;
}

export function PermissionGuard({
  anyOf,
  allOf,
  fallback,
  loadingFallback,
  children,
}: PermissionGuardProps) {
  const currentUserQuery = useCurrentUser();

  if (currentUserQuery.isLoading) {
    return loadingFallback ?? (
      <LoadingState
        title="Loading permission context"
        message="Checking the current user permissions for this workspace."
      />
    );
  }

  if (!currentUserQuery.data) {
    return fallback ?? (
      <RestrictedContentState
        title="Content restricted"
        message="This content is not available for the current account."
      />
    );
  }

  if (!hasRequiredPermissions(currentUserQuery.data.permissions, { anyOf, allOf })) {
    return fallback ?? (
      <RestrictedContentState
        title="Content restricted"
        message="This content is not available for the current account."
      />
    );
  }

  return <>{children}</>;
}
