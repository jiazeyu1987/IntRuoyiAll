export interface PreviewReadingSession {
  sessionId: string
  heartbeatIntervalSeconds: number
  endedAt: string | null
}
export interface PreviewReadingApi {
  start(clientSessionId: string): Promise<PreviewReadingSession>
  heartbeat(sessionId: string): Promise<PreviewReadingSession>
  stop(sessionId: string): Promise<PreviewReadingSession>
}
interface ReadingLifecycleOptions {
  api: PreviewReadingApi
  visible: () => boolean
  newClientId: () => string
  schedule: (tick: () => Promise<void>, intervalMs: number) => () => void
  onError: (error: unknown) => void
}

/** One instance owns one immutable preview capability; create a new instance after switching source. */
export const createPreviewReadingLifecycle = (options: ReadingLifecycleOptions) => {
  let generation = 0
  let disposed = false
  let active: PreviewReadingSession | undefined
  let starting: Promise<void> | undefined
  let beating: Promise<void> | undefined
  let closing: Promise<void> | undefined
  let cancel: (() => void) | undefined
  const clearTimer = () => { cancel?.(); cancel = undefined }
  const report = (error: unknown) => { clearTimer(); options.onError(error) }

  const pause = (): Promise<void> => {
    generation++
    clearTimer()
    if (closing) return closing
    const session = active
    active = undefined
    const pendingStart = starting
    const pendingBeat = beating
    closing = (async () => {
      // A late start response stops itself using its captured original capability.
      if (pendingStart) await pendingStart
      if (pendingBeat) await pendingBeat
      if (session) await options.api.stop(session.sessionId)
    })().catch(error => { report(error); throw error }).finally(() => { closing = undefined })
    return closing
  }

  const tick = (): Promise<void> => {
    if (beating) return Promise.resolve()
    if (!active || disposed) return Promise.resolve()
    if (!options.visible()) return pause()
    const session = active
    const ownGeneration = generation
    beating = options.api.heartbeat(session.sessionId).then(response => {
      if (response.sessionId !== session.sessionId) throw new Error('阅读会话响应身份不一致')
      if (response.endedAt === undefined) throw new Error('阅读会话响应缺少结束状态')
      if (response.endedAt !== null) throw new Error('服务端已结束该阅读会话，请重新加载正文后重试')
      if (!disposed && ownGeneration === generation) active = response
    }).catch(report).finally(() => { beating = undefined })
    return beating
  }

  const resume = (): Promise<void> => {
    if (disposed || !options.visible() || active) return Promise.resolve()
    if (closing) return closing.then(resume)
    if (starting) return starting.then(resume)
    const ownGeneration = ++generation
    const clientId = options.newClientId()
    starting = options.api.start(clientId).then(async session => {
      if (disposed || generation !== ownGeneration || !options.visible()) {
        await options.api.stop(session.sessionId)
        return
      }
      if (!session.sessionId || session.heartbeatIntervalSeconds !== 10) throw new Error('阅读会话响应缺少正式身份或心跳策略')
      if (session.endedAt === undefined) throw new Error('阅读会话响应缺少结束状态')
      if (session.endedAt !== null) throw new Error('该阅读会话已结束，请重新加载正文后重试')
      active = session
      cancel = options.schedule(tick, session.heartbeatIntervalSeconds * 1000)
    }).catch(error => { report(error); throw error }).finally(() => { starting = undefined })
    return starting
  }

  const dispose = (): Promise<void> => { disposed = true; return pause() }
  return { resume, pause, dispose }
}
