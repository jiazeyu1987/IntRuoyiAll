export interface ShowroomDashboardMetrics {
  liveHallCount: number
  productCount: number
  incompleteProductCount: number
  pendingApprovalCount: number
  pendingAssignmentCount: number
}

export interface ShowroomDashboardCard {
  key: string
  label: string
  value: string
  helper: string
  tagType: 'success' | 'warning' | 'info'
}

const PAGE_SIZE = 20

const expectArray = (value: unknown, fieldName: string): unknown[] => {
  if (!Array.isArray(value)) {
    throw new Error(`Dashboard 缺少数组字段：${fieldName}`)
  }
  return value
}

export const fetchPagedTotal = async (
  loader: (pageNo: number, pageSize: number) => Promise<unknown>
) => {
  let total = 0
  let pageNo = 1
  while (pageNo <= 500) {
    const rows = expectArray(await loader(pageNo, PAGE_SIZE), `page#${pageNo}`)
    total += rows.length
    if (rows.length < PAGE_SIZE) {
      return total
    }
    pageNo += 1
  }
  throw new Error('Dashboard 分页统计超过 500 页，无法继续计算总数')
}

export const createDashboardCards = (
  metrics: ShowroomDashboardMetrics
): ShowroomDashboardCard[] => {
  return [
    {
      key: 'live-halls',
      label: '展柜总数',
      value: String(metrics.liveHallCount),
      helper: '根据真实展柜分页汇总',
      tagType: 'success'
    },
    {
      key: 'products',
      label: '产品总数',
      value: String(metrics.productCount),
      helper: '根据真实产品分页汇总',
      tagType: 'success'
    },
    {
      key: 'incomplete-products',
      label: '资料未完善产品',
      value: String(metrics.incompleteProductCount),
      helper: '根据 incompleteStatus=INCOMPLETE 真实筛选',
      tagType: 'warning'
    },
    {
      key: 'pending-approvals',
      label: '待审批',
      value: String(metrics.pendingApprovalCount),
      helper: '根据真实审批待办列表统计',
      tagType: 'warning'
    },
    {
      key: 'pending-assignments',
      label: '补充指派待办',
      value: String(metrics.pendingAssignmentCount),
      helper: '根据真实 assignment/page 统计',
      tagType: 'info'
    },
    {
      key: 'stale-audio-blocked',
      label: '讲解音频陈旧',
      value: '统计暂不可用',
      helper: '讲解音频陈旧统计待后端契约补齐',
      tagType: 'warning'
    }
  ]
}
