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
  const sections = [...source.matchAll(/<ControlledFileAttachments\b[^>]*:file="fileDetail"[^>]*\/>/g)]
  assert.equal(sections.length, 2)
  const shared = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail/ControlledFileAttachments.vue'), 'utf8')
  for (const fn of ['attachmentPurposeText', 'attachmentUploaderText', 'attachmentRoundText', 'attachmentUploadTimeText']) assert.ok(shared.includes(fn), fn)
  assert.ok(shared.includes('文件附件'))
  const viewer=fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/detail/ControlledFileBrowseRecords.vue'),'utf8')
  assert.match(viewer, /<el-table v-else data-user-table-column-explicit :data="file.attachments \|\| \[\]"/)
  const overviewTable = viewer.slice(viewer.indexOf('<el-table '), viewer.indexOf('</el-table>'))
  assert.deepEqual([...overviewTable.matchAll(/label="([^"]+)"/g)].map(match => match[1]), ['附件名称', '用途', '大小', '操作'])
  assert.ok(viewer.includes('attachmentPurposeText(row)'))
  assert.ok(viewer.includes('row.canDownload === true'))
  assert.ok(viewer.includes('assertAttachmentBelongsToVersion(row, props.file.id)'))
  assert.ok(viewer.includes('readSelectionState.value.selection?.bodyReadContext'))
  const handler = shared.slice(shared.indexOf('const preview ='),shared.indexOf('const download ='))
  assert.ok(handler.indexOf('assertAttachmentBelongsToVersion') < handler.indexOf('selected.value = row'))
  assert.ok(handler.includes('assertAttachmentBelongsToVersion'))
})


function overviewHandlers() {
  const source = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail/ControlledFileBrowseRecords.vue'), 'utf8')
  const methods = source.slice(source.indexOf('const previewAttachment ='), source.indexOf('defineExpose'))
  const attachment = { ...row, canDownload: true }
  const file = { id: fileId, attachments: [attachment] }
  const ref = value => ({ value })
  const calls = { confirmed: 0, downloaded: 0, saved: 0, scrolled: 0, command: undefined }
  let confirm = async () => {}, finishDownload = async () => ({ blob: { type: 'application/pdf' }, fileName: row.fileName })
  const context = {
    props: { file }, generation: 0, assertAttachmentBelongsToVersion: load().assertAttachmentBelongsToVersion,
    attachmentError: ref(''), readSelectionState: ref({ error: '' }), attachmentSelected: ref(), attachmentPreviewVisible: ref(false), attachmentActionError: ref(''),
    attachmentDownloading: ref(''), highlightedAttachment: ref(''), clearTrainingHighlight: () => { context.highlightedAttachment.value = '' },
    setTimeout: callback => { calls.expireHighlight = callback; return 1 }, readBrowseAuthority: () => 'user-1', contextKey: ref('selected-version'),
    attachmentHost: ref({ querySelectorAll: () => [{ scrollIntoView: () => calls.scrolled++ }] }), nextTick: async () => {},
    ElMessageBox: { confirm: async () => { calls.confirmed++; await confirm() } },
    downloadControlledFileAttachmentWithName: async (...command) => { calls.downloaded++; calls.command = command; return finishDownload() },
    downloadByData: () => calls.saved++
  }
  vm.createContext(context)
  vm.runInContext(ts.transpileModule(methods + '\nglobalThis.api = { previewAttachment, downloadAttachment, locateTrainingAttachment }',
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return { context, attachment, file, calls, api: context.api,
    onConfirm: callback => { confirm = callback }, onDownload: callback => { finishDownload = callback } }
}

test('overview preview rejects foreign versions and invalid read context before selecting an attachment', () => {
  const { api, context, attachment } = overviewHandlers()
  api.previewAttachment({ ...attachment, controlledFileId: '10' })
  assert.equal(context.attachmentPreviewVisible.value, false)
  context.readSelectionState.value.error = '所选轮次无效'
  api.previewAttachment(attachment)
  assert.equal(context.attachmentPreviewVisible.value, false)
  assert.equal(context.attachmentActionError.value, '所选轮次无效')
  context.readSelectionState.value.error = ''
  api.previewAttachment(attachment)
  assert.equal(context.attachmentSelected.value, attachment)
  assert.equal(context.attachmentPreviewVisible.value, true)
})

test('training locator requires the exact attachment identity, purpose and viewed version', async () => {
  const { api, context, attachment, calls } = overviewHandlers()
  await assert.rejects(api.locateTrainingAttachment('99'))
  attachment.attachmentPurpose = 'ATTACHMENT'
  await assert.rejects(api.locateTrainingAttachment(attachment.attachmentId))
  attachment.attachmentPurpose = 'TRAINING_RECORD'
  attachment.controlledFileId = '10'
  await assert.rejects(api.locateTrainingAttachment(attachment.attachmentId))
  attachment.controlledFileId = fileId
  await api.locateTrainingAttachment(attachment.attachmentId)
  assert.equal(calls.scrolled, 1)
  assert.equal(context.highlightedAttachment.value, attachment.attachmentId)
  calls.expireHighlight()
  assert.equal(context.highlightedAttachment.value, '')
})

test('overview download retains confirmation, version and actual permission checks', async () => {
  const { api, attachment, calls } = overviewHandlers()
  attachment.canDownload = false
  await api.downloadAttachment(attachment)
  assert.equal(calls.confirmed, 0)
  assert.equal(calls.downloaded, 0)
  attachment.canDownload = true
  await api.downloadAttachment({ ...attachment, controlledFileId: '10' })
  assert.equal(calls.downloaded, 0)
  await api.downloadAttachment(attachment)
  assert.equal(calls.confirmed, 1)
  assert.equal(calls.downloaded, 1)
  assert.equal(calls.saved, 1)
  assert.equal(calls.command[0], fileId)
  assert.equal(calls.command[1], attachment.attachmentId)
  assert.equal(calls.command[2].nonControlledWarningConfirmed, true)
})

test('overview download rejects permission changes during confirmation and stale completion', async () => {
  const first = overviewHandlers()
  first.onConfirm(async () => { first.attachment.canDownload = false })
  await first.api.downloadAttachment(first.attachment)
  assert.equal(first.calls.downloaded, 0)
  assert.match(first.context.attachmentActionError.value, /权限已变化/)
  const second = overviewHandlers()
  second.onDownload(async () => {
    second.context.generation++
    return { blob: { type: 'application/pdf' }, fileName: row.fileName }
  })
  await second.api.downloadAttachment(second.attachment)
  assert.equal(second.calls.downloaded, 1)
  assert.equal(second.calls.saved, 0)
})


test('download menu translates policy denial codes and fixed attachment columns disable the global column tool', () => {
  const source = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail/index.vue'), 'utf8')
  const start = source.indexOf('const formatDownloadDeniedReason =')
  const method = source.slice(start, source.indexOf('const formatAccessExplanation =', start))
  const context = {}
  vm.runInNewContext(ts.transpileModule(method + '\nglobalThis.label = formatDownloadDeniedReason',
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  assert.equal(context.label('CATEGORY_DOWNLOAD_DENIED'), '当前账号未获此文件分类的下载权限')
  assert.equal(context.label('DIRECTORY_DOWNLOAD_DENIED'), '当前账号未获此目录的下载权限')
  assert.equal(context.label('PUBLISHED_FILE_MISSING'), '发布文件尚未生成，暂不可下载')
  assert.equal(context.label('STATUS_NOT_ACTIVE'), '当前查看版本尚未启用，暂不可下载')
  assert.equal(context.label('尚未完成下载授权'), '尚未完成下载授权')
  assert.doesNotMatch(context.label('UNRECOGNIZED_REASON'), /UNRECOGNIZED_REASON/)
  const menu = source.slice(source.indexOf('<section v-if="useControlledDetailLayout"'), source.indexOf('<el-tabs v-model="controlledDetailTab"'))
  assert.ok(menu.includes('formatDownloadDeniedReason(fileDetail?.accessExplanation?.downloadDeniedReason)'))
  assert.ok(!menu.includes("downloadDeniedReason ||"))
})


test('final overview separates reference counts, one-line empty attachments and actual application facts', () => {
  const viewer = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail/ControlledFileBrowseRecords.vue'), 'utf8')
  assert.match(viewer, /<p v-if="!file.attachments\?\.length"[^>]*>暂无文件附件<\/p>/)
  assert.ok(viewer.includes('metadata.referenceProjectCount === 0'))
  assert.ok(viewer.includes('<span v-if="metadata.referenceProjectCount === 0">0 个</span>'))
  assert.ok(viewer.includes(':application-selection="relationApplicationSelection"'))
  assert.ok(viewer.includes(':body-read-context="readSelectionState.selection?.bodyReadContext"'))
  assert.ok(viewer.includes('<DetailApplicationHistory v-if="hasRelationApplicationContext"'))
  assert.ok(viewer.includes('setTimeout(clearTrainingHighlight, 3000)'))
  assert.match(viewer, /onBeforeUnmount\(\(\) => \{\s+clearTrainingHighlight\(\)/)
  const relations = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail/DetailRelationsPanel.vue'), 'utf8')
  assert.ok(relations.includes('查看固定版本'))
  assert.ok(relations.includes('@click="openPreview(row)"'))
  assert.ok(relations.includes('target.versionNo !== versionNo'))
})

test('approval alone receives real user nicknames or account names without internal ID placeholders', () => {
  const source = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail/index.vue'), 'utf8')
  const start = source.indexOf('approvalUserNameMap.value = new Map(')
  const assignment = source.slice(start, source.indexOf('departmentList.value =', start))
  const context = { approvalUserNameMap: { value: undefined }, users: [
    { id: 1, nickname: '文控编制甲20261013', username: 'dccu131801' },
    { id: 2, nickname: '', username: '仅业务账号' },
    { id: 3 }, { id: 4, nickname: '姓名（业务原文）', username: '另一个账号' }
  ] }
  vm.runInNewContext(ts.transpileModule(assignment, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  assert.deepEqual([...context.approvalUserNameMap.value].map(([id, name]) => [id, name]), [
    [1, '文控编制甲20261013'], [2, '仅业务账号'], [3, ''], [4, '姓名（业务原文）']
  ])
  const panel = source.match(/<ControlledDetailApprovalPanel\b[^>]*>/)?.[0]
  assert.match(panel, /:user-names="approvalUserNameMap"/)
  assert.equal([...source.matchAll(/:user-names="approvalUserNameMap"/g)].length, 1)
  for (const component of ['ControlledDetailVersionPanel', 'ControlledDetailDistributionPanel']) {
    assert.match(source.match(new RegExp(`<${component}\\b[^>]*>`))?.[0] || '', /:user-names="userNameMap"/)
  }
})
