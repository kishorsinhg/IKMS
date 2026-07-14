import { Component, ErrorInfo, ReactNode } from "react";
import { ErrorState } from "../WorkspaceStates";

interface AppErrorBoundaryProps {
  children: ReactNode;
}

interface AppErrorBoundaryState {
  hasError: boolean;
}

export class AppErrorBoundary extends Component<AppErrorBoundaryProps, AppErrorBoundaryState> {
  override state: AppErrorBoundaryState = {
    hasError: false,
  };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  override componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    if (import.meta.env.DEV) {
      console.error("IKMS frontend render failure", error, errorInfo);
    }
  }

  override render() {
    if (this.state.hasError) {
      return (
        <main style={{ minHeight: "100vh", display: "grid", placeItems: "center", padding: 16 }}>
          <ErrorState
            title="Something went wrong"
            message="The workspace could not be rendered. Refresh the page or sign in again if the problem persists."
          />
        </main>
      );
    }

    return this.props.children;
  }
}
