import type {
  ControlledFileCategoryReviewMatrixRuleVO,
  ControlledFileCategoryReviewMatrixUserLookupSourceVO
} from '@/api/dcc/controlledFile/fileCategories'

export const groupReviewSources = (sources: ControlledFileCategoryReviewMatrixUserLookupSourceVO[] = []) => {
  const groups = new Map<string, { label: string; sources: ControlledFileCategoryReviewMatrixUserLookupSourceVO[] }>()
  for (const source of sources) {
    const key = JSON.stringify([source.stageNo, source.stageName])
    let group = groups.get(key)
    if (!group) {
      group = {
        label: source.stageNo
          ? `第 ${source.stageNo} 阶段：${source.stageName || '阶段名称未记录'}`
          : source.stageName || '其他查阅来源',
        sources: []
      }
      groups.set(key, group)
    }
    group.sources.push(source)
  }
  return [...groups.values()]
}

export const activeStageRules = (
  rules: ControlledFileCategoryReviewMatrixRuleVO[] = [],
  stageType: 'SIGNOFF' | 'APPROVAL'
) => rules.filter((rule) => rule.stageType === stageType && rule.active !== false)

export const ruleSubjectLabel = (rule: ControlledFileCategoryReviewMatrixRuleVO) =>
  rule.subjectLabel || rule.subjectName || rule.subjectDepartmentPath || '规则主体未记录'

export const ruleParticipationLabel = (marker?: string) => {
  if (!marker) return '参与条件未设置'
  if (marker === '●') return '必须参与'
  if (marker === '◎') return '可选参与'
  if (marker === '▲') return '沿用原标记（▲）'
  return `参与条件无法识别（${marker}）`
}

export const formatStageRuleSummary = (
  rules: ControlledFileCategoryReviewMatrixRuleVO[] = [],
  stageType: 'SIGNOFF' | 'APPROVAL'
) => activeStageRules(rules, stageType)
  .map((rule) => `${ruleSubjectLabel(rule)}：${ruleParticipationLabel(rule.marker)}`)
  .join('；') || '未配置规则'
