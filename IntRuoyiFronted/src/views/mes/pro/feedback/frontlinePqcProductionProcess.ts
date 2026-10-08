import type { FrontlinePqcProcessVO, FrontlinePqcTaskOptionVO } from '@/api/mes/pro/feedback'

export interface PqcProductionProcessIdentity {
  routeProcessId: number
  processId: number
}

export interface PqcProductionProcessOption extends PqcProductionProcessIdentity {
  key: string
  name: string
}

const positiveId = (value: unknown) =>
  typeof value === 'number' && Number.isSafeInteger(value) && value > 0

export const requirePqcProductionProcessIdentity = (
  identity?: Partial<PqcProductionProcessIdentity>
): PqcProductionProcessIdentity => {
  if (!positiveId(identity?.routeProcessId) || !positiveId(identity?.processId)) {
    throw new Error('PQC任务缺少正式生产工序身份，请核对路线工序和生产工序编号。')
  }
  return { routeProcessId: identity!.routeProcessId!, processId: identity!.processId! }
}

export const samePqcProductionProcess = (
  left: PqcProductionProcessIdentity,
  right: PqcProductionProcessIdentity
) => left.routeProcessId === right.routeProcessId && left.processId === right.processId

export const getPqcProductionProcessTasks = (
  process: FrontlinePqcProcessVO,
  selected?: PqcProductionProcessIdentity
): FrontlinePqcTaskOptionVO[] => {
  for (const task of process.pqcTaskOptions) requirePqcProductionProcessIdentity(task)
  if (!selected) return []
  requirePqcProductionProcessIdentity(selected)
  return process.pqcTaskOptions.filter(task => samePqcProductionProcess(task, selected))
}

export const buildPqcProductionProcessOptions = (
  processes: FrontlinePqcProcessVO[],
  activeOrderId: number
): PqcProductionProcessOption[] => {
  if (!positiveId(activeOrderId)) throw new Error('缺少正式活跃订单身份，无法选择生产工序。')
  const submitted = new Set<string>()
  for (const process of processes) {
    if (process.activeOrderId !== activeOrderId) throw new Error('PQC生产工序候选与当前订单不一致。')
    for (const candidate of process.productionSubmitCandidates) {
      requirePqcProductionProcessIdentity(candidate)
      if (candidate.activeOrderId !== activeOrderId || !positiveId(candidate.eventId)) {
        throw new Error('生产工序缺少当前订单的正式生产提交来源。')
      }
      submitted.add(`${candidate.routeProcessId}:${candidate.processId}`)
    }
  }
  const options = new Map<string, PqcProductionProcessOption>()
  for (const process of processes) {
    for (const task of process.pqcTaskOptions) {
      const identity = requirePqcProductionProcessIdentity(task)
      const key = `${identity.routeProcessId}:${identity.processId}`
      if (task.taskStatus !== 'PENDING' || !submitted.has(key)) continue
      const name = typeof task.productionProcessName === 'string' ? task.productionProcessName.trim() : ''
      const existing = options.get(key)
      if (existing && existing.name && name && existing.name !== name) {
        throw new Error('同一正式生产工序的冻结名称不一致。')
      }
      if (!existing || (!existing.name && name)) options.set(key, { ...identity, key, name })
    }
  }
  return [...options.values()]
}
