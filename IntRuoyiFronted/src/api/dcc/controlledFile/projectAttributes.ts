import request from '@/config/axios'
import type { ApplicationType, ProjectAttributes, ApplicationAttributesRow } from '@/views/dcc/controlled-file/project-attributes/state'
import { buildProjectFolderTree } from '@/views/dcc/controlled-file/basic-data/components/project-folder-tree'
import { projectConfigurationIdentity } from '@/views/dcc/controlled-file/basic-data/components/project-attribute-configuration'
import { validateAttributes } from '@/views/dcc/controlled-file/project-attributes/state'
export const getProjectDefaults = (projectId: string | number, applicationType: ApplicationType): Promise<ProjectAttributes> =>
  request.get({ url: `/dcc/project-codes/${projectId}/attributes/defaults`, params: { applicationType }, ignoreErrorMessage: true })
export const configureProjectAttributes = async (
  projectId: string | number,
  data: { projectLeaderUserId: string | number; defaultAttributes: ProjectAttributes; changeReason: string }
): Promise<boolean> => {
  const project = projectConfigurationIdentity(projectId), leader = projectConfigurationIdentity(data?.projectLeaderUserId)
  if (typeof data.changeReason !== 'string' || !data.changeReason.trim() || data.changeReason.length > 500)
    throw new Error('请填写500字以内的项目属性配置原因')
  const saved = await request.put<boolean>({
    url: `/dcc/project-codes/${project}/attributes/configuration`,
    data: { projectLeaderUserId: leader, defaultAttributes: JSON.parse(JSON.stringify(validateAttributes(data.defaultAttributes))), changeReason: data.changeReason.trim() },
    ignoreErrorMessage: true
  })
  if (saved !== true) throw new Error('项目默认属性与负责人保存未确认成功，请到原项目记录核对')
  return true
}
export interface FolderNode { key: string; parentKey?: string | null; name: string; sortOrder: number }
export interface FolderStructure { nodes: FolderNode[] }
export interface FolderTemplate {
  id: number; name: string; description?: string; active: boolean
  structureJson: string; editedByUserId: number; everUsed: boolean; updateTime?: string
}
export interface ProjectFolder {
  id: number | string; projectCodeId: number | string; parentId: number | string; name: string; sortOrder: number
  active: boolean; sourceTemplateId?: number | string | null; sourceNodeKey?: string | null
}
export interface ProjectFolderSave {
  id?: number | string; parentId: number | string; name: string; sortOrder: number; changeReason: string
}
export const saveProjectFolder = (projectId: number | string, data: ProjectFolderSave): Promise<ProjectFolder> =>
  request.put({ url: `/dcc/project-codes/${projectId}/folders`, data, ignoreErrorMessage: true })
export const deleteProjectFolder = (projectId: number | string, folderId: number | string, data: { confirmed: boolean; changeReason: string }): Promise<boolean> =>
  request.delete({ url: `/dcc/project-codes/${projectId}/folders/${folderId}`, data, ignoreErrorMessage: true })
export const getFolderTemplates = (): Promise<FolderTemplate[]> => request.get({ url: '/dcc/folder-templates', ignoreErrorMessage: true })
export const saveFolderTemplate = (data: { id?: number; name: string; description?: string; active: boolean; structure: FolderStructure; changeReason: string }): Promise<number> =>
  request.put({ url: '/dcc/folder-templates', data, ignoreErrorMessage: true })
export const deleteFolderTemplate = (id: number, reason: string): Promise<boolean> => request.delete({ url: `/dcc/folder-templates/${id}`, params: { reason }, ignoreErrorMessage: true })
const folderIdentity = (value: string | number, allowRoot = false): string => {
  if (typeof value === 'number' && !Number.isSafeInteger(value)) throw new Error('项目目录ID超出安全整数范围')
  const id = String(value)
  if (!(allowRoot ? /^(0|[1-9][0-9]*)$/ : /^[1-9][0-9]*$/).test(id)) throw new Error('项目目录ID不合法')
  return id
}
/** VIEW/EDIT/OWNER的正式只读目录；不从NAS、文件列表或缓存补空目录。 */
export const getProjectFolders = async (projectId: string | number): Promise<ProjectFolder[]> => {
  const project = folderIdentity(projectId)
  const response = await request.get<ProjectFolder[]>({ url: `/dcc/project-codes/${project}/folders`, ignoreErrorMessage: true })
  if (!Array.isArray(response)) throw new Error('项目目录响应缺失')
  const rows = response.map(row => ({
    ...row,
    id: folderIdentity(row.id),
    projectCodeId: folderIdentity(row.projectCodeId),
    parentId: folderIdentity(row.parentId, true),
    sourceTemplateId: row.sourceTemplateId == null ? row.sourceTemplateId : folderIdentity(row.sourceTemplateId)
  }))
  buildProjectFolderTree(project, rows) // 复用唯一结构校验：项目、重复、父级、循环、排序及状态。
  return rows
}

/** 项目逻辑目录与旧NAS存储目录不同，不按数字相等建立映射。 */
export interface ProjectFilePlacement {
  id: number | string
  projectCodeId: number | string
  projectFolderId: number | string
  controlledFileId: number | string
  storageDirectoryId: number | string
}
export const getProjectFilePlacements = (projectId: number | string, projectFolderId: number | string): Promise<ProjectFilePlacement[]> =>
  request.get({ url: `/dcc/project-codes/${projectId}/folders/${projectFolderId}/file-placements`, ignoreErrorMessage: true })

export const getApplicationAttributes = (projectId: number | string, applicationId: number | string, applicationType: ApplicationType, bpmRound: string): Promise<ApplicationAttributesRow> =>
  request.get({ url: `/dcc/project-codes/${projectId}/applications/${applicationId}/attributes`, params: { applicationType, bpmRound }, ignoreErrorMessage: true })
