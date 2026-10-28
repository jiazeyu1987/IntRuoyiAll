/** Generate permission audit IDs with the browser's random source on HTTP and HTTPS. */
export const createPermissionAuditId = (scope, targetId) => {
  if (typeof crypto !== 'object' || typeof crypto.getRandomValues !== 'function') {
    throw new Error('浏览器不支持安全随机数，无法提交权限变更，请使用支持的浏览器')
  }
  const bytes = crypto.getRandomValues(new Uint8Array(16))
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = Array.from(bytes, (value) => value.toString(16).padStart(2, '0')).join('')
  const uuid = `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
  return `SYSTEM-PERM-${scope}-${targetId}-${uuid}`
}
