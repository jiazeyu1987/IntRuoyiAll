const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const profileIndex = read('src/views/Profile/Index.vue')
const componentIndex = read('src/views/Profile/components/index.ts')
const component = read('src/views/Profile/components/ProfileNasTableAutoSyncSetting.vue')
const api = read('src/api/erp/nasTableSync/index.ts')

assert.match(
  profileIndex,
  /const GOLDEN_FINGER_PERMISSION = 'mes:pro-batch-record-execution:golden-finger'/,
  '个人工作台配置页签必须继续复用 golden-finger 权限边界。'
)
assert.match(profileIndex, /<el-tabs[\s\S]*NAS表格自动同步/, '配置页签内部必须新增 NAS 表格自动同步 tab。')
assert.match(profileIndex, /<ProfileNasTableAutoSyncSetting\s*\/>/, '配置页签必须渲染 NAS 表格自动同步组件。')
assert.match(componentIndex, /ProfileNasTableAutoSyncSetting/, 'Profile 组件导出必须包含 NAS 表格自动同步组件。')

for (const token of [
  '/erp/nas-table-sync/plan/get',
  '/erp/nas-table-sync/plan/save',
  '/erp/nas-table-sync/sync-types',
  '/erp/nas-table-sync/plan/test-nas-write',
  '/erp/nas-table-sync/plan/run-once',
  '/erp/nas-table-sync/run/page'
]) {
  assert.match(api, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `API wrapper 必须包含接口：${token}`)
}

for (const token of [
  'NAS表格自动同步',
  '每日开始时间',
  'ERP 表数据',
  'NAS 目录',
  '文件名规则',
  '测试NAS写入',
  '立即执行一次',
  '最近执行日志',
  'failureMessage',
  'ElMessage.error'
]) {
  assert.match(component, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `组件必须包含用户可见能力：${token}`)
}

assert.doesNotMatch(
  component,
  /JobApi|triggerJob|system\/nas|mock|placeholder/i,
  '组件不得直接拼接 infra job/NAS 或 mock 数据，必须通过业务 API。'
)

console.log('PASS: profile NAS table auto sync static contract')
