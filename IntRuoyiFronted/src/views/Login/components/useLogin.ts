import { Ref } from 'vue'

import * as LoginApi from '@/api/login'
import * as authUtil from '@/utils/auth'

export const resolveLoginErrorMessage = (error: unknown, context?: 'tenant' | 'auth' | 'permission') => {
  const errorLike = error as any
  const rawMessage =
    errorLike?.response?.data?.msg ||
    errorLike?.response?.data?.message ||
    errorLike?.message ||
    String(error || '')
  const message = String(rawMessage || '').trim()
  const status = Number(errorLike?.response?.status || errorLike?.status || 0)
  const code = Number(errorLike?.code || errorLike?.response?.data?.code || 0)

  if (context === 'tenant' || message.includes('TENANT_NOT_FOUND')) {
    return '无法识别租户，请核对租户名称；仍无法登录时请联系管理员。'
  }
  if (message === 'Network Error' || message.includes('Network Error') || message.includes('ECONNREFUSED')) {
    return '暂时无法连接服务，请稍后重试；仍无法登录时请联系管理员。'
  }
  if (message.includes('timeout')) {
    return '服务响应超时，请稍后重试；仍无法登录时请联系管理员。'
  }
  if (
    status === 401 ||
    code === 401 ||
    message.includes('账号密码') ||
    message.includes('密码不正确') ||
    message.includes('用户不存在')
  ) {
    return '账号或密码错误：请核对当前租户、账号和密码后重试。'
  }
  if (context === 'permission' || status === 403 || code === 403 || message.includes('无权限')) {
    return '您暂时没有访问此页面的权限，请联系管理员。'
  }
  return message || '登录失败，请核对租户和账号信息；仍无法登录时请联系管理员。'
}

export enum LoginStateEnum {
  LOGIN,
  REGISTER,
  RESET_PASSWORD,
  MOBILE,
  QR_CODE,
  SSO
}

const currentState = ref(LoginStateEnum.LOGIN)

export function useLoginState() {
  function setLoginState(state: LoginStateEnum) {
    currentState.value = state
  }
  const getLoginState = computed(() => currentState.value)

  function handleBackLogin() {
    setLoginState(LoginStateEnum.LOGIN)
  }

  return {
    setLoginState,
    getLoginState,
    handleBackLogin
  }
}

export function useFormValid(formRef: Ref<any>) {
  async function validForm() {
    const form = unref(formRef)
    if (!form) return
    // Element Plus reports invalid fields through the callback. Keep those
    // inline messages instead of sending its field-error object to the API error renderer.
    let valid = false
    await form.validate((isValid: boolean) => { valid = isValid })
    return valid
  }

  return {
    validForm
  }
}

export function useLoginTenant() {
  async function resolveTenantId(tenantEnable: boolean | string, tenantName: string) {
    const enabled = tenantEnable === true || tenantEnable === 'true'
    if (!enabled) {
      return true
    }
    let tenantId
    try {
      tenantId = await LoginApi.getTenantIdByName(tenantName)
    } catch (error) {
      authUtil.removeTenantId()
      throw new Error(resolveLoginErrorMessage(error, 'tenant'))
    }
    if (tenantId == null) {
      authUtil.removeTenantId()
      return false
    }
    authUtil.setTenantId(tenantId)
    return tenantId
  }

  return {
    resolveTenantId
  }
}
