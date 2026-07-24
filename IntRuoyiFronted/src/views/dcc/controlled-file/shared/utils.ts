export const flattenTree = <T extends { children?: T[] }>(nodes: T[]): T[] => {
  const result: T[] = []
  const stack = [...nodes]
  while (stack.length > 0) {
    const current = stack.shift()
    if (!current) {
      continue
    }
    result.push(current)
    if (current.children?.length) {
      stack.unshift(...current.children)
    }
  }
  return result
}

const toTrimmedText = (value: string | number | null | undefined) => {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value).trim()
}

export const isDccUnreadableText = (value: string | number | null | undefined) => {
  const text = toTrimmedText(value)
  if (!text || text === '-') {
    return true
  }
  if (text.includes('?') || text.includes('�') || text.includes('□')) {
    return true
  }
  return !text.replace(/[?�□]/g, '').trim()
}

export const formatDccSimpleUserLabel = (user?: {
  username?: string | null
  nickname?: string | null
  deptName?: string | null
} | null) => {
  const username = toTrimmedText(user?.username)
  const nickname = toTrimmedText(user?.nickname)
  const deptName = toTrimmedText(user?.deptName)
  const primary = isDccUnreadableText(nickname) ? username : nickname || username
  const detailParts = [username && username !== primary ? username : '', deptName].filter(Boolean)
  if (primary && detailParts.length) {
    return `${primary} (${detailParts.join(' / ')})`
  }
  return primary || deptName || '-'
}

export const buildDccSimpleUserLabelMap = <
  T extends {
    id: number
    username?: string | null
    nickname?: string | null
    deptName?: string | null
  }
>(
  users: ReadonlyArray<T>
) => new Map(users.map((item) => [item.id, formatDccSimpleUserLabel(item)]))

export const formatBooleanLabel = (value: boolean | undefined | null) => {
  return value ? '启用' : '停用'
}

export const getBooleanTagType = (value: boolean | undefined | null) => {
  return value ? 'success' : 'info'
}

export const formatRequirementLabel = (value: boolean | undefined | null) => {
  return value ? '必须' : '未开启'
}

export const getRequirementTagType = (
  value: boolean | undefined | null,
  enabledType: string
) => {
  return value ? enabledType : 'info'
}

const FIXED_DCC_POSITION_NAME_MAP = new Map<number, string>([
  [900333, '部门负责人'],
  [900334, '部门授权代表'],
  [900335, '编制部门负责人'],
  [900336, '授权代表']
])

export const resolveDccPositionName = (
  positionId: number,
  positions?: ReadonlyArray<{ id: number; name: string }>
) => {
  const matched = positions?.find((item) => item.id === positionId)?.name
  if (matched) {
    return matched
  }
  return FIXED_DCC_POSITION_NAME_MAP.get(positionId) || `审批角色#${positionId}`
}

const GENERIC_CONTROLLED_FILE_ERROR_MESSAGES = new Set([
  'error',
  '系统未知错误，请反馈给管理员',
  '服务器错误,请联系管理员!',
  'Server error, please contact the administrator!'
])

export const resolveControlledFileReadErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error) {
    const message = error.message?.trim()
    if (message && !GENERIC_CONTROLLED_FILE_ERROR_MESSAGES.has(message)) {
      return message
    }
  }
  if (typeof error === 'string') {
    const message = error.trim()
    if (message && !GENERIC_CONTROLLED_FILE_ERROR_MESSAGES.has(message)) {
      return message
    }
  }
  return fallback
}

export const getControlledFileStatusTagType = (status: string | undefined) => {
  switch (status) {
    case 'STAMPED':
      return 'success'
    case 'APPROVING':
    case 'APPROVED':
    case 'STAMPING':
      return 'primary'
    case 'REJECTED':
    case 'STAMP_FAILED':
    case 'SUBMIT_FAILED':
      return 'danger'
    case 'WITHDRAWN':
      return 'info'
    default:
      return 'info'
  }
}
