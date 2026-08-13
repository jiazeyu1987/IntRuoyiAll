import type { FrontlinePqcProcessVO, FrontlinePqcTaskOptionVO } from './index'

const comparePqcTaskOptions = (
  left: FrontlinePqcTaskOptionVO,
  right: FrontlinePqcTaskOptionVO
) =>
  left.businessDate.localeCompare(right.businessDate) ||
  left.ruleSort - right.ruleSort ||
  left.roundNo - right.roundNo ||
  left.pqcTaskId - right.pqcTaskId

export const projectFrontlinePqcProcesses = (
  processes: FrontlinePqcProcessVO[]
): FrontlinePqcProcessVO[] =>
  processes
    .map((process) => ({
      ...process,
      pqcTaskOptions: [...process.pqcTaskOptions].sort(comparePqcTaskOptions)
    }))
    .sort(
      (left, right) =>
        left.qaProcessSort - right.qaProcessSort || left.qaProcessId - right.qaProcessId
    )
