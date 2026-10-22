import request from '@/config/axios'
import type { SignoffAssignment, WorkflowId, WorkflowDistributionScope, WorkflowRecipientOption,
  PendingWorkflowDistributionFile } from '@/views/dcc/controlled-file/workflow/workflow-actions'
export type { WorkflowDistributionScope } from '@/views/dcc/controlled-file/workflow/workflow-actions'

export const assignWorkflowSignoff = (fileId: WorkflowId, payload: SignoffAssignment): Promise<boolean> =>
  request.post<boolean>({ url: `/dcc/controlled-file/workflow-lifecycle/${fileId}/assign-signoff`, data: payload })

export const distributeControlledWorkflow = (fileId: WorkflowId, scopes: WorkflowDistributionScope[]): Promise<boolean> =>
  request.post<boolean>({ url: `/dcc/controlled-file/workflow-lifecycle/${fileId}/distribute`, data: { scopes } })

export const getPendingWorkflowDistribution = (remindersOnly: boolean): Promise<PendingWorkflowDistributionFile[]> =>
  request.get({ url: '/dcc/controlled-file/workflow-lifecycle/pending-distribution', params: { remindersOnly }, ignoreErrorMessage: true })

export const getWorkflowDistributionRecipients = (fileId: WorkflowId, departmentId: WorkflowId): Promise<WorkflowRecipientOption[]> =>
  request.get({ url: `/dcc/controlled-file/workflow-lifecycle/${fileId}/distribution-recipient-options`, params: { departmentId } })
