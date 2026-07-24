const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const source = fs.readFileSync(pagePath, 'utf8')

for (const token of [
  '字段差异',
  'buildOperationLogDiffRows',
  'parseOperationSnapshot',
  'getOperationFieldLabel',
  '旧值',
  '新值',
  'beforeSnapshotJson',
  'afterSnapshotJson',
  '快照 JSON 解析失败',
  'row.operationType',
  'row.operatorName',
  'row.createTime'
]) {
  assert.ok(source.includes(token), `排产工单追溯必须展示字段差异与审计上下文: ${token}`)
}

assert.ok(!source.includes('catch {}'), '排产工单追溯不得静默吞掉快照解析异常')

console.log('PASS: MES schedule order trace diff frontend contract')
