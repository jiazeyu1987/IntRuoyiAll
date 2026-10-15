export const roleOptionLabel = (role: { id?: number; name: string }, roles: Array<{ id?: number; name: string }>): string =>
  roles.filter(item => item.name === role.name).length > 1 ? `${role.name}（角色编号 ${role.id}）` : role.name
