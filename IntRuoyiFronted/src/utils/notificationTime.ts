/** 通知时间仅接受毫秒时间戳或含时区的 ISO 时间，不猜测无时区字符串。 */
const beijingFormatter = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23'
})
export const formatNotificationSentAt = (value: string | number | null | undefined): string => {
  if (value === null || value === undefined || value === '') return '未记录'
  let milliseconds: number
  if (typeof value === 'number' || /^\d{13}$/.test(value)) {
    milliseconds = Number(value)
    if (!Number.isSafeInteger(milliseconds) || milliseconds <= 0) return '通知时间异常（无效毫秒时间戳）'
  } else {
    if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,3})?(?:Z|[+-]\d{2}:\d{2})$/.test(value)) {
      return '通知时间异常（需要毫秒时间戳或含时区时间）'
    }
    const [year, month, day, hour, minute, second] = value.slice(0, 19).split(/[-T:]/).map(Number)
    if (month < 1 || month > 12 || day < 1 || day > new Date(Date.UTC(year, month, 0)).getUTCDate()
      || hour > 23 || minute > 59 || second > 59) return '通知时间异常（无效日期）'
    milliseconds = Date.parse(value)
  }
  const date = new Date(milliseconds)
  if (!Number.isFinite(date.getTime())) return '通知时间异常（无效日期）'
  const parts = Object.fromEntries(beijingFormatter.formatToParts(date).map(part => [part.type, part.value]))
  return `${parts.year}-${parts.month}-${parts.day} ${parts.hour}:${parts.minute}:${parts.second}`
}
