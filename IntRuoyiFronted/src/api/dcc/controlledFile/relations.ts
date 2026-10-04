import request from '@/config/axios'
import { loadDccSelectorPage } from './applicationRead'
import type { SelectorQuery } from '@/views/dcc/controlled-file/relations/selector-state'
import { validateReferenceUsagePage } from '@/views/dcc/controlled-file/relations/reference-usage'
import type { ReferenceUsageContext, ReferenceUsagePage } from '@/views/dcc/controlled-file/relations/reference-usage'
import { referenceIdentity } from '@/views/dcc/controlled-file/relations/project-reference-contract'
// D independent wrappers. Shared workflow types and pages remain owned by management.
export const loadSelectorBrowserPage = async (tenantId: string, query: SelectorQuery) => {
  return loadDccSelectorPage(tenantId, query)
}
export const listProjectReferences = (projectId: string, folderId: string) => request.get({ url: '/dcc/project-file-references', params: { projectId, folderId } })
export const getProjectReferenceUsage = (selectedFileId: string) => request.get<{ masterId: string; referenceProjectCount: number; referenced: boolean }>({ url: '/dcc/project-file-references/usage', params: { selectedFileId } })
export const getProjectReferenceUsagePage = async (context: ReferenceUsageContext, pageNo: number, pageSize: number): Promise<ReferenceUsagePage> => {
  const selectedFileId = referenceIdentity(context.sourceControlledFileId)
  referenceIdentity(context.tenantId); referenceIdentity(context.masterId)
  if (!Number.isSafeInteger(pageNo) || pageNo < 1 || !Number.isSafeInteger(pageSize) || pageSize < 1 || pageSize > 200)
    throw new Error('引用使用明细分页无效')
  const response = await request.get({ url: '/dcc/project-file-references/usage-page', params: { selectedFileId, pageNo, pageSize }, ignoreErrorMessage: true })
  return validateReferenceUsagePage(response, context, pageNo, pageSize)
}
export const createProjectReference = (projectId: string, folderId: string, selectedFileId: string, reason: string) => request.post({ url: '/dcc/project-file-references', data: { projectId, folderId, selectedFileId, reason }, ignoreErrorMessage: true })
export const createProjectReferences = (projectId: string, folderId: string, selectedFileIds: string[], reason: string) => request.post({ url: '/dcc/project-file-references/batch', data: { projectId, folderId, selectedFileIds, reason }, ignoreErrorMessage: true })
export const cancelProjectReference = (projectId: string, folderId: string, masterId: string, referenceId: string, reason: string) => request.post({ url: '/dcc/project-file-references/cancel', data: { projectId, folderId, masterId, referenceId, confirmed: true, reason }, ignoreErrorMessage: true })
export const listCurrentRelations = (sourceFileId: string) => request.get({ url: `/dcc/file-relations/${sourceFileId}/current` })
export const listHistoricalRelations = (sourceFileId: string) => request.get({ url: `/dcc/file-relations/${sourceFileId}/history` })
export const replaceCurrentRelations = (sourceFileId: string, selectedFileIds: string[], expectedMasterIds: string[], expectedVersion: string, idempotencyKey: string, reason: string) => request.put<{ sourceControlledFileId: string; rowVersion: string; relatedMasterIds: string[] }>({ url: `/dcc/file-relations/${sourceFileId}`, data: { selectedFileIds, expectedMasterIds, expectedVersion, idempotencyKey, reason }, ignoreErrorMessage: true })
export const requestRelationNotificationRetry = (eventId: string, reason: string) => request.post({ url: `/dcc/relation-remediation/events/${eventId}/retry-notifications`, data: { reason }, ignoreErrorMessage: true })
export const listRelationNotificationStatus = (eventId: string) => request.get({ url: `/dcc/relation-remediation/events/${eventId}/notifications` })
export const listRelationArrangements = (sourceFileId: string, applicationRound: string) => request.get<{ relatedMasterId: string; assigneeUserId: string; dueAt: string }[]>({ url: '/dcc/relation-remediation/arrangements', params: { sourceFileId, applicationRound } })
