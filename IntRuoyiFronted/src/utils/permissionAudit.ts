import { createSecureUuid } from './uuid'

export class PermissionAuditKeyError extends Error {
  constructor(message: string, readonly cause?: unknown) {
    super(message)
    this.name = 'PermissionAuditKeyError'
  }
}

/** 权限审计使用密码学随机 UUID；getRandomValues 在内网 HTTP 页面也可用。 */
export const createPermissionAuditIdempotencyKey = (
  operation: 'USER-ROLE' | 'ROLE-MENU' | 'DATA-SCOPE',
  subjectId: number
): string => {
  if (typeof globalThis.crypto?.getRandomValues !== 'function') {
    throw new PermissionAuditKeyError('当前浏览器不支持安全随机数生成，无法生成权限审计幂等键')
  }
  try {
    return `SYSTEM-PERM-${operation}-${subjectId}-${createSecureUuid()}`
  } catch (error) {
    throw new PermissionAuditKeyError('权限审计幂等键生成失败，请检查浏览器后重试', error)
  }
}
