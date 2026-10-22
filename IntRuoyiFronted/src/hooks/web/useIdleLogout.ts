import { ElMessage } from 'element-plus'
import * as ConfigApi from '@/api/infra/config'
import { useTagsViewStore } from '@/store/modules/tagsView'
import { useUserStore } from '@/store/modules/user'
import { useLockStore } from '@/store/modules/lock'
import { deleteUserCache } from '@/hooks/web/useCache'
import { removeToken } from '@/utils/auth'

const IDLE_LOGOUT_MINUTES_CONFIG_KEY = 'system.login.idle-timeout-minutes'
const MIN_IDLE_LOGOUT_MINUTES = 1
const MAX_IDLE_LOGOUT_MINUTES = 1440
const IDLE_EVENTS = ['mousemove', 'mousedown', 'keydown', 'touchstart', 'scroll'] as const

export const useIdleLogout = () => {
  const { replace } = useRouter()
  const route = useRoute()
  const userStore = useUserStore()
  const tagsViewStore = useTagsViewStore()
  const lockStore = useLockStore()

  let timer: number | null = null
  let idleTimeoutMs: number | null = null
  let handlingLogout = false
  let disposed = false

  const clearTimer = () => {
    if (timer !== null) {
      window.clearTimeout(timer)
      timer = null
    }
  }

  const redirectToLogin = async () => {
    const redirect = encodeURIComponent(route.fullPath || '/')
    await replace('/login?redirect=' + redirect)
  }

  const clearLocalSession = () => {
    deleteUserCache()
    removeToken()
    userStore.resetState()
  }

  const handleLogout = async (options: { rethrowLogoutError?: boolean } = {}) => {
    if (handlingLogout) {
      return
    }
    handlingLogout = true
    clearTimer()
    let logoutError: unknown = null
    try {
      await userStore.loginOut()
    } catch (error) {
      logoutError = error
      console.error('Idle logout request failed before local session cleanup.', error)
      clearLocalSession()
    } finally {
      tagsViewStore.delAllViews()
      lockStore.resetLockInfo()
      await redirectToLogin()
      handlingLogout = false
    }
    if (logoutError && options.rethrowLogoutError !== false) {
      throw logoutError
    }
  }

  const resetTimer = () => {
    if (handlingLogout || idleTimeoutMs === null) {
      return
    }
    clearTimer()
    timer = window.setTimeout(() => {
      void handleLogout()
    }, idleTimeoutMs)
  }

  const parseIdleLogoutMinutes = (value: unknown) => {
    const normalized = typeof value === 'string' ? value : value == null ? '' : String(value)
    if (!/^[1-9]\d*$/.test(normalized)) {
      throw new Error('登录空闲退出配置无效')
    }
    const minutes = Number(normalized)
    if (
      !Number.isSafeInteger(minutes) ||
      minutes < MIN_IDLE_LOGOUT_MINUTES ||
      minutes > MAX_IDLE_LOGOUT_MINUTES
    ) {
      throw new Error('登录空闲退出配置无效')
    }
    return minutes
  }

  const handleIdleLogoutConfigError = async (error: unknown) => {
    console.error('Idle logout configuration is unavailable.', error)
    ElMessage.error('登录空闲退出配置无效，请联系管理员')
    await handleLogout({ rethrowLogoutError: false })
  }

  const loadIdleLogoutConfig = async () => {
    try {
      const value = await ConfigApi.getConfigKey(IDLE_LOGOUT_MINUTES_CONFIG_KEY)
      if (disposed) {
        return
      }
      idleTimeoutMs = parseIdleLogoutMinutes(value) * 60 * 1000
      resetTimer()
    } catch (error) {
      if (!disposed) {
        await handleIdleLogoutConfigError(error)
      }
    }
  }

  const handleActivity = () => {
    resetTimer()
  }

  const handleVisibilityChange = () => {
    if (!document.hidden) {
      resetTimer()
    }
  }

  onMounted(() => {
    disposed = false
    IDLE_EVENTS.forEach((eventName) => window.addEventListener(eventName, handleActivity, { passive: true }))
    document.addEventListener('visibilitychange', handleVisibilityChange)
    void loadIdleLogoutConfig()
  })

  onBeforeUnmount(() => {
    disposed = true
    IDLE_EVENTS.forEach((eventName) => window.removeEventListener(eventName, handleActivity))
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    clearTimer()
  })
}
