const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const source = fs
  .readFileSync(path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')
  .replace(/\r\n/g, '\n')

const rootStart = source.indexOf('<section')
const rootOpenEnd = source.indexOf('>', rootStart)
const rootOpenTag = rootStart >= 0 && rootOpenEnd > rootStart ? source.slice(rootStart, rootOpenEnd) : ''
const scriptStart = source.indexOf('\n<script')
const signatureModalIndex = source.indexOf('class="frontline-pqc-signature-modal"')
const signatureDialogDataIndex = source.indexOf('data-pqc-signature-dialog')
const signatureModalBlock =
  signatureModalIndex >= 0
    ? source.slice(Math.max(0, signatureModalIndex - 180), signatureModalIndex + 900)
    : ''
const signatureStylesIndex = source.indexOf('.frontline-pqc-signature-modal')
const signatureStylesBlock =
  signatureStylesIndex >= 0
    ? source.slice(signatureStylesIndex, signatureStylesIndex + 420)
    : ''

assert.ok(rootStart >= 0, 'PQC panel template must have a root section.')
assert.match(
  rootOpenTag,
  /ref="frontlinePanelRef"[\s\S]*data-pqc-fullscreen-root/,
  'PQC browser fullscreen root must expose a stable data-pqc-fullscreen-root selector.'
)
assert.match(
  rootOpenTag,
  /class="frontline-operator-panel"/,
  'PQC fullscreen root must remain the operator panel, not a nested child or body overlay.'
)
assert.ok(
  signatureModalIndex > rootStart && signatureModalIndex < scriptStart,
  'PQC submit signature modal must be rendered inside the fullscreen root template subtree.'
)
assert.ok(
  signatureDialogDataIndex > signatureModalIndex && signatureDialogDataIndex < scriptStart,
  'PQC submit signature modal must keep a stable data-pqc-signature-dialog selector.'
)
assert.doesNotMatch(
  signatureModalBlock,
  /<teleport\b|to=["']body["']|append-to-body(?!="false")|:append-to-body(?!="false")/,
  'PQC signature dialog must not teleport or append to body because body overlays are hidden behind browser fullscreen.'
)
assert.match(
  signatureStylesBlock,
  /position:\s*absolute[\s\S]*inset:\s*0[\s\S]*z-index:\s*70/,
  'PQC signature modal must cover the fullscreen root content with the existing absolute overlay.'
)

console.log('PASS: frontline PQC fullscreen submit dialog static contract')
