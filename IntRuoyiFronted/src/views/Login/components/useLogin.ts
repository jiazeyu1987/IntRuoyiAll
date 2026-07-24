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
    return '租户识别失败：请检查租户名称、本机后端服务和租户配置。'
  }
  if (message === 'Network Error' || message.includes('Network Error') || message.includes('ECONNREFUSED')) {
    return '后端服务不可达：请确认本机后端 48081 已启动，或联系管理员检查服务状态。'
  }
  if (message.includes('timeout')) {
    return '后端服务响应超时：请检查本机后端、数据库和网络连接后重试。'
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
    return '权限不足：登录成功但缺少目标页面权限，请联系管理员检查角色菜单和按钮权限。'
  }
  return message || '登录失败：请检查租户、账号、密码、权限和后端服务状态。'
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

export function useFormValid<T extends Object = any>(formRef: Ref<any>) {
  async function validForm() {
    const form = unref(formRef)
    if (!form) return
    const data = await form.validate()
    return data as T
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
