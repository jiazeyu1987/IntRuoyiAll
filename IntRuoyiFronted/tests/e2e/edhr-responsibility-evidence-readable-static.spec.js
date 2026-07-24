const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr/FieldAuditPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const expandStart = source.indexOf('<div class="edhr-field-audit__evidence">')
assert.ok(expandStart >= 0, '字段责任表格必须保留责任证据展开区。')

const expandEnd = source.indexOf('</template>', expandStart)
assert.ok(expandEnd > expandStart, '责任证据展开区必须位于展开列模板内。')

const evidencePanel = source.slice(expandStart, expandEnd)
const technicalStart = evidencePanel.indexOf('<details class="edhr-field-audit__technical-details">')
assert.ok(technicalStart > 0, '机器字段必须收敛到默认折叠的技术详情内。')

const defaultSurface = evidencePanel.slice(0, technicalStart)
const technicalDetails = evidencePanel.slice(technicalStart)

for (const label of ['字段名称', '当前填写', '填写状态', '填写责任', '操作记录']) {
  assert.ok(defaultSurface.includes(label), `责任证据默认视图必须优先展示业务可读信息：${label}`)
}

for (const machineToken of [
  '字段身份',
  '当前值 JSON',
  '当前值 hash',
  'rowIndex',
  'columnIndex',
  'currentValueJson',
  'currentValueHash'
]) {
  assert.ok(
    !defaultSurface.includes(machineToken),
    `责任证据默认视图不得直接暴露机器字段：${machineToken}`
  )
}

assert.ok(technicalDetails.includes('<summary>技术详情</summary>'), '技术字段必须有明确的折叠入口。')
assert.doesNotMatch(
  technicalDetails,
  /<details[^>]*\sopen(?:[\s=>]|$)/,
  '技术详情不得默认展开。'
)

for (const technicalToken of ['currentValueJson', 'currentValueHash', 'row.rowIndex', 'row.columnIndex']) {
  assert.ok(technicalDetails.includes(technicalToken), `技术详情仍需保留排查证据：${technicalToken}`)
}

console.log('PASS: EDHR responsibility evidence readable static contract')
