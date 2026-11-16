const governanceRoles = ['electronic_signature_admin', 'audit_admin', 'super_admin', 'tenant_admin']
const governanceTabs = new Set([
  'signature-records',
  'authorizations',
  'retention',
  'periodic-review',
  'csv-package',
  'policy'
])

// Navigation and page rendering share the backend's record-query permission boundary.
export const canAccessSignaturePath = (
  path: string,
  roles: string[],
  permissions: Set<string>
): boolean => {
  const normalized = path.replace(/\/+$/, '')
  if (!normalized.startsWith('/signature-governance/')) return true
  const tab = normalized.slice('/signature-governance/'.length)
  if (!governanceTabs.has(tab)) return true
  const hasPermission = (permission: string) =>
    permissions.has('*:*:*') || permissions.has(permission)
  if (tab === 'signature-records') {
    return hasPermission('signature-governance:policy:query')
  }
  if (tab === 'authorizations') {
    return (
      (roles.includes('electronic_signature_admin') || roles.includes('tenant_admin')) &&
      hasPermission('dcc:controlled-file:signature:manage')
    )
  }
  const hasGovernanceRole =
    permissions.has('*:*:*') || governanceRoles.some((role) => roles.includes(role))
  return hasGovernanceRole
}

export const filterSignatureMenus = (
  routes: AppRouteRecordRaw[],
  roles: string[],
  permissions: Set<string>,
  parentPath = ''
): AppRouteRecordRaw[] =>
  routes.flatMap((route) => {
    const path = route.path.startsWith('/') ? route.path : `${parentPath}/${route.path}`
    // Retired overview entries may also arrive below the system-management menu.
    if (path.replace(/\/+$/, '') === '/signature-governance/overview') return []
    if (!canAccessSignaturePath(path, roles, permissions)) return []
    const children = route.children
      ? filterSignatureMenus(route.children, roles, permissions, path)
      : undefined
    if (path === '/signature-governance' && route.children?.length && !children?.length) return []
    return [{ ...route, ...(children ? { children } : {}) }]
  })
