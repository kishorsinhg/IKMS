import { createContext } from "react";
import type { NotificationContextValue } from "./NotificationProvider";

export const NotificationContext = createContext<NotificationContextValue | null>(null);
