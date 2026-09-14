import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const frontRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const component = readFileSync(
  resolve(frontRoot, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'),
  'utf8'
)
const api = readFileSync(resolve(frontRoot, 'src/api/mes/pro/processpool/teamLeader.ts'), 'utf8')
const workOrderPage = readFileSync(resolve(frontRoot, 'src/views/mes/pro/workorder/index.vue'), 'utf8')
const detailPage = readFileSync(
  resolve(frontRoot, 'src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue'),
  'utf8'
)

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

assert(
  component.includes('label="生产工单"') &&
    component.includes('name="workOrder"') &&
    component.includes('data-team-leader-active-order-detail-work-order-tab'),
  '工序提交详情主 tab 必须在领料单/补料单同级新增生产工单 tab'
)

const workOrderTabMatch = component.match(
  /<el-tab-pane[\s\S]*?label="生产工单"[\s\S]*?data-team-leader-active-order-detail-work-order-tab[\s\S]*?<\/el-tab-pane>/
)
assert(workOrderTabMatch, '必须存在可独立定位的生产工单 tab 面板')
const workOrderTab = workOrderTabMatch[0]

for (const label of ['工单编号', '批次号', '数量', '规格型号', '产品代码', '产品名称', '创建时间']) {
  assert(workOrderTab.includes(label), `生产工单 tab 缺少重点字段：${label}`)
}

for (const field of [
  'activeOrderWorkOrderDisplay.workOrderCode',
  'activeOrderWorkOrderDisplay.batchCode',
  'activeOrderWorkOrderDisplay.quantity',
  'activeOrderWorkOrderDisplay.productSpecification',
  'activeOrderWorkOrderDisplay.productCode',
  'activeOrderWorkOrderDisplay.productName',
  'activeOrderWorkOrderDisplay.createTime'
]) {
  assert(workOrderTab.includes(field), `生产工单 tab 必须展示统一工单字段：${field}`)
}

assert(
  component.includes('team-leader-workbench__work-order-highlight-grid') &&
    component.includes('team-leader-workbench__work-order-highlight-value') &&
    component.includes('data-active-order-work-order-highlight'),
  '生产工单 tab 必须用突出卡片展示核心字段'
)

assert(
  component.includes('data-active-order-work-order-standard-list') &&
    component.includes(':data="[activeOrderWorkOrderDisplay]"') &&
    component.includes('data-active-order-work-order-list-link'),
  '生产工单 tab 必须用标准列表模板再次展示工单事实，并提供列表内可点击工单编号'
)

assert(
  component.includes('openWorkOrderList(activeOrderWorkOrderDisplay.workOrderCode)') &&
    component.includes("path: '/mes/pro/work-order'") &&
    component.includes('query: { code: workOrderCode }'),
  '工单号必须可点击跳转到生产工单列表并携带 code 查询参数'
)
assert(
  detailPage.includes('route.query.sourceWorkOrderCode') &&
    detailPage.includes('loadSourceWorkOrder') &&
    component.includes('sourceWorkOrder?: ProWorkOrderVO') &&
    component.includes('const source = props.sourceWorkOrder'),
  'Stage1 来源详情必须优先使用来源生产工单真实字段展示生产工单 tab'
)
assert(
  workOrderPage.includes('route.query.code') && workOrderPage.includes('queryParams.code'),
  '生产工单列表必须能从 URL query.code 回填查询条件'
)

assert(
  /export interface TeamLeaderActiveOrderDetailRespVO[\s\S]*workOrderId: number[\s\S]*workOrderCode: string[\s\S]*batchCode\?: string[\s\S]*productSpecification\?: string[\s\S]*workOrderQuantity\?: number \| string[\s\S]*productCode\?: string[\s\S]*productName\?: string[\s\S]*workOrderCreateTime\?: string \| number[\s\S]*routeName: string[\s\S]*processes: TeamLeaderActiveOrderProcessDetailRespVO\[\]/.test(api),
  '前端详情 API 类型必须具备生产工单 tab 所需正式字段'
)

console.log('PASS: active order detail work order tab static contract')
