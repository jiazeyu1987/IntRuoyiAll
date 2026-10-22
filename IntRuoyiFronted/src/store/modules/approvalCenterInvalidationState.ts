export type ApprovalView = 'TODO' | 'DONE' | 'MY_INITIATED' | 'CC'
export type ApprovalSession = Readonly<{ key: string; tenantId: string; loginUserId: string }>
export type EdhrSuccessIdentity = Readonly<{
  sourceTaskType: string
  sourceTaskId?: string
  businessSourceType?: string
  businessSourceId?: string
  processInstanceId?: string
  businessKey?: string
}>
export type EdhrSuccessEvent = Readonly<{
  moduleCode: 'EDHR'
  session: ApprovalSession
  identity: EdhrSuccessIdentity
  operationId: number
}>

export const approvalTaskIdentityKey = (task: { moduleCode: string; sourceTaskType: string; sourceTaskId: string; businessKey?: string; processInstanceId?: string }) =>
  JSON.stringify([task.moduleCode, task.sourceTaskType, task.sourceTaskId, task.businessKey || '', task.processInstanceId || ''])

// One component's old action must not modify a later route, source or dialog.
export const createFormalActionGuard = (contextKey: () => string) => {
  let generation = 0
  return {
    invalidate() { generation++ },
    capture() {
      const key = contextKey()
      const capturedGeneration = generation
      return () => capturedGeneration === generation && key === contextKey()
    }
  }
}

// A completed write stays terminal until its own read is reconciled.
export const createFormalWriteReceiptState = <T>() => {
  const receipts = new Map<string, T>()
  const writes = new Set<string>()
  return {
    isWriting: (key: string) => writes.has(key),
    begin(key: string) {
      if (writes.has(key) || receipts.has(key)) return false
      writes.add(key)
      return true
    },
    settle: (key: string) => { writes.delete(key) },
    has: (key: string) => receipts.has(key),
    get: (key: string) => receipts.get(key),
    complete: (key: string, receipt: T) => { receipts.set(key, receipt) },
    reconcile: (key: string) => { receipts.delete(key) }
  }
}

// Events describe completed official writes; they do not manufacture task or CC facts.
export const createApprovalInvalidationState = () => {
  let sessionKey = ''
  let sequence = 0
  const revisions = new Map<ApprovalView, number>()
  const published = new Set<number>()
  const setSession = (key: string) => {
    if (key === sessionKey) return
    sessionKey = key
    revisions.clear()
    published.clear()
  }
  return {
    setSession,
    capture(session: ApprovalSession, identity: EdhrSuccessIdentity): EdhrSuccessEvent {
      const hasTask = typeof identity.sourceTaskId === 'string' && /^[1-9]\d*$/.test(identity.sourceTaskId)
      const hasSource = Boolean(identity.businessSourceType) && typeof identity.businessSourceId === 'string' && /^[1-9]\d*$/.test(identity.businessSourceId)
      if (!session.loginUserId || !session.tenantId || !['EDHR_WORK_TASK', 'ACTIVE_ORDER_HANDOFF'].includes(identity.sourceTaskType) || !(hasTask || hasSource)) {
        throw new Error('审批缓存失效缺少正式账号、租户或任务身份')
      }
      return Object.freeze({ moduleCode: 'EDHR', session: Object.freeze({ ...session }), identity: Object.freeze({ ...identity }), operationId: ++sequence })
    },
    publish(event: EdhrSuccessEvent) {
      if (event.session.key !== sessionKey || published.has(event.operationId)) return false
      published.add(event.operationId)
      // The current EDHR provider declares only TODO and DONE.
      for (const view of ['TODO', 'DONE'] as const) revisions.set(view, (revisions.get(view) || 0) + 1)
      return true
    },
    revision(moduleCode: string | undefined, view: ApprovalView) {
      return !moduleCode || moduleCode === 'EDHR' ? revisions.get(view) || 0 : 0
    }
  }
}

type ListQuery = { pageNo: number; pageSize: number; viewType: ApprovalView; moduleCode?: string; keyword?: string }
type RequestContext<Q> = { sessionKey: string; routeKey: string; query: Q; revision: number }
export const createApprovalListLoader = <Q extends ListQuery, Row>(dependencies: {
  context: () => RequestContext<Q>
  request: (query: Q, isRequestContextCurrent: () => boolean) => Promise<{ list: Row[]; total: number }>
  commit: (data: { list: Row[]; total: number }, query: Q) => void
  refreshBadge: () => Promise<void>
}) => {
  let generation = 0
  let pending: { key: string; promise: Promise<'loaded' | 'stale'> } | undefined
  const consumed = new Map<string, number>()
  const partition = (context: RequestContext<Q>) => JSON.stringify([context.sessionKey, context.query.moduleCode || '', context.query.viewType])
  const semanticKey = (context: RequestContext<Q>) => JSON.stringify([context.sessionKey, context.routeKey, context.query])
  const isDirty = () => {
    const context = dependencies.context()
    return context.revision > (consumed.get(partition(context)) || 0)
  }
  return {
    isDirty,
    invalidate() { generation++; pending = undefined },
    reset() { generation++; pending = undefined; consumed.clear() },
    load(refreshBadge = false): Promise<'loaded' | 'stale'> {
      const currentContext = dependencies.context()
      const context = { ...currentContext, query: { ...currentContext.query } }
      const key = semanticKey(context)
      if (pending?.key === key) return pending.promise
      const requestGeneration = ++generation
      const current = () => generation === requestGeneration && key === semanticKey(dependencies.context())
      const promise: Promise<'loaded' | 'stale'> = (async () => {
        let query = { ...context.query }
        let data = await dependencies.request(query, current)
        if (!current()) return 'stale' as const
        const lastPage = Math.max(1, Math.ceil(data.total / query.pageSize))
        if (query.pageNo > lastPage) {
          query = { ...query, pageNo: lastPage }
          data = await dependencies.request(query, current)
          if (!current()) return 'stale' as const
        }
        if (refreshBadge || context.revision > (consumed.get(partition(context)) || 0)) {
          await dependencies.refreshBadge()
          if (!current()) return 'stale' as const
        }
        dependencies.commit(data, query)
        consumed.set(partition(context), context.revision)
        return 'loaded' as const
      })().catch((error) => {
        if (!current()) return 'stale' as const
        throw error
      }).finally(() => {
        if (pending?.promise === promise) pending = undefined
      })
      pending = { key, promise }
      return promise
    }
  }
}

// Read failure cannot change the already completed write into a failed approval.
export const completeApprovalWrite = async <T>(options: {
  write: () => Promise<T>
  publish: () => void
  success: (result: T) => void
  refresh: () => Promise<unknown>
  refreshFailed: (error: unknown) => void
}) => {
  const result = await options.write()
  options.publish()
  options.success(result)
  try { await options.refresh() } catch (error) { options.refreshFailed(error) }
  return result
}

export const createApprovalBadgeLoader = (options: {
  contextKey: () => string
  request: (isRequestContextCurrent: () => boolean) => Promise<unknown>
  commit: (total: unknown) => void
  start: () => void
  fail: (error: unknown) => void
  finish: () => void
}) => {
  let generation = 0
  let pending: { key: string; promise: Promise<void> } | undefined
  return {
    reset() { generation++; pending = undefined },
    load(): Promise<void> {
      const key = options.contextKey()
      if (pending?.key === key) return pending.promise
      const requestGeneration = ++generation
      const current = () => key === options.contextKey() && requestGeneration === generation
      options.start()
      const promise: Promise<void> = (async () => {
        try {
          const total = await options.request(current)
          if (current()) options.commit(total)
        } catch (error) {
          if (!current()) return
          options.fail(error)
          throw error
        } finally {
          if (requestGeneration === generation) options.finish()
        }
      })().finally(() => {
        // This callback runs after initialization even when request() throws synchronously.
        if (pending?.promise === promise) pending = undefined
      })
      pending = { key, promise }
      return promise
    }
  }
}
