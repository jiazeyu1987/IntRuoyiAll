import type { ModuleRegistration, SourceDirectory, SourceRegistration, WorkbenchManifest } from './contracts'

/** Pure contract logic; eager glob discovery remains in registry.ts. */
export const createSourceRegistry = (units: ModuleRegistration[], manifest: WorkbenchManifest) => {
  if (manifest.contractVersion !== 1 || !manifest.registryRevision || !Array.isArray(manifest.modules)) {
    throw new Error('CONTRACT_VERSION_UNSUPPORTED: 工作台启用清单无效。')
  }
  const expected = new Map<string, string>()
  const modules = new Map<string, boolean>()
  for (const module of manifest.modules) {
    if (!module.ownerModule || modules.has(module.ownerModule) || !module.sourceIds.length) {
      throw new Error('工作台模块启用清单重复或无效。')
    }
    modules.set(module.ownerModule, module.enabled)
    for (const id of module.sourceIds) {
      if (!id || expected.has(id)) throw new Error('工作台来源启用清单重复或无效。')
      expected.set(id, module.ownerModule)
    }
  }
  const discovered = new Set<string>()
  const sources = new Map<string, SourceRegistration>()
  const ranks = new Set<number>()
  const actions = new Set<string>()
  const discoveredSources = new Set<string>()
  for (const unit of units) {
    if (!unit || discovered.has(unit.ownerModule) || !modules.has(unit.ownerModule) ||
      unit.contractVersion !== manifest.contractVersion || !Array.isArray(unit.sources)) {
      throw new Error('工作台模块注册缺失、重复或版本不一致。')
    }
    discovered.add(unit.ownerModule)
    for (const source of unit.sources) {
      const d = source.descriptor
      if (!d || expected.get(d.sourceId) !== unit.ownerModule || d.ownerModule !== unit.ownerModule ||
        d.contractVersion !== manifest.contractVersion || d.payloadVersion !== 1 || !d.policyId ||
        !d.label || !d.taskType || !d.actionKind || !Number.isSafeInteger(d.sortRank) || d.sortRank < 0 ||
        typeof source.open !== 'function' || discoveredSources.has(d.sourceId) || ranks.has(d.sortRank) || actions.has(d.actionKind)) {
        throw new Error('工作台来源、动作、排序注册缺失、重复或版本不一致。')
      }
      discoveredSources.add(d.sourceId); ranks.add(d.sortRank); actions.add(d.actionKind)
      if (modules.get(unit.ownerModule)) sources.set(d.sourceId, source)
    }
  }
  for (const [module, enabled] of modules) {
    if (enabled && !discovered.has(module)) throw new Error('SOURCE_NOT_REGISTERED: 已启用模块缺少注册单元。')
  }
  for (const [id, module] of expected) {
    if (modules.get(module) && !sources.has(id)) throw new Error('SOURCE_NOT_REGISTERED: 已启用来源缺少处理器。')
  }
  return {
    require(id: string) {
      if (expected.has(id) && !modules.get(expected.get(id)!)) throw new Error('MODULE_DISABLED: 工作台来源已关闭。')
      const registration = sources.get(id)
      if (!registration) throw new Error('SOURCE_NOT_REGISTERED: 工作台来源未注册。')
      return registration
    },
    validateDirectory(directory: SourceDirectory) {
      if (!directory || directory.contractVersion !== manifest.contractVersion ||
        directory.registryRevision !== manifest.registryRevision || !Array.isArray(directory.sources)) {
        throw new Error('CONTRACT_VERSION_UNSUPPORTED: 工作台目录版本不一致。')
      }
      const ids = new Set<string>(), directoryRanks = new Set<number>(), directoryActions = new Set<string>()
      for (const d of directory.sources) {
        const local = this.require(d.sourceId).descriptor
        if (ids.has(d.sourceId) || directoryRanks.has(d.sortRank) || directoryActions.has(d.actionKind) ||
          Object.keys(local).some(key => local[key as keyof typeof local] !== d[key as keyof typeof d])) {
          throw new Error('工作台目录声明与模块处理器不一致。')
        }
        ids.add(d.sourceId); directoryRanks.add(d.sortRank); directoryActions.add(d.actionKind)
      }
      return directory
    }
  }
}
