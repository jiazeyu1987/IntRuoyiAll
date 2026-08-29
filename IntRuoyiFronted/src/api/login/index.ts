import request from '@/config/axios'
import type { RegisterVO, UserLoginVO } from './types'

export interface SmsCodeVO {
  mobile: string
  scene: number
}

export interface SmsLoginVO {
  mobile: string
  code: string
}

export interface InvoiceVoucherPrintTicketRespVO {
  ticket: string
  expiresTime: string
}

export interface InvoiceVoucherPrintAssistantStatusRespVO {
  running: boolean
  launchable: boolean
  message?: string
}

const INVOICE_VOUCHER_PRINT_ASSISTANT_REQUEST_TIMEOUT = 120000

// 登录
export const login = (data: UserLoginVO, tenantId?: number | boolean) => {
  return request.post({
    url: '/system/auth/login',
    data,
    headers: {
      isEncrypt: false,
      ...(typeof tenantId === 'number' ? { 'tenant-id': tenantId } : {})
    }
  })
}

// 注册
export const register = (data: RegisterVO) => {
  return request.post({ url: '/system/auth/register', data })
}

// 使用租户名，获得租户编号
export const getTenantIdByName = (name: string) => {
  return request.get({ url: '/system/tenant/get-id-by-name?name=' + name })
}

// 使用租户域名，获得租户信息
export const getTenantByWebsite = (website: string) => {
  return request.get({ url: '/system/tenant/get-by-website?website=' + website })
}

// 登出
export const loginOut = () => {
  return request.post({ url: '/system/auth/logout' })
}

// 获取用户权限信息
export const getInfo = () => {
  return request.get({ url: '/system/auth/get-permission-info' })
}

// 创建发票凭证打印助手访问票据
export const createInvoiceVoucherPrintTicket = () => {
  return request.post<InvoiceVoucherPrintTicketRespVO>({
    url: '/system/auth/invoice-voucher-print-ticket'
  })
}

// 获得发票凭证打印助手运行状态
export const getInvoiceVoucherPrintAssistantStatus = () => {
  return request.get<InvoiceVoucherPrintAssistantStatusRespVO>({
    url: '/system/auth/invoice-voucher-print-assistant/status',
    timeout: INVOICE_VOUCHER_PRINT_ASSISTANT_REQUEST_TIMEOUT
  })
}

// 启动发票凭证打印助手
export const startInvoiceVoucherPrintAssistant = () => {
  return request.post<InvoiceVoucherPrintAssistantStatusRespVO>({
    url: '/system/auth/invoice-voucher-print-assistant/start',
    timeout: INVOICE_VOUCHER_PRINT_ASSISTANT_REQUEST_TIMEOUT
  })
}

//获取登录验证码
export const sendSmsCode = (data: SmsCodeVO) => {
  return request.post({ url: '/system/auth/send-sms-code', data })
}

// 短信验证码登录
export const smsLogin = (data: SmsLoginVO) => {
  return request.post({ url: '/system/auth/sms-login', data })
}

// 社交快捷登录，使用 code 授权码
export function socialLogin(type: string, code: string, state: string) {
  return request.post({
    url: '/system/auth/social-login',
    data: {
      type,
      code,
      state
    }
  })
}

// 社交授权的跳转
export const socialAuthRedirect = (type: number, redirectUri: string) => {
  return request.get({
    url: '/system/auth/social-auth-redirect?type=' + type + '&redirectUri=' + redirectUri
  })
}
// 获取验证图片以及 token
export const getCode = (data: any) => {
  return request.postOriginal({ url: 'system/captcha/get', data })
}

// 滑动或者点选验证
export const reqCheck = (data: any) => {
  return request.postOriginal({ url: 'system/captcha/check', data })
}

// 通过短信重置密码
export const smsResetPassword = (data: any) => {
  return request.post({ url: '/system/auth/reset-password', data })
}
