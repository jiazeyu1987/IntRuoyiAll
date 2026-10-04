/** Exact Java Long identities at the project-configuration boundary. */
export const projectConfigurationIdentity = (value: unknown): string => {
  if ((typeof value !== 'number' && typeof value !== 'string') ||
      (typeof value === 'number' && !Number.isSafeInteger(value)))
    throw new Error('项目或负责人账号身份缺失或精度损失')
  const id = String(value)
  if (!/^[1-9][0-9]*$/.test(id) || BigInt(id) > 9223372036854775807n)
    throw new Error('项目或负责人账号身份无效')
  return id
}
export interface ProjectConfigurationAccount { id: string; nickname: string; username?: string }
/** The formal simple-list endpoint supplies enabled same-tenant accounts; never infer an account from project text. */
export const projectConfigurationAccounts = (value: unknown): ProjectConfigurationAccount[] => {
  if (!Array.isArray(value)) throw new Error('项目负责人正式账号目录缺失')
  const ids = new Set<string>()
  return value.map(row => {
    if (!row || typeof row !== 'object') throw new Error('项目负责人账号目录身份缺失')
    const id = projectConfigurationIdentity(row.id)
    if (ids.has(id)) throw new Error('项目负责人账号目录身份重复')
    if (typeof row.nickname !== 'string' || !row.nickname.trim() ||
        (row.username != null && (typeof row.username !== 'string' || !row.username.trim())) ||
        (row.status != null && row.status !== 0) || row.disabled === true)
      throw new Error('项目负责人账号名称或启用状态不完整')
    ids.add(id)
    return { id, nickname: row.nickname, ...(row.username == null ? {} : { username: row.username }) }
  })
}
