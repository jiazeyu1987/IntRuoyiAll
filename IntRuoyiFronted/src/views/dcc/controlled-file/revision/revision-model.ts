import { validateAttributes } from '../project-attributes/state'
import type { ProjectAttributes } from '../project-attributes/state'

export type RevisionIntent = 'PARTIAL' | 'REPLACEMENT'
export type RevisionId = string | number
export interface RevisionTarget { versionNo: string | null; unavailableReason: string | null }
export interface RevisionApplicationFacts {
  projectAttributes: ProjectAttributes
  selectedSignoffDepartmentIds: RevisionId[]
  needTraining: boolean
  effectiveDate: string
}
export interface RevisionFacts {
  revisionChangeType?: string | null
  revisionSourceVersionNo?: string | null
  selectedIterationVersionNo?: string | null
  changeDescription?: string | null
}

export const revisionChangeLabel = (type?: string | null): string => {
  switch (type) {
    case 'INITIAL': return '初始上传'
    case 'PARTIAL': return '局部变更'
    case 'REPLACEMENT': return '换版变更'
    default: return '未记录'
  }
}

export const requireRevisionId = (id: RevisionId): string => {
  const text = String(id)
  if (!/^[1-9][0-9]*$/.test(text) || (typeof id === 'number' && !Number.isSafeInteger(id))) {
    throw new Error('文件版本身份无效')
  }
  return text
}

export const revisionCheckoutLabel = (actorId?: RevisionId, actorName?: string, checkedOutTime?: string): string => {
  if (actorId == null && !checkedOutTime) return ''
  if (actorId == null) throw new Error('缺少实际检出账号')
  const id = requireRevisionId(actorId)
  return `检出账号：${id}；检出人：${actorName || '未记录'}；检出时间：${checkedOutTime || '未记录'}`
}

const requireRevisionDate = (value: string): string => {
  if (typeof value !== 'string' || !/^\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])$/.test(value)) {
    throw new Error('请选择本次申请的预设生效日期')
  }
  const [year, month, day] = value.split('-').map(Number)
  const leapYear = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0)
  const days = [31, leapYear ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  if (year === 0 || day > days[month - 1]) throw new Error('预设生效日期不是有效日历日期')
  return value
}

const buildApplicationFields = (changeDescription: string, idempotencyKey: string,
  application: RevisionApplicationFacts) => {
  if (!changeDescription.trim() || !idempotencyKey.trim()) throw new Error('变更说明和提交标识不能为空')
  if (!application || typeof application.needTraining !== 'boolean' || !Array.isArray(application.selectedSignoffDepartmentIds)) {
    throw new Error('正式申请属性、会签部门和培训选择尚未就绪')
  }
  const actual = validateAttributes(application.projectAttributes)
  const effectiveDate = requireRevisionDate(application.effectiveDate)
  const departments = application.selectedSignoffDepartmentIds.map(requireRevisionId)
  if (new Set(departments).size !== departments.length) throw new Error('会签部门不能重复')
  return {
    changeDescription: changeDescription.trim(), idempotencyKey: idempotencyKey.trim(),
    projectAttributes: {
      targetMarkets: [...actual.targetMarkets], otherMarket: actual.otherMarket,
      licenseHolder: actual.licenseHolder, actualManufacturer: actual.actualManufacturer,
      documentTransfer: actual.documentTransfer, transferTo: actual.transferTo
    },
    selectedSignoffDepartmentIds: departments,
    needTraining: application.needTraining,
    effectiveDate
  }
}

export const buildInitialCommand = (changeDescription: string, idempotencyKey: string,
  application: RevisionApplicationFacts) => ({
  ...buildApplicationFields(changeDescription, idempotencyKey, application),
  revisionChangeType: 'INITIAL' as const
})

export const buildRevisionCommand = (baselineId: RevisionId, selectedIterationId: RevisionId,
  revisionChangeType: RevisionIntent, changeDescription: string, idempotencyKey: string,
  application: RevisionApplicationFacts) => {
  if (!['PARTIAL', 'REPLACEMENT'].includes(revisionChangeType)) throw new Error('请选择实际变更类型')
  return {
    ...buildApplicationFields(changeDescription, idempotencyKey, application),
    controlledBaselineId: requireRevisionId(baselineId),
    selectedIterationId: requireRevisionId(selectedIterationId),
    revisionChangeType
  }
}
export type RevisionCommand = ReturnType<typeof buildRevisionCommand>
