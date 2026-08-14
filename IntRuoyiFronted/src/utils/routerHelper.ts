import type { RouteLocationNormalized, Router, RouteRecordNormalized } from 'vue-router'
import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router'
import { cloneDeep, omit } from 'lodash-es'
import qs from 'qs'
import { isUrl } from '@/utils/is'

const modules = import.meta.glob('../views/**/*.{vue,tsx}')

const DCC_UPLOAD_ROUTE_COMPONENT = 'dcc/controlled-file/upload/index'
const DCC_UPLOAD_ROUTE_PATH = 'controlled-file/upload'
const DCC_BROWSER_ROUTE_COMPONENT = 'dcc/controlled-file/browser/index'
const DCC_BROWSER_ROUTE_PATH = 'controlled-file/browser'
const DCC_UPLOAD_BROWSER_CACHE_ROUTE_COMPONENTS = new Set([
  DCC_UPLOAD_ROUTE_COMPONENT,
  DCC_BROWSER_ROUTE_COMPONENT
])
const DCC_UPLOAD_BROWSER_CACHE_ROUTE_PATHS = new Set([
  DCC_UPLOAD_ROUTE_PATH,
  DCC_BROWSER_ROUTE_PATH
])
const DCC_PERMISSION_CATEGORIES_ROUTE_COMPONENT = 'dcc/controlled-file/categories/index'
const DCC_PERMISSION_CATEGORIES_ROUTE_PATH = 'controlled-file/categories'
const WORKSTATION_ROUTE_COMPONENTS = new Set(['mes/md/workstation/index', 'mes/md/workstation'])
const WORKSTATION_ROUTE_PATHS = new Set(['mes/md/workstation', 'md/workstation'])
const MES_PRO_ROUTE_LIST_COMPONENT = 'mes/pro/route/index'
const MES_PRO_ROUTE_MENU_PATHS = new Set(['mes/pro/route', 'pro/route'])
const MES_PRO_BATCH_RECORD_FORM_LIST_COMPONENT = 'mes/pro/batchrecordformlist/index'
const MES_PRO_BATCH_RECORD_FORM_LIST_ROUTE_PATHS = new Set([
  'mes/pro/batch-record-form-list',
  'pro/batch-record-form-list'
])
const MES_ROUTE_BATCH_RECORD_TAB_CACHE_ROUTE_COMPONENTS = new Set([
  MES_PRO_ROUTE_LIST_COMPONENT,
  MES_PRO_BATCH_RECORD_FORM_LIST_COMPONENT
])
const MES_ROUTE_BATCH_RECORD_TAB_CACHE_ROUTE_PATHS = new Set([
  ...MES_PRO_ROUTE_MENU_PATHS,
  ...MES_PRO_BATCH_RECORD_FORM_LIST_ROUTE_PATHS
])
const MES_PRO_WORK_ORDER_ROUTE_COMPONENTS = new Set([
  'mes/pro/workorder/index',
  'mes/pro/workorder'
])
const MES_PRO_WORK_ORDER_ROUTE_PATHS = new Set([
  'mes/pro/workorder',
  'pro/workorder',
  'mes/pro/work-order',
  'pro/work-order'
])
const MES_FEEDBACK_ROUTE_COMPONENTS = new Set(['mes/pro/feedback', 'mes/pro/feedback/index'])
const MES_FEEDBACK_ROUTE_PATHS = new Set(['mes/pro/feedback', 'pro/feedback'])
const APPROVAL_CENTER_REDIRECT_SHELL_ROUTE_COMPONENTS = new Set([
  'mes/pro/edhr/ApprovalPage',
  'dcc/controlled-file/approval-tasks',
  'dcc/controlled-file/approval-tasks/index'
])
const APPROVAL_CENTER_REDIRECT_SHELL_ROUTE_PATHS = new Set([
  'mes/pro/feedback/edhr-approval',
  'pro/feedback/edhr-approval',
  'dcc/controlled-file/approval-tasks',
  'controlled-file/approval-tasks'
])

const normalizeInternalRoutePath = (value?: string) =>
  String(value || '')
    .split('?')[0]
    .replace(/^\/+/, '')
    .replace(/\/+$/, '')

const normalizeInternalComponentPath = (value?: string) =>
  normalizeInternalRoutePath(value)
    .replace(/^@\/views\//, '')
    .replace(/^src\/views\//, '')
    .replace(/^views\//, '')
    .replace(/\.(vue|tsx)$/, '')

const normalizeMesProRouteMenuComponent = (route: AppCustomRouteRecordRaw) => {
  const routePath = normalizeInternalRoutePath(route.path)
  const componentPath = normalizeInternalComponentPath(route.component)
  const isRouteMenuPath =
    MES_PRO_ROUTE_MENU_PATHS.has(routePath) ||
    (routePath === 'route' && componentPath.startsWith('mes/pro/route/'))
  if (!isRouteMenuPath) {
    return
  }
  route.component = MES_PRO_ROUTE_LIST_COMPONENT
}

const applyRouteMetaOverrides = (
  route: AppCustomRouteRecordRaw,
  meta: AppRouteRecordRaw['meta']
) => {
  const routePath = normalizeInternalRoutePath(route.path)
  const componentPath = normalizeInternalComponentPath(route.component)
  if (
    DCC_UPLOAD_BROWSER_CACHE_ROUTE_PATHS.has(routePath) ||
    DCC_UPLOAD_BROWSER_CACHE_ROUTE_COMPONENTS.has(componentPath) ||
    MES_ROUTE_BATCH_RECORD_TAB_CACHE_ROUTE_PATHS.has(routePath) ||
    MES_ROUTE_BATCH_RECORD_TAB_CACHE_ROUTE_COMPONENTS.has(componentPath) ||
    routePath === DCC_PERMISSION_CATEGORIES_ROUTE_PATH ||
    componentPath === DCC_PERMISSION_CATEGORIES_ROUTE_COMPONENT ||
    WORKSTATION_ROUTE_PATHS.has(routePath) ||
    WORKSTATION_ROUTE_COMPONENTS.has(componentPath) ||
    MES_PRO_WORK_ORDER_ROUTE_PATHS.has(routePath) ||
    MES_PRO_WORK_ORDER_ROUTE_COMPONENTS.has(componentPath) ||
    MES_FEEDBACK_ROUTE_PATHS.has(routePath) ||
    MES_FEEDBACK_ROUTE_COMPONENTS.has(componentPath)
  ) {
    meta.tagsViewKeyMode = 'path'
  }
  if (
    DCC_UPLOAD_BROWSER_CACHE_ROUTE_PATHS.has(routePath) ||
    DCC_UPLOAD_BROWSER_CACHE_ROUTE_COMPONENTS.has(componentPath) ||
    MES_ROUTE_BATCH_RECORD_TAB_CACHE_ROUTE_PATHS.has(routePath) ||
    MES_ROUTE_BATCH_RECORD_TAB_CACHE_ROUTE_COMPONENTS.has(componentPath)
  ) {
    meta.noCache = false
  }
  if (
    APPROVAL_CENTER_REDIRECT_SHELL_ROUTE_PATHS.has(routePath) ||
    APPROVAL_CENTER_REDIRECT_SHELL_ROUTE_COMPONENTS.has(componentPath)
  ) {
    meta.noTagsView = true
  }
}

export const registerComponent = (componentPath: string) => {
  const component = resolveViewModule(componentPath)
  if (component) {
    // @ts-ignore
    return defineAsyncComponent(component)
  }
}

export const Layout = () => import('@/layout/Layout.vue')

export const getParentLayout = () => {
  return () =>
    new Promise((resolve) => {
      resolve({
        name: 'ParentLayout'
      })
    })
}

export const ascending = (arr: any[]) => {
  arr.forEach((v) => {
    if (v?.meta?.rank === null) v.meta.rank = undefined
    if (v?.meta?.rank === 0) {
      if (v.name !== 'home' && v.path !== '/') {
        console.warn('rank only the home page can be 0')
      }
    }
  })
  return arr.sort((a: { meta: { rank: number } }, b: { meta: { rank: number } }) => {
    return a?.meta?.rank - b?.meta?.rank
  })
}

export const getRawRoute = (route: RouteLocationNormalized): RouteLocationNormalized => {
  if (!route) return route
  const { matched, ...opt } = route
  return {
    ...opt,
    matched: (matched
      ? matched.map((item) => ({
          meta: item.meta,
          name: item.name,
          path: item.path
        }))
      : undefined) as RouteRecordNormalized[]
  }
}

const normalizeViewModuleKey = (value: string) =>
  normalizeInternalComponentPath(value.replace(/^\.\.\/views\//, ''))

const resolveViewModule = (componentPath?: string, fallbackPath?: string) => {
  const target = normalizeInternalComponentPath(componentPath || fallbackPath)
  if (!target) {
    return undefined
  }
  const candidates = target.endsWith('/index') ? [target] : [`${target}/index`, target]
  const modulesRoutesKeys = Object.keys(modules)
  const moduleKey = modulesRoutesKeys.find((item) => candidates.includes(normalizeViewModuleKey(item)))
  return moduleKey ? modules[moduleKey] : undefined
}

const resolveViewComponent = (componentPath?: string, fallbackPath?: string) =>
  resolveViewModule(componentPath, fallbackPath)

const buildParentRouteChild = (
  route: AppCustomRouteRecordRaw,
  meta: AppRouteRecordRaw['meta']
): AppRouteRecordRaw => {
  return {
    path: '',
    name:
      route.componentName && route.componentName.length > 0
        ? `${route.componentName}Overview`
        : `${toCamelCase(route.path, true)}Overview`,
    meta: {
      ...meta,
      hidden: true,
      canTo: true
    },
    component: resolveViewComponent(route.component)
  }
}

export const generateRoute = (routes: AppCustomRouteRecordRaw[]): AppRouteRecordRaw[] => {
  const res: AppRouteRecordRaw[] = []

  for (const route of routes) {
    const hasChildren = !!route.children?.length
    const hasComponent = !!route.component
    const hasParentRoute = hasChildren && hasComponent

    const meta = {
      title: route.name,
      icon: route.icon,
      hidden: !route.visible,
      noCache: !route.keepAlive,
      hasParentRoute,
      alwaysShow:
        route.children &&
        route.children.length > 0 &&
        (route.alwaysShow !== undefined ? route.alwaysShow : true)
    } as any

    if (route.component && route.component.indexOf('?') > -1) {
      const query = route.component.split('?')[1]
      route.component = route.component.split('?')[0]
      meta.query = qs.parse(query)
    }
    normalizeMesProRouteMenuComponent(route)
    applyRouteMetaOverrides(route, meta)

    let data: AppRouteRecordRaw = {
      path: route.path.indexOf('?') > -1 && !isUrl(route.path) ? route.path.split('?')[0] : route.path,
      name:
        route.componentName && route.componentName.length > 0
          ? route.componentName
          : toCamelCase(route.path, true),
      redirect: route.redirect,
      meta
    }

    if (!hasChildren && route.parentId == 0 && route.component) {
      data.component = Layout
      data.meta = {
        title: meta.title,
        icon: meta.icon,
        hidden: meta.hidden
      }
      data.name = toCamelCase(route.path, true) + 'Parent'
      data.redirect = ''
      meta.alwaysShow = true
      const childrenData: AppRouteRecordRaw = {
        path: '',
        name:
          route.componentName && route.componentName.length > 0
            ? route.componentName
            : toCamelCase(route.path, true),
        redirect: route.redirect,
        meta
      }
      childrenData.component = resolveViewComponent(route.component, route.path)
      data.children = [childrenData]
      res.push(data)
      continue
    }

    if (hasChildren) {
      data.component = Layout
      const childRoutes = generateRoute(route.children!)
      if (hasParentRoute) {
        data.children = [buildParentRouteChild(route, meta), ...childRoutes]
      } else {
        data.redirect = getRedirect(route.path, route.children!)
        data.children = childRoutes
      }
      res.push(data)
      continue
    }

    if (isUrl(route.path)) {
      data = {
        path: '/external-link',
        component: Layout,
        meta: {
          name: route.name
        },
        children: [data]
      } as AppRouteRecordRaw
      res.push(data)
      continue
    }

    data.component = resolveViewComponent(route.component, route.path)
    if (route.children) {
      data.children = generateRoute(route.children)
    }
    res.push(data)
  }

  return res
}

export const getRedirect = (parentPath: string, children: AppCustomRouteRecordRaw[]) => {
  if (!children || children.length == 0) {
    return parentPath
  }
  const path = generateRoutePath(parentPath, children[0].path)
  if (children[0].children) return getRedirect(path, children[0].children)
}

const generateRoutePath = (parentPath: string, path: string) => {
  if (isUrl(path) || path.startsWith('/')) {
    return path
  }
  if (parentPath.endsWith('/')) {
    parentPath = parentPath.slice(0, -1)
  }
  if (!path.startsWith('/')) {
    path = '/' + path
  }
  return parentPath + path
}

export const pathResolve = (parentPath: string, path: string) => {
  if (isUrl(path)) return path
  if (!path) return parentPath
  if (path.startsWith('/')) return path.replace(/\/+/g, '/')
  const childPath = path.startsWith('/') ? path : `/${path}`
  return `${parentPath}${childPath}`.replace(/\/+/g, '/')
}

export const flatMultiLevelRoutes = (routes: AppRouteRecordRaw[]) => {
  const modules: AppRouteRecordRaw[] = cloneDeep(routes)
  for (let index = 0; index < modules.length; index++) {
    const route = modules[index]
    if (!isMultipleRoute(route)) {
      continue
    }
    promoteRouteLevel(route)
  }
  return modules
}

const isMultipleRoute = (route: AppRouteRecordRaw) => {
  if (!route || !Reflect.has(route, 'children') || !route.children?.length) {
    return false
  }

  const children = route.children
  let flag = false
  for (let index = 0; index < children.length; index++) {
    const child = children[index]
    if (child.children?.length) {
      flag = true
      break
    }
  }
  return flag
}

const promoteRouteLevel = (route: AppRouteRecordRaw) => {
  let router: Router | null = createRouter({
    routes: [route as RouteRecordRaw],
    history: createWebHashHistory()
  })

  const routes = router.getRoutes()
  addToChildren(routes, route.children || [], route)
  router = null

  route.children = route.children?.map((item) => omit(item, 'children'))
}

const addToChildren = (
  routes: RouteRecordNormalized[],
  children: AppRouteRecordRaw[],
  routeModule: AppRouteRecordRaw
) => {
  for (let index = 0; index < children.length; index++) {
    const child = children[index]
    const route = routes.find((item) => item.name === child.name)
    if (!route) {
      continue
    }
    routeModule.children = routeModule.children || []
    if (!routeModule.children.find((item) => item.name === route.name)) {
      routeModule.children?.push(route as unknown as AppRouteRecordRaw)
    }
    if (child.children?.length) {
      addToChildren(routes, child.children, routeModule)
    }
  }
}

const toCamelCase = (str: string, upperCaseFirst: boolean) => {
  str = (str || '')
    .replace(/-(.)/g, function (group1: string) {
      return group1.toUpperCase()
    })
    .replaceAll('-', '')

  if (upperCaseFirst && str) {
    str = str.charAt(0).toUpperCase() + str.slice(1)
  }

  return str
}
