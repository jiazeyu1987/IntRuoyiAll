import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import ts from 'typescript'
import test from 'node:test'
import vm from 'node:vm'

const evaluate = (filename, require) => {
  const context = { exports: {}, require, Error, Map, Set, Array, Number, String, BigInt }; vm.createContext(context)
  vm.runInContext(ts.transpileModule(readFileSync(filename, 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.exports
}
const state = evaluate(new URL('../src/views/dcc/controlled-file/relations/selector-state.ts', import.meta.url), () => { throw new Error('Unexpected state dependency') })
// H07 Root loader imports B's runtime validator; load the actual owner source, never a fake validate port.
const projectAttributes = evaluate(new URL('../src/views/dcc/controlled-file/project-attributes/state.ts', import.meta.url), () => { throw new Error('Unexpected B state dependency') })
const root = request => evaluate(new URL('../src/api/dcc/controlledFile/applicationRead.ts', import.meta.url), name => {
  if (name === '@/config/axios') return { default: request }
  if (name === '@/views/dcc/controlled-file/project-attributes/state') return projectAttributes
  throw new Error(`Unexpected Root dependency ${name}`)
})
const wrapper = (request, loader = root(request)) => evaluate(new URL('../src/api/dcc/controlledFile/relations.ts', import.meta.url), name => {
  if (name === '@/config/axios') return { default: request }
  if (name === './applicationRead') return loader
  throw new Error(`Unexpected D dependency ${name}`)
})
const row = (id, master, overrides = {}) => ({ id, tenantId: '1', latestControlledFileId: String(id), masterId: master,
  dccProjectCodeId: 5, projectName: '来源项目', projectFolderId: '500', projectFolderName: '项目文件夹',
  fileName: 'Template.pdf', sourceOriginalFileName: 'SOURCE.pdf', fileNumber: 'N-1', versionNo: 'A/2',
  status: 'CONTROLLED_PENDING_EFFECTIVE', controlled: true, pendingEffect: true, executable: false, canPreview: false, ...overrides })
const load = (page, query = { keyword: '', pageNo: 1, pageSize: 20 }) => wrapper({ get: async () => page }).loadSelectorBrowserPage('1', query)

test('directory query goes through actual Root loader with projectFolderId, without NAS or status filters', async () => {
  let params
  const api = wrapper({ get: async request => { params = request.params; assert.equal(request.url, '/dcc/controlled-files/browser-page'); return { total: 1, list: [row(21, 10)] } } })
  await api.loadSelectorBrowserPage('1', { projectId: '5', folderId: '500', keyword: '项目', pageNo: 2, pageSize: 20 })
  assert.equal(JSON.stringify(params), JSON.stringify({ selectorScope: 'PROJECT_FOLDER', dccProjectCodeId: '5', projectFolderId: '500', keyword: '项目', pageNo: 2, pageSize: 20 }))
  assert.equal(params.directoryId, undefined); assert.equal(params.status, undefined)
})
test('global query has no project or directory filter, and a project root cannot guess a folder', async () => {
  let params
  const api = wrapper({ get: async request => { params = request.params; return { total: 0, list: [] } } })
  await api.loadSelectorBrowserPage('1', { keyword: '源', pageNo: 3, pageSize: 20 })
  assert.equal(params.selectorScope, 'GLOBAL'); assert.equal(params.dccProjectCodeId, undefined); assert.equal(params.projectFolderId, undefined)
  assert.equal(params.pageNo, 3); assert.equal(params.keyword, '源')
  await assert.rejects(api.loadSelectorBrowserPage('1', { projectId: '5', keyword: '', pageNo: 1, pageSize: 20 }))
})
test('safe numeric and Long identities retain exact values; displayed source filename is canonical', async () => {
  const page = await load({ total: 3, list: [row(21, 10), row('9223372036854775807', '9223372036854775806')] })
  assert.equal(page.total, 3); assert.equal(page.list[0].controlledFileId, '21'); assert.equal(page.list[0].projectId, '5')
  assert.equal(page.list[1].controlledFileId, '9223372036854775807'); assert.equal(page.list[1].masterId, '9223372036854775806')
  assert.equal(page.list[0].fileName, 'SOURCE.pdf'); assert.equal(page.list[0].folderName, '项目文件夹'); assert.equal(page.list[0].canPreview, false)
})
test('unknown formal placement displays unrecorded without inferring a NAS folder', async () => {
  const page = await load({ total: 1, list: [row(21, 10, { projectFolderId: null, projectFolderName: null, directoryId: 5, directoryName: 'NAS目录' })] })
  assert.equal(page.list[0].folderName, '未记录')
  await assert.rejects(load({ total: 1, list: [row(21, 10, { projectFolderId: null, projectFolderName: '猜测位置' })] }))
})
test('Root rejects wrong tenant, rounded or overflowing identity, non-latest and contradictory states', async () => {
  for (const invalid of [row(21, 10, { tenantId: '2' }), row(Number.MAX_SAFE_INTEGER + 1, 10), row('9223372036854775808', 10), row(21, 10, { latestControlledFileId: '20' }),
    row(21, 10, { executable: true }), row(21, 10, { controlled: false }), row(21, 10, { canPreview: undefined })]) await assert.rejects(load({ total: 1, list: [invalid] }))
})
test('malformed totals, duplicate masters and folder scope mismatch never become deduplicated success', async () => {
  for (const total of [-1, 1.5, Number.MAX_SAFE_INTEGER + 1]) await assert.rejects(load({ total, list: [] }))
  await assert.rejects(load({ total: 2, list: [row(21, 10), row(22, 10)] }))
  await assert.rejects(load({ total: 1, list: [row(21, 10, { projectFolderId: '501' })] }, { projectId: '5', folderId: '500', keyword: '', pageNo: 1, pageSize: 20 }))
  assert.equal((await load({ total: 3, list: [] })).total, 3)
})
test('Root mapped pages preserve previous cross-project selection in real D selector state', async () => {
  const selected = new state.FileSelectorState({ contextKey: 't1:f2', tenantId: '1', masterId: '2' })
  selected.resolve(selected.begin({ keyword: '', pageNo: 1, pageSize: 1 }), await load({ total: 3, list: [row(21, 10)] })); selected.select(selected.rows[0])
  selected.resolve(selected.begin({ keyword: '', pageNo: 2, pageSize: 1 }), await load({ total: 3, list: [row(22, 11, { dccProjectCodeId: 6, projectFolderId: '501' })] }))
  assert.equal(selected.total, 3); assert.equal(selected.selected[0].controlledFileId, '21'); assert.equal(selected.rows[0].projectId, '6')
  assert.throws(() => selected.assertPreview(selected.rows[0]), /PERMISSION/)
})
test('D entry propagates actual Root network error without fake empty pages', async () => {
  const failure = new Error('正式名称授权拒绝')
  await assert.rejects(wrapper({ get: async () => { throw failure } }).loadSelectorBrowserPage('1', { keyword: '', pageNo: 1, pageSize: 20 }), error => error === failure)
})
test('D entry delegates single synchronized Root loader and preserves result identity', async () => {
  const result = { total: 0, list: [] }; let invocation
  const api = wrapper({ get: () => { throw new Error('D entry must delegate Root') } }, { loadDccSelectorPage: async (...args) => { invocation = args; return result } })
  const query = { keyword: '', pageNo: 1, pageSize: 20 }
  assert.equal(await api.loadSelectorBrowserPage('1', query), result); assert.equal(invocation[0], '1'); assert.equal(invocation[1], query)
})
test('existing Root upload payload builder preserves string related IDs at runtime pending its type correction', () => {
  const submitter = evaluate(new URL('../src/views/dcc/controlled-file/upload/submitter.ts', import.meta.url), name => {
    if (name === '../project-attributes/state') return projectAttributes
    throw new Error('Unexpected upload dependency ' + name)
  })
  const ids = ['9007199254740993', '9223372036854775807']
  const draft = { categoryId: 1, directoryId: 5, fileName: '真实上传.pdf', fileNumber: 'NEW-1', productCode: '', dccProjectCodeId: 5,
    fileTypeTaxonomyId: 8, relatedControlledFileIds: ids, needTraining: false, processType: 'CONTROLLED_FILE', changeType: 'NEW', versionNo: 'A/1', effectiveDate: '2026-10-03',
    projectAttributes: { targetMarkets: ['NMPA'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' },
    projectFolderId: '500', projectFolderChangeReason: '归入正式项目文件夹' }
  const payload = submitter.buildSubmitPayload(draft, { sessionId: 'actual-upload-session', uploadTicket: 'actual-upload-ticket', fileName: '真实上传.pdf' })
  assert.deepEqual([...payload.relatedControlledFileIds], ids); assert.notEqual(payload.relatedControlledFileIds, ids)
  assert.equal(JSON.stringify(payload.relatedControlledFileIds), '["9007199254740993","9223372036854775807"]')
  assert.equal(payload.sourceFileName, '真实上传.pdf'); assert.equal(payload.sessionId, 'actual-upload-session')
})
if (process.argv[2]) test('actual C Query Mapper Jackson response passes through Root loader into D state', async () => {
  const response = JSON.parse(readFileSync(process.argv[2], 'utf8'))
  const first = await load(response.globalFirst), second = await load(response.globalSecond)
  const directory = await load(response.projectFolder, { projectId: '5', folderId: '500', keyword: '', pageNo: 1, pageSize: 20 })
  assert.equal(first.total, 3); assert.equal(first.list[0].controlledFileId, '21'); assert.equal(first.list[0].pendingEffect, true); assert.equal(first.list[0].canPreview, false)
  assert.equal(second.list[0].controlledFileId, '9007199254740993'); assert.equal(second.list[0].folderName, '未记录')
  assert.equal(directory.total, 1); assert.equal(directory.list[0].controlledFileId, '21')
  const selected = new state.FileSelectorState({ contextKey: 'actual-t1:f2', tenantId: '1', masterId: '2' })
  selected.resolve(selected.begin({ keyword: '', pageNo: 1, pageSize: 2 }), first); selected.select(selected.rows[0])
  selected.resolve(selected.begin({ keyword: '', pageNo: 2, pageSize: 2 }), second)
  assert.equal(selected.selected[0].controlledFileId, '21'); assert.equal(selected.total, 3)
  assert.throws(() => selected.assertPreview(selected.rows[0]), /PERMISSION/)
})
