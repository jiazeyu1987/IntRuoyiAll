import type { ControlledFileCategoryVO } from '@/api/dcc/controlledFile/fileCategories'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'

export const DCC_BROWSER_STATE_CACHE_SCHEMA_VERSION = 1
export const DCC_BROWSER_STATE_STORAGE_PREFIX = 'dcc.controlledFile.browser.state'
export const DCC_BROWSER_METADATA_STORAGE_PREFIX = 'dcc.controlledFile.browser.metadata'

export type BrowserSearchScopeValue = 'current' | 'global'

export interface DccBrowserRememberedState {
  scope?: BrowserSearchScopeValue
  directoryId?: string
  lastOpenedDirectoryId?: string
  pageNo?: number
  pageSize?: number
  categoryId?: string
  status?: string
  keyword?: string
  recognitionStatus?: string
  batchRecognitionTaskId?: string
}

export interface DccBrowserMetadataDirectoryNode {
  id?: number | string
  parentId?: number | string | null
  code: string
  name: string
  active: boolean
  sort: number
  hasChildren?: boolean
  directoryPath?: string
  remark?: string
  createTime?: Date | string
}

export interface DccBrowserMetadataCache {
  categories?: ControlledFileCategoryVO[]
  directoryChildrenByParentKey?: Record<string, DccBrowserMetadataDirectoryNode[]>
  expandedDirectoryIds?: string[]
}

export interface DccBrowserCacheContext {
  tenantId: string
  visitTenantId: string
  userId: string
  schemaVersion: number
}

const normalizeCachePart = (value: unknown) => {
  const normalized = String(value ?? '').trim()
  return normalized || 'none'
}

const normalizePositiveNumber = (value: unknown, fieldName: string) => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new Error(`DCC browser cache field ${fieldName} must be a positive number.`)
  }
  return parsed
}

const normalizePositiveIdText = (value: unknown, fieldName: string) => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  const normalized = parsePositiveRouteQueryId(value)
  if (!normalized) {
    throw new Error(`DCC browser cache field ${fieldName} must be a positive integer id.`)
  }
  return normalized
}

const normalizePositiveIdTextList = (value: unknown, fieldName: string) => {
  if (value === undefined || value === null) {
    return undefined
  }
  if (!Array.isArray(value)) {
    throw new Error(`DCC browser cache field ${fieldName} must be an array.`)
  }
  const normalizedValues = new Set<string>()
  value.forEach((item) => {
    const normalized = parsePositiveRouteQueryId(item)
    if (!normalized) {
      throw new Error(`DCC browser cache field ${fieldName} must only contain positive integer ids.`)
    }
    normalizedValues.add(normalized)
  })
  return Array.from(normalizedValues)
}

const normalizeOptionalString = (value: unknown, fieldName: string) => {
  if (value === undefined || value === null) {
    return undefined
  }
  if (typeof value !== 'string') {
    throw new Error(`DCC browser cache field ${fieldName} must be a string.`)
  }
  const normalized = value.trim()
  return normalized || undefined
}

const normalizeScope = (value: unknown) => {
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  if (value === 'current' || value === 'global') {
    return value
  }
  throw new Error('DCC browser cache field scope must be current or global.')
}

const normalizeRememberedState = (value: unknown): DccBrowserRememberedState | undefined => {
  if (value === undefined || value === null) {
    return undefined
  }
  if (typeof value !== 'object' || Array.isArray(value)) {
    throw new Error('DCC browser remembered state must be an object.')
  }
  const source = value as Record<string, unknown>
  return {
    scope: normalizeScope(source.scope),
    directoryId: normalizePositiveIdText(source.directoryId, 'directoryId'),
    lastOpenedDirectoryId: normalizePositiveIdText(
      source.lastOpenedDirectoryId,
      'lastOpenedDirectoryId'
    ),
    pageNo: normalizePositiveNumber(source.pageNo, 'pageNo'),
    pageSize: normalizePositiveNumber(source.pageSize, 'pageSize'),
    categoryId: normalizePositiveIdText(source.categoryId, 'categoryId'),
    status: normalizeOptionalString(source.status, 'status'),
    keyword: normalizeOptionalString(source.keyword, 'keyword'),
    recognitionStatus: normalizeOptionalString(source.recognitionStatus, 'recognitionStatus'),
    batchRecognitionTaskId: normalizePositiveIdText(
      source.batchRecognitionTaskId,
      'batchRecognitionTaskId'
    )
  }
}

const normalizeMetadataCache = (value: unknown): DccBrowserMetadataCache | undefined => {
  if (value === undefined || value === null) {
    return undefined
  }
  if (typeof value !== 'object' || Array.isArray(value)) {
    throw new Error('DCC browser metadata cache must be an object.')
  }
  const source = value as DccBrowserMetadataCache
  if (source.categories !== undefined && !Array.isArray(source.categories)) {
    throw new Error('DCC browser metadata categories must be an array.')
  }
  if (
    source.directoryChildrenByParentKey !== undefined &&
    (typeof source.directoryChildrenByParentKey !== 'object' ||
      Array.isArray(source.directoryChildrenByParentKey))
  ) {
    throw new Error('DCC browser directory children cache must be an object.')
  }
  return {
    categories: source.categories,
    directoryChildrenByParentKey: source.directoryChildrenByParentKey,
    expandedDirectoryIds: normalizePositiveIdTextList(
      source.expandedDirectoryIds,
      'expandedDirectoryIds'
    )
  }
}

export const buildDccBrowserCacheContext = (context: {
  tenantId?: unknown
  visitTenantId?: unknown
  userId?: unknown
  schemaVersion?: number
}): DccBrowserCacheContext => ({
  tenantId: normalizeCachePart(context.tenantId),
  visitTenantId: normalizeCachePart(context.visitTenantId),
  userId: normalizeCachePart(context.userId),
  schemaVersion: context.schemaVersion ?? DCC_BROWSER_STATE_CACHE_SCHEMA_VERSION
})

const buildStorageKey = (prefix: string, context: DccBrowserCacheContext) =>
  [
    prefix,
    `schema-${context.schemaVersion}`,
    `tenant-${context.tenantId}`,
    `visit-${context.visitTenantId}`,
    `user-${context.userId}`
  ].join(':')

const getLocalStorage = () => {
  if (typeof window === 'undefined' || !window.localStorage) {
    throw new Error('Browser localStorage is unavailable.')
  }
  return window.localStorage
}

const readStorageJson = <T>(storageKey: string): T | undefined => {
  const raw = getLocalStorage().getItem(storageKey)
  if (!raw) {
    return undefined
  }
  return JSON.parse(raw) as T
}

const writeStorageJson = (storageKey: string, value: unknown) => {
  getLocalStorage().setItem(storageKey, JSON.stringify(value))
}

export const readDccBrowserRememberedState = (
  context: DccBrowserCacheContext
): DccBrowserRememberedState | undefined => {
  const value = readStorageJson<unknown>(buildStorageKey(DCC_BROWSER_STATE_STORAGE_PREFIX, context))
  return normalizeRememberedState(value)
}

export const writeDccBrowserRememberedState = (
  context: DccBrowserCacheContext,
  state: DccBrowserRememberedState
) => {
  writeStorageJson(
    buildStorageKey(DCC_BROWSER_STATE_STORAGE_PREFIX, context),
    normalizeRememberedState(state)
  )
}

export const clearDccBrowserRememberedState = (context: DccBrowserCacheContext) => {
  getLocalStorage().removeItem(buildStorageKey(DCC_BROWSER_STATE_STORAGE_PREFIX, context))
}

export const readDccBrowserMetadataCache = (
  context: DccBrowserCacheContext
): DccBrowserMetadataCache | undefined => {
  const value = readStorageJson<unknown>(buildStorageKey(DCC_BROWSER_METADATA_STORAGE_PREFIX, context))
  return normalizeMetadataCache(value)
}

export const writeDccBrowserMetadataCache = (
  context: DccBrowserCacheContext,
  metadata: DccBrowserMetadataCache
) => {
  writeStorageJson(
    buildStorageKey(DCC_BROWSER_METADATA_STORAGE_PREFIX, context),
    normalizeMetadataCache(metadata)
  )
}
