export interface DeptLeaderAwareDept {
  id: number
  leaderUserId?: number | null
}

export interface DeptLeaderAwareUser {
  id: number
  deptId?: number | null
}

export interface DisplayNameOption {
  id?: number | string | null
  name?: string | null
}

const normalizeDeptId = (deptId?: number | null) => (typeof deptId === 'number' ? deptId : undefined)

const normalizeLookupId = (id?: number | string | null) => {
  if (typeof id === 'number' && Number.isFinite(id)) {
    return id
  }
  if (typeof id === 'string' && id.trim() !== '') {
    const parsed = Number(id)
    if (Number.isFinite(parsed)) {
      return parsed
    }
  }
  return undefined
}

export const buildDeptLeaderLookup = <T extends DeptLeaderAwareDept>(depts: T[]) => {
  const deptLeaderByDeptId = new Map<number, number>()
  for (const dept of depts) {
    if (typeof dept.leaderUserId === 'number') {
      deptLeaderByDeptId.set(dept.id, dept.leaderUserId)
    }
  }
  return deptLeaderByDeptId
}

export const isDeptLeader = <T extends DeptLeaderAwareUser>(
  user: T,
  deptLeaderByDeptId: Map<number, number>
) => {
  const deptId = normalizeDeptId(user.deptId)
  if (deptId === undefined) {
    return false
  }
  return deptLeaderByDeptId.get(deptId) === user.id
}

export const buildDisplayNameLookup = <T extends DisplayNameOption>(items: T[]) => {
  const displayNameById = new Map<number, string>()
  for (const item of items) {
    const id = normalizeLookupId(item.id)
    const name = item.name?.trim()
    if (id !== undefined && name) {
      displayNameById.set(id, name)
    }
  }
  return displayNameById
}

export const resolveDisplayNames = (
  ids: Array<number | string | null | undefined> | undefined,
  displayNameById: Map<number, string>
) => {
  if (!Array.isArray(ids) || ids.length === 0) {
    return [] as string[]
  }

  const seenIds = new Set<number>()
  const names: string[] = []
  for (const rawId of ids) {
    const id = normalizeLookupId(rawId)
    if (id === undefined || seenIds.has(id)) {
      continue
    }
    seenIds.add(id)
    const name = displayNameById.get(id)
    if (name) {
      names.push(name)
    }
  }
  return names
}

export const formatDisplayNames = (names: string[], placeholder = '-') =>
  names.length > 0 ? names.join('、') : placeholder

export const sortUsersByDeptLeader = <T extends DeptLeaderAwareUser>(
  users: T[],
  deptLeaderByDeptId: Map<number, number>,
  priorityDeptId?: number
) => {
  const groupedUsers = new Map<number, { leaders: T[]; members: T[] }>()
  const deptOrder: number[] = []
  const detachedUsers: T[] = []

  for (const user of users) {
    const deptId = normalizeDeptId(user.deptId)
    if (deptId === undefined) {
      detachedUsers.push(user)
      continue
    }

    let bucket = groupedUsers.get(deptId)
    if (!bucket) {
      bucket = { leaders: [], members: [] }
      groupedUsers.set(deptId, bucket)
      deptOrder.push(deptId)
    }

    if (isDeptLeader(user, deptLeaderByDeptId)) {
      bucket.leaders.push(user)
    } else {
      bucket.members.push(user)
    }
  }

  const orderedDeptIds = deptOrder.filter((deptId) => deptId !== priorityDeptId)
  if (priorityDeptId !== undefined && groupedUsers.has(priorityDeptId)) {
    orderedDeptIds.unshift(priorityDeptId)
  }

  return orderedDeptIds
    .flatMap((deptId) => {
      const bucket = groupedUsers.get(deptId)
      return bucket ? [...bucket.leaders, ...bucket.members] : []
    })
    .concat(detachedUsers)
}
