import { apiClient } from "./client";

export interface OperationsJob {
  jobId: string;
  jobType: string;
  submittedBy: string | null;
  submittedAt: string;
  startedAt: string | null;
  completedAt: string | null;
  duration: number | null;
  status: string;
  progress: number;
  errorSummary: string | null;
  retryCount: number;
  queueKey: string | null;
  targetType: string | null;
  targetId: string | null;
  priority: number;
  cancelRequested: boolean;
  details: Record<string, string>;
}

export interface OperationsQueue {
  queueKey: string;
  queueName: string;
  status: string;
  paused: boolean;
  depth: number;
  runningItems: number;
  failedItems: number;
  updatedAt: string;
  explanation: string;
}

export interface OperationsSchedulerExecution {
  executionId: string;
  startedAt: string;
  completedAt: string | null;
  status: string;
  triggerSource: string;
  details: string | null;
}

export interface OperationsScheduler {
  schedulerKey: string;
  displayName: string;
  description: string;
  enabled: boolean;
  nextExecution: string | null;
  lastExecution: string | null;
  lastStatus: string | null;
  history: OperationsSchedulerExecution[];
}

export interface OperationsCache {
  cacheKey: string;
  displayName: string;
  entryCount: number;
  lastAction: string;
  lastActionAt: string;
}

export interface OperationsHealthComponent {
  component: string;
  status: string;
  explanation: string;
}

export interface OperationsHealth {
  overallStatus: string;
  components: OperationsHealthComponent[];
}

export interface OperationsMetric {
  metricGroup: string;
  metricKey: string;
  metricUnit: string | null;
  value: string;
  recordedAt: string;
}

export interface OperationsDiagnostics {
  systemInformation: Record<string, string>;
  activeWorkers: Record<string, number>;
  queueDepth: Record<string, number>;
  failedJobs: number;
  bottlenecks: string[];
  configurationValidation: string[];
  dependencyValidation: string[];
  recentFailures: OperationsJob[];
  metrics: OperationsMetric[];
}

export function listOperationsJobs() {
  return apiClient.get<OperationsJob[]>("/api/operations/jobs");
}

export function retryOperationsJob(jobId: string) {
  return apiClient.post<OperationsJob>(`/api/operations/jobs/${jobId}/retry`);
}

export function cancelOperationsJob(jobId: string) {
  return apiClient.post<OperationsJob>(`/api/operations/jobs/${jobId}/cancel`);
}

export function listOperationsQueues() {
  return apiClient.get<OperationsQueue[]>("/api/operations/queues");
}

export function pauseOperationsQueue(queueKey: string) {
  return apiClient.post<OperationsQueue>(`/api/operations/queues/${queueKey}/pause`);
}

export function resumeOperationsQueue(queueKey: string) {
  return apiClient.post<OperationsQueue>(`/api/operations/queues/${queueKey}/resume`);
}

export function listOperationsSchedulers() {
  return apiClient.get<OperationsScheduler[]>("/api/operations/schedulers");
}

export function enableOperationsScheduler(schedulerKey: string) {
  return apiClient.post<OperationsScheduler>(`/api/operations/schedulers/${schedulerKey}/enable`);
}

export function disableOperationsScheduler(schedulerKey: string) {
  return apiClient.post<OperationsScheduler>(`/api/operations/schedulers/${schedulerKey}/disable`);
}

export function runOperationsScheduler(schedulerKey: string) {
  return apiClient.post<OperationsScheduler>(`/api/operations/schedulers/${schedulerKey}/run`);
}

export function listOperationsCaches() {
  return apiClient.get<OperationsCache[]>("/api/operations/cache");
}

export function clearOperationsCache(cacheKey: string) {
  return apiClient.post<OperationsCache>(`/api/operations/cache/${cacheKey}/clear`);
}

export function invalidateOperationsCache(cacheKey: string) {
  return apiClient.post<OperationsCache>(`/api/operations/cache/${cacheKey}/invalidate`);
}

export function refreshOperationsCache(cacheKey: string) {
  return apiClient.post<OperationsCache>(`/api/operations/cache/${cacheKey}/refresh`);
}

export function getOperationsHealth() {
  return apiClient.get<OperationsHealth>("/api/operations/health");
}

export function getOperationsDiagnostics() {
  return apiClient.get<OperationsDiagnostics>("/api/operations/diagnostics");
}
