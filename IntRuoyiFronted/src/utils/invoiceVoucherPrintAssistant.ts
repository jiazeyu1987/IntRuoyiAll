/** Resolve the configured same-server template without selecting another environment. */
export const resolveInvoiceVoucherPrintAssistantUrl = (configured: string, hostname: string): string => {
  const template = configured.trim()
  if (!template) return ''
  const url = new URL(template.replace('{hostname}', hostname))
  if (!['http:', 'https:'].includes(url.protocol)) {
    throw new Error('发票凭证打印助手地址必须使用 HTTP 或 HTTPS')
  }
  return url.toString()
}
