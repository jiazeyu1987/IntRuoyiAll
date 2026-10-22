/** Read intent and exact business keys. The server independently proves all access rights. */
export type DccBodyReadApplicationType = 'UPLOAD' | 'REVISION' | 'OBSOLETE'
type SelectedVersion = { controlledFileId: string; expectedVersionNo: string }
type ApplicationRound = { applicationType: DccBodyReadApplicationType; bpmRound: string }
export type DccBodyReadContext =
  | { intent: 'DAILY'; controlledFileId: string }
  | (SelectedVersion & { intent: 'AUTHORING' } & (
      | { applicationType?: never; bpmRound?: never }
      | ApplicationRound
    ))
  | (SelectedVersion & ApplicationRound & { intent: 'APPROVAL_TASK'; taskId: string })
  | (SelectedVersion & ApplicationRound & { intent: 'APPROVAL_HISTORY'; taskId?: string })
  | (SelectedVersion & { intent: 'VERSION_HISTORY' })
  | (SelectedVersion & ApplicationRound & { intent: 'TRAINING_APPLICATION' })

export const DCC_BODY_READ_CONTEXT_HEADER = 'X-DCC-Body-Read-Context'
const fail = (): never => { throw new Error('正文读取上下文缺少精确业务身份或包含不允许的字段') }
const exactId = (value: unknown): string => {
  if ((typeof value !== 'string' && typeof value !== 'number')
    || (typeof value === 'number' && !Number.isSafeInteger(value))) return fail()
  const text = String(value)
  if (!/^[1-9][0-9]*$/.test(text) || BigInt(text) > 9223372036854775807n) return fail()
  return text
}
const exactText = (value: unknown, limit: number): string => {
  if (typeof value !== 'string' || !value || value !== value.trim() || value.length > limit
    || /[\u0000-\u001f\u007f]/.test(value)) return fail()
  return value
}
const applicationRound = (value: Record<string, unknown>): ApplicationRound => {
  if (typeof value.applicationType !== 'string' || !['UPLOAD', 'REVISION', 'OBSOLETE'].includes(value.applicationType)) return fail()
  return { applicationType: value.applicationType as DccBodyReadApplicationType,
    bpmRound: exactText(value.bpmRound, 64) }
}

export const normalizeDccBodyReadContext = (
  fileId: number | string,
  context?: unknown
): Readonly<DccBodyReadContext> => {
  const controlledFileId = exactId(fileId)
  if (context === undefined) return Object.freeze({ intent: 'DAILY', controlledFileId })
  if (!context || typeof context !== 'object' || Array.isArray(context)
    || Object.getOwnPropertySymbols(context).length) return fail()
  const value = context as Record<string, unknown>
  if (exactId(value.controlledFileId) !== controlledFileId) return fail()
  const intent = value.intent
  let result: DccBodyReadContext
  switch (intent) {
    case 'DAILY': result = { intent, controlledFileId }; break
    case 'VERSION_HISTORY':
      result = { intent, controlledFileId, expectedVersionNo: exactText(value.expectedVersionNo, 128) }; break
    case 'AUTHORING': {
      const selected = { intent, controlledFileId, expectedVersionNo: exactText(value.expectedVersionNo, 128) } as const
      result = Object.hasOwn(value, 'applicationType') || Object.hasOwn(value, 'bpmRound')
        ? { ...selected, ...applicationRound(value) } : selected
      if (result.applicationType === 'OBSOLETE') return fail()
      break
    }
    case 'APPROVAL_TASK':
      result = { intent, controlledFileId, expectedVersionNo: exactText(value.expectedVersionNo, 128),
        ...applicationRound(value), taskId: exactText(value.taskId, 64) }; break
    case 'APPROVAL_HISTORY':
      result = { intent, controlledFileId, expectedVersionNo: exactText(value.expectedVersionNo, 128),
        ...applicationRound(value), ...(Object.hasOwn(value, 'taskId') ? { taskId: exactText(value.taskId, 64) } : {}) }; break
    case 'TRAINING_APPLICATION':
      result = { intent, controlledFileId, expectedVersionNo: exactText(value.expectedVersionNo, 128), ...applicationRound(value) }; break
    default: return fail()
  }
  if (result.intent === 'TRAINING_APPLICATION' && result.applicationType === 'OBSOLETE') return fail()
  if (Object.keys(value).length !== Object.keys(result).length
    || Object.keys(value).some(key => !Object.hasOwn(result, key))) return fail()
  return Object.freeze(result)
}

/** Fixed property order produces the same bytes for metadata, binary and navigation. */
export const serializeDccBodyReadContext = (fileId: number | string, context?: unknown): string => {
  const bytes = new TextEncoder().encode(JSON.stringify(normalizeDccBodyReadContext(fileId, context)))
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}
export const readDccBodyReadContextRoute = (
  fileId: number | string,
  query: Record<string, unknown>
): Readonly<DccBodyReadContext> => {
  if (!Object.hasOwn(query, 'bodyReadContext')) return normalizeDccBodyReadContext(fileId)
  const wire = query.bodyReadContext
  if (typeof wire !== 'string' || !/^[A-Za-z0-9_-]+$/.test(wire) || wire.length > 2048) return fail()
  const binary = atob(wire.replace(/-/g, '+').replace(/_/g, '/'))
  const text = new TextDecoder('utf-8', { fatal: true }).decode(Uint8Array.from(binary, char => char.charCodeAt(0)))
  const normalized = normalizeDccBodyReadContext(fileId, JSON.parse(text))
  if (serializeDccBodyReadContext(fileId, normalized) !== wire) return fail()
  return normalized
}
export const buildDccBodyReadQuery = (fileId: number | string, context?: unknown) => ({
  bodyReadContext: serializeDccBodyReadContext(fileId, context)
})
export const buildDccBodyReadHeaders = (fileId: number | string, context?: unknown) => ({
  [DCC_BODY_READ_CONTEXT_HEADER]: serializeDccBodyReadContext(fileId, context)
})
