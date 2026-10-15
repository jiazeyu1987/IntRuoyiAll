const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const helper = path.join(root, 'src/views/dcc/controlled-file/detail/attachment-facts.ts')
function load() {
  assert.ok(fs.existsSync(helper), 'attachment snapshot projection must be formally implemented')
  const context = { exports: {}, Error, BigInt }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(helper, 'utf8'),
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.exports
}
const fileId = '9223372036854775000'
const row = { attachmentId: '9223372036854775001', controlledFileId: fileId, fileName: '培训记录.pdf',
  attachmentPurpose: 'TRAINING_RECORD', applicationProcessInstanceId: 'round-1',
  uploadedBy: '9', uploadedUsernameSnapshot: 'uploader', uploadedNicknameSnapshot: '张三', uploadedAt: '2026-10-11 18:00:00' }
test('training attachment displays exact saved uploader, round and time', () => {
  const api = load()
  assert.equal(api.attachmentPurposeText(row), '培训记录单')
  assert.equal(api.attachmentUploaderText(row), '张三（uploader）')
  assert.equal(api.attachmentRoundText(row), 'round-1')
  assert.equal(api.attachmentUploadTimeText(row), '2026-10-11 18:00:00')
  api.assertAttachmentBelongsToVersion(row, fileId)
})
test('historical attachment does not invent upload evidence', () => {
  const api = load(), legacy = { attachmentId: '10', controlledFileId: '11', fileName: '旧附件.pdf' }
  for (const fn of ['attachmentUploaderText', 'attachmentRoundText', 'attachmentUploadTimeText']) {
    assert.equal(api[fn](legacy), '未记录')
  }
  assert.equal(api.attachmentPurposeText(legacy), '用途未记录')
})
test('foreign-version and lossy attachment identities are rejected', () => {
  const api = load()
  for (const wrong of [{ ...row, controlledFileId: '10' }, { ...row, controlledFileId: undefined },
    { ...row, attachmentId: Number(row.attachmentId) }, { ...row, attachmentId: '0' }]) {
    assert.throws(() => api.assertAttachmentBelongsToVersion(wrong, fileId))
  }
})
test('all formal attachment entry modes share snapshot facts and validate before preview', () => {
  const source = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail/index.vue'), 'utf8')
  const sections = [...source.matchAll(/<ControlledFileAttachments\s+:file="fileDetail"\s*\/>/g)]
  assert.equal(sections.length, 2)
  const shared = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail/ControlledFileAttachments.vue'), 'utf8')
  for (const fn of ['attachmentPurposeText', 'attachmentUploaderText', 'attachmentRoundText', 'attachmentUploadTimeText']) assert.ok(shared.includes(fn), fn)
  assert.ok(shared.includes('文件附件'))
  const viewer=fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/detail/ControlledFileBrowseRecords.vue'),'utf8')
  assert.ok(viewer.includes('<ControlledFileAttachments :file="file" />'))
  const handler = shared.slice(shared.indexOf('const preview ='),shared.indexOf('const download ='))
  assert.ok(handler.indexOf('assertAttachmentBelongsToVersion') < handler.indexOf('selected.value = row'))
  assert.ok(handler.includes('assertAttachmentBelongsToVersion'))
})
