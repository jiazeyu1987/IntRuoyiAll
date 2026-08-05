const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const profileIndex = read('src/views/Profile/Index.vue')
const componentIndex = read('src/views/Profile/components/index.ts')
const component = read('src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue')
const api = read('src/api/erp/kingdeeTableAutoSync/index.ts')

assert.match(
  profileIndex,
  /const GOLDEN_FINGER_PERMISSION = 'mes:pro-batch-record-execution:golden-finger'/,
  '个人工作台配置页签必须继续复用 golden-finger 权限边界。'
)
assert.match(profileIndex, /<el-tabs[\s\S]*ERP表格自动同步/, '配置页签内部必须新增 ERP 表格自动同步 tab。')
assert.match(profileIndex, /<ProfileErpTableAutoSyncSetting\s*\/>/, '配置页签必须渲染 ERP 表格自动同步组件。')
assert.match(componentIndex, /ProfileErpTableAutoSyncSetting/, 'Profile 组件导出必须包含 ERP 表格自动同步组件。')

for (const token of [
  '/erp/kingdee-table-auto-sync/plan/get',
  '/erp/kingdee-table-auto-sync/plan/save',
  '/erp/kingdee-table-auto-sync/sync-types',
  '/erp/kingdee-table-auto-sync/plan/run-once',
  '/erp/kingdee-table-auto-sync/run/page',
  '/erp/kingdee-table-auto-sync/watermark/list'
]) {
  assert.match(api, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `API wrapper 必须包含接口：${token}`)
}

for (const token of [
  'ERP表格自动同步',
  '每日开始时间',
  'ERP 表格',
  '同步水位',
  '立即执行一次',
  '最近执行记录',
  'failureMessage',
  'ElMessage.error',
  'PRODUCT',
  'BOM'
]) {
  assert.match(component, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `组件必须包含用户可见能力：${token}`)
}

assert.doesNotMatch(
  component,
  /NasTableSync|nas-table-sync|testNasWrite|NAS 目录|文件名规则|mock|placeholder/i,
  'ERP 表格自动同步组件不得混用 NAS 导出 API、NAS 字段或 mock 数据。'
)

console.log('PASS: profile ERP table auto sync static contract')
