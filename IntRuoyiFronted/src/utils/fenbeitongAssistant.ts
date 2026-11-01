/** Resolve the explicitly configured same-server template for each deployment. */
export const resolveFenbeitongAssistantUrl = (configured: string, hostname: string): string => {
  const template = configured.trim()
  if (!template) return ''
  const url = new URL(template.replace('{hostname}', hostname))
  if (!['http:', 'https:'].includes(url.protocol)) {
    throw new Error('分贝通助手地址必须使用 HTTP 或 HTTPS')
  }
  return url.toString()
}
