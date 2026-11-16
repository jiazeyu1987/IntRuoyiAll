import router from './router'
import type { RouteRecordRaw } from 'vue-router'
import { isRelogin } from '@/config/axios/service'
import { getAccessToken } from '@/utils/auth'
import { useTitle } from '@/hooks/web/useTitle'
import { useNProgress } from '@/hooks/web/useNProgress'
import { usePageLoading } from '@/hooks/web/usePageLoading'
import { useDictStoreWithOut } from '@/store/modules/dict'
import { useUserStoreWithOut } from '@/store/modules/user'
import { usePermissionStoreWithOut } from '@/store/modules/permission'
import { canAccessOperationLogs, isOperationLogPath } from '@/utils/operationLogAccess'

const { start, done } = useNProgress()

const { loadStart, loadDone } = usePageLoading()

const parseURL = (
  url: string | null | undefined
): { basePath: string; paramsObject: { [key: string]: string } } => {
  // 如果输入为 null 或 undefined，返回空字符串和空对象
  if (url == null) {
    return { basePath: '', paramsObject: {} }
  }

  // 找到问号 (?) 的位置，它之前是基础路径，之后是查询参数
  const questionMarkIndex = url.indexOf('?')
  let basePath = url
  const paramsObject: { [key: string]: string } = {}

  // 如果找到了问号，说明有查询参数
  if (questionMarkIndex !== -1) {
    // 获取 basePath
    basePath = url.substring(0, questionMarkIndex)

    // 从 URL 中获取查询字符串部分
    const queryString = url.substring(questionMarkIndex + 1)

    // 使用 URLSearchParams 遍历参数
    const searchParams = new URLSearchParams(queryString)
    searchParams.forEach((value, key) => {
      // 封装进 paramsObject 对象
      paramsObject[key] = value
    })
  }

  // 返回 basePath 和 paramsObject
  return { basePath, paramsObject }
}

// 路由不重定向白名单
const whiteList = [
  '/login',
  '/social-login',
  '/auth-redirect',
  '/bind',
  '/register',
  '/oauthLogin/gitee'
]

// These existing management entries are removed from the dynamic menu when unauthorized.
// Report an explicit access denial instead of letting the catch-all render a missing page.
const managementEntryPermissions: Record<string, string> = {
  '/system/user': 'system:user:query',
  '/system/role/organization-role': 'system:post:query'
}

// 路由加载前
router.beforeEach(async (to, from, next) => {
  start()
  loadStart()
  if (getAccessToken()) {
    if (to.path === '/login') {
      next({ path: '/' })
    } else {
      const dictStore = useDictStoreWithOut()
      const userStore = useUserStoreWithOut()
      const permissionStore = usePermissionStoreWithOut()
      // 异步加载字典
      // 另外，间接 issue：https://gitee.com/yudaocode/yudao-ui-admin-vue3/issues/ID9FLI
      if (!dictStore.getIsSetDict) {
        dictStore.setDictMap().then()
      }
      if (!userStore.getIsSetUser) {
        permissionStore.initializationError = ''
        isRelogin.show = true
        try {
          await userStore.setUserInfoAction()
          // 菜单生成失败必须回到登录页，不能保留已完成用户初始化的状态。
          await permissionStore.generateRoutes()
        } catch (error) {
          permissionStore.initializationError = error instanceof Error
            ? error.message
            : '系统菜单初始化失败，请联系管理员后重新登录。'
          console.error('系统初始化失败', error)
          userStore.clearSession()
          done()
          loadDone()
          next({ path: '/login', query: { redirect: to.fullPath }, replace: true })
          return
        } finally {
          isRelogin.show = false
        }
        // 移除已由业务菜单接管的静态外壳，避免相同地址仍匹配空白布局。
        permissionStore.staticRouteNamesToRemove.forEach((name) => router.removeRoute(name))
        permissionStore.getAddRouters.forEach((route) => {
          router.addRoute(route as unknown as RouteRecordRaw) // 动态添加可访问路由表
        })
        const redirectPath =
          to.path === '/user/profile' && to.query.tab === 'resetPwd'
            ? to.fullPath
            : from.query.redirect || to.fullPath
        // 修复跳转时不带参数的问题
        const redirect = decodeURIComponent(redirectPath as string)
        const { basePath, paramsObject: query } = parseURL(redirect)
        // 登录回到默认入口时统一进入个人中心，保留业务页面的登录回跳。
        const path = basePath === '/' || basePath === '/index' ? '/user/profile' : basePath
        const nextData = to.path === path ? { path, query, replace: true } : { path, query }
        next(nextData)
      } else {
        if (isOperationLogPath(to.path) && !canAccessOperationLogs(userStore.getRoles)) {
          next({ path: '/403', query: { redirect: to.fullPath }, replace: true })
          return
        }
        const requiredPermission = managementEntryPermissions[to.path]
        if (
          requiredPermission &&
          // A granted organization-role page permits reading; action buttons keep their own permissions.
          !(
            to.path === '/system/role/organization-role' &&
            router.getRoutes().some((route) => route.path === to.path)
          ) &&
          !userStore.getPermissions.has('*:*:*') &&
          !userStore.getPermissions.has(requiredPermission)
        ) {
          next({ path: '/403', query: { redirect: to.fullPath }, replace: true })
        } else {
          next()
        }
      }
    }
  } else {
    if (whiteList.indexOf(to.path) !== -1) {
      next()
    } else {
      next(`/login?redirect=${to.fullPath}`) // 否则全部重定向到登录页
    }
  }
})

router.afterEach((to) => {
  useTitle(to?.meta?.title as string)
  done() // 结束Progress
  loadDone()
})

// 路由组件等其他加载错误也必须结束全局加载状态；错误仍由路由及调用方处理。
router.onError(() => {
  done()
  loadDone()
})
