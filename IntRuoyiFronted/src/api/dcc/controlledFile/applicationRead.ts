import request from '@/config/axios'
import type {
  ProjectAttributes,
  AttributeSnapshot
} from '@/views/dcc/controlled-file/project-attributes/state'
import { validateAttributes } from '@/views/dcc/controlled-file/project-attributes/state'
import type {
  FileCandidate,
  SelectorPage,
  SelectorQuery
} from '@/views/dcc/controlled-file/relations/selector-state'
import type { RevisionTarget } from '@/views/dcc/controlled-file/revision/revision-model'

type Identity = number | string
export type DccApplicationType = 'UPLOAD' | 'REVISION' | 'OBSOLETE'
export interface ControlledFileApplicationRound {
  controlledFileId: string
  applicationType: DccApplicationType
  bpmRound: string
  attributeRound: number
}
export interface ControlledFileRelationPermissions {
  controlledFileId: string
  tenantId: string
  masterId: string
  projectId: string
  projectName: string
  projectFolderId: string | null
  projectFolderName: string | null
  fileNumber: string
  fileName: string
  versionNo: string
  status: string
  controlled: boolean
  pendingEffect: boolean
  executable: boolean
  canEdit: boolean
  canPreview: boolean
}
export interface WorkingApplicationAttributes extends AttributeSnapshot {
  controlledFileId: string
  effectiveDate: string | null
  needTraining: boolean | null
  selectedSignoffDepartmentIds: string[] | null
  changeDescription: string | null
  canSubmit: boolean
  unavailableReason: string | null
}
export interface SignoffAssignmentContext {
  controlledFileId: string
  processInstanceId: string
  taskId: string
  processDefinitionKey: 'dcc-controlled-file-upload' | 'dcc-controlled-file-revision' | 'dcc-controlled-file-obsolete'
  obligationId: string
  departmentId: string
  departmentName: string | null
  assigneeUserId: string | null
  assigned: boolean
  canAssign: boolean
  assigneeOptions: Array<{ id: string; name: string }>
}
export interface ControlledFileRevisionOptions {
  controlledBaselineId: Identity
  baselineVersionNo: string
  masterId: Identity
  sourceOriginalFileName: string
  partialTarget: RevisionTarget
  replacementTarget: RevisionTarget
  checkedOutBy: Identity | null
  checkedOutByName: string | null
  checkedOutTime: string | number | null
  lockedReason: string | null
  iterations: Array<{
    id: Identity
    versionNo: string
    canPartial: boolean
    canReplacement: boolean
    canPreview: boolean
  }>
}
export interface ControlledFileApplicationEvidence {
  controlledFileId: Identity
  versionNo: string
  applicationType: DccApplicationType
  bpmRound: string
  attributeRound: number | null
  recorded: boolean
  unavailableReason: 'NOT_RECORDED' | 'NOT_FROZEN' | null
  defaultSource: ProjectAttributes | null
  actualAttributes: ProjectAttributes | null
  signatures: Array<{
    id?: Identity
    processInstanceId: string
    actorNicknameSnapshot?: string | null
    actionType?: string | null
    actorDeptNameSnapshot?: string | null
    signedAt?: string | number | readonly number[] | null
  }>
}
interface SelectorRow {
  tenantId: Identity
  id: Identity
  masterId: Identity
  latestControlledFileId: Identity
  dccProjectCodeId: Identity
  projectName: string
  projectFolderId?: Identity | null
  projectFolderName?: string | null
  sourceOriginalFileName: string
  fileNumber: string
  versionNo: string
  status: string
  controlled: boolean
  pendingEffect: boolean
  executable: boolean
  canPreview: boolean
}
const identity = (value: Identity): string => {
  const text = String(value)
  if (
    (typeof value === 'number' && !Number.isSafeInteger(value)) ||
    !/^[1-9][0-9]*$/.test(text) ||
    BigInt(text) > 9223372036854775807n
  )
    throw new Error('文件或项目身份无效')
  return text
}

type WorkingApplicationResponse = Omit<WorkingApplicationAttributes, 'effectiveDate'> & {
  effectiveDate: string | [number, number, number] | null
}
const applicationDate = (value: WorkingApplicationResponse['effectiveDate']): string | null => {
  if (value === null) return null
  const date =
    Array.isArray(value) && value.length === 3 && value.every(Number.isInteger)
      ? `${String(value[0]).padStart(4, '0')}-${String(value[1]).padStart(2, '0')}-${String(value[2]).padStart(2, '0')}`
      : value
  if (typeof date !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(date))
    throw new Error('草稿生效日期格式无效')
  const [year, month, day] = date.split('-').map(Number)
  const calendar = new Date(0)
  calendar.setUTCFullYear(year, month - 1, day)
  if (
    year < 1 ||
    calendar.getUTCFullYear() !== year ||
    calendar.getUTCMonth() !== month - 1 ||
    calendar.getUTCDate() !== day
  )
    throw new Error('草稿生效日期不是有效日历日期')
  return date
}
const validateWorkingAttributes = (
  value: WorkingApplicationResponse
): WorkingApplicationAttributes => {
  if (
    !value ||
    !['UPLOAD', 'REVISION'].includes(value.applicationType) ||
    (value.needTraining !== null && typeof value.needTraining !== 'boolean') ||
    typeof value.canSubmit !== 'boolean' ||
    (value.selectedSignoffDepartmentIds !== null &&
      !Array.isArray(value.selectedSignoffDepartmentIds))
  )
    throw new Error('草稿申请准备信息不完整')
  identity(value.controlledFileId)
  identity(value.projectId)
  validateAttributes(value.defaultSource)
  validateAttributes(value.actual)
  value.selectedSignoffDepartmentIds?.forEach(identity)
  return { ...value, effectiveDate: applicationDate(value.effectiveDate) }
}
export const getWorkingApplicationAttributes = async (
  fileId: Identity
): Promise<WorkingApplicationAttributes> => {
  const id = identity(fileId)
  const value = validateWorkingAttributes(
    await request.get({ url: `/dcc/controlled-files/${id}/working-attributes` })
  )
  if (identity(value.controlledFileId) !== id) throw new Error('草稿申请响应与所选文件不一致')
  return value
}

export const restoreWorkingApplicationAttributes = async (
  fileId: Identity
): Promise<WorkingApplicationAttributes> => {
  const id = identity(fileId)
  const saved = await request.post({
    url: `/dcc/controlled-files/${id}/working-attributes/restore`,
    ignoreErrorMessage: true
  })
  if (saved !== true) throw new Error('恢复原始默认属性未确认成功，请刷新核对')
  return getWorkingApplicationAttributes(id)
}

export const getSignoffAssignmentContext = async (
  fileId: Identity,
  taskId: string
): Promise<SignoffAssignmentContext> => {
  const id = identity(fileId)
  if (!taskId || taskId !== taskId.trim()) throw new Error('请指定真实会签任务')
  const result = await request.get<SignoffAssignmentContext>({
    url: `/dcc/controlled-file/workflow-lifecycle/${id}/signoff-assignment-context`,
    params: { taskId }
  })
  if (
    !result ||
    identity(result.controlledFileId) !== id ||
    result.taskId !== taskId ||
    !result.processInstanceId ||
    !['dcc-controlled-file-upload', 'dcc-controlled-file-revision', 'dcc-controlled-file-obsolete'].includes(result.processDefinitionKey) ||
    !result.obligationId ||
    typeof result.assigned !== 'boolean' ||
    typeof result.canAssign !== 'boolean' ||
    !Array.isArray(result.assigneeOptions)
  )
    throw new Error('会签指派响应身份或资格事实缺失')
  identity(result.departmentId)
  result.assigneeOptions.forEach((user) => {
    identity(user.id)
    if (!user.name) throw new Error('会签候选人名称缺失')
  })
  return result
}

export const getControlledFileRevisionOptions = async (
  baselineId: Identity
): Promise<ControlledFileRevisionOptions> => {
  const id = identity(baselineId)
  const result = await request.get<ControlledFileRevisionOptions>({
    url: `/dcc/controlled-files/${id}/revision-options`
  })
  if (!result || identity(result.controlledBaselineId) !== id)
    throw new Error('升版准备响应与所选受控版本不一致')
  return result
}

export const saveWorkingApplicationAttributes = async (
  fileId: Identity,
  actual: ProjectAttributes
): Promise<boolean> => {
  const id = identity(fileId)
  const data: ProjectAttributes = JSON.parse(JSON.stringify(validateAttributes(actual)))
  const saved = await request.post<boolean>({
    url: `/dcc/controlled-files/${id}/working-attributes`,
    data,
    ignoreErrorMessage: true
  })
  if (saved !== true) throw new Error('草稿属性保存未确认成功，请刷新后核对')
  return true
}

export const getControlledFileApplicationEvidence = async (
  fileId: Identity,
  applicationType: DccApplicationType,
  bpmRound: string
): Promise<ControlledFileApplicationEvidence> => {
  const id = identity(fileId)
  if (
    !['UPLOAD', 'REVISION', 'OBSOLETE'].includes(applicationType) ||
    !bpmRound ||
    bpmRound !== bpmRound.trim()
  ) {
    throw new Error('请指定文件申请类型及真实流程轮次')
  }
  const result = await request.get<ControlledFileApplicationEvidence>({
    url: `/dcc/controlled-files/${id}/application-evidence`,
    params: { applicationType, bpmRound }
  })
  if (
    !result ||
    identity(result.controlledFileId) !== id ||
    result.applicationType !== applicationType ||
    result.bpmRound !== bpmRound ||
    typeof result.recorded !== 'boolean' ||
    !Array.isArray(result.signatures)
  ) {
    throw new Error('申请证据响应与所选文件或流程轮次不一致')
  }
  if (
    result.recorded
      ? !result.defaultSource ||
        !result.actualAttributes ||
        !Number.isInteger(result.attributeRound) ||
        (result.attributeRound || 0) < 1
      : !['NOT_RECORDED', 'NOT_FROZEN'].includes(result.unavailableReason || '') ||
        result.actualAttributes != null ||
        result.defaultSource != null
  ) {
    throw new Error('申请证据缺少正式冻结事实')
  }
  if (result.recorded) {
    validateAttributes(result.defaultSource!)
    validateAttributes(result.actualAttributes!)
  }
  if (result.signatures.some(signature => signature.processInstanceId !== bpmRound))
    throw new Error('申请签名证据属于其他流程轮次')
  return result
}

export const getControlledFileReplacementAttributes = async (
  selectedIterationId: Identity,
  controlledBaselineId: Identity
): Promise<WorkingApplicationAttributes> => {
  const id = identity(selectedIterationId), baseline = identity(controlledBaselineId)
  const value = validateWorkingAttributes(await request.get({
    url: `/dcc/controlled-files/${id}/replacement-attributes`,
    params: { controlledBaselineId: baseline },
    ignoreErrorMessage: true
  }))
  if (identity(value.controlledFileId) !== id || value.applicationType !== 'REVISION')
    throw new Error('换版申请响应与所选工作正文不一致')
  return value
}

export const loadDccSelectorPage = async (
  tenantId: Identity,
  query: SelectorQuery
): Promise<SelectorPage> => {
  const tenant = identity(tenantId)
  const directory = query.folderId !== undefined
  if (
    (directory && !query.projectId) ||
    (!directory && query.projectId !== undefined) ||
    !Number.isInteger(query.pageNo) ||
    query.pageNo < 1 ||
    !Number.isInteger(query.pageSize) ||
    query.pageSize < 1 ||
    query.pageSize > 200
  )
    throw new Error('文件选择器查询范围或分页无效')
  const response = await request.get<{ list: SelectorRow[]; total: number }>({
    url: '/dcc/controlled-files/browser-page',
    params: {
      selectorScope: directory ? 'PROJECT_FOLDER' : 'GLOBAL',
      ...(directory
        ? {
            dccProjectCodeId: identity(query.projectId!),
            projectFolderId: identity(query.folderId!)
          }
        : {}),
      keyword: query.keyword,
      pageNo: query.pageNo,
      pageSize: query.pageSize
    }
  })
  if (
    !response ||
    !Array.isArray(response.list) ||
    !Number.isSafeInteger(response.total) ||
    response.total < 0
  ) {
    throw new Error('文件选择器响应分页无效')
  }
  const masters = new Set<string>()
  const list: FileCandidate[] = response.list.map((row) => {
    const id = identity(row.id),
      masterId = identity(row.masterId),
      projectId = identity(row.dccProjectCodeId)
    if (
      identity(row.tenantId) !== tenant ||
      identity(row.latestControlledFileId) !== id ||
      masters.has(masterId) ||
      row.controlled !== true ||
      !['ACTIVE', 'CONTROLLED_PENDING_EFFECTIVE'].includes(row.status) ||
      typeof row.canPreview !== 'boolean' ||
      typeof row.pendingEffect !== 'boolean' ||
      typeof row.executable !== 'boolean' ||
      row.pendingEffect !== (row.status === 'CONTROLLED_PENDING_EFFECTIVE') ||
      (row.pendingEffect && row.executable) ||
      !row.projectName ||
      !row.sourceOriginalFileName ||
      !row.fileNumber ||
      !row.versionNo ||
      (row.projectFolderId != null) !== Boolean(row.projectFolderName) ||
      (directory &&
        (projectId !== query.projectId ||
          row.projectFolderId == null ||
          identity(row.projectFolderId) !== query.folderId))
    ) {
      throw new Error('文件选择器响应包含失配的版本、项目或权限事实')
    }
    masters.add(masterId)
    return {
      tenantId: tenant,
      controlledFileId: id,
      masterId,
      projectId,
      projectName: row.projectName,
      folderName: row.projectFolderName || '未记录',
      fileName: row.sourceOriginalFileName,
      fileNumber: row.fileNumber,
      versionNo: row.versionNo,
      status: row.status,
      controlled: row.controlled,
      pendingEffect: row.pendingEffect,
      executable: row.executable,
      canPreview: row.canPreview
    }
  })
  return { list, total: response.total }
}

export const getControlledFileApplicationRounds = async (
  fileId: Identity
): Promise<ControlledFileApplicationRound[]> => {
  const id = identity(fileId)
  const rows = await request.get<ControlledFileApplicationRound[]>({
    url: `/dcc/controlled-files/${id}/application-rounds`,
    ignoreErrorMessage: true
  })
  if (!Array.isArray(rows)) throw new Error('申请轮次响应缺失')
  const keys = new Set<string>()
  return rows.map((row) => {
    if (
      !row || identity(row.controlledFileId) !== id ||
      !['UPLOAD', 'REVISION', 'OBSOLETE'].includes(row.applicationType) ||
      typeof row.bpmRound !== 'string' || !row.bpmRound || row.bpmRound !== row.bpmRound.trim() ||
      !Number.isSafeInteger(row.attributeRound) || row.attributeRound < 1
    ) throw new Error('申请轮次与所选文件不一致或映射缺失')
    const key = JSON.stringify([row.applicationType, row.bpmRound])
    if (keys.has(key)) throw new Error('申请轮次映射重复')
    keys.add(key)
    return { ...row, controlledFileId: id }
  })
}

export const getControlledFileRelationPermissions = async (
  fileId: Identity
): Promise<ControlledFileRelationPermissions> => {
  const id = identity(fileId)
  const result = await request.get<ControlledFileRelationPermissions>({
    url: `/dcc/controlled-files/${id}/relation-permissions`,
    ignoreErrorMessage: true
  })
  if (!result || identity(result.controlledFileId) !== id ||
      typeof result.canEdit !== 'boolean' || typeof result.canPreview !== 'boolean' ||
      typeof result.projectName !== 'string' || !result.projectName.trim() ||
      typeof result.fileNumber !== 'string' || !result.fileNumber.trim() ||
      typeof result.fileName !== 'string' || !result.fileName ||
      typeof result.versionNo !== 'string' || !result.versionNo || typeof result.status !== 'string' || !result.status ||
      typeof result.controlled !== 'boolean' || typeof result.pendingEffect !== 'boolean' || typeof result.executable !== 'boolean' ||
      result.pendingEffect !== (result.status === 'CONTROLLED_PENDING_EFFECTIVE') ||
      (result.controlled && !['ACTIVE', 'CONTROLLED_PENDING_EFFECTIVE'].includes(result.status)) ||
      (result.pendingEffect && (!result.controlled || result.executable)) ||
      (result.executable && (!result.controlled || result.status !== 'ACTIVE')) ||
      (result.projectFolderId != null) !== Boolean(result.projectFolderName))
    throw new Error('关联权限响应与所选文件不一致或权限事实缺失')
  return { ...result, controlledFileId: id, tenantId: identity(result.tenantId), masterId: identity(result.masterId),
    projectId: identity(result.projectId), projectFolderId: result.projectFolderId == null ? null : identity(result.projectFolderId) }
}

interface ProjectBrowserRow extends Omit<SelectorRow, 'latestControlledFileId'> {
  latestControlledFileId?: Identity | null
  fileName: string
}
export const DCC_PROJECT_BROWSER_STATUSES = [
  'DRAFT', 'WORKING', 'PENDING_DOC_CONTROL_REVIEW', 'PENDING_MATRIX_REVIEW',
  'PENDING_MATRIX_APPROVAL', 'PENDING_DOC_CONTROL_APPROVAL', 'PENDING_APPLICANT_REWORK',
  'PENDING_APPLICANT_TRAINING_RECORD', 'READY_TO_PUBLISH', 'FINALIZING', 'TRAINING_IN_PROGRESS',
  'PENDING_MANUAL_DISTRIBUTION', 'ACTIVE', 'CONTROLLED_PENDING_EFFECTIVE', 'REJECTED',
  'WITHDRAWN', 'OBSOLETE', 'SUPERSEDED', 'FINALIZATION_FAILED', 'SUBMIT_FAILED',
  'APPROVING', 'APPROVED', 'STAMPING', 'STAMP_FAILED', 'STAMPED'
] as const
export type DccProjectBrowserStatus = (typeof DCC_PROJECT_BROWSER_STATUSES)[number]
export interface ProjectBrowserOptions { latestVersionOnly?: boolean; status?: DccProjectBrowserStatus }
const projectBrowserStatuses = new Set<string>(DCC_PROJECT_BROWSER_STATUSES)

export const loadDccProjectBrowserPage = async (
  tenantId: Identity,
  query: SelectorQuery,
  options: ProjectBrowserOptions = {}
): Promise<SelectorPage> => {
  const tenant = identity(tenantId)
  if (!options || (options.latestVersionOnly !== undefined && typeof options.latestVersionOnly !== 'boolean') ||
      (options.status !== undefined && (typeof options.status !== 'string' || !projectBrowserStatuses.has(options.status))))
    throw new Error('项目浏览版本选项或状态无效')
  const latestVersionOnly = options.latestVersionOnly ?? true
  const directory = query.folderId !== undefined
  if (
    (directory && !query.projectId) ||
    (!directory && query.projectId !== undefined) ||
    !Number.isInteger(query.pageNo) || query.pageNo < 1 ||
    !Number.isInteger(query.pageSize) || query.pageSize < 1 || query.pageSize > 200
  ) throw new Error('项目文件查询范围或分页无效')
  const projectId = directory ? identity(query.projectId!) : undefined
  const folderId = directory ? identity(query.folderId!) : undefined
  const response = await request.get<{ list: ProjectBrowserRow[]; total: number }>({
    url: '/dcc/controlled-files/browser-page',
    params: {
      browserScope: directory ? 'PROJECT_FOLDER' : 'GLOBAL',
      ...(directory ? { dccProjectCodeId: projectId, projectFolderId: folderId } : {}),
      latestVersionOnly,
      ...(options.status === undefined ? {} : { status: options.status }),
      keyword: query.keyword, pageNo: query.pageNo, pageSize: query.pageSize
    }
  })
  if (!response || !Array.isArray(response.list) || !Number.isSafeInteger(response.total) || response.total < 0)
    throw new Error('项目文件响应分页无效')
  const versions = new Set<string>()
  const list = response.list.map((row): FileCandidate => {
    const id = identity(row.id), masterId = identity(row.masterId), rowProjectId = identity(row.dccProjectCodeId)
    const rowFolderId = row.projectFolderId == null ? null : identity(row.projectFolderId)
    if (
      identity(row.tenantId) !== tenant || versions.has(id) ||
      typeof row.controlled !== 'boolean' || typeof row.pendingEffect !== 'boolean' ||
      typeof row.executable !== 'boolean' || typeof row.canPreview !== 'boolean' ||
      !row.status || !row.fileName || !row.fileNumber || !row.versionNo || !row.projectName ||
      (rowFolderId !== null) !== Boolean(row.projectFolderName) ||
      row.pendingEffect !== (row.status === 'CONTROLLED_PENDING_EFFECTIVE') ||
      (row.pendingEffect && (!row.controlled || row.executable)) ||
      (row.executable && (!row.controlled || row.status !== 'ACTIVE')) ||
      (options.status !== undefined && row.status !== options.status) ||
      (latestVersionOnly && (row.latestControlledFileId == null || identity(row.latestControlledFileId) !== id || !row.controlled)) ||
      (directory && (rowProjectId !== projectId || rowFolderId !== folderId))
    ) throw new Error('项目文件响应包含失配的版本、项目或权限事实')
    if (row.latestControlledFileId != null) identity(row.latestControlledFileId)
    versions.add(id)
    return {
      tenantId: tenant, controlledFileId: id, masterId, projectId: rowProjectId,
      projectName: row.projectName, projectFolderId: rowFolderId,
      folderName: row.projectFolderName || '未记录',
      fileName: row.fileName, fileNumber: row.fileNumber, versionNo: row.versionNo,
      status: row.status, controlled: row.controlled, pendingEffect: row.pendingEffect,
      executable: row.executable, canPreview: row.canPreview
    }
  })
  return { list, total: response.total }
}
