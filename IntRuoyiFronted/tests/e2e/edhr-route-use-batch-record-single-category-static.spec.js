import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, '../..')
const routeFlowConfigPanel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteFlowConfigPanel.vue'),
  'utf8'
)

assert.doesNotMatch(
  routeFlowConfigPanel,
  /v-model="report\.recordCategory"[\s\S]*recordCategoryOptions/,
  '批记录表格绑定入口不得再提供记录类型下拉'
)

assert.doesNotMatch(
  routeFlowConfigPanel,
  /内部记录表/,
  '批记录表格绑定入口不得把内部记录表展示为另一份上传类型'
)

assert.match(
  routeFlowConfigPanel,
  /const BATCH_RECORD_CATEGORY:[\s\S]*=\s*'BATCH_RECORD'/,
  '批记录表格绑定入口必须定义固定 BATCH_RECORD 类型'
)

assert.match(
  routeFlowConfigPanel,
  /const BATCH_RECORD_VALIDATION_PROFILE:[\s\S]*=\s*'CONTROLLED_BATCH'/,
  '批记录表格绑定入口必须定义固定受控批记录校验'
)

assert.match(
  routeFlowConfigPanel,
  /recordCategory:\s*BATCH_RECORD_CATEGORY/,
  '新增和保存批记录表格必须使用固定 BATCH_RECORD 常量'
)

assert.match(
  routeFlowConfigPanel,
  /validationProfile:\s*BATCH_RECORD_VALIDATION_PROFILE/,
  '新增和保存批记录表格必须使用固定受控批记录校验常量'
)
