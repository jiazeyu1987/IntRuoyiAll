const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const filename = 'src/api/dcc/controlledFile/applicationRead.ts'
function load(page) {
  assert.ok(fs.existsSync(filename), 'formal application/selector API bridge must exist')
  const calls = []
  const context = { exports: {}, Error, BigInt, Set, require: name => {
    if (name === '@/config/axios') return { default: { get: async request => { calls.push(request); return page } } }
    if (name.endsWith('project-attributes/state')) return { validateAttributes: value => value }
    throw new Error('Unexpected dependency: ' + name)
  }}
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(filename, 'utf8'),
    { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return { api: context.exports, calls }
}
const row = { tenantId: '1', id: '9223372036854775000', masterId: '20', latestControlledFileId: '9223372036854775000',
  dccProjectCodeId: '5', projectName: '项目五', projectFolderId: '9', projectFolderName: '设计',
  sourceOriginalFileName: 'SOP.docx', fileNumber: 'SOP-1', versionNo: 'B/1', status: 'CONTROLLED_PENDING_EFFECTIVE',
  controlled: true, pendingEffect: true, executable: false, canPreview: false }
test('directory loader uses projectFolder identity and preserves actual latest, total and content permission', async () => {
  const state = load({ list: [row], total: 17 })
  const result = await state.api.loadDccSelectorPage('1', { projectId: '5', folderId: '9', keyword: 'SOP', pageNo: 2, pageSize: 10 })
  const params = state.calls[0].params
  assert.equal(state.calls[0].url, '/dcc/controlled-files/browser-page')
  assert.equal(params.selectorScope, 'PROJECT_FOLDER'); assert.equal(params.projectFolderId, '9')
  assert.equal(params.dccProjectCodeId, '5'); assert.equal(params.directoryId, undefined)
  assert.equal(result.total, 17); assert.equal(result.list[0].controlledFileId, row.id)
  assert.equal(result.list[0].fileName, 'SOP.docx'); assert.equal(result.list[0].canPreview, false)
  assert.equal(result.list[0].pendingEffect, true); assert.equal(result.list[0].executable, false)
})
test('global loader carries no project or NAS scope and displays missing historical location as unrecorded', async () => {
  const state = load({ list: [{ ...row, projectFolderId: null, projectFolderName: null }], total: 1 })
  const result = await state.api.loadDccSelectorPage('1', { keyword: '项目', pageNo: 1, pageSize: 20 })
  assert.equal(state.calls[0].params.selectorScope, 'GLOBAL')
  assert.equal(state.calls[0].params.projectFolderId, undefined); assert.equal(state.calls[0].params.dccProjectCodeId, undefined)
  assert.equal(result.list[0].folderName, '未记录')
})
test('loader rejects foreign tenant, lossy identities, duplicate masters and inconsistent controlled facts', async () => {
  for (const list of [[{ ...row, tenantId: '2' }], [{ ...row, id: 9223372036854775000 }], [row, row],
    [{ ...row, latestControlledFileId: '99' }], [{ ...row, executable: true }], [{ ...row, canPreview: undefined }]]) {
    const state = load({ list, total: list.length })
    await assert.rejects(state.api.loadDccSelectorPage('1', { keyword: '', pageNo: 1, pageSize: 20 }))
  }
})
test('evidence bridge sends exact selected file, action and opaque BPM string without draft/default substitution', async () => {
  const evidence = { controlledFileId: '9223372036854775000', versionNo: 'B/1', applicationType: 'OBSOLETE',
    bpmRound: 'flowable-round:opaque', attributeRound: 2, recorded: false, unavailableReason: 'NOT_RECORDED',
    defaultSource: null, actualAttributes: null, signatures: [] }
  const state = load(evidence)
  const result = await state.api.getControlledFileApplicationEvidence(evidence.controlledFileId, 'OBSOLETE', evidence.bpmRound)
  assert.equal(state.calls[0].url, `/dcc/controlled-files/${evidence.controlledFileId}/application-evidence`)
  assert.equal(state.calls[0].params.bpmRound, evidence.bpmRound)
  assert.equal(result.recorded, false); assert.equal(result.unavailableReason, 'NOT_RECORDED')
})
