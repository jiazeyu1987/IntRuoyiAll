import request from '@/config/axios'

export interface RoleVO {
  id: number
  name: string
  code: string
  sort: number
  categoryId: number
  categoryName?: string
  categoryCode?: string
  assignedUserCount?: number
  status: number
  type: number
  dataScope: number
  dataScopeDeptIds: number[]
  createTime: Date
}

export interface RoleCategoryVO {
  id: number
  name: string
  code: string
  sort: number
  status: number
  remark?: string
  createTime?: Date
}

// 查询角色列表
export const getRolePage = async (params: PageParam) => {
  return await request.get({ url: '/system/role/page', params })
}

// 查询角色（精简)列表
export const getSimpleRoleList = async (): Promise<RoleVO[]> => {
  return await request.get({ url: '/system/role/simple-list' })
}

// 查询角色详情
export const getRole = async (id: number) => {
  return await request.get({ url: '/system/role/get?id=' + id })
}

// 新增角色
export const createRole = async (data: RoleVO) => {
  return await request.post({ url: '/system/role/create', data })
}

// 修改角色
export const updateRole = async (data: RoleVO) => {
  return await request.put({ url: '/system/role/update', data })
}

// 删除角色
export const deleteRole = async (id: number) => {
  return await request.delete({ url: '/system/role/delete?id=' + id })
}

// 批量删除角色
export const deleteRoleList = async (ids: number[]) => {
  return await request.delete({ url: '/system/role/delete-list', params: { ids: ids.join(',') } })
}

// 导出角色
export const exportRole = (params: any) => {
  return request.download({
    url: '/system/role/export-excel',
    params
  })
}

export const exportRoleConfigPackage = () => {
  return request.download({
    url: '/system/role/config-package/export'
  })
}

export const importRoleConfigPackage = async (data: FormData) => {
  return await request.upload({
    url: '/system/role/config-package/import',
    data
  })
}

// 查询角色分类列表
export const getRoleCategoryList = async (): Promise<RoleCategoryVO[]> => {
  return await request.get({ url: '/system/role-category/list' })
}

// 查询启用角色分类列表
export const getEnabledRoleCategoryList = async (): Promise<RoleCategoryVO[]> => {
  return await request.get({ url: '/system/role-category/enabled-list' })
}

// 新增角色分类
export const createRoleCategory = async (data: RoleCategoryVO) => {
  return await request.post({ url: '/system/role-category/create', data })
}

// 修改角色分类
export const updateRoleCategory = async (data: RoleCategoryVO) => {
  return await request.put({ url: '/system/role-category/update', data })
}

// 删除角色分类
export const deleteRoleCategory = async (id: number) => {
  return await request.delete({ url: '/system/role-category/delete?id=' + id })
}
