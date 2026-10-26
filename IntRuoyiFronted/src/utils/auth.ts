import { useCache, CACHE_KEY } from '@/hooks/web/useCache'
import { TokenType } from '@/api/login/types'
import { decrypt } from '@/utils/jsencrypt'
import { readonly, ref } from 'vue'

const { wsCache } = useCache()
const contextRevision = ref(0)
// Neutral observable lifetime; auth never depends on a business consumer.
export const authContextRevision = readonly(contextRevision)

const AccessTokenKey = 'ACCESS_TOKEN'
const RefreshTokenKey = 'REFRESH_TOKEN'
const AuthSessionIdKey = 'AUTH_SESSION_ID'
const LegacyLoginTenantNameMap = new Map([['瑛泰源码', '芋道源码']])
const LegacyDefaultLoginUsernames = new Set(['aoteman', 'admin'])
const LegacyDefaultLoginPasswords = new Set(['', 'admin123'])
const LoginFormCacheExpireSeconds = 30 * 24 * 60 * 60
const LoginTenantHistoryLimit = 10

const clearAuthenticatedUserCache = () => {
  wsCache.delete(CACHE_KEY.USER)
  wsCache.delete(CACHE_KEY.ROLE_ROUTERS)
  wsCache.delete(CACHE_KEY.VisitTenantId)
}

// 获取token
export const getAccessToken = () => {
  // 此处与TokenKey相同，此写法解决初始化时Cookies中不存在TokenKey报错
  const accessToken = wsCache.get(AccessTokenKey)
  return accessToken ? accessToken : wsCache.get('ACCESS_TOKEN')
}

// 刷新token
export const getRefreshToken = () => {
  return wsCache.get(RefreshTokenKey)
}

// A non-secret login incarnation survives reloads without persisting a token in UI snapshots.
export const getAuthSessionId = (): string | undefined => {
  if (!getAccessToken()) return undefined
  let sessionId = wsCache.get(AuthSessionIdKey) as string | undefined
  if (!sessionId) {
    sessionId = crypto.randomUUID()
    wsCache.set(AuthSessionIdKey, sessionId)
  }
  return sessionId
}

// 设置token
export const setToken = (token: TokenType) => {
  clearAuthenticatedUserCache()
  setRefreshedToken(token)
  wsCache.set(AuthSessionIdKey, crypto.randomUUID())
  contextRevision.value += 1
}

// 同一会话续期只更新令牌，保留当前身份、菜单和访问租户。
export const setRefreshedToken = (token: TokenType) => {
  wsCache.set(RefreshTokenKey, token.refreshToken)
  wsCache.set(AccessTokenKey, token.accessToken)
}

// 删除token
export const removeToken = () => {
  wsCache.delete(AccessTokenKey)
  wsCache.delete(RefreshTokenKey)
  wsCache.delete(AuthSessionIdKey)
  contextRevision.value += 1
}

/** 格式化token（jwt格式） */
export const formatToken = (token: string): string => {
  return 'Bearer ' + token
}
// ========== 账号相关 ==========

export type LoginFormType = {
  tenantName: string
  username: string
  password: string
  rememberMe: boolean
}

export type LoginTenantHistoryRecord = LoginFormType & {
  updatedAt: number
}

const normalizeTenantName = (tenantName?: string) => {
  const resolvedTenantName = tenantName || ''
  return LegacyLoginTenantNameMap.get(resolvedTenantName) || resolvedTenantName
}

const normalizeLoginForm = (loginForm?: Partial<LoginFormType>): LoginFormType => ({
  tenantName: normalizeTenantName(loginForm?.tenantName),
  username: loginForm?.username || '',
  password: loginForm?.password || '',
  rememberMe: Boolean(loginForm?.rememberMe)
})

const cacheSafeLoginForm = (loginForm: LoginFormType) => {
  const normalizedLoginForm = normalizeLoginForm(loginForm)
  return {
    tenantName: normalizedLoginForm.tenantName,
    username: normalizedLoginForm.username,
    rememberMe: normalizedLoginForm.rememberMe
  }
}

const decryptLoginForm = (loginForm?: Partial<LoginFormType>) => {
  if (!loginForm) {
    return undefined
  }
  const normalizedLoginForm = normalizeLoginForm(loginForm)
  return {
    ...normalizedLoginForm,
    password: normalizedLoginForm.password
      ? (decrypt(normalizedLoginForm.password) as string)
      : normalizedLoginForm.password
  }
}

const isLegacyDefaultLoginForm = (loginForm?: Partial<LoginFormType>) => {
  if (!loginForm) {
    return false
  }
  return (
    LegacyDefaultLoginUsernames.has(loginForm.username || '') &&
    LegacyDefaultLoginPasswords.has(loginForm.password || '')
  )
}

const setEncryptedLoginForm = (loginForm: LoginFormType) => {
  wsCache.set(CACHE_KEY.LoginForm, cacheSafeLoginForm(loginForm), {
    exp: LoginFormCacheExpireSeconds
  })
}

const setEncryptedLoginTenantHistory = (loginHistory: LoginTenantHistoryRecord[]) => {
  if (!loginHistory.length) {
    wsCache.delete(CACHE_KEY.LoginTenantHistory)
    return
  }
  wsCache.set(
    CACHE_KEY.LoginTenantHistory,
    loginHistory.map((item) => ({
      ...cacheSafeLoginForm(item),
      updatedAt: item.updatedAt
    })),
    { exp: LoginFormCacheExpireSeconds }
  )
}

export const getLoginForm = () => {
  const loginForm: LoginFormType = wsCache.get(CACHE_KEY.LoginForm)
  if (loginForm) {
    const decryptedLoginForm = decryptLoginForm(loginForm)
    if (isLegacyDefaultLoginForm(decryptedLoginForm)) {
      wsCache.delete(CACHE_KEY.LoginForm)
    } else {
      if (decryptedLoginForm.password) {
        decryptedLoginForm.password = ''
        setEncryptedLoginForm(decryptedLoginForm)
      }
      return decryptedLoginForm
    }
  }
  const loginHistory = getLoginFormHistory()
  return loginHistory.length ? normalizeLoginForm(loginHistory[0]) : undefined
}

export const getLoginFormHistory = () => {
  const loginHistory: LoginTenantHistoryRecord[] = wsCache.get(CACHE_KEY.LoginTenantHistory) || []
  if (!Array.isArray(loginHistory)) {
    return []
  }
  let updatedLegacyLoginCache = false
  const nextLoginHistory = loginHistory
    .map((item) => {
      const decryptedLoginForm = decryptLoginForm(item)
      if (!decryptedLoginForm) {
        return undefined
      }
      if (isLegacyDefaultLoginForm(decryptedLoginForm)) {
        updatedLegacyLoginCache = true
        return undefined
      }
      if (decryptedLoginForm.password) {
        decryptedLoginForm.password = ''
        updatedLegacyLoginCache = true
      }
      return {
        ...decryptedLoginForm,
        updatedAt: Number(item.updatedAt) || 0
      }
    })
    .filter((item): item is LoginTenantHistoryRecord => Boolean(item?.tenantName))
    .sort((left, right) => right.updatedAt - left.updatedAt)
  if (updatedLegacyLoginCache) {
    setEncryptedLoginTenantHistory(nextLoginHistory)
  }
  return nextLoginHistory
}

export const getLoginFormByTenantName = (tenantName: string) => {
  const normalizedTenantName = normalizeTenantName(tenantName)
  return getLoginFormHistory().find((item) => item.tenantName === normalizedTenantName)
}

export const setLoginForm = (loginForm: LoginFormType) => {
  const normalizedLoginForm = normalizeLoginForm(loginForm)
  const loginHistory = getLoginFormHistory().filter(
    (item) => item.tenantName !== normalizedLoginForm.tenantName
  )
  const nextLoginHistory = [
    {
      ...normalizedLoginForm,
      updatedAt: Date.now()
    },
    ...loginHistory
  ].slice(0, LoginTenantHistoryLimit)
  setEncryptedLoginForm(normalizedLoginForm)
  setEncryptedLoginTenantHistory(nextLoginHistory)
}

export const removeLoginForm = (tenantName?: string) => {
  wsCache.delete(CACHE_KEY.LoginForm)
  if (!tenantName) {
    wsCache.delete(CACHE_KEY.LoginTenantHistory)
    return
  }
  const normalizedTenantName = normalizeTenantName(tenantName)
  const nextLoginHistory = getLoginFormHistory().filter(
    (item) => item.tenantName !== normalizedTenantName
  )
  if (!nextLoginHistory.length) {
    wsCache.delete(CACHE_KEY.LoginTenantHistory)
    return
  }
  setEncryptedLoginTenantHistory(nextLoginHistory)
  setEncryptedLoginForm(nextLoginHistory[0])
}

// ========== 租户相关 ==========

export const getTenantId = () => {
  return wsCache.get(CACHE_KEY.TenantId)
}

export const setTenantId = (tenantId: number) => {
  const changed = getTenantId() !== tenantId
  wsCache.set(CACHE_KEY.TenantId, tenantId)
  if (changed) contextRevision.value += 1
}

export const removeTenantId = () => {
  const changed = getTenantId() != null
  wsCache.delete(CACHE_KEY.TenantId)
  if (changed) contextRevision.value += 1
}

export const getVisitTenantId = () => {
  return wsCache.get(CACHE_KEY.VisitTenantId)
}

export const setVisitTenantId = (visitTenantId: number) => {
  const changed = getVisitTenantId() !== visitTenantId
  wsCache.set(CACHE_KEY.VisitTenantId, visitTenantId)
  if (changed) contextRevision.value += 1
}
