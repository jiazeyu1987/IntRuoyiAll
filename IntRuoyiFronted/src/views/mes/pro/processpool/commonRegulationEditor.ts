import type {
  QaInspectionRegulationPublishedVersionVO,
  QaInspectionRegulationVersionOptionVO
} from '@/api/mes/qc/template'
import { nextVersion, type Draft } from '@/views/form-center/parser/simplePqc'

import { validateCommonDraft } from '@/views/form-center/simple-common-parser/draft'

export function createCommonEditorDraft(
  version: QaInspectionRegulationPublishedVersionVO,
  versions: QaInspectionRegulationVersionOptionVO[]
): Draft {
  const copy = JSON.parse(JSON.stringify(version)) as QaInspectionRegulationPublishedVersionVO
  return {
    ownerModule: 'MES_QA_COMMON',
    regulationId: copy.regulationId,
    regulationCode: copy.regulationCode,
    regulationName: copy.regulationName,
    versionNo:
      copy.lifecycleStatus === 'DRAFT'
        ? copy.versionNo
        : nextVersion(
            copy.versionNo,
            versions.map((option) => option.versionNo)
          ),
    expectedDraftRevision: copy.lifecycleStatus === 'DRAFT' ? copy.draftRevision : 0,
    effectiveDate: copy.effectiveDate,
    finalInspectionApplicable: copy.finalInspectionApplicable,
    finalInspectionNotApplicableReason: copy.finalInspectionNotApplicableReason,
    inspectionTypeRules: copy.inspectionTypeRules,
    processes: copy.processes
  }
}

export function commonEditorIssues(draft: Draft): string[] {
  const issues = validateCommonDraft(draft).map((issue) => issue.message)
  if (!draft.versionNo.trim()) issues.unshift('请填写新版本号')
  const processCodes = new Set<string>()
  const itemCodes = new Set<string>()
  for (const process of draft.processes) {
    if (!process.processCode.trim() || processCodes.has(process.processCode.trim()))
      issues.push('工序编码不能为空或重复')
    processCodes.add(process.processCode.trim())
    for (const item of process.items) {
      if (!item.itemCode.trim() || itemCodes.has(item.itemCode.trim()))
        issues.push('检验项目编码不能为空或重复')
      itemCodes.add(item.itemCode.trim())
    }
  }
  if (
    draft.finalInspectionApplicable &&
    !draft.processes.some((process) =>
      process.items.some((item) => item.applicableInspectionTypes.includes('FINAL'))
    )
  )
    issues.push('末检适用时至少需要一个末检项目')
  return [...new Set(issues)]
}

export function normalizeCommonEditorDraft(draft: Draft): Draft {
  const copy = JSON.parse(JSON.stringify(draft)) as Draft
  copy.regulationName = copy.regulationName.trim()
  copy.versionNo = copy.versionNo.trim()
  copy.processes.forEach((process, processIndex) => {
    process.sort = processIndex + 1
    process.items.forEach((item, itemIndex) => {
      item.itemSort = itemIndex + 1
      if (!item.applicableInspectionTypes.includes('FIRST'))
        item.firstInspectionQuantity = undefined
      if (!item.applicableInspectionTypes.includes('PATROL')) item.inspectionLevel = undefined
    })
  })
  copy.inspectionTypeRules.forEach((rule) => {
    if (rule.key === 'FINAL') {
      rule.required = copy.finalInspectionApplicable
      rule.notApplicableReason = copy.finalInspectionApplicable
        ? undefined
        : copy.finalInspectionNotApplicableReason
    }
  })
  if (copy.finalInspectionApplicable) copy.finalInspectionNotApplicableReason = undefined
  return copy
}
