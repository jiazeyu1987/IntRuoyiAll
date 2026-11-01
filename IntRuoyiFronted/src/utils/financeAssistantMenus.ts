const ASSISTANT_PATHS = new Set([
  '/erp/finance/fenbeitong-voucher',
  '/erp/finance/invoice-voucher-print'
])

/** Retain finance assistants only when present in the user's authorized menus. */
export const retainFinanceAssistantMenus = (
  routes: AppRouteRecordRaw[],
  parentPath = '/erp'
): AppRouteRecordRaw[] =>
  routes.flatMap((route) => {
    const path = route.path.startsWith('/') ? route.path : `${parentPath}/${route.path}`
    const normalizedPath = `/${path.replace(/^\/+|\/+$/g, '')}`
    if (ASSISTANT_PATHS.has(normalizedPath)) return [{ ...route }]
    const children = retainFinanceAssistantMenus(route.children || [], normalizedPath)
    if (!children.length) return []
    const first = children[0]
    const redirect = first.redirect || (first.path.startsWith('/')
      ? first.path
      : `${normalizedPath}/${first.path}`)
    return [{ ...route, children, redirect }]
  })
