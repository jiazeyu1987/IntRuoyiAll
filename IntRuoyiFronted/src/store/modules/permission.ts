import { defineStore } from 'pinia'
import { store } from '@/store'
import { cloneDeep } from 'lodash-es'
import remainingRouter from '@/router/modules/remaining'
import showroomRoutes from '@/router/modules/showroom'
import { flatMultiLevelRoutes, generateRoute } from '@/utils/routerHelper'
import { CACHE_KEY, useCache } from '@/hooks/web/useCache'
import { isUrl } from '@/utils/is'

const { wsCache } = useCache()
const permissionControlledStaticRoutes = showroomRoutes
const SIGNATURE_GOVERNANCE_ROUTE_PATH = '/signature-governance'
const SIGNATURE_GOVERNANCE_ROUTE_NAME = 'SignatureGovernance'
const SIGNATURE_RECORDS_ROUTE_PATH = 'signature-records'
const SIGNATURE_RECORDS_ROUTE_NAME = 'SignatureGovernanceSignatureRecords'
const SIGNATURE_RECORDS_ROUTE_TITLE = '签名记录'
const LEGACY_SIGNATURE_GOVERNANCE_OVERVIEW_CHILD_PATH = 'overview'
const LEGACY_SIGNATURE_GOVERNANCE_OVERVIEW_ROUTE_NAME = 'SignatureGovernanceOverview'
const LEGACY_SIGNATURE_GOVERNANCE_OVERVIEW_TITLE = '总览'
const APPROVAL_CENTER_ROUTE_PATH = '/approval-center'
const APPROVAL_CENTER_ROUTE_NAME = 'ApprovalCenter'

const normalizeTopLevelRoutePath = (path: string) => {
  if (!path || isUrl(path) || path.startsWith('/')) {
    return path
  }
  return `/${path}`
}

const mergeStaticShowroomRoute = (
  staticRoute: AppRouteRecordRaw,
  dynamicRoute: AppRouteRecordRaw
): AppRouteRecordRaw => {
  const mergedRoute = cloneDeep(staticRoute)
  const dynamicChildren = dynamicRoute.children || []
  const dynamicChildNames = new Set(dynamicChildren.map((child) => String(child.name || '')))
  const dynamicChildPaths = new Set(dynamicChildren.map((child) => String(child.path || '')))

  mergedRoute.children = (mergedRoute.children || []).filter((child) => {
    if (child.meta?.hidden) {
      return true
    }
    return (
      dynamicChildNames.has(String(child.name || '')) ||
      dynamicChildPaths.has(String(child.path || ''))
    )
  })

  mergedRoute.meta = {
    ...mergedRoute.meta,
    icon: dynamicRoute.meta?.icon ?? mergedRoute.meta?.icon,
    title: dynamicRoute.meta?.title ?? mergedRoute.meta?.title,
    alwaysShow: dynamicRoute.meta?.alwaysShow ?? mergedRoute.meta?.alwaysShow,
    hidden: dynamicRoute.meta?.hidden ?? mergedRoute.meta?.hidden
  }
  mergedRoute.redirect = dynamicRoute.redirect ?? mergedRoute.redirect

  return mergedRoute
}

const getRouteName = (route: AppRouteRecordRaw) => String(route.name || '')

const getRoutePath = (route: AppRouteRecordRaw) => String(route.path || '')

const normalizeComparableRoutePath = (path: string) =>
  String(path || '')
    .split('?')[0]
    .replace(/\\/g, '/')
    .replace(/\/+/g, '/')
    .replace(/^\/+/, '')
    .replace(/\/+$/, '')

const isSignatureGovernanceShellRoute = (route: AppRouteRecordRaw) =>
  normalizeTopLevelRoutePath(getRoutePath(route)).replace(/\/+$/, '') ===
    SIGNATURE_GOVERNANCE_ROUTE_PATH || getRouteName(route) === SIGNATURE_GOVERNANCE_ROUTE_NAME

const isLegacySignatureGovernanceOverviewChild = (route: AppRouteRecordRaw) => {
  const routePath = normalizeComparableRoutePath(getRoutePath(route))
  const fullOverviewPath = normalizeComparableRoutePath(
    `${SIGNATURE_GOVERNANCE_ROUTE_PATH}/${LEGACY_SIGNATURE_GOVERNANCE_OVERVIEW_CHILD_PATH}`
  )

  return (
    routePath === LEGACY_SIGNATURE_GOVERNANCE_OVERVIEW_CHILD_PATH ||
    routePath === fullOverviewPath ||
    getRouteName(route) === LEGACY_SIGNATURE_GOVERNANCE_OVERVIEW_ROUTE_NAME ||
    String(route.meta?.title || '') === LEGACY_SIGNATURE_GOVERNANCE_OVERVIEW_TITLE
  )
}

const isSignatureRecordsRoute = (route: AppRouteRecordRaw) => {
  const routePath = normalizeComparableRoutePath(getRoutePath(route))
  const fullSignatureRecordsPath = normalizeComparableRoutePath(
    `${SIGNATURE_GOVERNANCE_ROUTE_PATH}/${SIGNATURE_RECORDS_ROUTE_PATH}`
  )

  return (
    routePath === SIGNATURE_RECORDS_ROUTE_PATH ||
    routePath === fullSignatureRecordsPath ||
    getRouteName(route) === SIGNATURE_RECORDS_ROUTE_NAME ||
    String(route.meta?.title || '') === SIGNATURE_RECORDS_ROUTE_TITLE
  )
}

const normalizeSignatureGovernanceLeafChildren = (
  staticRoute: AppRouteRecordRaw,
  dynamicChildren: AppRouteRecordRaw[]
) => {
  if (!isSignatureGovernanceShellRoute(staticRoute)) {
    return dynamicChildren
  }

  return dynamicChildren.map((dynamicChild) => {
    if (!isSignatureRecordsRoute(dynamicChild)) {
      return dynamicChild
    }

    return {
      ...cloneDeep(dynamicChild),
      children: undefined,
      meta: {
        ...dynamicChild.meta,
        alwaysShow: false
      }
    }
  })
}

const filterSignatureGovernanceDynamicChildren = (
  staticRoute: AppRouteRecordRaw,
  dynamicChildren: AppRouteRecordRaw[]
) => {
  if (!isSignatureGovernanceShellRoute(staticRoute)) {
    return dynamicChildren
  }

  return normalizeSignatureGovernanceLeafChildren(
    staticRoute,
    dynamicChildren.filter((dynamicChild) => !isLegacySignatureGovernanceOverviewChild(dynamicChild))
  )
}

const isLegacySignatureGovernanceStandaloneOverviewRoute = (route: AppRouteRecordRaw) => {
  const routePath = normalizeComparableRoutePath(getRoutePath(route))
  const fullOverviewPath = normalizeComparableRoutePath(
    `${SIGNATURE_GOVERNANCE_ROUTE_PATH}/${LEGACY_SIGNATURE_GOVERNANCE_OVERVIEW_CHILD_PATH}`
  )

  return (
    routePath === fullOverviewPath ||
    getRouteName(route) === LEGACY_SIGNATURE_GOVERNANCE_OVERVIEW_ROUTE_NAME
  )
}

const resolveHiddenShellRedirect = (
  staticRoute: AppRouteRecordRaw,
  dynamicRoute: AppRouteRecordRaw,
  currentRedirect: AppRouteRecordRaw['redirect']
) => {
  if (isSignatureGovernanceShellRoute(staticRoute)) {
    return staticRoute.redirect ?? currentRedirect
  }

  return dynamicRoute.redirect ?? currentRedirect
}

const getHiddenStaticChildKey = (route: AppRouteRecordRaw) =>
  `${getRouteName(route)}::${getRoutePath(route)}`

const isSameRouteIdentity = (
  staticChild: AppRouteRecordRaw,
  dynamicChild: AppRouteRecordRaw
) => {
  const staticName = getRouteName(staticChild)
  const dynamicName = getRouteName(dynamicChild)
  if (staticName && dynamicName && staticName === dynamicName) {
    return true
  }

  const staticPath = getRoutePath(staticChild)
  const dynamicPath = getRoutePath(dynamicChild)
  return !!staticPath && !!dynamicPath && staticPath === dynamicPath
}

const isApprovalCenterShellRoute = (route: AppRouteRecordRaw) =>
  normalizeTopLevelRoutePath(getRoutePath(route)).replace(/\/+$/, '') === APPROVAL_CENTER_ROUTE_PATH ||
  getRouteName(route) === APPROVAL_CENTER_ROUTE_NAME

const findMatchingDynamicRoute = (
  staticRoute: AppRouteRecordRaw,
  dynamicRoutes: AppRouteRecordRaw[]
) => {
  return dynamicRoutes.find((dynamicRoute) => isSameRouteIdentity(staticRoute, dynamicRoute))
}

const mergeAuthorizedApprovalCenterChildren = (
  staticChildren: AppRouteRecordRaw[] = [],
  dynamicChildren: AppRouteRecordRaw[] = []
): AppRouteRecordRaw[] => {
  return staticChildren.reduce<AppRouteRecordRaw[]>((authorizedChildren, staticChild) => {
    const dynamicChild = findMatchingDynamicRoute(staticChild, dynamicChildren)
    if (!dynamicChild && !staticChild.meta?.hidden) {
      return authorizedChildren
    }

    const mergedChild = cloneDeep(staticChild)
    if (dynamicChild) {
      mergedChild.redirect = dynamicChild.redirect ?? mergedChild.redirect
      mergedChild.meta = {
        ...mergedChild.meta,
        title: dynamicChild.meta?.title ?? mergedChild.meta?.title,
        icon: dynamicChild.meta?.icon ?? mergedChild.meta?.icon,
        hidden: dynamicChild.meta?.hidden ?? mergedChild.meta?.hidden,
        alwaysShow: dynamicChild.meta?.alwaysShow ?? mergedChild.meta?.alwaysShow
      }
      if (mergedChild.children?.length) {
        mergedChild.children = mergeAuthorizedApprovalCenterChildren(
          mergedChild.children,
          dynamicChild.children || []
        )
      } else if (dynamicChild.children?.length) {
        mergedChild.children = cloneDeep(dynamicChild.children)
      }
    }

    authorizedChildren.push(mergedChild)
    return authorizedChildren
  }, [])
}

const mergeApprovalCenterRoute = (
  staticRoute: AppRouteRecordRaw,
  dynamicRoute: AppRouteRecordRaw
): AppRouteRecordRaw => {
  const mergedRoute = cloneDeep(staticRoute)
  mergedRoute.children = mergeAuthorizedApprovalCenterChildren(
    staticRoute.children || [],
    dynamicRoute.children || []
  )
  mergedRoute.meta = {
    ...mergedRoute.meta,
    title: dynamicRoute.meta?.title ?? mergedRoute.meta?.title,
    icon: dynamicRoute.meta?.icon ?? mergedRoute.meta?.icon,
    hidden: dynamicRoute.meta?.hidden ?? mergedRoute.meta?.hidden,
    alwaysShow: dynamicRoute.meta?.alwaysShow ?? mergedRoute.meta?.alwaysShow
  }
  mergedRoute.redirect = dynamicRoute.redirect ?? mergedRoute.redirect
  return mergedRoute
}

const mergeHiddenStaticChildWithDynamicChild = (
  staticChild: AppRouteRecordRaw,
  dynamicChild: AppRouteRecordRaw
): AppRouteRecordRaw => {
  const dynamicMeta = dynamicChild.meta || {}
  const staticMeta = staticChild.meta || {}
  const mergedChild = {
    ...cloneDeep(dynamicChild),
    ...cloneDeep(staticChild),
    component: staticChild.component,
    meta: {
      ...dynamicMeta,
      ...staticMeta,
      hidden: dynamicChild.meta?.hidden ?? staticChild.meta?.hidden,
      alwaysShow: dynamicChild.meta?.alwaysShow ?? staticChild.meta?.alwaysShow
    }
  }

  return mergedChild
}

const mergeHiddenStaticShellRoute = (
  staticRoute: AppRouteRecordRaw,
  dynamicRoute: AppRouteRecordRaw
): AppRouteRecordRaw => {
  const mergedRoute = cloneDeep(staticRoute)
  const dynamicChildren = filterSignatureGovernanceDynamicChildren(
    staticRoute,
    dynamicRoute.children || []
  )
  const dynamicChildNames = new Set<string>()
  const dynamicChildPaths = new Set<string>()
  const collectDynamicDescendants = (routes: AppRouteRecordRaw[], parentPath = '') => {
    for (const route of routes) {
      const routeName = String(route.name || '')
      if (routeName) {
        dynamicChildNames.add(routeName)
      }
      const routePath = String(route.path || '')
      const normalizedPath = routePath
        ? `${parentPath}/${routePath}`.replace(/\/+/g, '/').replace(/^\//, '')
        : parentPath
      if (normalizedPath) {
        dynamicChildPaths.add(normalizedPath)
      }
      if (route.children?.length) {
        collectDynamicDescendants(route.children, normalizedPath)
      }
    }
  }
  collectDynamicDescendants(dynamicChildren)
  const hiddenStaticChildren = (staticRoute.children || []).filter((child) => child.meta?.hidden)
  const mergedStaticChildKeys = new Set<string>()

  const isHiddenStaticChildCovered = (child: AppRouteRecordRaw) => {
    if (mergedStaticChildKeys.has(getHiddenStaticChildKey(child))) {
      return true
    }

    return (
      dynamicChildNames.has(getRouteName(child)) ||
      dynamicChildPaths.has(getRoutePath(child))
    )
  }

  mergedRoute.children = [
    ...dynamicChildren.map((dynamicChild) => {
      const staticChild = hiddenStaticChildren.find((child) =>
        isSameRouteIdentity(child, dynamicChild)
      )
      if (!staticChild) {
        return cloneDeep(dynamicChild)
      }

      mergedStaticChildKeys.add(getHiddenStaticChildKey(staticChild))
      return mergeHiddenStaticChildWithDynamicChild(staticChild, dynamicChild)
    }),
    ...hiddenStaticChildren
      .filter((child) => !isHiddenStaticChildCovered(child))
      .map((child) => cloneDeep(child))
  ]

  mergedRoute.meta = {
    ...mergedRoute.meta,
    ...dynamicRoute.meta,
    hidden: dynamicRoute.meta?.hidden ?? false
  }
  mergedRoute.redirect = resolveHiddenShellRedirect(staticRoute, dynamicRoute, mergedRoute.redirect)

  return mergedRoute
}

const mergeStaticRoutesWithDynamicRoutes = (
  staticRoutes: AppRouteRecordRaw[],
  dynamicRoutes: AppRouteRecordRaw[],
  permissionControlledRoutes: AppRouteRecordRaw[] = []
) => {
  const mergedStaticRoutes = cloneDeep(staticRoutes)
  const controlledStaticRoutes = cloneDeep(permissionControlledRoutes)
  const authorizedStaticRoutes: AppRouteRecordRaw[] = []
  const dynamicRoutesToAdd: AppRouteRecordRaw[] = []
  const mergedRoutesToReplace: AppRouteRecordRaw[] = []

  for (const dynamicRoute of dynamicRoutes) {
    const normalizedDynamicPath = normalizeTopLevelRoutePath(String(dynamicRoute.path || ''))
    if (
      isLegacySignatureGovernanceStandaloneOverviewRoute({
        ...dynamicRoute,
        path: normalizedDynamicPath || dynamicRoute.path
      })
    ) {
      continue
    }
    const findDuplicateStaticRouteIndex = (routes: AppRouteRecordRaw[]) =>
      routes.findIndex((staticRoute) => {
        const normalizedStaticPath = normalizeTopLevelRoutePath(String(staticRoute.path || ''))
        return (
          normalizedStaticPath === normalizedDynamicPath ||
          String(staticRoute.name || '') === String(dynamicRoute.name || '')
        )
      })
    const duplicateStaticRouteIndex = findDuplicateStaticRouteIndex(mergedStaticRoutes)
    const duplicateControlledStaticRouteIndex =
      duplicateStaticRouteIndex === -1 ? findDuplicateStaticRouteIndex(controlledStaticRoutes) : -1

    if (duplicateStaticRouteIndex === -1 && duplicateControlledStaticRouteIndex === -1) {
      dynamicRoutesToAdd.push({
        ...dynamicRoute,
        path: normalizedDynamicPath || dynamicRoute.path
      })
      continue
    }

    const duplicateStaticRoute =
      duplicateStaticRouteIndex !== -1
        ? mergedStaticRoutes[duplicateStaticRouteIndex]
        : controlledStaticRoutes[duplicateControlledStaticRouteIndex]
    if (String(duplicateStaticRoute.name || '') === 'Showroom') {
      const mergedRoute = mergeStaticShowroomRoute(
        duplicateStaticRoute,
        dynamicRoute
      )
      if (duplicateStaticRouteIndex !== -1) {
        mergedStaticRoutes[duplicateStaticRouteIndex] = mergedRoute
      } else {
        authorizedStaticRoutes.push(mergedRoute)
      }
      mergedRoutesToReplace.push(mergedRoute)
      continue
    }

    if (isApprovalCenterShellRoute(duplicateStaticRoute)) {
      const mergedRoute = mergeApprovalCenterRoute(
        duplicateStaticRoute,
        {
          ...dynamicRoute,
          path: normalizedDynamicPath || dynamicRoute.path
        }
      )
      if (duplicateStaticRouteIndex !== -1) {
        mergedStaticRoutes[duplicateStaticRouteIndex] = mergedRoute
      } else {
        authorizedStaticRoutes.push(mergedRoute)
      }
      mergedRoutesToReplace.push(mergedRoute)
      continue
    }

    if (duplicateStaticRoute.meta?.hidden) {
      const mergedRoute = mergeHiddenStaticShellRoute(
        duplicateStaticRoute,
        {
          ...dynamicRoute,
          path: normalizedDynamicPath || dynamicRoute.path
        }
      )
      mergedStaticRoutes[duplicateStaticRouteIndex] = mergedRoute
      mergedRoutesToReplace.push(mergedRoute)
    }
  }

  return {
    authorizedStaticRoutes,
    mergedStaticRoutes,
    dynamicRoutesToAdd,
    mergedRoutesToReplace
  }
}

export interface PermissionState {
  routers: AppRouteRecordRaw[]
  addRouters: AppRouteRecordRaw[]
  menuTabRouters: AppRouteRecordRaw[]
}

export const usePermissionStore = defineStore('permission', {
  state: (): PermissionState => ({
    routers: [],
    addRouters: [],
    menuTabRouters: []
  }),
  getters: {
    getRouters(): AppRouteRecordRaw[] {
      return this.routers
    },
    getAddRouters(): AppRouteRecordRaw[] {
      return flatMultiLevelRoutes(cloneDeep(this.addRouters))
    },
    getMenuTabRouters(): AppRouteRecordRaw[] {
      return this.menuTabRouters
    }
  },
  actions: {
    async generateRoutes(): Promise<unknown> {
      return new Promise<void>(async (resolve) => {
        // 获得菜单列表，它在登录的时候，setUserInfoAction 方法中已经进行获取
        let res: AppCustomRouteRecordRaw[] = []
        const roleRouters = wsCache.get(CACHE_KEY.ROLE_ROUTERS)
        if (roleRouters) {
          res = roleRouters as AppCustomRouteRecordRaw[]
        }
        const routerMap: AppRouteRecordRaw[] = generateRoute(res)
        const {
          authorizedStaticRoutes,
          mergedStaticRoutes,
          dynamicRoutesToAdd,
          mergedRoutesToReplace
        } = mergeStaticRoutesWithDynamicRoutes(
          remainingRouter,
          routerMap,
          permissionControlledStaticRoutes
        )
        // 动态路由，404一定要放到最后面
        // preschooler：vue-router@4以后已支持静态404路由，此处可不再追加
        this.addRouters = mergedRoutesToReplace.concat(dynamicRoutesToAdd, [
          {
            path: '/:path(.*)*',
            // redirect: '/404',
            component: () => import('@/views/Error/404.vue'),
            name: '404Page',
            meta: {
              hidden: true,
              breadcrumb: false
              }
          }
        ])
        // 渲染菜单的所有路由
        this.routers = authorizedStaticRoutes.concat(mergedStaticRoutes, dynamicRoutesToAdd)
        resolve()
      })
    },
    setMenuTabRouters(routers: AppRouteRecordRaw[]): void {
      this.menuTabRouters = routers
    }
  },
  persist: false
})

export const usePermissionStoreWithOut = () => {
  return usePermissionStore(store)
}
