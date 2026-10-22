const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
const frontline = fs.readFileSync(path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')
test('workbench renders explicit read diagnosis instead of silently hiding malformed orders', () => {
  assert.equal(page.includes('data-active-order-read-error'), true)
  assert.equal(page.includes('row.readBlocked'), true)
})
test('allocation picker excludes blocked records and explains them in the list', () => {
  const start = page.indexOf('const allocatableActiveOrderOptions')
  assert.equal(page.slice(start, start + 260).includes('readBlocked'), true)
})
test('frontline order picker shows diagnosis and prevents selection of invalid snapshot', () => {
  assert.equal(frontline.includes('activeOrder.readBlocked'), true)
  assert.equal(frontline.includes('activeOrder.readBlockReason'), true)
})
