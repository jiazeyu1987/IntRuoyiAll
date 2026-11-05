import type { RoleVO } from '@/api/system/role'
import type { UserVO } from '@/api/system/user'

type Page<T> = { list: T[]; total: number }

// Match the exact role codes consumed by the production and market release services.
export const loadReleaseOwner = async (
  code: string,
  getRoles: (query: { pageNo: number; pageSize: number; code: string }) => Promise<Page<RoleVO>>,
  getUsers: (query: { pageNo: number; pageSize: number; roleId: number }) => Promise<Page<UserVO>>
) => {
  const matches: RoleVO[] = []
  for (let pageNo = 1; ; pageNo++) {
    const page = await getRoles({ pageNo, pageSize: 100, code })
    matches.push(...page.list.filter(role => role.code === code))
    if (pageNo * 100 >= page.total) break
    if (!page.list.length) throw new Error(`角色列表分页数据不完整：${code}`)
  }
  if (matches.length !== 1) throw new Error(`默认权限角色必须唯一，请检查：${code}`)
  const role = matches[0]
  if (role.status !== 0) throw new Error(`默认权限角色已停用：${role.name}`)
  const users: UserVO[] = []
  for (let pageNo = 1; ; pageNo++) {
    const page = await getUsers({ pageNo, pageSize: 100, roleId: role.id })
    users.push(...page.list)
    if (pageNo * 100 >= page.total) break
    if (!page.list.length) throw new Error(`授权人员分页数据不完整：${role.name}`)
  }
  return { role, users }
}
