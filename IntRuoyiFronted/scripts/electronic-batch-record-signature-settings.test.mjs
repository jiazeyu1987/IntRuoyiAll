import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('signature settings support double-click removal without recreating a marker', () => {
  const source = readText('src/views/mes/pro/batchrecordtemplate/index.vue')
  const templateBlock = source.match(/<table v-if="signatureRows\.length"[\s\S]*?<\/table>/)?.[0] || ''
  const handlerBlock = source.match(/const handleSignatureCellClick[\s\S]*?const saveSignatureCells/)?.[0] || ''

  assert.match(templateBlock, /@click="handleSignatureCellClick\(cell\)"/)
  assert.match(templateBlock, /@dblclick\.stop="handleSignatureCellDoubleClick\(cell\)"/)
  assert.match(source, /let signatureCellClickTimer/)
  assert.match(handlerBlock, /clearSignatureCellClickTimer\(\)/)
  assert.match(handlerBlock, /window\.setTimeout\(\(\) => \{[\s\S]*selectSignatureCell\(cell\)/)
  assert.match(handlerBlock, /const handleSignatureCellDoubleClick = \(cell: SignatureRenderedCell\) => \{[\s\S]*removeSignatureCell\(cell\)/)
})

test('signature settings can apply current action to a selected cell range', () => {
  const source = readText('src/views/mes/pro/batchrecordtemplate/index.vue')
  const signatureDialogBlock = source.match(/const signatureDialog = reactive\(\{[\s\S]*?\n\}\)/)?.[0] || ''
  const rangeBlock = source.match(/const selectSignatureCell[\s\S]*?const removeSignatureMarker/)?.[0] || ''

  assert.match(source, /v-model="signatureDialog\.rangeMode"/)
  assert.match(source, /范围设置/)
  assert.match(signatureDialogBlock, /rangeMode:\s*false/)
  assert.match(signatureDialogBlock, /rangeAnchor:\s*undefined/)
  assert.match(rangeBlock, /signatureDialog\.rangeMode/)
  assert.match(rangeBlock, /signatureDialog\.rangeAnchor = \{ rowIndex: cell\.rowIndex, columnIndex: cell\.columnIndex \}/)
  assert.match(rangeBlock, /resolveSignatureRangeCells\(signatureDialog\.rangeAnchor,\s*cell\)/)
  assert.match(rangeBlock, /rangeCells\.forEach\(\(rangeCell\) => \{[\s\S]*upsertSignatureMarker\(buildSignatureMarker\(rangeCell\.rowIndex, rangeCell\.columnIndex\)\)/)
  assert.match(source, /const resolveSignatureRangeCells = \(/)
})

test('signature settings color markers by fill review and approval action', () => {
  const source = readText('src/views/mes/pro/batchrecordtemplate/index.vue')
  const rowBlock = source.match(/const signatureRows = computed[\s\S]*?\nconst cellRuleLayout/)?.[0] || ''

  assert.match(rowBlock, /is-marker-fill/)
  assert.match(rowBlock, /marker\?\.actionType === 'SUBMIT'/)
  assert.match(rowBlock, /is-marker-review/)
  assert.match(rowBlock, /marker\?\.actionType === 'FORM_REVIEW'/)
  assert.match(rowBlock, /is-marker-approve/)
  assert.match(rowBlock, /marker\?\.actionType === 'APPROVE'/)
  assert.match(source, /\.batch-record-signature-sheet__cell\.is-marker-fill/)
  assert.match(source, /\.batch-record-signature-sheet__cell\.is-marker-review/)
  assert.match(source, /\.batch-record-signature-sheet__cell\.is-marker-approve/)
  assert.match(source, /\.batch-record-signature-sheet__cell\.is-range-anchor/)
})
