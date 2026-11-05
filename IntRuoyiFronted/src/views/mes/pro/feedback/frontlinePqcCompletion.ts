import type { FrontlinePqcTaskOptionVO } from '@/api/mes/pro/feedback'

type CompletionTask = Pick<FrontlinePqcTaskOptionVO, 'inspectionType' | 'roundNo' | 'taskStatus'>

export function getCompletedPqcInspections(tasks: CompletionTask[]) {
  const isCompleted = (group: CompletionTask[]) =>
    group.length > 0 &&
    group.every((task) => task.taskStatus === 'SUBMITTED' || task.taskStatus === 'CONFIRMED')
  const patrols = new Map<number, CompletionTask[]>()
  for (const task of tasks) {
    if (task.inspectionType !== 'PATROL') continue
    const group = patrols.get(task.roundNo) || []
    group.push(task)
    patrols.set(task.roundNo, group)
  }
  return {
    first: isCompleted(tasks.filter((task) => task.inspectionType === 'FIRST')),
    patrolRounds: [...patrols]
      .filter(([, group]) => isCompleted(group))
      .map(([roundNo]) => roundNo)
      .sort((a, b) => a - b),
    final: isCompleted(tasks.filter((task) => task.inspectionType === 'FINAL'))
  }
}
