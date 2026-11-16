const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const query = { keyword: '文件', pageNo: 1, pageSize: 20 }
const active = (id, project = '5', folder = '51') => ({
  tenantId: '1', id, masterId: String(BigInt(id) + 1000n), currentActiveControlledFileId: id,
  latestControlledFileId: id, dccProjectCodeId: project, projectName: '项目' + project,
  projectFolderId: folder, projectFolderName: '文件夹' + folder,
  fileName: '文件' + id + '.pdf', fileNumber: 'DOC-' + id, versionNo: 'A/1', status: 'ACTIVE',
  controlled: true, pendingEffect: false, executable: true, canPreview: true
})
function moduleAt(file, dependencies = {}) {
  const context = { exports: {}, Error, BigInt, Set, Map, Date, JSON, Number, require: name => {
    if (!Object.hasOwn(dependencies, name)) throw Error('Unexpected dependency: ' + name)
    return dependencies[name]
  } }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(root, file), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return context.exports
}
function fixture(rows = [], references = []) {
  const calls = []
  let sourcePermissionReads = 0
  const adapter = moduleAt('src/api/dcc/controlledFile/applicationRead.ts', {
    '@/config/axios': { default: { get: async request => { calls.push(request); const list=request.params.browserScope === 'PROJECT_FOLDER' ? [...rows,...references.map(referenceProjection => ({referenceProjection}))] : rows; return { list, total: list.length } } } },
    '@/views/dcc/controlled-file/project-attributes/state': moduleAt('src/views/dcc/controlled-file/project-attributes/state.ts')
  })
  const { ProjectBrowserState } = moduleAt('src/views/dcc/controlled-file/browser/project-browser.ts', {
    '@/api/dcc/controlledFile/projectDiscovery': { getProjectDiscovery: async id => ({ id, projectName: '项目' + id, projectCode: 'P' + id, projectLeaderUserId: '9' }) },
    '@/api/dcc/controlledFile/projectAttributes': { getProjectFolders: async projectCodeId => ['51', '52'].map((id, i) => ({ id, projectCodeId, parentId: '0', name: '文件夹' + id, active: true, sortOrder: i })) },
    '@/api/dcc/controlledFile/applicationRead': { ...adapter, getControlledFileRelationPermissions: async () => {
      sourcePermissionReads++; throw new Error('SOURCE_NAME_DENIED')
    } },
    '@/api/dcc/controlledFile/workflow': {},
    '@/api/system/user': {},
    '@/hooks/web/useListColumnFilter': { loadAllListPages: () => { throw Error('Unexpected full-list read') } },
    './folder-file-list': moduleAt('src/views/dcc/controlled-file/browser/folder-file-list.ts'),
    '@/api/dcc/controlledFile/relations': { listSelectedProjectReferences: async () => references,
      createProjectReferences: async (_project, _folder, ids) => ids.map(id => ({ reference: { id: '91', selectedControlledFileId: id } })),
      getProjectReferenceUsageBatch: async ids => ids.map(controlledFileId => ({ masterId: String(BigInt(controlledFileId) + 1000n), referenceProjectCount: 0, referenced: false })) },
    '../basic-data/components/project-folder-tree': moduleAt('src/views/dcc/controlled-file/basic-data/components/project-folder-tree.ts'),
    '../relations/project-reference-contract': moduleAt('src/views/dcc/controlled-file/relations/project-reference-contract.ts')
  })
  return { calls, adapter, state: new ProjectBrowserState('1', '9'), sourcePermissionReads: () => sourcePermissionReads }
}

const listedReference = (status = 'ACTIVE', canPreview = false) => ({
  reference: { id: '91', projectId: '5', folderId: '51', masterId: '1101', selectedControlledFileId: '101', createdBy: '9' },
  selectedVersion: { tenantId: '1', controlledFileId: '101', masterId: '1101', projectId: '6', fileName: '引用.pdf', fileNumber: 'REF',
    versionNo: 'A/1', status, controlled: status === 'ACTIVE', pendingEffect: false, executable: status === 'ACTIVE' },
  referenceProjectCount: 1,
  permissions: { controlledFileId: '101', tenantId: '1', masterId: '1101', projectId: '6', projectName: '来源项目',
    projectFolderId: '61', projectFolderName: '来源目录', fileName: '引用.pdf', fileNumber: 'REF', versionNo: 'A/1', status,
    controlled: status === 'ACTIVE', pendingEffect: false, executable: status === 'ACTIVE', canPreview }
})
test('destination viewer sees existing active and retained references without source name permission', async () => {
  for (const status of ['ACTIVE', 'SUPERSEDED', 'OBSOLETE']) {
    const { state, sourcePermissionReads } = fixture([], [listedReference(status)])
    await state.selectProject('5'); await state.selectFolder(state.directories[0]); const rows = state.references
    assert.equal(rows.length, 1); assert.equal(rows[0].sourceProjectName, '来源项目')
    assert.equal(rows[0].selectedVersion.folderName, '来源目录')
    assert.equal(rows[0].selectedVersion.canPreview, false)
    assert.equal(sourcePermissionReads(), 0)
    state.project = { id: '5' }; state.folder = { folderId: '51' }
    await assert.rejects(state.readReferenceForAction(rows[0]), /SOURCE_NAME_DENIED/)
  }
})
test('reference list preserves body grants and rejects mismatched tenant, folder or permission identity', async () => {
  const allowed = listedReference('ACTIVE', true)
  const positive = fixture([], [allowed]).state; await positive.selectProject('5'); await positive.selectFolder(positive.directories[0]); assert.equal(positive.references[0].selectedVersion.canPreview, true)
  for (const mutate of [row => row.selectedVersion.tenantId = '2', row => row.reference.folderId = '52',
    row => row.permissions.controlledFileId = '102', row => delete row.permissions]) {
    const row = listedReference(); mutate(row)
    const invalid=fixture([], [row]).state; await invalid.selectProject('5'); await assert.rejects(invalid.selectFolder(invalid.directories[0]))
  }
})
test('creating references reads the destination list and reports an unconfirmed saved version explicitly', async () => {
  for (const present of [true, false]) {
    const { state, sourcePermissionReads } = fixture([], present ? [listedReference()] : [])
    state.project = { id: '5', projectLeaderUserId: '9' }; state.folder = { folderId: '51' }; state.actorEnabled = true
    if (present) {
      const rows = await state.createReferences('5', '51', ['101'], '正式引用')
      assert.equal(rows[0].reference.selectedControlledFileId, '101'); assert.equal(sourcePermissionReads(), 0)
    } else {
      await assert.rejects(state.createReferences('5', '51', ['101'], '正式引用'), error => error.referenceSaved === true && /引用列表未确认/.test(error.message))
    }
  }
})
test('current-project adapter uses GLOBAL with exact project and accepts two different folders', async () => {
  const { adapter, calls } = fixture([active('101'), active('102', '5', '52')])
  const result = await adapter.loadDccProjectBrowserPage('1', { ...query, projectId: '5' })
  assert.equal(calls.length, 1)
  assert.equal(calls[0].params.browserScope, 'GLOBAL')
  assert.equal(calls[0].params.dccProjectCodeId, '5')
  assert.equal(calls[0].params.projectFolderId, undefined)
  assert.equal(calls[0].params.selectorScope, undefined)
  assert.equal(result.list.length, 2)
  assert.deepEqual(Array.from(result.list, row => row.projectFolderId), ['51', '52'])
})
test('current-project adapter rejects a foreign-project row, including a mixed response', async () => {
  for (const rows of [[active('103', '6')], [active('101'), active('103', '6')]]) {
    const { adapter, calls } = fixture(rows)
    await assert.rejects(adapter.loadDccProjectBrowserPage('1', { ...query, projectId: '5' }), /响应包含失配/)
    assert.equal(calls.length, 1)
  }
})
test('actual State.search project sends selected project with no folder and displays both folders', async () => {
  const { state, calls } = fixture([active('101'), active('102', '5', '52')])
  await state.selectProject('5')
  assert.equal(state.folder, undefined)
  await state.search('project', '共同编号')
  assert.equal(state.error, '')
  assert.equal(state.scope, 'project')
  assert.equal(calls[0].params.dccProjectCodeId, '5')
  assert.equal(calls[0].params.projectFolderId, undefined)
  assert.equal(calls[0].params.browserScope, 'GLOBAL')
  assert.equal(calls[0].params.keyword, '共同编号')
  assert.equal(state.rows.length, 2)
  assert.equal(state.total, 2)
})
test('current-project search ignores selected folder without losing it for later folder search', async () => {
  const { state, calls } = fixture([active('101')])
  await state.selectProject('5')
  await state.selectFolder(state.directories[0])
  await state.search('project', 'DOC')
  assert.equal(calls.at(-1).params.dccProjectCodeId, '5')
  assert.equal(calls.at(-1).params.projectFolderId, undefined)
  assert.equal(state.folder.folderId, '51')
})
test('current-project search without a selected project shows an error and sends no request', async () => {
  const { state, calls } = fixture()
  await assert.rejects(state.search('project', 'DOC'), /请先选择项目/)
  assert.match(state.error, /请先选择项目/)
  assert.equal(calls.length, 0)
  assert.equal(state.rows.length, 0)
})
test('global search retains its full authorized scope despite selected project and folder', async () => {
  const { state, calls } = fixture([active('101')])
  await state.selectProject('5'); await state.selectFolder(state.directories[0])
  await state.search('global', 'DOC')
  assert.equal(state.error, '')
  assert.equal(calls.at(-1).params.browserScope, 'GLOBAL')
  assert.equal(calls.at(-1).params.dccProjectCodeId, undefined)
  assert.equal(calls.at(-1).params.projectFolderId, undefined)
})
test('folder search still sends both exact identities and rejects another folder', async () => {
  const { state, calls } = fixture([active('101')])
  await state.selectProject('5'); await state.selectFolder(state.directories[0])
  await state.search('directory', 'DOC')
  assert.equal(state.error, '')
  assert.equal(calls.at(-1).params.browserScope, 'PROJECT_FOLDER')
  assert.equal(calls.at(-1).params.dccProjectCodeId, '5')
  assert.equal(calls.at(-1).params.projectFolderId, '51')
  await assert.rejects(fixture([active('102', '5', '52')]).adapter.loadDccProjectBrowserPage('1', { ...query, projectId: '5', folderId: '51' }), /响应包含失配/)
  await assert.rejects(fixture([active('103', '6')]).adapter.loadDccProjectBrowserPage('1', { ...query, projectId: '5', folderId: '51' }), /响应包含失配/)
})
test('folder query without project is rejected before issuing a request', async () => {
  const { adapter, calls } = fixture()
  await assert.rejects(adapter.loadDccProjectBrowserPage('1', { ...query, folderId: '51' }), /查询范围/)
  assert.equal(calls.length, 0)
})
test('global adapter still accepts different authorized projects without inventing a project filter', async () => {
  const { adapter, calls } = fixture([active('101'), active('103', '6')])
  const result = await adapter.loadDccProjectBrowserPage('1', query)
  assert.equal(result.list.length, 2)
  assert.equal(calls[0].params.dccProjectCodeId, undefined)
  assert.equal(calls[0].params.projectFolderId, undefined)
})

test('independent library loads its own folders and current active files without project discovery', async () => {
  const { state, calls }=fixture([active('101','700')])
  await state.selectLibrary({id:'700',projectName:'QMS文件夹',projectCode:'QMS',status:'ENABLE',projectLeaderUserId:null,projectLeader:null,associatedFileCount:1})
  assert.equal(state.error,'');assert.equal(state.folderError,'')
  assert.equal(state.project.id,'700');assert.equal(state.directories.length,2)
  assert.equal(calls.at(-1).params.dccProjectCodeId,'700')
  assert.equal(calls.at(-1).params.latestVersionOnly,true)
  assert.equal(state.rows[0].controlledFileId,'101')
})
