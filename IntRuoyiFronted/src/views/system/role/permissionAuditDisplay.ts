export const permissionAuditResultLabel = (value?: string) => {
  if (!value) return '未记录'
  if (value === 'SUCCESS') return '操作成功'
  if (value === 'FAILED') return '操作失败'
  if (value === 'DENIED') return '权限拒绝'
  return `未识别结果（${value}）`
}
export const permissionAuditSubjectLabel = (value: string) => {
  const match = /^(SYSTEM_ROLE|SYSTEM_USER):(.+)$/.exec(value)
  if (!match) return `对象标识：${value}`
  return `${match[1] === 'SYSTEM_ROLE' ? '角色' : '用户'}编号 ${match[2]}`
}
