/* API types are generated from openapi.json by `npm run generate`. */
import type { components } from "./schema";
type DeepRequired<T> = T extends readonly (infer Item)[]
  ? DeepRequired<Item>[]
  : T extends object
    ? { [Key in keyof T]-?: DeepRequired<T[Key]> }
    : T;
export type UserView = DeepRequired<components["schemas"]["UserView"]>;
export type AuthResponse = DeepRequired<components["schemas"]["AuthResponse"]>;
export type OrganizationMembership = DeepRequired<
  components["schemas"]["OrganizationMembershipResponse"]
>;
export type CurrentUser = DeepRequired<components["schemas"]["CurrentUserResponse"]>;
export type IncidentView = DeepRequired<components["schemas"]["IncidentView"]>;
export type IncidentPage = DeepRequired<components["schemas"]["IncidentPage"]>;
export type IncidentDetail = DeepRequired<components["schemas"]["IncidentDetail"]>;
export type EvidenceView = DeepRequired<components["schemas"]["EvidenceView"]>;
export type CommentView = DeepRequired<components["schemas"]["CommentView"]>;
export type ExecutionView = DeepRequired<components["schemas"]["ExecutionView"]>;
export type StepView = DeepRequired<components["schemas"]["StepView"]>;
export type EventView = DeepRequired<components["schemas"]["EventView"]>;
export type ExecutionTimeline = DeepRequired<components["schemas"]["ExecutionTimeline"]>;
export type AnalysisView = DeepRequired<components["schemas"]["AnalysisView"]>;
export type ProviderConfigView = DeepRequired<components["schemas"]["ProviderConfigView"]>;
export type RunbookSummary = DeepRequired<components["schemas"]["RunbookSummary"]>;
export type RunbookDetail = DeepRequired<components["schemas"]["RunbookDetail"]>;
export type RunbookStepView = DeepRequired<components["schemas"]["RunbookStepView"]>;
export type PolicyView = DeepRequired<components["schemas"]["PolicyView"]>;
export type PolicyPreview = DeepRequired<components["schemas"]["PolicyPreview"]>;
export type ApprovalView = DeepRequired<components["schemas"]["ApprovalView"]>;
export type AuthorizationResult = DeepRequired<components["schemas"]["AuthorizationResult"]>;
export type ProviderRequest = DeepRequired<components["schemas"]["ProviderRequest"]>;
export type StepsRequest = DeepRequired<components["schemas"]["StepsRequest"]>;
export type RuleRequest = DeepRequired<components["schemas"]["RuleRequest"]>;
export type DecisionRequest = DeepRequired<components["schemas"]["DecisionRequest"]>;
export type IntegrationView = DeepRequired<components["schemas"]["IntegrationView"]>;
export type IntegrationSetupRequest = DeepRequired<components["schemas"]["SetupRequest"]>;
export type IntegrationUsageView = DeepRequired<components["schemas"]["UsageView"]>;
export type PostmortemView = DeepRequired<components["schemas"]["PostmortemView"]>;
export type OperationsOverview = DeepRequired<components["schemas"]["OperationsOverview"]>;
export type AuditPage = DeepRequired<components["schemas"]["AuditPage"]>;
export type AuditView = DeepRequired<components["schemas"]["AuditView"]>;
export type DeadLetterView = DeepRequired<components["schemas"]["DeadLetterView"]>;
export type ErrorResponse = DeepRequired<components["schemas"]["ErrorResponse"]>;
