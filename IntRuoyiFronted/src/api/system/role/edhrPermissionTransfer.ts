import request from '@/config/axios'

export interface AccountMapping {
  sourceUsername: string
  sourceName: string
  targetUsername: string
}
export interface TransferPreview {
  errors: string[]
  mappings: AccountMapping[]
  createCategory: boolean
  token: string
  roles: {
    sourceCode: string
    code: string
    name: string
    dataScope: string
    roleId: number | null
    existingMembers: string[]
    menus: { id: number; name: string; permission: string; purpose: string; added: boolean }[]
  }[]
  accounts: {
    sourceUsername: string
    sourceName: string
    targetUsername: string
    targetName: string
    existingRoles: string[]
    roleCodes: string[]
    addedRoles: string[]
  }[]
}
export interface TransferResult {
  createdRoles: number
  changedRoles: number
  changedAccounts: number
  unchanged: boolean
}

export const exportEdhrPermissions = () =>
  request.download({ url: '/system/role/edhr-transfer/export' })

const form = (file: File, mappings?: AccountMapping[]) => {
  const data = new FormData()
  data.append('file', file)
  if (mappings !== undefined) data.append('mappings', JSON.stringify(mappings))
  return data
}
export const previewEdhrPermissions = async (
  file: File,
  mappings?: AccountMapping[]
): Promise<TransferPreview> => {
  const response = await request.upload<{ data: TransferPreview }>({
    url: '/system/role/edhr-transfer/preview',
    data: form(file, mappings)
  })
  return response.data
}

export const confirmEdhrPermissions = async (
  file: File,
  mappings: AccountMapping[],
  token: string,
  reason: string
): Promise<TransferResult> => {
  const data = form(file, mappings)
  data.append('token', token)
  data.append('reason', reason)
  const response = await request.upload<{ data: TransferResult }>({
    url: '/system/role/edhr-transfer/confirm',
    data
  })
  return response.data
}
