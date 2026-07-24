import request from '@/config/axios'

export interface UserTableColumnConfigColumnVO {
  key: string
  visible: boolean
  width?: number
}

export interface UserTableColumnConfigVO {
  schemaVersion: number
  tableKey: string
  columns: UserTableColumnConfigColumnVO[]
  updatedAt?: string
}

export interface UserTableColumnConfigSaveReqVO {
  tableKey: string
  columns: UserTableColumnConfigColumnVO[]
}

export const getUserTableColumnConfig = (tableKey: string) => {
  return request.get<UserTableColumnConfigVO | null>({
    url: '/system/user-table-column-config/get',
    params: { tableKey }
  })
}

export const saveUserTableColumnConfig = (data: UserTableColumnConfigSaveReqVO) => {
  return request.put<boolean>({
    url: '/system/user-table-column-config/save',
    data
  })
}

export const resetUserTableColumnConfig = (tableKey: string) => {
  return request.delete<boolean>({
    url: '/system/user-table-column-config/reset',
    params: { tableKey }
  })
}
