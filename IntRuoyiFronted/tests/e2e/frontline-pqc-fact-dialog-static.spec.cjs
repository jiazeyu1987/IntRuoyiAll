const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const panelSource = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

const blockBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const standardDialog = blockBetween(
  panelSource,
  'data-pqc-standard-dialog',
  '<div\n        v-if="activePqcMethodItem"'
)
const methodDialog = blockBetween(
  panelSource,
  'data-pqc-method-dialog',
  '<main class="frontline-operator-main is-pqc"'
)

const fullscreenRootIndex = panelSource.indexOf('data-pqc-fullscreen-root')
assert.ok(fullscreenRootIndex >= 0, 'PQC panel must keep a stable fullscreen root.')
assert.ok(
  panelSource.indexOf('data-pqc-standard-dialog') > fullscreenRootIndex &&
    panelSource.indexOf('data-pqc-method-dialog') > fullscreenRootIndex,
  'Standard and method dialogs must render inside the fullscreen root instead of body teleport.'
)

assert.match(
  standardDialog,
  /class="frontline-pqc-fact-dialog__panel"[\s\S]*data-pqc-standard-dialog-panel/,
  'The 接收标准 dialog must use the polished fact-dialog panel layout.'
)
assert.match(
  standardDialog,
  /aria-labelledby="pqc-standard-dialog-title"/,
  'The 接收标准 dialog must expose a stable title for assistive technology.'
)
assert.match(
  standardDialog,
  /data-pqc-standard-detail-text[\s\S]*activePqcStandardItem\.acceptanceStandard/,
  'The 接收标准 dialog must display the formal QA process acceptanceStandard detail.'
)
assert.match(
  standardDialog,
  /class="frontline-pqc-fact-dialog__body is-standard"/,
  'The 接收标准 dialog must use a full-width single-column detail layout.'
)
assert.ok(
  !standardDialog.includes('data-pqc-standard-bound-grid'),
  'The 接收标准 dialog must not render the lower limit, upper limit, unit, or precision grid.'
)
for (const field of [
  'standardLowerLimit',
  'standardUpperLimit',
  'standardUnit',
  'standardPrecision'
]) {
  assert.ok(
    !standardDialog.includes(`activePqcStandardItem.${field}`),
    `The 接收标准 dialog must not display ${field}.`
  )
}
assert.match(
  standardDialog,
  /aria-label="关闭接收标准弹框"[\s\S]*@click="closePqcStandardDialog"/,
  'The 接收标准 dialog must provide an explicit close button.'
)

assert.match(
  methodDialog,
  /class="frontline-pqc-fact-dialog__panel"[\s\S]*data-pqc-method-dialog-panel/,
  'The 检验方法 dialog must use the polished fact-dialog panel layout.'
)
assert.match(
  methodDialog,
  /aria-labelledby="pqc-method-dialog-title"/,
  'The 检验方法 dialog must expose a stable title for assistive technology.'
)
assert.match(
  methodDialog,
  /data-pqc-method-detail-text[\s\S]*formatPqcMethodSummary\(activePqcMethodItem\)/,
  'The 检验方法 dialog must display the normalized formal QA process inspection method detail.'
)
assert.match(
  methodDialog,
  /pqc-method-dialog-title[\s\S]*activePqcMethodItem\.samplingPlanText/,
  'The 检验方法 dialog title area must display the formal QA sampling plan.'
)
assert.match(
  methodDialog,
  /data-pqc-method-equipment-text[\s\S]*activePqcMethodItem\.inspectionTool/,
  'The 检验方法 dialog side area must display the formal QA inspection tool text.'
)
assert.doesNotMatch(
  methodDialog,
  /data-pqc-method-meta-grid|<dt>检验项目<\/dt>|<dt>结果类型<\/dt>|<dt>单位<\/dt>|<dt>来源<\/dt>/,
  'The 检验方法 dialog must not retain the old four-card metadata grid.'
)
assert.match(
  methodDialog,
  /aria-label="关闭检验方法弹框"[\s\S]*@click="closePqcMethodDialog"/,
  'The 检验方法 dialog must provide an explicit close button.'
)

assert.match(
  panelSource,
  /\.frontline-pqc-fact-dialog\s*\{[\s\S]*position:\s*absolute[\s\S]*backdrop-filter:/,
  'Fact dialogs must have an in-panel overlay style that works in browser fullscreen.'
)
assert.match(
  panelSource,
  /\.frontline-pqc-fact-dialog__body\s*\{[\s\S]*grid-template-columns:/,
  'Fact dialog content must use a deliberate two-column responsive layout.'
)
assert.match(
  panelSource,
  /\.frontline-pqc-fact-dialog__body\.is-standard\s*\{[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)/,
  'The 接收标准 dialog body must override the shared layout with one full-width column.'
)
assert.match(
  panelSource,
  /@media\s*\(max-width:\s*900px\)[\s\S]*\.frontline-pqc-fact-dialog__body\s*\{[\s\S]*grid-template-columns:\s*1fr/,
  'Fact dialog layout must collapse cleanly on narrow screens.'
)

console.log('PASS: frontline PQC standard and method dialogs use polished in-panel details')
