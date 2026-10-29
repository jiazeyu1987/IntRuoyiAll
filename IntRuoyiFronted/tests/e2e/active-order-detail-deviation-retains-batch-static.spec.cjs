const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { execFileSync } = require('node:child_process')
const { test } = require('node:test')

const root = path.resolve(__dirname, '../..')
const relative = 'IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionActiveOrderDetailPage.vue'
const source = process.argv.includes('--head-fixture')
  ? execFileSync('git', ['show', `HEAD:${relative}`], { cwd: root, encoding: 'utf8' })
  : fs.readFileSync(path.join(root, '../', relative), 'utf8')
const panel = fs.readFileSync(path.join(root, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'), 'utf8')
const block = (name, text = source) => {
  const value = text.match(new RegExp(String.raw`const ${name} = async \(\) => \{([\s\S]*?)\n\}`, 'm'))?.[1]
  assert.ok(value, `${name} 必须存在`)
  return value
}

test('偏差页签使用正式批次身份，保留零批次空态和多批次展示', () => {
  assert.doesNotMatch(source, /label="偏差"|loadDeviationBatches|deviationSequence/)
  assert.match(panel, /label="偏差" name="deviation"/)
  assert.match(panel, /v-for="batch in deviationBatches"/)
  assert.match(panel, /:batch-execution-id="batch.batchExecutionId"/)
  assert.match(panel, /暂无正式批记录，无法查看偏差/)
})

test('主详情按活跃订单直接加载，偏差失败或悬挂不阻断详情', () => {
  const detail = block('loadDetail')
  assert.match(detail, /const query = resolveDetailQuery\(\)/)
  assert.match(detail, /getEdhrBatchActiveOrderDetail\(query\)/)
  assert.doesNotMatch(detail, /getDeviationBatchOptionsByActiveOrder|loadDeviationBatches|deviationBatches/)
  assert.match(source, /if \(activeOrderId\) \{\s*return \{ activeOrderId \}/)
  assert.match(source, /audit-scope-id="auditDetailQuery"/)
  assert.match(source, /<BatchReverseTracePanel/)
})

test('偏差关联独立加载且显式显示失败，不要求唯一正式批次', () => {
  const deviations = block('loadDeviationBatches', panel)
  assert.match(deviations, /getDeviationBatchOptionsByActiveOrder\(activeOrderId!\)/)
  assert.match(deviations, /deviationLoading.value = true/)
  assert.match(deviations, /deviationError.value = cause/)
  assert.match(panel, /v-else-if="deviationError"/)
  assert.match(panel, /@click="loadDeviationBatches"/)
  assert.doesNotMatch(deviations, /length === 1|length !== 1/)
})

test('切换身份清除旧偏差关联并拒绝过期响应，详情失败不清除偏差', () => {
  const deviations = block('loadDeviationBatches', panel)
  assert.match(deviations, /if \(sequence !== deviationSequence\) return\s*deviationBatches.value = batches/)
  assert.ok(deviations.indexOf('deviationBatches.value = []') < deviations.indexOf('await getDeviationBatchOptionsByActiveOrder'))
  assert.match(panel, /\[props.detail\?\.activeOrderId, activeTab.value\]/)
  assert.match(panel, /deviationSequence\+\+\s*deviationBatches.value = \[\]/)
  assert.match(panel, /if \(activeTab.value === 'deviation'\) void loadDeviationBatches\(\)/)
  assert.doesNotMatch(block('loadDetail'), /deviationBatches.value = \[\]/)
})
