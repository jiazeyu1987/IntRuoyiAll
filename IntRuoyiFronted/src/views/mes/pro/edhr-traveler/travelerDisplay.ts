export const travelerEventLabel = (value?: string) => {
  if (!value) return '未记录'
  const labels: Record<string, string> = {
    GENERATE: '生成流转单',
    GENERATE_DUPLICATE: '重复生成请求'
  }
  return labels[value] || `未识别事件（${value}）`
}
export const travelerResultLabel = (value?: string) => {
  if (!value) return '未记录'
  const labels: Record<string, string> = { SUCCESS: '操作成功', BLOCKED: '已阻止' }
  return labels[value] || `未识别结果（${value}）`
}
