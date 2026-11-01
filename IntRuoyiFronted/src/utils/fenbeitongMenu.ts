const VOUCHER_PATH = '/erp/finance/fenbeitong-voucher'

/** Keep only the voucher branch already present in the user's authorized menus. */
export const retainFenbeitongMenu = (
  routes: AppRouteRecordRaw[],
  parentPath = '/erp'
): AppRouteRecordRaw[] =>
  routes.flatMap((route) => {
    const path = route.path.startsWith('/') ? route.path : `${parentPath}/${route.path}`
    const normalizedPath = `/${path.replace(/^\/+|\/+$/g, '')}`
    if (normalizedPath === VOUCHER_PATH) return [{ ...route }]
    const children = retainFenbeitongMenu(route.children || [], normalizedPath)
    return children.length ? [{ ...route, children, redirect: VOUCHER_PATH }] : []
  })
