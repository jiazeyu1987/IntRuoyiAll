const operationLogRoles = new Set(['super_admin', 'tenant_admin', 'audit_admin', 'edhr_auditor'])

export const canAccessOperationLogs = (roles: string[]): boolean =>
  roles.some(role => operationLogRoles.has(role))

export const isOperationLogPath = (path: string): boolean =>
  ['/system/log/operate-log', '/system/operate-log'].includes(path.replace(/\/+$/, ''))

export const filterOperationLogMenus = (
  routes: AppRouteRecordRaw[], roles: string[], parentPath = ''
): AppRouteRecordRaw[] => routes.flatMap(route => {
  const path = route.path.startsWith('/') ? route.path : `${parentPath}/${route.path}`
  if (!canAccessOperationLogs(roles) && (isOperationLogPath(path) || route.name === 'SystemOperateLog')) return []
  return [{ ...route, ...(route.children ? { children: filterOperationLogMenus(route.children, roles, path) } : {}) }]
})
