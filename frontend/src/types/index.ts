export type UserRole = "ADMIN" | "ARCHITECT" | "VIEWER";

export type AssessmentStatus =
  | "DRAFT"
  | "IMPORTED"
  | "IN_REVIEW"
  | "COMPLETED"
  | "IMPORT_PENDING"
  | "IMPORTING"
  | "ANALYZING"
  | "READY"
  | "PUBLISHED";

export type FindingStatus =
  | "OPEN"
  | "APPROVED"
  | "EDITED"
  | "RISK_ACCEPTED"
  | "DISMISSED";

export type FindingSeverity = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW" | "INFO";

export interface Assessment {
  id: string;
  name: string;
  description?: string;
  status: AssessmentStatus;
  organizationId: string;
  targetProjectName?: string;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

export interface CreateAssessmentRequest {
  name: string;
  description?: string;
  targetProjectName?: string;
}

export interface ImportJob {
  id: string;
  assessmentId: string;
  status: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
  contentHash?: string;
  errorMessage?: string;
  startedAt?: string;
  completedAt?: string;
}

export interface InventoryModule {
  name: string;
  path: string;
  buildSystem: "MAVEN" | "GRADLE" | "UNKNOWN";
}

export interface InventoryPackage {
  name: string;
  fileCount: number;
  lineCount: number;
}

export interface InventoryType {
  fqn: string;
  kind: "CLASS" | "INTERFACE" | "ENUM" | "ANNOTATION" | "RECORD" | "UNKNOWN";
  packageName: string;
  path: string;
  lineCount: number;
  annotations?: string[];
}

export interface InventoryEndpoint {
  type: "JAX_RS" | "SERVLET" | "SPRING_MVC" | "SOAP" | "OTHER";
  pathOrName: string;
  sourcePath: string;
  evidenceId: string;
}

export interface Inventory {
  modules: InventoryModule[];
  packages: InventoryPackage[];
  types: InventoryType[];
  endpoints: InventoryEndpoint[];
  persistence: Array<{
    technology: string;
    name: string;
    sourcePath: string;
    evidenceId: string;
  }>;
  messaging: Array<{
    technology: string;
    name: string;
    sourcePath: string;
    evidenceId: string;
  }>;
}

export interface Finding {
  id: string;
  assessmentId: string;
  ruleId: string;
  title: string;
  description: string;
  severity: FindingSeverity;
  status: FindingStatus;
  category: string;
  evidence?: string;
  recommendation?: string;
  updatedAt: string;
  updatedBy?: string;
}

export interface UpdateFindingStatusRequest {
  status: FindingStatus;
  comment?: string;
}

export interface QuestionnaireField {
  id: string;
  label: string;
  type: "text" | "textarea" | "select" | "number" | "boolean";
  required: boolean;
  options?: string[];
  value?: string | number | boolean;
  helpText?: string;
}

export interface QuestionnaireSection {
  id: string;
  title: string;
  description?: string;
  fields: QuestionnaireField[];
}

export interface Questionnaire {
  version: string;
  sections: QuestionnaireSection[];
  completedAt?: string;
}

export interface DimensionScore {
  id: string;
  name: string;
  score: number;
  maxScore: number;
  weight: number;
  explanation: string;
  mermaidDiagram?: string;
  factors: Array<{
    name: string;
    impact: number;
    description: string;
  }>;
}

export interface RoadmapItem {
  id: string;
  title: string;
  description: string;
  priority?: "HIGH" | "MEDIUM" | "LOW";
  effort?: "SMALL" | "MEDIUM" | "LARGE" | string;
  phase: number | string;
  order?: number;
  status?: "PLANNED" | "IN_PROGRESS" | "DONE";
  linkedFindingIds?: string[];
}

export interface ReportSnapshot {
  id: string;
  assessmentId: string;
  version: number;
  status: "DRAFT" | "PUBLISHED";
  published?: boolean;
  contentHash?: string;
  generatedAt: string;
  createdAt?: string;
  publishedAt?: string;
  htmlUrl?: string;
  pdfUrl?: string;
}

export interface AuditEvent {
  id: string;
  assessmentId: string;
  timestamp: string;
  actor: string;
  action: string;
  resourceType?: string;
  resourceId?: string;
  details?: string;
}

export interface OrganizationSummary {
  id: string;
  name: string;
  assessmentCount: number;
  activeAssessments?: number;
  findingCount?: number;
}

export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface ApiError {
  status: number;
  message: string;
  details?: string;
}
