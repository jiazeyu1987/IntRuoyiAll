export interface SupervisorHandoverContext {
  assigneeUserId: number | string
  assigneeName: string
  applicantUserId?: string
  suggestedAssigneeUserId?: string
  reason: string
  notice: string
}
