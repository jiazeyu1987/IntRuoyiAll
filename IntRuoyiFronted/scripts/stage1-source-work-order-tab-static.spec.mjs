import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const frontRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const detailPage = readFileSync(
  resolve(frontRoot, 'src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue'),
  'utf8'
)
const component = readFileSync(
  resolve(frontRoot, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'),
  'utf8'
)
const workOrderApi = readFileSync(resolve(frontRoot, 'src/api/mes/pro/workorder/index.ts'), 'utf8')

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

assert(
  detailPage.includes('route.query.sourceWorkOrderCode') &&
    detailPage.includes('loadSourceWorkOrder') &&
    detailPage.includes('ProWorkOrderApi.getWorkOrderPage'),
  '详情页必须在 sourceWorkOrderCode 存在时读取来源生产工单列表'
)

assert(
  detailPage.includes('row.code === sourceWorkOrderCode') &&
    detailPage.includes('rows.length !== 1') &&
    detailPage.includes('未找到或不唯一'),
  '来源生产工单必须按工单编号精确匹配；缺失或不唯一时必须失败，不能静默降级'
)

assert(
  detailPage.includes(':source-work-order="sourceWorkOrder"'),
  '详情页必须把来源生产工单传给详情面板'
)

assert(
  component.includes('sourceWorkOrder?: ProWorkOrderVO') &&
    component.includes('const activeOrderWorkOrderDisplay = computed') &&
    component.includes('const source = props.sourceWorkOrder'),
  '详情面板必须接收并优先计算来源生产工单展示对象'
)

for (const sourceField of [
  'source.batchCode',
  'source.code',
  'source.quantity',
  'source.productSpecification',
  'source.productCode',
  'source.productName',
  'source.createTime'
]) {
  assert(component.includes(sourceField), `来源生产工单展示缺少真实字段：${sourceField}`)
}

for (const detailField of [
  'detail?.batchCode',
  'detail?.workOrderCode',
  'detail?.workOrderQuantity',
  'detail?.productSpecification',
  'detail?.productCode',
  'detail?.productName',
  'detail?.workOrderCreateTime'
]) {
  assert(component.includes(detailField), `普通详情展示必须仍保留当前详情字段：${detailField}`)
}

assert(
  component.includes('{{ activeOrderWorkOrderDisplay.workOrderCode }}') &&
    component.includes(':data="[activeOrderWorkOrderDisplay]"') &&
    component.includes('@click="openWorkOrderList(activeOrderWorkOrderDisplay.workOrderCode)"'),
  '生产订单汇总、生产工单标准列表和重点卡片必须使用统一展示对象'
)

assert(
  workOrderApi.includes('createTime?: string | number | Date'),
  '生产工单 API 类型必须声明 createTime，供来源工单真实创建时间展示'
)

console.log('PASS: stage1 source work order tab static contract')
