import request from '@/config/axios'

export interface UserVO {
  id: number
  username: string
  yintaiAdminUsernameAllowed?: boolean
  nickname: string
  deptId: number
  deptName?: string
  postIds: Array<number | string>
  email: string
  mobile: string
  sex: number
  avatar: string
  loginFailureCount?: number
  loginLocked?: number
  loginLockedTime?: Date
  lifecycleDocumentType?: string
  lifecycleDocumentNo?: string
  lifecycleDocumentTime?: Date | string
  lifecycleEffectiveTime?: Date | string
  lifecycleDeactivatedTime?: Date | string
  loginIp: string
  status: number
  remark: string
  loginDate: Date
  createTime: Date
  disabled?: boolean
}

export interface UserListVO extends UserVO {
  postNames: string[]
  roleNames: string[]
}

export interface UserAssignedPostVO {
  id: number | string
  name: string
  status: number
}

export interface UserEditVO extends UserVO {
  assignedPosts: UserAssignedPostVO[]
}
export type UserSaveVO = Omit<UserVO, 'deptId'> & { deptId: number | null }

export interface UserDingTalkImportRespVO {
  createUsernames: string[]
  failureUsernames: Record<string, string>
  createdDeptPaths: string[]
  enabledDeptPaths: string[]
  leaderAssignedDeptPaths: Record<string, string>
  leaderSkippedDeptPaths: Record<string, string>
}

export interface UserLifecycleDeactivateReqVO {
  id: number
  documentType: 'RESIGNATION' | 'TRANSFER'
  documentNo: string
  documentTime: string
  effectiveTime: string
}

// 查询用户管理列表
export interface UserSelectionQuery extends PageParam {
  username?: string
  nickname?: string
  mobile?: string
  status?: number
  deptId?: number
  deptIds?: number[]
  deptLeaderMode?: 'ALL' | 'MEMBER' | 'LEADER'
  roleId?: number
  postId?: number
  includeChildDepts?: boolean
  scopeDeptId?: number
  scopeRoleId?: number
  scopePostId?: number
}
export const getUserPage = (params: UserSelectionQuery) => {
  return request.get({ url: '/system/user/page', params })
}

// 查询用户管理列表
export const getUserList = (ids: number[]) => {
  return request.get({ url: '/system/user/list', params: { ids: ids.join(',') } })
}

// 查询用户详情
export const getUser = (id: number) => {
  return request.get({ url: '/system/user/get?id=' + id })
}

// 查询包含正式既有岗位的用户编辑详情
export const getUserForUpdate = (id: number): Promise<UserEditVO> => {
  return request.get({ url: '/system/user/get-for-update?id=' + id, ignoreErrorMessage: true })
}

// 新增用户
export const createUser = (data: UserSaveVO, options?: { ignoreErrorMessage?: boolean }) => {
  return request.post({ url: '/system/user/create', data, ignoreErrorMessage: options?.ignoreErrorMessage })
}

// 修改用户
export const updateUser = (data: UserSaveVO, options?: { ignoreErrorMessage?: boolean }) => {
  return request.put({ url: '/system/user/update', data, ignoreErrorMessage: options?.ignoreErrorMessage })
}

// 删除用户
export const deleteUser = (id: number) => {
  return request.delete({ url: '/system/user/delete?id=' + id })
}

// 批量删除用户
export const deleteUserList = (ids: number[]) => {
  return request.delete({ url: '/system/user/delete-list', params: { ids: ids.join(',') } })
}

// 导出用户
export const exportUser = (params: any) => {
  return request.download({ url: '/system/user/export-excel', params })
}

// 导出通用账户不合规清单
export const exportGenericAccountUsers = () => {
  return request.download({ url: '/system/user/export-generic-account-excel' })
}

// 下载用户导入模板
export const importUserTemplate = () => {
  return request.download({ url: '/system/user/get-import-template' })
}

// 钉钉组织导入
export const importDingTalkUsers = async (formData: FormData) => {
  return await request.upload({ url: '/system/user/import-dingtalk', data: formData })
}

// 用户密码重置
export const resetUserPassword = (id: number, password: string) => {
  const data = {
    id,
    password
  }
  return request.put({ url: '/system/user/update-password', data: data, ignoreErrorMessage: true })
}

// 用户状态修改
export const updateUserStatus = (id: number, status: number) => {
  const data = {
    id,
    status
  }
  return request.put({ url: '/system/user/update-status', data: data })
}

// 登记离职/转岗账号联动停用
export const recordUserLifecycleDeactivation = (data: UserLifecycleDeactivateReqVO) => {
  return request.put({ url: '/system/user/lifecycle-deactivation', data })
}

// 解锁用户
export const lockUser = (id: number, reason: string) => {
  return request.put({ url: '/system/user/lock', params: { id, reason } })
}

export const unlockUser = (id: number, reason: string) => {
  return request.put({ url: '/system/user/unlock', params: { id, reason } })
}

// 获取用户精简信息列表
export const getSimpleUserList = (): Promise<UserVO[]> => {
  return request.get({ url: '/system/user/simple-list' })
}

// 用户查询页只读组织元数据，不要求部门管理权限。
export const getDepartmentTree = (): Promise<import('@/api/system/dept').DeptVO[]> =>
  request.get({ url: '/system/user/department-tree' })

export const getSupervisorImpact = (ids: number[]): Promise<import('@/api/system/dept').DeptVO[]> =>
  request.get({ url: '/system/user/supervisor-impact', params: { ids: ids.join(',') } })

// 人员选择只读精简身份接口，不使用用户管理详情权限。
export const getSelectionPage = (params: UserSelectionQuery): Promise<{ list: import('@/views/system/user/components/userPicker').UserOption[]; total: number }> =>
  request.get({ url: '/system/user/selection-page', params })
export const getSelectionList = async (
  ids: Array<number | string>,
  scope: Pick<UserSelectionQuery, 'scopeDeptId' | 'scopeRoleId' | 'scopePostId'> = {}
): Promise<import('@/views/system/user/components/userPicker').UserOption[]> => {
  const rows: import('@/views/system/user/components/userPicker').UserOption[] = []
  for (let offset = 0; offset < ids.length; offset += 200) {
    const batch = await request.get({
      url: '/system/user/selection-list',
      params: { ids: ids.slice(offset, offset + 200).join(','), ...scope }
    })
    rows.push(...batch)
  }
  return rows
}
