import { shallowRef } from 'vue'
import type { Router } from 'vue-router'
import request from '@/config/axios'
import { beginWorkbenchRead, bindWorkbenchDirectoryContext, isWorkbenchReadCurrent, workbenchContextIdentity, workbenchLifetime } from './queryContext'
import type { ActionEnvelope, ModuleRegistration, SourceDirectory } from './contracts'
import manifest from './manifest.json'
import { createSourceRegistry } from './registryContract'

const units = import.meta.glob<{ default: ModuleRegistration }>('../workbench-sources/*/registration.ts', { eager: true })
export const workbenchRegistry = createSourceRegistry(Object.values(units).map(unit => unit.default), manifest)
const directory = shallowRef<SourceDirectory>()
const directoryScope = shallowRef('')
export const workbenchDirectoryState = shallowRef<'unknown' | 'loading' | 'loaded' | 'error'>('unknown')
export const workbenchDirectoryError = shallowRef('')
let directoryEpoch = 0
let pendingDirectory: { scope: string; epoch: number; promise: Promise<SourceDirectory> } | undefined
export const workbenchIdentityKey = workbenchContextIdentity
export const authorizedWorkbenchSources = () =>
  directoryScope.value === workbenchIdentityKey() ? [...(directory.value?.sources ?? [])] : []
export const workbenchDirectoryRevision = () =>
  directoryScope.value === workbenchIdentityKey() ? directory.value?.registryRevision ?? '' : ''

export const refreshWorkbenchDirectory = (): Promise<SourceDirectory> => {
  if (pendingDirectory?.scope === `${workbenchIdentityKey()}:${workbenchLifetime.value}`) return pendingDirectory.promise
  const lease = beginWorkbenchRead()
  const scope = `${workbenchIdentityKey()}:${workbenchLifetime.value}`, epoch = ++directoryEpoch
  directory.value = undefined
  directoryScope.value = ''
  workbenchDirectoryState.value = 'loading'
  workbenchDirectoryError.value = ''
  const promise = (async () => {
    try {
      const response = workbenchRegistry.validateDirectory(await request.get<SourceDirectory>({
        url: '/workbench/sources', ignoreErrorMessage: true
      }))
      if (!isWorkbenchReadCurrent(lease) || epoch !== directoryEpoch) {
        throw new Error('工作台目录登录、租户或权限上下文已变化，请重新加载。')
      }
      bindWorkbenchDirectoryContext(response.registryRevision, response.sources.map(source => source.sourceId))
      directoryScope.value = workbenchIdentityKey()
      directory.value = response
      workbenchDirectoryState.value = 'loaded'
      return response
    } catch (error) {
      if (isWorkbenchReadCurrent(lease) && epoch === directoryEpoch) {
        workbenchDirectoryState.value = 'error'
        workbenchDirectoryError.value = error instanceof Error ? error.message : String(error)
      }
      throw error
    } finally {
      if (pendingDirectory?.epoch === epoch) pendingDirectory = undefined
    }
  })()
  pendingDirectory = { scope, epoch, promise }
  return promise
}
export const requireWorkbenchDirectory = () => {
  if (workbenchDirectoryState.value !== 'loaded' || directoryScope.value !== workbenchIdentityKey() || !directory.value) {
    throw new Error('工作台授权目录尚未成功加载。')
  }
  return directory.value
}
export const requireAuthorizedSource = (id: string) => {
  requireWorkbenchDirectory()
  const source = authorizedWorkbenchSources().find(source => source.sourceId === id)
  if (!source) throw new Error('SOURCE_FORBIDDEN: 工作台来源未获当前目录授权。')
  workbenchRegistry.require(id)
  return source
}
export const openWorkbenchAction = async (router: Router, sourceId: string, action: ActionEnvelope, callerCurrent: () => boolean) => {
  const lease = beginWorkbenchRead()
  const isCurrent = () => isWorkbenchReadCurrent(lease) && callerCurrent()
  if (!isCurrent()) throw new Error('工作台动作上下文已变化。')
  await refreshWorkbenchDirectory()
  if (!isCurrent()) throw new Error('工作台动作上下文已变化。')
  const d = requireAuthorizedSource(sourceId)
  if (!action || action.actionKind !== d.actionKind || action.payloadVersion !== d.payloadVersion ||
    !action.payload || typeof action.payload !== 'object' || Array.isArray(action.payload) || Object.values(action.payload).some(value => typeof value !== 'string')) {
    throw new Error('ACTION_IDENTITY_INVALID: 工作台动作合同不一致。')
  }
  await workbenchRegistry.require(sourceId).open(router, action.payload, isCurrent)
}
