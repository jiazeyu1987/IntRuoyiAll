export interface PortalAttachmentDisplay {
  address: string
  name: string
  host: string
  error: string
}
// 申请表按每行一个地址保存；查询参数中的逗号属于地址内容，不作为分隔符。
export const readPortalAttachments = (source?: string): PortalAttachmentDisplay[] =>
  (source || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((address, index) => {
      const item = { address, name: `附件 ${index + 1}（未提供文件名）`, host: '', error: '' }
      let url: URL
      try {
        url = new URL(address)
      } catch {
        return { ...item, error: '附件地址格式无效，请核对原始地址' }
      }
      if (!['http:', 'https:'].includes(url.protocol))
        return { ...item, error: '附件地址不是网页地址，请核对原始地址' }
      item.host = url.host
      const lastSegment = url.pathname.split('/').pop()
      if (lastSegment) {
        try {
          item.name = decodeURIComponent(lastSegment)
        } catch {
          item.error = '文件名编码无效，请核对原始地址'
        }
      }
      return item
    })
