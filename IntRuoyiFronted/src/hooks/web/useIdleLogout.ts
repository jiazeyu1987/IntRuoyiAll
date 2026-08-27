import { useTagsViewStore } from '@/store/modules/tagsView'
import { useUserStore } from '@/store/modules/user'
import { useLockStore } from '@/store/modules/lock'

const IDLE_TIMEOUT = 15 * 60 * 1000
const IDLE_EVENTS = ['mousemove', 'mousedown', 'keydown', 'touchstart', 'scroll'] as const

export const useIdleLogout = () => {
  const { replace } = useRouter()
  const route = useRoute()
  const userStore = useUserStore()
  const tagsViewStore = useTagsViewStore()
  const lockStore = useLockStore()

  let timer: number | null = null
  let handlingLogout = false

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

  const handleLogout = async () => {
    if (handlingLogout) {
      return
    }
    handlingLogout = true
    clearTimer()
    try {
      await userStore.loginOut().catch(() => {})
      tagsViewStore.delAllViews()
      lockStore.resetLockInfo()
      await redirectToLogin()
    } finally {
      handlingLogout = false
    }
  }

  const resetTimer = () => {
    if (handlingLogout) {
      return
    }
    clearTimer()
    timer = window.setTimeout(() => {
      void handleLogout()
    }, IDLE_TIMEOUT)
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
    IDLE_EVENTS.forEach((eventName) => window.addEventListener(eventName, handleActivity, { passive: true }))
    document.addEventListener('visibilitychange', handleVisibilityChange)
    resetTimer()
  })

  onBeforeUnmount(() => {
    IDLE_EVENTS.forEach((eventName) => window.removeEventListener(eventName, handleActivity))
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    clearTimer()
  })
}
