import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const repoRoot = process.cwd()
const listPath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/batchExecution.ts')
const listPage = fs.readFileSync(listPath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

assert.match(
  apiSource,
  /export interface EdhrBatchExecutionRouteOptionRespVO[\s\S]*routeId: number[\s\S]*routeCode\?: string[\s\S]*routeName\?: string[\s\S]*batchRouteEnabled\?: boolean/,
  'eDHR 批次执行 API 必须暴露工单可用路线选项类型。'
)

assert.match(
  apiSource,
  /export const getEdhrBatchExecutionRouteOptions\s*=\s*async\s*\(workOrderId: number\)[\s\S]*work-order-route-options/,
  'eDHR 批次执行 API 必须提供按工单加载可用路线的接口。'
)

assert.match(
  listPage,
  /createForm\s*=\s*reactive\([\s\S]*routeId:\s*undefined as number \| undefined/,
  '打开或创建表单必须保存 routeId，避免多路线产品靠后端猜测。'
)

assert.match(
  listPage,
  /const\s+createRouteOptions\s*=\s*ref<EdhrBatchExecutionRouteOptionRespVO\[\]>\(\[\]\)/,
  '打开或创建弹窗必须维护工单可用路线选项。'
)

assert.match(
  listPage,
  /<el-form-item label="工艺路线" required>[\s\S]*v-model="createForm\.routeId"[\s\S]*v-for="routeOption in createRouteOptions"/,
  '打开或创建弹窗必须提供工艺路线选择框。'
)

assert.match(
  listPage,
  /const\s+loadCreateRouteOptions\s*=\s*async\s*\(workOrderId\?: number\)[\s\S]*getEdhrBatchExecutionRouteOptions\(workOrderId\)[\s\S]*createForm\.routeId\s*=\s*createRouteOptions\.value\[0\]\.routeId/,
  '选择工单后必须加载路线选项，只有一条路线时才自动选中。'
)

assert.match(
  listPage,
  /handleWorkOrderChange[\s\S]*await loadCreateRouteOptions\(workOrderId\)/,
  '工单变更后必须刷新路线选项，不能复用上一工单路线。'
)

assert.match(
  listPage,
  /if\s*\(createForm\.routeId == null\)\s*\{[\s\S]*请选择工艺路线。'[\s\S]*return[\s\S]*routeId:\s*createForm\.routeId/,
  '提交打开或创建时必须校验路线选择并在正式请求中传递 routeId。'
)
