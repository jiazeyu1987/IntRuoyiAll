// Module-owned numeric identities must preserve the full positive Java Long range.
export const requireExactJavaLong = (value: unknown, label: string): string => {
  if (typeof value === 'number' && !Number.isSafeInteger(value)) {
    throw new Error(`${label}不是精确标识。`)
  }
  const identity = typeof value === 'string' || typeof value === 'number' ? String(value) : ''
  if (!/^[1-9]\d*$/.test(identity) || identity.length > 19 ||
    (identity.length === 19 && identity > '9223372036854775807')) {
    throw new Error(`缺少有效${label}。`)
  }
  return identity
}
