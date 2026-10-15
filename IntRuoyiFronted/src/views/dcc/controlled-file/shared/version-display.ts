// A working iteration appends -n to the configured formal slash-separated identity.
const WORKING_ITERATION_VERSION = /^[A-Z]+(?:\/[1-9][0-9]*)+-[1-9][0-9]*$/i

/** Display only. Keep the original versionNo and exact file ID for every request. */
export const getDccVersionDisplayText = (versionNo: string | null | undefined): string => {
  if (versionNo == null) return '未记录'
  if (typeof versionNo !== 'string') throw new TypeError('版本显示值必须为字符串')
  const normalized = versionNo.trim()
  if (!normalized) return '未记录'
  return WORKING_ITERATION_VERSION.test(normalized) ? '草稿' : versionNo
}
