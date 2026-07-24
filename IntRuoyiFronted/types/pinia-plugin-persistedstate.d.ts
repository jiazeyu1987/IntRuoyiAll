import type { PiniaPlugin, PiniaPluginContext, StateTree } from 'pinia'

type StorageLike = Pick<Storage, 'getItem' | 'setItem'>

interface PersistedStateSerializer {
  serialize: (value: StateTree) => string
  deserialize: (value: string) => StateTree
}

interface PersistedStateOptions {
  key?: string | ((id: string) => string)
  storage?: StorageLike
  paths?: string[]
  serializer?: PersistedStateSerializer
  beforeRestore?: (context: PiniaPluginContext) => void
  afterRestore?: (context: PiniaPluginContext) => void
  debug?: boolean
}

declare module 'pinia' {
  interface DefineStoreOptionsBase<S extends StateTree, Store> {
    persist?: boolean | PersistedStateOptions | PersistedStateOptions[]
  }

  interface PiniaCustomProperties {
    $hydrate: (opts?: { runHooks?: boolean }) => void
    $persist: () => void
  }
}

declare module 'pinia-plugin-persistedstate' {
  const persistedStatePlugin: PiniaPlugin
  export default persistedStatePlugin
}
