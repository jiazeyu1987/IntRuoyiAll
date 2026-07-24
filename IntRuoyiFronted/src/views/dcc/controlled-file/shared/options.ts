export const ACTIVE_STATUS_OPTIONS = [
  { label: '启用', value: true },
  { label: '停用', value: false }
]


export const ACCESS_SUBJECT_TYPE_OPTIONS = [
  { label: '用户', value: 'USER' },
  { label: '部门', value: 'DEPT' },
  { label: '权限角色', value: 'ROLE' },
  { label: '组织角色', value: 'POSITION' }
]

export const POSITION_ASSIGNMENT_TYPE_OPTIONS = [
  { label: '指定用户', value: 'USER' },
  { label: '组织角色', value: 'POST' }
]

export const ROUTE_CANDIDATE_SOURCE_OPTIONS = [
  { label: '指定用户', value: 'USER' },
  { label: '审批角色', value: 'POSITION' }
]

export const ROUTE_APPROVE_METHOD_OPTIONS = [
  { label: '任意通过', value: 'ANY' },
  { label: '全部通过', value: 'ALL' }
]

export const ROUTE_PREVIEW_MODE_OPTIONS = [
  { label: '任意通过', value: 1 },
  { label: '全部通过', value: 2 }
]

export const ROUTE_PREVIEW_APPROVER_TYPE_OPTIONS = [
  { label: '用户', value: 1 },
  { label: '审批角色', value: 2 }
]

export const CONTROLLED_FILE_STATUS_OPTIONS = [
  { label: '提交失败', value: 'SUBMIT_FAILED' },
  { label: '审批中', value: 'APPROVING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '盖章中', value: 'STAMPING' },
  { label: '盖章失败', value: 'STAMP_FAILED' },
  { label: '已发布', value: 'STAMPED' },
  { label: '已撤回', value: 'WITHDRAWN' }
]

export const getOptionLabel = (
  options: Array<{ label: string; value: string | number | boolean }>,
  value: string | number | boolean | undefined | null
) => {
  return options.find((item) => item.value === value)?.label ?? '-'
}
