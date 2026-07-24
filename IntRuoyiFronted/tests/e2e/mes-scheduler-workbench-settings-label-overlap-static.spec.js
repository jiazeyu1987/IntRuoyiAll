const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const pageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/scheduler-workbench/index.vue'),
  'utf8'
)

const settingsLabelRule = pageSource.match(
  /\.scheduler-workbench__settings-field :deep\(\.el-form-item__label\) \{[\s\S]*?\n\}/
)
const policyLabelRule = pageSource.match(
  /\.scheduler-workbench__policy-item :deep\(\.el-form-item__label\) \{[\s\S]*?\n\}/
)

assert.ok(settingsLabelRule, '班时和排程规则字段必须定义标签布局规则。')
assert.ok(policyLabelRule, '策略字段必须定义标签布局规则。')

for (const [name, rule] of [
  ['班时/排程规则', settingsLabelRule[0]],
  ['策略', policyLabelRule[0]]
]) {
  assert.match(
    rule,
    /width:\s*var\(--scheduler-settings-label-width\)\s*!important/,
    `${name}标签必须覆盖 Element Plus label-width=0 的内联宽度，避免标签文字被输入控件覆盖。`
  )
  assert.match(
    rule,
    /min-width:\s*var\(--scheduler-settings-label-width\)/,
    `${name}标签必须保留固定最小标签列。`
  )
  assert.match(
    rule,
    /overflow:\s*visible/,
    `${name}标签不得裁切中文标签文字。`
  )
}

assert.match(
  pageSource,
  /--scheduler-settings-label-width:\s*12[0-9]px/,
  '排产设置弹框标签列必须至少 120px，避免“同步时/产能模式/保护项”等标签挤压。'
)

console.log('mes-scheduler-workbench-settings-label-overlap-static.spec.js passed')
