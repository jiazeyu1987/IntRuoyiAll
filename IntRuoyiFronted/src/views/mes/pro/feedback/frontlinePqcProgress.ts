import type { FrontlinePqcInspectionRuleKey, FrontlinePqcProcessVO } from '@/api/mes/pro/feedback'

export function getPqcProcessProgress(
  processes: FrontlinePqcProcessVO[],
  ruleKey: FrontlinePqcInspectionRuleKey,
  selectedPatrolRound?: number
) {
  const patrolRound =
    selectedPatrolRound ??
    Math.max(
      0,
      ...processes.flatMap((process) =>
        process.pqcTaskOptions
          .filter((task) => task.inspectionRuleKey === 'PATROL' && task.taskStatus !== 'CANCELLED')
          .map((task) => task.roundNo)
      )
    )
  let submittedCount = 0
  let totalCount = 0
  for (const process of processes) {
    const tasks = process.pqcTaskOptions.filter(
      (task) =>
        task.inspectionRuleKey === ruleKey && (ruleKey !== 'PATROL' || task.roundNo === patrolRound)
    )
    const applicable =
      tasks.length > 0 ||
      (ruleKey !== 'FIRST' &&
        process.inspectionTypeRules.some((rule) => rule.key === ruleKey && rule.required) &&
        process.inspectionItems.some((item) => item.applicableInspectionTypes.includes(ruleKey)))
    if (!applicable) continue
    totalCount++
    // A process can contain several item tasks; it counts only after all are submitted.
    if (
      tasks.length > 0 &&
      tasks.every((task) => task.taskStatus === 'SUBMITTED' || task.taskStatus === 'CONFIRMED')
    )
      submittedCount++
  }
  return { submittedCount, totalCount }
}
