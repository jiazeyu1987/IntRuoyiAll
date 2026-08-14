const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const profileIndex = read('src/views/Profile/Index.vue')
const componentIndex = read('src/views/Profile/components/index.ts')

assert.match(
  profileIndex,
  /const GOLDEN_FINGER_PERMISSION = 'mes:pro-batch-record-execution:golden-finger'/,
  '个人工作台配置页签必须继续复用 golden-finger 权限边界。'
)

assert.doesNotMatch(
  profileIndex,
  /NAS表格自动同步|<ProfileNasTableAutoSyncSetting\s*\/>|name="nasTableSync"/,
  '配置页签不得继续显示或渲染 NAS 表格自动同步入口。'
)

assert.doesNotMatch(
  componentIndex,
  /ProfileNasTableAutoSyncSetting/,
  'Profile 组件导出不得继续暴露 NAS 表格自动同步组件。'
)

assert.match(profileIndex, /ERP表格自动同步/, '配置页签必须保留 ERP 表格自动同步入口。')
assert.match(profileIndex, /<ProfileErpTableAutoSyncSetting\s*\/>/, '配置页签必须继续渲染 ERP 表格自动同步组件。')

console.log('PASS: profile NAS table auto sync removal static contract')
