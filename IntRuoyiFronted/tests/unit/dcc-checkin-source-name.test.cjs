const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const existing = fs.readFileSync('tests/unit/dcc-browser-checkin-minor-only.test.cjs', 'utf8')
const setup = existing.slice(0, existing.indexOf("test('actual submit always"))
const { stateFor, block } = new Function('require', setup + '\nreturn {stateFor,block}')(require)

test('version selection preserves the exact source filename for current and historical versions', () => {
  const context = { exports: {} }
  vm.runInNewContext(ts.transpileModule(
    block('buildCurrentVersionOption', 'hydrateCurrentBrowserVersionActionState') + '\n' +
    block('hydrateCurrentBrowserVersionActionState', 'getVersionOptions') +
    '\nexports.build=buildCurrentVersionOption; exports.hydrate=hydrateCurrentBrowserVersionActionState',
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  const row = { id: 51, sourceOriginalFileName: '当前原文件.docx' }
  assert.equal(context.exports.build(row).sourceOriginalFileName, '当前原文件.docx')
  assert.equal(context.exports.build({ id: 51, sourceOriginalFileName: null }).sourceOriginalFileName, null)
  assert.equal(context.exports.build({ id: 51 }).sourceOriginalFileName, undefined)
  const historical = { id: 41, sourceOriginalFileName: '历史原文件.docx' }
  assert.equal(context.exports.hydrate(row, historical).sourceOriginalFileName, '历史原文件.docx')
  assert.equal(context.exports.hydrate(row, { id: 41 }).sourceOriginalFileName, undefined)
})

test('renamed source is rejected before upload with the exact original name', async () => {
  const { state, calls } = stateFor()
  state.checkinTarget.value.sourceOriginalFileName = '原文件.docx'
  let uploads = 0, failures = 0
  state.uploadControlledFilePreview = async () => { uploads++; throw Error('unexpected upload') }
  vm.runInContext(ts.transpileModule(block('uploadCheckinSource', 'uploadCheckinDrawingPdf') + '\nexports.upload=uploadCheckinSource',
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, state)
  for (const name of ['更名.docx', '原文件.pdf', '原文件.DOCX']) {
    await state.exports.upload({ file: { name }, onError: () => failures++, onSuccess: () => assert.fail('renamed source accepted') })
  }
  assert.equal(uploads, 0)
  assert.equal(failures, 3)
  assert.equal(state.checkinSourceState.value, 'failed')
  assert.ok(calls.errors.every(text => text.includes('原文件.docx')))
})

test('a previously uploaded renamed source cannot bypass the check at submission', async () => {
  const { state, calls } = stateFor()
  state.checkinTarget.value.sourceOriginalFileName = '原文件.docx'
  await state.exports.submit()
  assert.equal(calls.writes.length, 0)
  assert.ok(calls.errors.some(text => text.includes('原文件.docx')))
})

test('matching source filename still permits checkin with multiple optional attachments', async () => {
  const { state, calls } = stateFor()
  state.checkinTarget.value.sourceOriginalFileName = 'updated.docx'
  await state.exports.submit()
  assert.equal(calls.writes.length, 1)
  assert.equal(calls.successes.length, 1)
})
