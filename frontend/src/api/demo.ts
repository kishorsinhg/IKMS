export const isDemoDataEnabled =
  import.meta.env.DEV &&
  !import.meta.env.VITEST &&
  import.meta.env.VITE_ENABLE_DEMO_DATA !== "false";

type ClientType = "INDIVIDUAL" | "BUSINESS";
type ClientStatus = "ACTIVE" | "INACTIVE" | "ARCHIVED";
type NoteStatus = "ACTIVE" | "DELETED";
type UserStatus = "ACTIVE" | "LOCKED" | "DISABLED";
type UserRole = "INDEXER" | "PROCESSOR" | "SUPERVISOR" | "ADMINISTRATOR";
type Permission =
  | "CLIENT_VIEW"
  | "REVIEW_QUEUE_ACCESS"
  | "INTAKE_ACCESS"
  | "SEARCH_CLIENT_KNOWLEDGE"
  | "ASK_CLIENT_AI"
  | "VIEW_REDACTED_DOCUMENTS"
  | "VIEW_ORIGINAL_DOCUMENTS"
  | "VIEW_PII"
  | "VIEW_AUDIT"
  | "EXPORT_AUDIT"
  | "MANAGE_CONFIGURATION"
  | "MANAGE_USERS";
type ReviewQueueItemType = "DOCUMENT" | "EMAIL" | "DOCUMENT_VERSION";
type ReviewQueueReason =
  | "UNLINKED"
  | "LOW_CLIENT_CONFIDENCE"
  | "LOW_CLASSIFICATION_CONFIDENCE"
  | "LOW_EXTRACTION_CONFIDENCE"
  | "DUPLICATE_UNCERTAINTY"
  | "REDACTION_FAILED"
  | "PROMPT_INJECTION_RISK"
  | "PROCESSING_FAILED";
type ReviewQueueStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "REJECTED";
type CitationQuality = "HIGH" | "MEDIUM" | "LOW";

interface DemoCurrentUser {
  id: string;
  username: string;
  displayName: string;
  email: string | null;
  status: UserStatus;
  roles: UserRole[];
  permissions: Permission[];
}

interface DemoClientSummary {
  id: string;
  clientId: string;
  clientIdTemporary: boolean;
  clientType: ClientType;
  status: ClientStatus;
  displayName: string;
}

interface DemoClientProfile extends DemoClientSummary {
  legalName: string | null;
  primaryEmail: string | null;
  primaryPhone: string | null;
  contactPerson: string | null;
  createdAt: string;
  updatedAt: string;
}

interface DemoNote {
  id: string;
  clientId: string;
  noteText: string;
  status: NoteStatus;
  createdAt: string;
  updatedAt: string;
}

interface DemoDocument {
  id: string;
  clientId: string | null;
  title: string;
  source: string;
  processingStatus: string;
  reviewStatus: string;
  redactionStatus: "NOT_NEEDED" | "PENDING" | "AVAILABLE" | "FAILED" | "BLOCKED";
  containsPii: boolean;
  currentVersionId: string | null;
  parentEmailId: string | null;
  createdAt: string;
}

interface DemoEmail {
  id: string;
  clientId: string | null;
  subject: string;
  sender: string;
  recipients: string;
  processingStatus: string;
  reviewStatus: string;
  receivedAt: string;
}

interface DemoSearchResult {
  sourceType: string;
  sourceId: string;
  title: string;
  excerpt: string;
  citation: string;
  pageNumber: number | null;
  sourceSection: string | null;
  retrievalPath: string;
  citationQuality: CitationQuality;
  occurredAt: string;
}

interface DemoSourceCitation {
  sourceType: string;
  sourceId: string;
  title: string;
  excerpt: string;
  pageNumber: number | null;
  sourceSection: string | null;
}

interface DemoAskClientResponse {
  interactionId: string;
  status: "Answered" | "NoEvidence" | "Refused" | "Failed";
  answer: string;
  citations: DemoSourceCitation[];
  retrievalMode: string;
  warnings: string[];
  createdAt: string;
}

interface DemoReviewQueueItem {
  id: string;
  itemType: ReviewQueueItemType;
  itemId: string;
  reason: ReviewQueueReason;
  status: ReviewQueueStatus;
  assignedTo: string | null;
  title: string | null;
  clientId: string | null;
  documentTypeId: string | null;
  metadataValues: Record<string, string>;
}

interface DemoAuditLogEntry {
  id: string;
  occurredAt: string;
  retainedUntil: string;
  actorUserId: string | null;
  actorUsername: string | null;
  clientId: string | null;
  category: string;
  action: string;
  outcome: string;
  targetType: string | null;
  targetId: string | null;
  piiAccess: boolean;
  details: Record<string, string>;
}

interface DemoDocumentTypeConfig {
  id: string;
  name: string;
  description: string | null;
  active: boolean;
  createdAt: string;
}

interface DemoMetadataFieldConfig {
  id: string;
  fieldKey: string;
  label: string;
  pii: boolean;
  active: boolean;
  createdAt: string;
}

export interface DemoPolicyReference {
  id: string;
  policyNumber: string;
  lineOfBusiness: string;
  carrier: string;
  effectiveDate: string;
  expirationDate: string;
  premium: string;
  insuredAsset: string;
  status: "ACTIVE" | "PENDING_REVIEW" | "RENEWAL_IN_PROGRESS" | "LAPSED";
  brokerOwner: string;
  summary: string;
}

export interface DemoClaimReference {
  id: string;
  claimNumber: string;
  carrier: string;
  lossDate: string;
  reportedDate: string;
  status: "OPEN" | "RESERVED" | "CLOSED" | "SUBRO_PENDING";
  amountIncurred: string;
  examiner: string;
  summary: string;
}

export interface DemoAiSummary {
  id: string;
  title: string;
  summary: string;
  confidence: "High" | "Medium" | "Low";
  evidence: string[];
  createdAt: string;
}

export interface DemoActivityItem {
  id: string;
  occurredAt: string;
  type: "EMAIL" | "DOCUMENT" | "NOTE" | "REVIEW" | "AUDIT" | "AI";
  title: string;
  description: string;
  actor: string;
}

export type DemoSearchGroup =
  | "Customers"
  | "Documents"
  | "Emails"
  | "Notes"
  | "Knowledge"
  | "Policy References"
  | "Claim References";

export interface DemoWorkspaceSearchItem {
  id: string;
  group: DemoSearchGroup;
  title: string;
  summary: string;
  meta: string;
  clientId: string;
}

export interface DemoClientWorkspace {
  policyReferences: DemoPolicyReference[];
  claimReferences: DemoClaimReference[];
  aiSummaries: DemoAiSummary[];
  recentActivity: DemoActivityItem[];
  reviewQueue: DemoReviewQueueItem[];
  auditEvents: DemoAuditLogEntry[];
}

interface DemoStore {
  loggedInUser: keyof typeof demoUsers | null;
  clients: DemoClientProfile[];
  notes: DemoNote[];
  reviewQueue: DemoReviewQueueItem[];
  auditEvents: DemoAuditLogEntry[];
  interactions: Array<{ id: string; clientId: string; helpful: boolean | null; question: string }>;
}

const demoUsers = {
  processor: {
    id: "user-processor",
    username: "processor",
    displayName: "Maya Chen",
    email: "maya.chen@harborcrestbrokers.com",
    status: "ACTIVE",
    roles: ["PROCESSOR"],
    permissions: [
      "CLIENT_VIEW",
      "REVIEW_QUEUE_ACCESS",
      "INTAKE_ACCESS",
      "SEARCH_CLIENT_KNOWLEDGE",
      "ASK_CLIENT_AI",
      "VIEW_REDACTED_DOCUMENTS",
    ],
  },
  supervisor: {
    id: "user-supervisor",
    username: "supervisor",
    displayName: "Sloane Reyes",
    email: "sloane.reyes@harborcrestbrokers.com",
    status: "ACTIVE",
    roles: ["SUPERVISOR"],
    permissions: [
      "CLIENT_VIEW",
      "REVIEW_QUEUE_ACCESS",
      "INTAKE_ACCESS",
      "SEARCH_CLIENT_KNOWLEDGE",
      "ASK_CLIENT_AI",
      "VIEW_REDACTED_DOCUMENTS",
      "VIEW_ORIGINAL_DOCUMENTS",
      "VIEW_PII",
      "VIEW_AUDIT",
      "EXPORT_AUDIT",
    ],
  },
  admin: {
    id: "user-admin",
    username: "admin",
    displayName: "Daniel Ortega",
    email: "daniel.ortega@harborcrestbrokers.com",
    status: "ACTIVE",
    roles: ["ADMINISTRATOR"],
    permissions: [
      "CLIENT_VIEW",
      "REVIEW_QUEUE_ACCESS",
      "INTAKE_ACCESS",
      "SEARCH_CLIENT_KNOWLEDGE",
      "ASK_CLIENT_AI",
      "VIEW_REDACTED_DOCUMENTS",
      "VIEW_ORIGINAL_DOCUMENTS",
      "VIEW_PII",
      "VIEW_AUDIT",
      "EXPORT_AUDIT",
      "MANAGE_CONFIGURATION",
      "MANAGE_USERS",
    ],
  },
} as const satisfies Record<string, DemoCurrentUser>;

const baseClients: DemoClientProfile[] = [
  {
    id: "client-harborview",
    clientId: "CB-20418",
    clientIdTemporary: false,
    clientType: "BUSINESS",
    status: "ACTIVE",
    displayName: "Harborview Marine Logistics LLC",
    legalName: "Harborview Marine Logistics LLC",
    primaryEmail: "risk@harborviewmarine.com",
    primaryPhone: "+1-206-555-0148",
    contactPerson: "Nina Patel, Risk Manager",
    createdAt: "2026-06-12T14:15:00Z",
    updatedAt: "2026-07-12T19:42:00Z",
  },
  {
    id: "client-northvalley",
    clientId: "CB-19877",
    clientIdTemporary: false,
    clientType: "BUSINESS",
    status: "ACTIVE",
    displayName: "North Valley Dental Group PC",
    legalName: "North Valley Dental Group Professional Corporation",
    primaryEmail: "office@northvalleydental.com",
    primaryPhone: "+1-503-555-0122",
    contactPerson: "Dr. Kevin Morales",
    createdAt: "2026-04-21T09:00:00Z",
    updatedAt: "2026-07-11T16:08:00Z",
  },
  {
    id: "client-briggs",
    clientId: "CB-21103",
    clientIdTemporary: false,
    clientType: "INDIVIDUAL",
    status: "ACTIVE",
    displayName: "Eleanor Briggs",
    legalName: "Eleanor Anne Briggs",
    primaryEmail: "eleanor.briggs@outlook.com",
    primaryPhone: "+1-415-555-0174",
    contactPerson: null,
    createdAt: "2026-05-05T12:20:00Z",
    updatedAt: "2026-07-13T08:12:00Z",
  },
  {
    id: "client-silverridge",
    clientId: "TMP-4471",
    clientIdTemporary: true,
    clientType: "BUSINESS",
    status: "ACTIVE",
    displayName: "Silver Ridge Property Management",
    legalName: "Silver Ridge Property Management Inc.",
    primaryEmail: "claims@silverridgepm.com",
    primaryPhone: "+1-303-555-0106",
    contactPerson: "Alyssa Grant, Controller",
    createdAt: "2026-07-09T10:11:00Z",
    updatedAt: "2026-07-13T06:45:00Z",
  },
];

const demoDocuments: DemoDocument[] = [
  {
    id: "doc-harbor-renewal",
    clientId: "client-harborview",
    title: "2026 Marine Cargo Renewal Submission",
    source: "EMAIL_ATTACHMENT",
    processingStatus: "CLASSIFIED",
    reviewStatus: "APPROVED",
    redactionStatus: "AVAILABLE",
    containsPii: true,
    currentVersionId: "ver-harbor-renewal",
    parentEmailId: "email-harbor-underwriter",
    createdAt: "2026-07-11T15:24:00Z",
  },
  {
    id: "doc-harbor-lossrun",
    clientId: "client-harborview",
    title: "Five-Year Loss Run - Harborview Marine",
    source: "MANUAL_UPLOAD",
    processingStatus: "INDEXED",
    reviewStatus: "APPROVED",
    redactionStatus: "NOT_NEEDED",
    containsPii: false,
    currentVersionId: "ver-harbor-lossrun",
    parentEmailId: null,
    createdAt: "2026-07-08T18:40:00Z",
  },
  {
    id: "doc-dental-cyber",
    clientId: "client-northvalley",
    title: "Cyber Liability Quote Comparison",
    source: "SHARED_FOLDER",
    processingStatus: "INDEXED",
    reviewStatus: "APPROVED",
    redactionStatus: "AVAILABLE",
    containsPii: true,
    currentVersionId: "ver-dental-cyber",
    parentEmailId: null,
    createdAt: "2026-07-10T13:20:00Z",
  },
  {
    id: "doc-briggs-claim-estimate",
    clientId: "client-briggs",
    title: "Water Damage Mitigation Estimate",
    source: "EMAIL_ATTACHMENT",
    processingStatus: "CLASSIFIED",
    reviewStatus: "APPROVED",
    redactionStatus: "AVAILABLE",
    containsPii: true,
    currentVersionId: "ver-briggs-claim-estimate",
    parentEmailId: "email-briggs-restoration",
    createdAt: "2026-07-12T20:01:00Z",
  },
  {
    id: "doc-silverridge-accord",
    clientId: "client-silverridge",
    title: "ACORD 125 Property Schedule",
    source: "EMAIL_ATTACHMENT",
    processingStatus: "REVIEW_PENDING",
    reviewStatus: "PENDING_REVIEW",
    redactionStatus: "PENDING",
    containsPii: true,
    currentVersionId: "ver-silverridge-accord",
    parentEmailId: "email-silverridge-submission",
    createdAt: "2026-07-13T05:40:00Z",
  },
];

const demoEmails: DemoEmail[] = [
  {
    id: "email-harbor-underwriter",
    clientId: "client-harborview",
    subject: "Updated marine cargo terms for July committee",
    sender: "brooke.ellis@tridentspecialty.com",
    recipients: "risk@harborviewmarine.com; sloane.reyes@harborcrestbrokers.com",
    processingStatus: "LINKED",
    reviewStatus: "APPROVED",
    receivedAt: "2026-07-11T15:17:00Z",
  },
  {
    id: "email-harbor-claim",
    clientId: "client-harborview",
    subject: "Claim reserve review for Seattle container incident",
    sender: "marcus.ford@tridentspecialty.com",
    recipients: "claims@harborcrestbrokers.com",
    processingStatus: "LINKED",
    reviewStatus: "APPROVED",
    receivedAt: "2026-07-09T22:10:00Z",
  },
  {
    id: "email-dental-renewal",
    clientId: "client-northvalley",
    subject: "Cyber renewal questions before binding",
    sender: "office@northvalleydental.com",
    recipients: "service@harborcrestbrokers.com",
    processingStatus: "LINKED",
    reviewStatus: "APPROVED",
    receivedAt: "2026-07-10T12:57:00Z",
  },
  {
    id: "email-briggs-restoration",
    clientId: "client-briggs",
    subject: "Dry-out vendor estimate and moisture readings",
    sender: "dispatch@pacificrestoration.com",
    recipients: "eleanor.briggs@outlook.com; claims@harborcrestbrokers.com",
    processingStatus: "LINKED",
    reviewStatus: "APPROVED",
    receivedAt: "2026-07-12T19:54:00Z",
  },
  {
    id: "email-silverridge-submission",
    clientId: null,
    subject: "Silver Ridge schedule with unresolved location values",
    sender: "alyssa.grant@silverridgepm.com",
    recipients: "submissions@harborcrestbrokers.com",
    processingStatus: "REVIEW_PENDING",
    reviewStatus: "PENDING_REVIEW",
    receivedAt: "2026-07-13T05:35:00Z",
  },
];

const baseNotes: DemoNote[] = [
  {
    id: "note-harbor-1",
    clientId: "client-harborview",
    noteText:
      "Underwriter will hold the current deductible if Harborview confirms the Tacoma terminal sprinkler impairment was corrected before July 20.",
    status: "ACTIVE",
    createdAt: "2026-07-11T16:02:00Z",
    updatedAt: "2026-07-11T16:02:00Z",
  },
  {
    id: "note-harbor-2",
    clientId: "client-harborview",
    noteText:
      "Risk manager asked for side-by-side comparison of Trident renewal terms versus incumbent cargo form, with special focus on refrigerated cargo spoilage sublimit.",
    status: "ACTIVE",
    createdAt: "2026-07-12T08:18:00Z",
    updatedAt: "2026-07-12T08:18:00Z",
  },
  {
    id: "note-briggs-1",
    clientId: "client-briggs",
    noteText:
      "Client prefers text updates after 5 PM Pacific and wants all contractor invoices routed through the carrier adjuster before payment.",
    status: "ACTIVE",
    createdAt: "2026-07-13T00:10:00Z",
    updatedAt: "2026-07-13T00:10:00Z",
  },
];

const demoPolicyReferencesByClient: Record<string, DemoPolicyReference[]> = {
  "client-harborview": [
    {
      id: "policy-harbor-cargo",
      policyNumber: "TSM-CAR-884210",
      lineOfBusiness: "Marine Cargo",
      carrier: "Trident Specialty Marine",
      effectiveDate: "2026-08-01",
      expirationDate: "2027-08-01",
      premium: "$482,600",
      insuredAsset: "Domestic and international containerized cargo",
      status: "RENEWAL_IN_PROGRESS",
      brokerOwner: "Sloane Reyes",
      summary: "Renewal includes reefer spoilage sublimit review and increased theft deductible at the Tacoma terminal.",
    },
    {
      id: "policy-harbor-auto",
      policyNumber: "NW-AUT-194557",
      lineOfBusiness: "Commercial Auto",
      carrier: "Northwest Mutual Casualty",
      effectiveDate: "2026-03-01",
      expirationDate: "2027-03-01",
      premium: "$128,450",
      insuredAsset: "34 tractors and 51 chassis units",
      status: "ACTIVE",
      brokerOwner: "Maya Chen",
      summary: "Fleet remains on scheduled driver review after two preventable backing losses in Q1.",
    },
  ],
  "client-northvalley": [
    {
      id: "policy-dental-bop",
      policyNumber: "HCC-BOP-771943",
      lineOfBusiness: "Businessowners",
      carrier: "Heritage Commercial Casualty",
      effectiveDate: "2026-06-15",
      expirationDate: "2027-06-15",
      premium: "$36,940",
      insuredAsset: "Dental offices in Salem and Keizer",
      status: "ACTIVE",
      brokerOwner: "Maya Chen",
      summary: "Carrier requested confirmation that all imaging workstations now enforce MFA before cyber endorsement is bound.",
    },
  ],
  "client-briggs": [
    {
      id: "policy-briggs-home",
      policyNumber: "PPA-HO-662381",
      lineOfBusiness: "High-Value Homeowners",
      carrier: "Pacific Pinnacle Assurance",
      effectiveDate: "2026-02-01",
      expirationDate: "2027-02-01",
      premium: "$19,880",
      insuredAsset: "Primary residence in Tiburon, CA",
      status: "ACTIVE",
      brokerOwner: "Sloane Reyes",
      summary: "Water backup endorsement includes $150,000 sublimit; current claim falls under the separate sudden discharge coverage grant.",
    },
  ],
  "client-silverridge": [
    {
      id: "policy-silverridge-property",
      policyNumber: "TMP-PROP-90814",
      lineOfBusiness: "Real Estate Property",
      carrier: "Mountain West Indemnity",
      effectiveDate: "2026-07-15",
      expirationDate: "2027-07-15",
      premium: "$214,700",
      insuredAsset: "Twelve habitational locations across Colorado",
      status: "PENDING_REVIEW",
      brokerOwner: "Maya Chen",
      summary: "Statement of values still needs confirmation for three Denver addresses before terms can be finalized.",
    },
  ],
};

const demoClaimReferencesByClient: Record<string, DemoClaimReference[]> = {
  "client-harborview": [
    {
      id: "claim-harbor-001",
      claimNumber: "CLM-SEA-240118",
      carrier: "Trident Specialty Marine",
      lossDate: "2026-05-28",
      reportedDate: "2026-05-29",
      status: "RESERVED",
      amountIncurred: "$184,000",
      examiner: "Marcus Ford",
      summary: "Container shift during terminal transfer damaged refrigerated inventory; reserve review pending updated salvage values.",
    },
  ],
  "client-briggs": [
    {
      id: "claim-briggs-002",
      claimNumber: "PPA-GL-772901",
      carrier: "Pacific Pinnacle Assurance",
      lossDate: "2026-07-03",
      reportedDate: "2026-07-04",
      status: "OPEN",
      amountIncurred: "$42,600",
      examiner: "Kendra Holt",
      summary: "Kitchen supply line failure caused flooring and cabinet damage; mitigation vendor estimate received and adjuster site visit scheduled.",
    },
  ],
  "client-silverridge": [
    {
      id: "claim-silverridge-003",
      claimNumber: "MWI-GL-114920",
      carrier: "Mountain West Indemnity",
      lossDate: "2026-06-30",
      reportedDate: "2026-07-01",
      status: "SUBRO_PENDING",
      amountIncurred: "$67,300",
      examiner: "Jordan Pike",
      summary: "Tenant slip-and-fall at the Aspen Grove property remains open pending contractor snow-removal records and indemnity review.",
    },
  ],
};

const demoAiSummariesByClient: Record<string, DemoAiSummary[]> = {
  "client-harborview": [
    {
      id: "ai-harbor-renewal",
      title: "Renewal posture",
      summary:
        "Harborview is broadly marketable, but the Tacoma sprinkler impairment and reefer spoilage history are the two issues most likely to affect final pricing.",
      confidence: "High",
      evidence: [
        "2026 Marine Cargo Renewal Submission",
        "Updated marine cargo terms for July committee",
        "Five-Year Loss Run - Harborview Marine",
      ],
      createdAt: "2026-07-12T09:22:00Z",
    },
    {
      id: "ai-harbor-claim",
      title: "Open claim exposure",
      summary:
        "The container incident remains a managed reserve rather than a deteriorating severity claim; the largest uncertainty is the final salvage recovery.",
      confidence: "Medium",
      evidence: [
        "Claim reserve review for Seattle container incident",
        "Five-Year Loss Run - Harborview Marine",
      ],
      createdAt: "2026-07-12T09:28:00Z",
    },
  ],
  "client-briggs": [
    {
      id: "ai-briggs-water",
      title: "Claim handling summary",
      summary:
        "The Briggs loss is progressing normally, with coverage indicators favorable so long as mitigation invoices continue to flow through the carrier-approved vendor path.",
      confidence: "High",
      evidence: [
        "Water Damage Mitigation Estimate",
        "Dry-out vendor estimate and moisture readings",
        "Broker note on carrier invoice routing",
      ],
      createdAt: "2026-07-13T02:04:00Z",
    },
  ],
};

const demoRecentActivityByClient: Record<string, DemoActivityItem[]> = {
  "client-harborview": [
    {
      id: "activity-harbor-1",
      occurredAt: "2026-07-12T08:18:00Z",
      type: "NOTE",
      title: "Broker note added",
      description: "Requested side-by-side renewal comparison focused on spoilage sublimit and deductible movement.",
      actor: "Maya Chen",
    },
    {
      id: "activity-harbor-2",
      occurredAt: "2026-07-11T15:24:00Z",
      type: "DOCUMENT",
      title: "Renewal submission indexed",
      description: "Marine cargo submission was linked from underwriter email and classified without manual correction.",
      actor: "IKMS Intake Worker",
    },
    {
      id: "activity-harbor-3",
      occurredAt: "2026-07-09T22:10:00Z",
      type: "EMAIL",
      title: "Claim reserve email linked",
      description: "Carrier examiner update attached to open claim reference CLM-SEA-240118.",
      actor: "IKMS Mailbox Intake",
    },
    {
      id: "activity-harbor-4",
      occurredAt: "2026-07-09T19:10:00Z",
      type: "AI",
      title: "AI summary refreshed",
      description: "Renewal posture summary updated after new underwriter terms arrived.",
      actor: "Client AI",
    },
  ],
  "client-briggs": [
    {
      id: "activity-briggs-1",
      occurredAt: "2026-07-13T02:04:00Z",
      type: "AI",
      title: "Claim handling summary generated",
      description: "AI summary captured claim stage, vendor coordination, and likely next adjuster action.",
      actor: "Client AI",
    },
    {
      id: "activity-briggs-2",
      occurredAt: "2026-07-12T20:01:00Z",
      type: "DOCUMENT",
      title: "Mitigation estimate attached",
      description: "Restoration vendor estimate was linked to the open homeowners claim file.",
      actor: "IKMS Intake Worker",
    },
  ],
  "client-silverridge": [
    {
      id: "activity-silverridge-1",
      occurredAt: "2026-07-13T05:40:00Z",
      type: "REVIEW",
      title: "Submission routed to review queue",
      description: "Property schedule missing confirmed values for three locations and could not be fully linked.",
      actor: "IKMS Review Routing",
    },
  ],
};

const baseReviewQueue: DemoReviewQueueItem[] = [
  {
    id: "review-1",
    itemType: "EMAIL",
    itemId: "email-silverridge-submission",
    reason: "LOW_CLIENT_CONFIDENCE",
    status: "OPEN",
    assignedTo: "processor",
    title: "Silver Ridge schedule with unresolved location values",
    clientId: null,
    documentTypeId: "doc-type-submission",
    metadataValues: {
      carrier: "Mountain West Indemnity",
      policyNumber: "TMP-PROP-90814",
    },
  },
  {
    id: "review-2",
    itemType: "DOCUMENT",
    itemId: "doc-silverridge-accord",
    reason: "LOW_EXTRACTION_CONFIDENCE",
    status: "OPEN",
    assignedTo: "processor",
    title: "ACORD 125 Property Schedule",
    clientId: "client-silverridge",
    documentTypeId: "doc-type-accord",
    metadataValues: {
      carrier: "Mountain West Indemnity",
      policyNumber: "TMP-PROP-90814",
    },
  },
  {
    id: "review-3",
    itemType: "DOCUMENT_VERSION",
    itemId: "ver-harbor-renewal",
    reason: "DUPLICATE_UNCERTAINTY",
    status: "IN_PROGRESS",
    assignedTo: "supervisor",
    title: "2026 Marine Cargo Renewal Submission",
    clientId: "client-harborview",
    documentTypeId: "doc-type-renewal",
    metadataValues: {
      carrier: "Trident Specialty Marine",
      policyNumber: "TSM-CAR-884210",
    },
  },
];

const baseAuditEvents: DemoAuditLogEntry[] = [
  {
    id: "audit-1",
    occurredAt: "2026-07-13T06:10:00Z",
    retainedUntil: "2033-07-13T06:10:00Z",
    actorUserId: "user-supervisor",
    actorUsername: "supervisor",
    clientId: "client-harborview",
    category: "DOCUMENT_ACCESS",
    action: "PREVIEW_ORIGINAL_DOCUMENT",
    outcome: "SUCCESS",
    targetType: "DOCUMENT",
    targetId: "doc-harbor-renewal",
    piiAccess: true,
    details: {
      documentTitle: "2026 Marine Cargo Renewal Submission",
      reason: "renewal-review",
    },
  },
  {
    id: "audit-2",
    occurredAt: "2026-07-13T05:42:00Z",
    retainedUntil: "2033-07-13T05:42:00Z",
    actorUserId: null,
    actorUsername: null,
    clientId: null,
    category: "INTAKE",
    action: "ROUTE_TO_REVIEW_QUEUE",
    outcome: "SUCCESS",
    targetType: "EMAIL",
    targetId: "email-silverridge-submission",
    piiAccess: false,
    details: {
      queueReason: "LOW_CLIENT_CONFIDENCE",
      mailbox: "submissions@harborcrestbrokers.com",
    },
  },
  {
    id: "audit-3",
    occurredAt: "2026-07-13T02:05:00Z",
    retainedUntil: "2033-07-13T02:05:00Z",
    actorUserId: "user-supervisor",
    actorUsername: "supervisor",
    clientId: "client-briggs",
    category: "AI",
    action: "ASK_CLIENT_QUESTION",
    outcome: "SUCCESS",
    targetType: "CLIENT",
    targetId: "client-briggs",
    piiAccess: false,
    details: {
      promptTopic: "claim-status",
      retrievalMode: "RAG_EVIDENCE_ONLY",
    },
  },
  {
    id: "audit-4",
    occurredAt: "2026-07-12T08:18:00Z",
    retainedUntil: "2033-07-12T08:18:00Z",
    actorUserId: "user-processor",
    actorUsername: "processor",
    clientId: "client-harborview",
    category: "NOTE",
    action: "CREATE_NOTE",
    outcome: "SUCCESS",
    targetType: "NOTE",
    targetId: "note-harbor-2",
    piiAccess: false,
    details: {
      noteCategory: "renewal-strategy",
    },
  },
];

const demoDocumentTypes: DemoDocumentTypeConfig[] = [
  {
    id: "doc-type-renewal",
    name: "Renewal Submission",
    description: "Carrier-facing renewal or remarketing submission package.",
    active: true,
    createdAt: "2026-06-01T00:00:00Z",
  },
  {
    id: "doc-type-accord",
    name: "ACORD Form",
    description: "Standardized application or statement-of-values form.",
    active: true,
    createdAt: "2026-06-01T00:00:00Z",
  },
  {
    id: "doc-type-submission",
    name: "Submission Email",
    description: "Inbound producer or insured submission correspondence.",
    active: true,
    createdAt: "2026-06-01T00:00:00Z",
  },
];

const demoMetadataFields: DemoMetadataFieldConfig[] = [
  {
    id: "meta-carrier",
    fieldKey: "carrier",
    label: "Carrier",
    pii: false,
    active: true,
    createdAt: "2026-06-01T00:00:00Z",
  },
  {
    id: "meta-policy-number",
    fieldKey: "policyNumber",
    label: "Policy Number",
    pii: false,
    active: true,
    createdAt: "2026-06-01T00:00:00Z",
  },
  {
    id: "meta-loss-location",
    fieldKey: "lossLocation",
    label: "Loss Location",
    pii: true,
    active: true,
    createdAt: "2026-06-01T00:00:00Z",
  },
];

const store: DemoStore = {
  loggedInUser: "supervisor",
  clients: clone(baseClients),
  notes: clone(baseNotes),
  reviewQueue: clone(baseReviewQueue),
  auditEvents: clone(baseAuditEvents),
  interactions: [],
};

export async function getDemoCurrentUser() {
  if (!store.loggedInUser) {
    throw createUnauthorizedError();
  }
  return clone(demoUsers[store.loggedInUser]);
}

export async function demoLogin(request: { username: string; password: string }) {
  const requestedUser = request.username.trim().toLowerCase();
  const resolvedUser = requestedUser in demoUsers ? (requestedUser as keyof typeof demoUsers) : "supervisor";
  store.loggedInUser = resolvedUser;
  return clone(demoUsers[resolvedUser]);
}

export async function demoLogout() {
  store.loggedInUser = null;
}

export async function listDemoClients(query = "") {
  const normalized = query.trim().toLowerCase();
  const results = store.clients.filter((client) => {
    if (!normalized) {
      return true;
    }
    const policyHits = demoPolicyReferencesByClient[client.id]?.some((policy) =>
      [policy.policyNumber, policy.carrier, policy.lineOfBusiness].join(" ").toLowerCase().includes(normalized),
    );
    return [
      client.displayName,
      client.clientId,
      client.primaryEmail ?? "",
      client.contactPerson ?? "",
      client.legalName ?? "",
    ].join(" ").toLowerCase().includes(normalized) || Boolean(policyHits);
  });
  return clone(results.map(toClientSummary));
}

export async function createDemoClient(request: {
  clientId?: string;
  clientType: ClientType;
  displayName: string;
  legalName?: string;
  primaryEmail?: string;
  primaryPhone?: string;
  contactPerson?: string;
}) {
  const slug = request.displayName.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");
  const id = `client-${slug || "new"}`;
  const now = "2026-07-13T09:00:00Z";
  const client: DemoClientProfile = {
    id,
    clientId: request.clientId?.trim() || `TMP-${4200 + store.clients.length}`,
    clientIdTemporary: !request.clientId?.trim(),
    clientType: request.clientType,
    status: "ACTIVE",
    displayName: request.displayName.trim(),
    legalName: request.legalName?.trim() || null,
    primaryEmail: request.primaryEmail?.trim() || null,
    primaryPhone: request.primaryPhone?.trim() || null,
    contactPerson: request.contactPerson?.trim() || null,
    createdAt: now,
    updatedAt: now,
  };
  store.clients = [client, ...store.clients];
  return clone(client);
}

export async function getDemoClient(clientId: string) {
  return clone(requireClient(clientId));
}

export async function listDemoNotes(clientId: string) {
  return clone(store.notes.filter((note) => note.clientId === clientId && note.status === "ACTIVE"));
}

export async function createDemoNote(clientId: string, request: { noteText: string }) {
  const now = "2026-07-13T09:05:00Z";
  const note: DemoNote = {
    id: `note-${store.notes.length + 1}`,
    clientId,
    noteText: request.noteText.trim(),
    status: "ACTIVE",
    createdAt: now,
    updatedAt: now,
  };
  store.notes = [note, ...store.notes];
  appendAuditEvent({
    id: `audit-note-${store.auditEvents.length + 1}`,
    occurredAt: now,
    retainedUntil: "2033-07-13T09:05:00Z",
    actorUserId: demoUsers.supervisor.id,
    actorUsername: store.loggedInUser,
    clientId,
    category: "NOTE",
    action: "CREATE_NOTE",
    outcome: "SUCCESS",
    targetType: "NOTE",
    targetId: note.id,
    piiAccess: false,
    details: {
      noteCategory: "broker-note",
    },
  });
  return clone(note);
}

export async function updateDemoNote(noteId: string, request: { noteText: string }) {
  const note = store.notes.find((item) => item.id === noteId);
  if (!note) {
    throw new Error(`Demo note ${noteId} not found`);
  }
  note.noteText = request.noteText.trim();
  note.updatedAt = "2026-07-13T09:12:00Z";
  return clone(note);
}

export async function deleteDemoNote(noteId: string) {
  const note = store.notes.find((item) => item.id === noteId);
  if (note) {
    note.status = "DELETED";
    note.updatedAt = "2026-07-13T09:18:00Z";
  }
}

export async function listDemoClientDocuments(clientId: string) {
  return clone(demoDocuments.filter((document) => document.clientId === clientId));
}

export async function listDemoClientEmails(clientId: string) {
  return clone(demoEmails.filter((email) => email.clientId === clientId));
}

export async function listDemoReviewQueue(status?: ReviewQueueStatus | "", reason?: ReviewQueueReason | "") {
  return clone(store.reviewQueue.filter((item) => {
    if (status && item.status !== status) {
      return false;
    }
    if (reason && item.reason !== reason) {
      return false;
    }
    return true;
  }));
}

export async function getDemoReviewQueueItem(itemId: string) {
  const item = store.reviewQueue.find((entry) => entry.id === itemId);
  if (!item) {
    throw new Error(`Demo review item ${itemId} not found`);
  }
  return clone(item);
}

export async function demoLinkReviewItemClient(itemId: string, clientId: string) {
  const item = requireReviewItem(itemId);
  item.clientId = clientId;
  item.status = "IN_PROGRESS";
  return clone(item);
}

export async function demoCorrectReviewItemMetadata(
  itemId: string,
  request: { title: string; documentTypeId?: string; metadataValues?: Record<string, string> },
) {
  const item = requireReviewItem(itemId);
  item.title = request.title;
  item.documentTypeId = request.documentTypeId ?? null;
  item.metadataValues = {
    ...item.metadataValues,
    ...(request.metadataValues ?? {}),
  };
  return clone(item);
}

export async function demoApproveReviewItem(itemId: string) {
  const item = requireReviewItem(itemId);
  item.status = "RESOLVED";
  return clone(item);
}

export async function demoRejectReviewItem(itemId: string, reason: string) {
  const item = requireReviewItem(itemId);
  item.status = "REJECTED";
  item.metadataValues.rejectionReason = reason;
  return clone(item);
}

export async function searchDemoAuditLogs(filters: {
  actor?: string;
  action?: string;
  clientId?: string;
  from?: string;
  to?: string;
}) {
  const actor = filters.actor?.toLowerCase();
  const action = filters.action?.toLowerCase();
  return clone(store.auditEvents.filter((entry) => {
    if (actor && !(entry.actorUsername ?? "").toLowerCase().includes(actor)) {
      return false;
    }
    if (action && !entry.action.toLowerCase().includes(action)) {
      return false;
    }
    if (filters.clientId && entry.clientId !== filters.clientId) {
      return false;
    }
    if (filters.from && entry.occurredAt < filters.from) {
      return false;
    }
    if (filters.to && entry.occurredAt > filters.to) {
      return false;
    }
    return true;
  }));
}

export async function exportDemoAuditLogs(filters: {
  actor?: string;
  action?: string;
  clientId?: string;
  from?: string;
  to?: string;
}) {
  const rows = await searchDemoAuditLogs(filters);
  const header = "occurredAt,actor,category,action,outcome,clientId,piiAccess,details";
  const body = rows.map((entry) => (
    [
      entry.occurredAt,
      entry.actorUsername ?? "system",
      entry.category,
      entry.action,
      entry.outcome,
      entry.clientId ?? "",
      entry.piiAccess ? "true" : "false",
      Object.entries(entry.details).map(([key, value]) => `${key}=${value}`).join("|"),
    ].map(csvEscape).join(",")
  ));
  return [header, ...body].join("\n");
}

export async function listDemoDocumentTypes() {
  return clone(demoDocumentTypes);
}

export async function listDemoMetadataFields() {
  return clone(demoMetadataFields);
}

export async function getDemoClientWorkspace(clientId: string): Promise<DemoClientWorkspace> {
  return clone({
    policyReferences: demoPolicyReferencesByClient[clientId] ?? [],
    claimReferences: demoClaimReferencesByClient[clientId] ?? [],
    aiSummaries: demoAiSummariesByClient[clientId] ?? [],
    recentActivity: demoRecentActivityByClient[clientId] ?? [],
    reviewQueue: store.reviewQueue.filter((item) => item.clientId === clientId),
    auditEvents: store.auditEvents.filter((entry) => entry.clientId === clientId),
  });
}

export async function getDemoRecentActivity(limit = 6) {
  const combined = Object.values(demoRecentActivityByClient).flat().sort((left, right) =>
    right.occurredAt.localeCompare(left.occurredAt),
  );
  return clone(combined.slice(0, limit));
}

export async function searchDemoWorkspace(query: string) {
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return [] as DemoWorkspaceSearchItem[];
  }

  const results: DemoWorkspaceSearchItem[] = [];

  for (const client of store.clients) {
    const relatedPolicyMatch = (demoPolicyReferencesByClient[client.id] ?? []).some((policy) =>
      matches(normalized, [policy.policyNumber, policy.carrier, policy.lineOfBusiness, policy.summary]),
    );
    const relatedClaimMatch = (demoClaimReferencesByClient[client.id] ?? []).some((claim) =>
      matches(normalized, [claim.claimNumber, claim.carrier, claim.summary, claim.examiner]),
    );
    const relatedKnowledgeMatch = (demoAiSummariesByClient[client.id] ?? []).some((summary) =>
      matches(normalized, [summary.title, summary.summary, summary.evidence.join(" ")]),
    );
    if (
      matches(normalized, [client.displayName, client.clientId, client.contactPerson, client.primaryEmail]) ||
      relatedPolicyMatch ||
      relatedClaimMatch ||
      relatedKnowledgeMatch
    ) {
      results.push({
        id: `customer-${client.id}`,
        group: "Customers",
        title: client.displayName,
        summary: `${client.clientType} · ${client.status}`,
        meta: `${client.clientId}${client.clientIdTemporary ? " (Temporary)" : ""}`,
        clientId: client.id,
      });
    }
  }

  for (const document of demoDocuments) {
    if (document.clientId && matches(normalized, [document.title, document.source, document.reviewStatus])) {
      results.push({
        id: `document-${document.id}`,
        group: "Documents",
        title: document.title,
        summary: document.source,
        meta: `${document.reviewStatus} · ${document.createdAt.slice(0, 10)}`,
        clientId: document.clientId,
      });
    }
  }

  for (const email of demoEmails) {
    if (email.clientId && matches(normalized, [email.subject, email.sender, email.recipients])) {
      results.push({
        id: `email-${email.id}`,
        group: "Emails",
        title: email.subject,
        summary: email.sender,
        meta: `${email.reviewStatus} · ${email.receivedAt.slice(0, 10)}`,
        clientId: email.clientId,
      });
    }
  }

  for (const note of store.notes.filter((item) => item.status === "ACTIVE")) {
    if (matches(normalized, [note.noteText])) {
      results.push({
        id: `note-${note.id}`,
        group: "Notes",
        title: requireClient(note.clientId).displayName,
        summary: note.noteText,
        meta: `Note updated ${note.updatedAt.slice(0, 10)}`,
        clientId: note.clientId,
      });
    }
  }

  for (const [clientId, summaries] of Object.entries(demoAiSummariesByClient)) {
    for (const summary of summaries) {
      if (matches(normalized, [summary.title, summary.summary, summary.evidence.join(" ")])) {
        results.push({
          id: `knowledge-${summary.id}`,
          group: "Knowledge",
          title: summary.title,
          summary: summary.summary,
          meta: `${summary.confidence} confidence`,
          clientId,
        });
      }
    }
  }

  for (const [clientId, policies] of Object.entries(demoPolicyReferencesByClient)) {
    for (const policy of policies) {
      if (matches(normalized, [policy.policyNumber, policy.carrier, policy.lineOfBusiness, policy.summary])) {
        results.push({
          id: `policy-${policy.id}`,
          group: "Policy References",
          title: policy.policyNumber,
          summary: `${policy.lineOfBusiness} · ${policy.carrier}`,
          meta: policy.summary,
          clientId,
        });
      }
    }
  }

  for (const [clientId, claims] of Object.entries(demoClaimReferencesByClient)) {
    for (const claim of claims) {
      if (matches(normalized, [claim.claimNumber, claim.carrier, claim.summary, claim.examiner])) {
        results.push({
          id: `claim-${claim.id}`,
          group: "Claim References",
          title: claim.claimNumber,
          summary: `${claim.status} · ${claim.carrier}`,
          meta: claim.summary,
          clientId,
        });
      }
    }
  }

  return clone(results);
}

export async function searchDemoClientKnowledge(clientId: string, query: string) {
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return [] as DemoSearchResult[];
  }

  const results: DemoSearchResult[] = [];
  const documents = demoDocuments.filter((item) => item.clientId === clientId);
  const emails = demoEmails.filter((item) => item.clientId === clientId);
  const notes = store.notes.filter((item) => item.clientId === clientId && item.status === "ACTIVE");
  const policies = demoPolicyReferencesByClient[clientId] ?? [];
  const claims = demoClaimReferencesByClient[clientId] ?? [];

  documents.forEach((document) => {
    if (matches(normalized, [document.title])) {
      results.push({
        sourceType: "DOCUMENT",
        sourceId: document.id,
        title: document.title,
        excerpt: `${document.reviewStatus} ${document.source.toLowerCase().replace(/_/g, " ")} for ${requireClient(clientId).displayName}.`,
        citation: `${document.title} · created ${document.createdAt.slice(0, 10)}`,
        pageNumber: 1,
        sourceSection: "Overview",
        retrievalPath: "document-title-match",
        citationQuality: "HIGH",
        occurredAt: document.createdAt,
      });
    }
  });

  emails.forEach((email) => {
    if (matches(normalized, [email.subject, email.sender])) {
      results.push({
        sourceType: "EMAIL",
        sourceId: email.id,
        title: email.subject,
        excerpt: `Email from ${email.sender} regarding ${requireClient(clientId).displayName}.`,
        citation: `${email.subject} · received ${email.receivedAt.slice(0, 10)}`,
        pageNumber: null,
        sourceSection: "Message",
        retrievalPath: "email-subject-match",
        citationQuality: "HIGH",
        occurredAt: email.receivedAt,
      });
    }
  });

  notes.forEach((note) => {
    if (matches(normalized, [note.noteText])) {
      results.push({
        sourceType: "NOTE",
        sourceId: note.id,
        title: "Broker note",
        excerpt: note.noteText,
        citation: `Broker note updated ${note.updatedAt.slice(0, 10)}`,
        pageNumber: null,
        sourceSection: "Note",
        retrievalPath: "note-keyword-match",
        citationQuality: "MEDIUM",
        occurredAt: note.updatedAt,
      });
    }
  });

  policies.forEach((policy) => {
    if (matches(normalized, [policy.policyNumber, policy.summary, policy.carrier])) {
      results.push({
        sourceType: "POLICY_REFERENCE",
        sourceId: policy.id,
        title: policy.policyNumber,
        excerpt: policy.summary,
        citation: `${policy.lineOfBusiness} with ${policy.carrier}`,
        pageNumber: null,
        sourceSection: "Policy reference",
        retrievalPath: "policy-reference-match",
        citationQuality: "HIGH",
        occurredAt: `${policy.effectiveDate}T00:00:00Z`,
      });
    }
  });

  claims.forEach((claim) => {
    if (matches(normalized, [claim.claimNumber, claim.summary, claim.carrier])) {
      results.push({
        sourceType: "CLAIM_REFERENCE",
        sourceId: claim.id,
        title: claim.claimNumber,
        excerpt: claim.summary,
        citation: `${claim.status} claim with ${claim.carrier}`,
        pageNumber: null,
        sourceSection: "Claim reference",
        retrievalPath: "claim-reference-match",
        citationQuality: "HIGH",
        occurredAt: `${claim.reportedDate}T00:00:00Z`,
      });
    }
  });

  return clone(results.sort((left, right) => right.occurredAt.localeCompare(left.occurredAt)));
}

export async function askDemoClientQuestion(clientId: string, question: string): Promise<DemoAskClientResponse> {
  const normalized = question.toLowerCase();
  const interactionId = `interaction-${store.interactions.length + 1}`;
  let response: DemoAskClientResponse;

  if (normalized.includes("renewal") || normalized.includes("policy") || normalized.includes("carrier")) {
    const policy = (demoPolicyReferencesByClient[clientId] ?? [])[0];
    const summary = (demoAiSummariesByClient[clientId] ?? [])[0];
    response = {
      interactionId,
      status: "Answered",
      answer: policy
        ? `${policy.carrier} is currently carrying the primary renewal discussion for ${policy.lineOfBusiness}. The main issue still open is ${policy.summary.toLowerCase()}`
        : "No renewal policy reference is available for this client.",
      citations: compact([
        policy && {
          sourceType: "POLICY_REFERENCE",
          sourceId: policy.id,
          title: policy.policyNumber,
          excerpt: policy.summary,
          pageNumber: null,
          sourceSection: "Policy reference",
        },
        summary && {
          sourceType: "AI_SUMMARY",
          sourceId: summary.id,
          title: summary.title,
          excerpt: summary.summary,
          pageNumber: null,
          sourceSection: "Knowledge summary",
        },
      ]),
      retrievalMode: "RAG_EVIDENCE_ONLY",
      warnings: [],
      createdAt: "2026-07-13T09:30:00Z",
    };
  } else if (normalized.includes("claim") || normalized.includes("loss")) {
    const claim = (demoClaimReferencesByClient[clientId] ?? [])[0];
    response = claim
      ? {
        interactionId,
        status: "Answered",
        answer: `${claim.claimNumber} is ${claim.status.toLowerCase()} with incurred value of ${claim.amountIncurred}. Current direction: ${claim.summary}`,
        citations: [
          {
            sourceType: "CLAIM_REFERENCE",
            sourceId: claim.id,
            title: claim.claimNumber,
            excerpt: claim.summary,
            pageNumber: null,
            sourceSection: "Claim reference",
          },
        ],
        retrievalMode: "RAG_EVIDENCE_ONLY",
        warnings: [],
        createdAt: "2026-07-13T09:31:00Z",
      }
      : {
        interactionId,
        status: "NoEvidence",
        answer: "No claim evidence is available for this client in the demo dataset.",
        citations: [],
        retrievalMode: "RAG_EVIDENCE_ONLY",
        warnings: ["No claim references were indexed for this customer."],
        createdAt: "2026-07-13T09:31:00Z",
      };
  } else {
    response = {
      interactionId,
      status: "Answered",
      answer:
        "The strongest evidence in this record points to active servicing work across documents, emails, notes, and policy references. Ask about renewal, claim status, carrier position, or recent activity for a more precise answer.",
      citations: [
        {
          sourceType: "CLIENT",
          sourceId: clientId,
          title: requireClient(clientId).displayName,
          excerpt: "Cross-source client workspace summary from documents, notes, and references.",
          pageNumber: null,
          sourceSection: "Workspace summary",
        },
      ],
      retrievalMode: "RAG_EVIDENCE_ONLY",
      warnings: ["Answer generalized because the question did not match a specific evidence category."],
      createdAt: "2026-07-13T09:32:00Z",
    };
  }

  store.interactions.push({
    id: interactionId,
    clientId,
    helpful: null,
    question,
  });

  appendAuditEvent({
    id: `audit-ai-${store.auditEvents.length + 1}`,
    occurredAt: response.createdAt,
    retainedUntil: "2033-07-13T09:32:00Z",
    actorUserId: demoUsers.supervisor.id,
    actorUsername: store.loggedInUser,
    clientId,
    category: "AI",
    action: "ASK_CLIENT_QUESTION",
    outcome: response.status === "Answered" ? "SUCCESS" : "NO_EVIDENCE",
    targetType: "CLIENT",
    targetId: clientId,
    piiAccess: false,
    details: {
      retrievalMode: response.retrievalMode,
      question,
    },
  });

  return clone(response);
}

export async function sendDemoAiFeedback(interactionId: string, helpful: boolean) {
  const interaction = store.interactions.find((item) => item.id === interactionId);
  if (interaction) {
    interaction.helpful = helpful;
  }
}

function requireClient(clientId: string) {
  const client = store.clients.find((entry) => entry.id === clientId);
  if (!client) {
    throw new Error(`Demo client ${clientId} not found`);
  }
  return client;
}

function requireReviewItem(itemId: string) {
  const item = store.reviewQueue.find((entry) => entry.id === itemId);
  if (!item) {
    throw new Error(`Demo review item ${itemId} not found`);
  }
  return item;
}

function appendAuditEvent(event: DemoAuditLogEntry) {
  store.auditEvents = [event, ...store.auditEvents];
}

function toClientSummary(client: DemoClientProfile): DemoClientSummary {
  return {
    id: client.id,
    clientId: client.clientId,
    clientIdTemporary: client.clientIdTemporary,
    clientType: client.clientType,
    status: client.status,
    displayName: client.displayName,
  };
}

function createUnauthorizedError() {
  const error = new Error("Unauthorized") as Error & { status: number; data: { message: string } };
  error.status = 401;
  error.data = { message: "Unauthorized" };
  return error;
}

function matches(query: string, values: Array<string | null | undefined>) {
  return values.some((value) => value?.toLowerCase().includes(query));
}

function csvEscape(value: string) {
  const escaped = value.replace(/"/g, "\"\"");
  return `"${escaped}"`;
}

function compact<T>(values: Array<T | null | undefined>): T[] {
  return values.filter(Boolean) as T[];
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}
