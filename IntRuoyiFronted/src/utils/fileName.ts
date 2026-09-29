const isContinuationByte = (value: number) => value >= 0x80 && value <= 0xbf

const hasValidUtf8PercentRun = (value: string) => {
  if (!value.includes('%') || /%(?![0-9a-f]{2})/i.test(value)) return false
  const runs = [...value.matchAll(/(?:%[0-9a-f]{2})+/gi)]
  if (!runs.length) return false

  return runs.every((run) => {
    const bytes = [...run[0].matchAll(/%([0-9a-f]{2})/gi)].map((match) =>
      Number.parseInt(match[1], 16)
    )
    for (let index = 0; index < bytes.length; index++) {
      const first = bytes[index]
      if (first <= 0x7f) continue
      if (first >= 0xc2 && first <= 0xdf) {
        if (!isContinuationByte(bytes[++index])) return false
        continue
      }
      if (first >= 0xe0 && first <= 0xef) {
        const second = bytes[++index]
        const third = bytes[++index]
        if (!isContinuationByte(second) || !isContinuationByte(third)) return false
        if (first === 0xe0 && second < 0xa0) return false
        if (first === 0xed && second > 0x9f) return false
        continue
      }
      if (first >= 0xf0 && first <= 0xf4) {
        const second = bytes[++index]
        const third = bytes[++index]
        const fourth = bytes[++index]
        if (!isContinuationByte(second) || !isContinuationByte(third) || !isContinuationByte(fourth)) return false
        if (first === 0xf0 && second < 0x90) return false
        if (first === 0xf4 && second > 0x8f) return false
        continue
      }
      return false
    }
    return true
  })
}

/** Decode a URL pathname segment once; malformed/literal percent names stay opaque. */
export const decodeUrlPathFileName = (value: string) => {
  if (!value.includes('%') || !hasValidUtf8PercentRun(value)) return value
  return decodeURIComponent(value)
}

export const resolveUrlPathFileName = (url: string, origin = window.location.origin) => {
  const pathname = new URL(url, origin).pathname
  const fileName = pathname.substring(pathname.lastIndexOf('/') + 1)
  if (!fileName) throw new Error('文件路径缺少文件名，无法显示。')
  return decodeUrlPathFileName(fileName)
}
