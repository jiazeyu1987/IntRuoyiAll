import { formatDate } from '@/utils/formatTime'
import type { RuntimeControlDateTime } from '@/api/infra/runtimeControl'

export type OpsTagType = 'success' | 'warning' | 'danger' | 'info'

export const formatRuntimeDate = (value?: RuntimeControlDateTime) => {
  if (!value) return '-'
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    return formatDate(new Date(year, month - 1, day, hour, minute, second))
  }
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : formatDate(date)
}

export const opsStatusText = (status?: string) => {
  const textMap: Record<string, string> = {
    PASS: '通过',
    WARN: '预警',
    BLOCKED: '已阻断',
    NO_GO: '不放行',
    AVAILABLE: '可用',
    SENT: '已发送',
    FAILED: '失败',
    running: '运行中',
    succeeded: '成功',
    degraded: '异常',
    stopped: '已停止',
    failed: '失败',
    error: '错误',
    OPEN: '处理中',
    CLOSED: '已关闭',
    PASSED: '已通过',
    DONE: '已完成'
  }
  return status ? textMap[status] || status : '-'
}

export const opsTagType = (status?: string): OpsTagType => {
  if (['PASS', 'AVAILABLE', 'SENT', 'running', 'succeeded', 'PASSED', 'CLOSED', 'DONE'].includes(status || '')) {
    return 'success'
  }
  if (['WARN', 'degraded', 'OPEN'].includes(status || '')) {
    return 'warning'
  }
  if (['BLOCKED', 'NO_GO', 'FAILED', 'failed', 'error', 'stopped'].includes(status || '')) {
    return 'danger'
  }
  return 'info'
}

export const environmentText = (environment?: string) => {
  const textMap: Record<string, string> = {
    local: 'Local',
    test: 'Test',
    prod: 'Production',
    backup: 'Backup'
  }
  return environment ? textMap[environment] || environment : '-'
}

export const actionText = (action?: string) => {
  const textMap: Record<string, string> = {
    'build-release': '构建发布包',
    'publish-test': '部署发布包到测试服',
    'mark-release-tested': '标记测试通过',
    'promote-prod': '上线已验证发布包',
    'promote-backup': '上线备份服务器',
    'backup-now': '立即备份',
    'rollback-app': '回滚版本',
    'restore-data': '恢复数据',
    restart: '重启'
  }
  return action ? textMap[action] || action : '-'
}

export const joinReasons = (reasons?: string[]) => {
  return reasons && reasons.length > 0 ? reasons.join('；') : '-'
}

export const bytesText = (value?: number) => {
  if (value === undefined || value === null) return '-'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = value
  let unitIndex = 0
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex += 1
  }
  return `${size.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
}

export const percentText = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return `${Number(value).toFixed(1)}%`
}
