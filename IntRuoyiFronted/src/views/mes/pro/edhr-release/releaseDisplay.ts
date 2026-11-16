export const dossierFileSizeLabel = (source: string | number) => {
  if (typeof source === 'number' && !Number.isSafeInteger(source)) return '文件大小格式无效'
  if (!/^\d+$/.test(String(source))) return '文件大小格式无效'
  const bytes = BigInt(source)
  if (bytes < 1024n) return `${bytes} 字节`
  const units = ['KB', 'MB', 'GB', 'TB']
  let divisor = 1024n,
    unit = 0
  while (bytes >= divisor * 1024n && unit < units.length - 1) {
    divisor *= 1024n
    unit++
  }
  const tenths = (bytes * 10n + divisor / 2n) / divisor
  return `${tenths / 10n}.${tenths % 10n} ${units[unit]}`
}
