import { apiClient } from "./client";

export interface ClassificationPolicyConfig {
  levels: string[];
  defaultClassification: string;
  aiRestrictionThreshold: string;
  exportRestrictionThreshold: string;
  updatedAt: string;
}

export interface RetentionPolicyEntry {
  contentType: string;
  retentionDays: number;
  reviewAfterDays: number | null;
  archivalAfterDays: number | null;
  disposalAfterDays: number | null;
}

export interface RetentionPolicyConfig {
  policies: RetentionPolicyEntry[];
  updatedAt: string;
}

export interface LegalHoldConfig {
  id: string;
  targetType: string;
  targetId: string;
  clientId: string | null;
  legalHold: boolean;
  holdType: string;
  retentionPolicyKey: string | null;
  reviewAt: string | null;
  archivalEligibleAt: string | null;
  disposalEligibleAt: string | null;
  executedAt: string | null;
  reason: string | null;
}

export interface AiGovernancePolicyConfig {
  approvedModels: string[];
  promptPolicyVersion: string;
  responsePolicyVersion: string;
  citationRequired: boolean;
  groundingValidationRequired: boolean;
  updatedAt: string;
}

export interface SecurityPolicyConfig {
  encryptionAtRest: string;
  encryptionInTransit: string;
  keyManagement: string;
  secretManagement: string;
  exportApprovalRequired: boolean;
  watermarkByDefault: boolean;
  updatedAt: string;
}

export interface ComplianceReport {
  activeLegalHolds: number;
  retentionExceptions: number;
  sensitiveDocuments: number;
  restrictedDocuments: number;
  piiAuditEvents: number;
  exportEvents: number;
  aiInteractions: number;
  stewardshipSignals: string[];
}

export function getClassificationPolicy() {
  return apiClient.get<ClassificationPolicyConfig>("/api/governance/classification");
}

export function getRetentionPolicy() {
  return apiClient.get<RetentionPolicyConfig>("/api/governance/retention");
}

export function listLegalHolds() {
  return apiClient.get<LegalHoldConfig[]>("/api/governance/legal-holds");
}

export function getAiGovernancePolicy() {
  return apiClient.get<AiGovernancePolicyConfig>("/api/governance/ai");
}

export function updateAiGovernancePolicy(request: {
  approvedModels: string[];
  promptPolicyVersion: string;
  responsePolicyVersion: string;
  citationRequired: boolean;
  groundingValidationRequired: boolean;
}) {
  return apiClient.post<AiGovernancePolicyConfig>("/api/governance/ai", request);
}

export function getSecurityPolicy() {
  return apiClient.get<SecurityPolicyConfig>("/api/governance/security");
}

export function updateSecurityPolicy(request: {
  encryptionAtRest: string;
  encryptionInTransit: string;
  keyManagement: string;
  secretManagement: string;
  exportApprovalRequired: boolean;
  watermarkByDefault: boolean;
}) {
  return apiClient.post<SecurityPolicyConfig>("/api/governance/security", request);
}

export function getComplianceReport() {
  return apiClient.get<ComplianceReport>("/api/governance/reports/compliance");
}
