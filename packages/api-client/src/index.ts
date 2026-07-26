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
export type ErrorResponse = DeepRequired<components["schemas"]["ErrorResponse"]>;
