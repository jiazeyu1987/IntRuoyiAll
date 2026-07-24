export const showroomPhase1E2ECases = [
  {
    id: 'showroom-phase1-admin-content-approval',
    title: 'Phase 1 后台公司/产品内容与审批入口 E2E',
    modulePath: './showroom-phase1-admin-content-approval.e2e.mjs',
    owner: 'Agent A',
    phase1Scope: ['T2 content workflow', 'T4 admin frontend', 'T6 integration'],
    scenarios: [
      '编辑人从真实登录进入展厅后台公司与产品工作台',
      '后台工作台通过实时接口加载公司、产品、展厅和审批数据',
      '审批中心、版本历史、补充指派、产品讨论入口可见',
      '缺少真实编辑、提交或审批控件时显式失败'
    ]
  }
]
