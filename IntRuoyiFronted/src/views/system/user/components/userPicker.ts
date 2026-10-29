import type { UserVO } from '@/api/system/user'

export type UserId = string | number
export type UserRelationship = 'DIRECT_SUPERVISOR' | 'DEPT_LEADER'
export const USER_RELATIONSHIPS: Array<{ value: UserRelationship; label: string }> = [
  { value: 'DIRECT_SUPERVISOR', label: '申请人的直接主管' },
  { value: 'DEPT_LEADER', label: '申请人的部门负责人' }
]
export interface UserPickerSelection { users: UserOption[]; relationships: UserRelationship[] }
export type UserOption = Omit<Partial<UserVO>, 'id'> & { id: UserId; roleIds?: number[] }
export interface UserFilterOption {
  id: number
  name: string
  parentId?: number
  leaderUserId?: UserId | null
}
export interface UserPickerProps {
  allowRelationships?: boolean
  relationships?: UserRelationship[]
  multiple?: boolean
  deptId?: number
  scopeRoleId?: number
  scopePostId?: number
  min?: number
  max?: number
  disabledIds?: UserId[]
  excludeCurrentUser?: boolean
  allowInactive?: boolean
  userOptions?: any[]
  loadUserOptions?: () => Promise<any[]>
  optionMapper?: (row: any) => UserOption
  selectedUsers?: UserOption[]
  candidateLoading?: boolean
  candidateError?: string
  departmentOptions?: UserFilterOption[]
  roleOptions?: UserFilterOption[]
  postOptions?: UserFilterOption[]
}
export const normalizeOptions = (rows: any[], mapper?: (row: any) => UserOption): UserOption[] => {
  const ids = new Set<string>()
  return rows.map((source) => {
    const row = mapper ? mapper(source) : source
    if (
      !row ||
      !['number', 'string'].includes(typeof row.id) ||
      (typeof row.id === 'number' && (!Number.isSafeInteger(row.id) || row.id <= 0)) ||
      (typeof row.id === 'string' && !/^[1-9]\d*$/.test(row.id))
    )
      throw new Error('候选人员编号必须为安全整数或整数字符串')
    if (ids.has(String(row.id))) throw new Error(`候选人员编号重复：${row.id}`)
    ids.add(String(row.id))
    return { ...row }
  })
}
export const departmentIds = (
  id: number,
  children: boolean,
  options: UserFilterOption[]
): number[] => {
  const ids = new Set([id])
  if (children) {
    let changed = true
    while (changed) {
      changed = false
      for (const option of options) {
        if (option.parentId != null && ids.has(option.parentId) && !ids.has(option.id)) {
          ids.add(option.id)
          changed = true
        }
      }
    }
  }
  return [...ids]
}
export const selectionError = (
  rows: UserOption[],
  min: number,
  max: number,
  disabled: UserId[]
): string => {
  if (rows.some((row) => row.disabled || disabled.some((id) => String(id) === String(row.id))))
    return '已选人员中包含不可选人员，请移除后重试'
  if (rows.length < min) return `请至少选择 ${min} 名人员`
  if (rows.length > max) return `最多选择 ${max} 名人员`
  return ''
}

/** 同一候选集提供的直系部门；不推断父子层级。 */
export const candidateDepartments = (rows: UserOption[]): UserFilterOption[] => {
  const departments = new Map<number, UserFilterOption>()
  for (const row of rows) {
    if (row.deptId != null && row.deptName) departments.set(row.deptId, { id: row.deptId, name: row.deptName })
  }
  return [...departments.values()]
}
export const validateSelectionLimits = (min: number, max: number | undefined, multiple: boolean): string => {
  if (!Number.isInteger(min) || min < 0 || (max !== undefined && (!Number.isInteger(max) || max < 0))) return '选人数量上下限必须为非负整数'
  if (min > (multiple ? max ?? Infinity : Math.min(max ?? 1, 1))) return '选人最少数量不能大于最多数量'
  return ''
}
