import type { FrontlinePqcInspectionRuleKey, FrontlinePqcProcessVO } from '@/api/mes/pro/feedback'
import { getPqcProcessProgress } from './frontlinePqcProgress'

export function getPqcInspectionProgressSummary(processes: FrontlinePqcProcessVO[]) {
  const progress = (ruleKey: FrontlinePqcInspectionRuleKey, label: string, roundNo?: number) => {
    const counts = getPqcProcessProgress(processes, ruleKey, roundNo)
    return {
      key: roundNo === undefined ? ruleKey : `${ruleKey}-${roundNo}`,
      label,
      ...counts,
      completed: counts.totalCount > 0 && counts.submittedCount === counts.totalCount
    }
  }
  const patrolRounds = [...new Set(processes.flatMap((process) => process.pqcTaskOptions
    .filter((task) => task.inspectionRuleKey === 'PATROL' && task.taskStatus !== 'CANCELLED')
    .map((task) => task.roundNo)))].sort((a, b) => a - b)
  return [
    progress('FIRST', '首检'),
    ...patrolRounds.map((round) => progress('PATROL', `巡检 ${round}`, round)),
    progress('FINAL', '末检')
  ].filter((stage) => stage.totalCount > 0)
}
