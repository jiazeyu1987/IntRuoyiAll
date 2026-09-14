import { useCache, CACHE_KEY } from '@/hooks/web/useCache'
import { TokenType } from '@/api/login/types'
import { decrypt, encrypt } from '@/utils/jsencrypt'

const { wsCache } = useCache()

const AccessTokenKey = 'ACCESS_TOKEN'
const RefreshTokenKey = 'REFRESH_TOKEN'
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

// 设置token
export const setToken = (token: TokenType) => {
  clearAuthenticatedUserCache()
  wsCache.set(RefreshTokenKey, token.refreshToken)
  wsCache.set(AccessTokenKey, token.accessToken)
}

// 删除token
export const removeToken = () => {
  wsCache.delete(AccessTokenKey)
  wsCache.delete(RefreshTokenKey)
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

const encryptLoginForm = (loginForm: LoginFormType) => {
  const normalizedLoginForm = normalizeLoginForm(loginForm)
  return {
    ...normalizedLoginForm,
    password: normalizedLoginForm.password
      ? (encrypt(normalizedLoginForm.password) as string)
      : normalizedLoginForm.password
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
  wsCache.set(CACHE_KEY.LoginForm, encryptLoginForm(loginForm), {
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
      ...encryptLoginForm(item),
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
  let removedLegacyDefaultLoginForm = false
  const nextLoginHistory = loginHistory
    .map((item) => {
      const decryptedLoginForm = decryptLoginForm(item)
      if (!decryptedLoginForm) {
        return undefined
      }
      if (isLegacyDefaultLoginForm(decryptedLoginForm)) {
        removedLegacyDefaultLoginForm = true
        return undefined
      }
      return {
        ...decryptedLoginForm,
        updatedAt: Number(item.updatedAt) || 0
      }
    })
    .filter((item): item is LoginTenantHistoryRecord => Boolean(item?.tenantName))
    .sort((left, right) => right.updatedAt - left.updatedAt)
  if (removedLegacyDefaultLoginForm) {
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
  wsCache.set(CACHE_KEY.TenantId, tenantId)
}

export const removeTenantId = () => {
  wsCache.delete(CACHE_KEY.TenantId)
}

export const getVisitTenantId = () => {
  return wsCache.get(CACHE_KEY.VisitTenantId)
}

export const setVisitTenantId = (visitTenantId: number) => {
  wsCache.set(CACHE_KEY.VisitTenantId, visitTenantId)
}
