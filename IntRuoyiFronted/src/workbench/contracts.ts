import type { Router } from 'vue-router'

export interface SourceDescriptor {
  sourceId: string
  ownerModule: string
  contractVersion: number
  sortRank: number
  label: string
  taskType: string
  policyId: string
  actionKind: string
  payloadVersion: number
}
export interface ActionEnvelope {
  actionKind: string
  payloadVersion: number
  payload: Record<string, string>
}
export interface SourceDirectory {
  contractVersion: number
  registryRevision: string
  sources: SourceDescriptor[]
}
export interface SourceRegistration {
  descriptor: SourceDescriptor
  open: (router: Router, payload: Record<string, string>, isCurrent?: () => boolean) => Promise<unknown>
}
export interface ModuleRegistration {
  ownerModule: string
  contractVersion: number
  sources: SourceRegistration[]
}
export interface WorkbenchManifest {
  contractVersion: number
  registryRevision: string
  modules: { ownerModule: string; enabled: boolean; sourceIds: string[] }[]
}
