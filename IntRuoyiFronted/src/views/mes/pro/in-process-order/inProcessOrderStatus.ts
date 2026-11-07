import type { InProcessOrder } from '@/api/mes/pro/inProcessOrder'

type Status = { label: string; type: 'success' | 'warning' | 'danger' | 'info'; reason?: string }

const releaseStatuses: Record<string, Status> = {
  PQC_RELEASE_PENDING: { label: '待PQC放行', type: 'warning' },
  PQC_RELEASE_REJECTED: { label: 'PQC放行已驳回', type: 'danger' },
  REPORT_UPLOAD_PENDING: { label: '待补放行资料', type: 'warning' },
  MANAGER_RELEASE_PENDING: { label: '待上市放行', type: 'warning' },
  MARKET_RELEASE_REJECTED: { label: '上市放行已驳回', type: 'danger' },
  MARKET_RELEASE_WITHDRAWN: { label: '上市放行已撤回', type: 'warning' },
  RELEASED: { label: '已上市放行', type: 'success' },
  NONCONFORMANCE_VOID: { label: 'QA已作废', type: 'danger' },
  NONCONFORMANCE_REWORK: { label: '已结束并转入返工', type: 'info' }
}

export const resolveInProcessOrderStatus = (row: InProcessOrder): Status => {
  if (row.readBlocked) return { label: '数据异常', type: 'danger', reason: row.readBlockReason }
  const release = row.releaseApplicationStatus
  if (release === 'NONCONFORMANCE_VOID' || release === 'NONCONFORMANCE_REWORK') {
    return releaseStatuses[release]
  }
  if (row.businessStatus === 'VERSION_UPGRADE_PENDING') return { label: '升版审批中', type: 'warning' }
  if (row.abnormal) return { label: '异常处理中', type: 'warning', reason: row.abnormalReason }
  if (release) {
    return releaseStatuses[release] || { label: `未知放行状态：${release}`, type: 'danger' }
  }
  if (row.businessStatus === 'COMPLETED') return { label: '已完工待放行', type: 'info' }
  return { label: '在制', type: 'success' }
}
