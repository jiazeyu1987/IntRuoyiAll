/** 使用密码学随机数生成 UUID v4，支持内网 HTTP 页面。 */
export const createSecureUuid = (): string => {
  if (typeof globalThis.crypto?.getRandomValues !== 'function') {
    throw new Error('当前浏览器不支持安全随机数生成，无法生成 UUID')
  }
  const bytes = new Uint8Array(16)
  globalThis.crypto.getRandomValues(bytes)
  // UUID v4：版本位 0100，变体位 10，保留其余 122 位随机数据。
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}
