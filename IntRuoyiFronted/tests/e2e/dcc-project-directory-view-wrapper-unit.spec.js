const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')
const root = path.resolve(__dirname, '../..')
const transpile = file => ts.transpileModule(fs.readFileSync(path.join(root, file), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
}).outputText
const tree = {}
vm.runInNewContext(transpile('src/views/dcc/controlled-file/basic-data/components/project-folder-tree.ts'), { exports: tree, Error })
const setup = result => {
  const calls = []
  const api = {}
  const request = { get: async args => { calls.push(args); if (result instanceof Error) throw result; return result } }
  vm.runInNewContext(transpile('src/api/dcc/controlledFile/projectAttributes.ts'), {
    exports: api, Error, require: id => {
      if (id === '@/config/axios') return { default: request }
      if (id.includes('project-folder-tree')) return tree
      throw new Error('Unexpected module: ' + id)
    }
  })
  return { api, calls }
}
const projectId = '9223372036854775700'
const parentId = '9223372036854775701'
const childId = '9223372036854775702'
const row = (id, parent, overrides = {}) => ({ id, parentId: parent, projectCodeId: projectId,
  name: 'empty-folder', sortOrder: 0, active: true, sourceTemplateId: null, sourceNodeKey: null, ...overrides })

test('VIEW directory wrapper preserves complete empty folders and exact long parent/project IDs', async () => {
  const source = [row(parentId, 0), row(childId, parentId)]
  const { api, calls } = setup(source)
  const rows = await api.getProjectFolders(projectId)
  assert.equal(rows[0].id, parentId)
  assert.equal(rows[0].parentId, '0')
  assert.equal(rows[1].parentId, parentId)
  assert.equal(rows[1].projectCodeId, projectId)
  assert.equal(calls[0].url, `/dcc/project-codes/${projectId}/folders`)
  assert.equal(calls[0].data, undefined)
  assert.equal(calls[0].params, undefined)
  assert.equal(source[0].parentId, 0, 'does not mutate the received data')
  const nodes = tree.buildProjectFolderTree(projectId, rows)
  assert.equal(nodes[0].children[0].id, childId)
})

test('VIEW wrapper rejects unsafe numeric project ID before issuing a request', async () => {
  const { api, calls } = setup([])
  await assert.rejects(() => api.getProjectFolders(Number(projectId)))
  assert.equal(calls.length, 0)
})

test('VIEW wrapper rejects unsafe numeric folder/parent/project/template IDs', async () => {
  for (const field of ['id', 'parentId', 'projectCodeId', 'sourceTemplateId']) {
    const { api } = setup([row(parentId, 0, { [field]: Number(projectId) })])
    await assert.rejects(() => api.getProjectFolders(projectId), field)
  }
})

test('VIEW wrapper rejects cross-project rows, missing parents, duplicate folders and cycles', async () => {
  for (const rows of [
    [row(parentId, 0, { projectCodeId: '2' })], [row(childId, parentId)],
    [row(parentId, 0), row(parentId, 0)], [row(parentId, childId), row(childId, parentId)]
  ]) await assert.rejects(() => setup(rows).api.getProjectFolders(projectId))
})

test('VIEW wrapper keeps legal empty tree separate from missing response', async () => {
  assert.equal((await setup([]).api.getProjectFolders(projectId)).length, 0)
  await assert.rejects(() => setup(null).api.getProjectFolders(projectId))
})

test('VIEW permission failure clears previous tree and never becomes an empty success or cached default', async () => {
  const state = tree.createProjectFolderState()
  await tree.loadProjectFolderTree(state, projectId, setup([row(parentId, 0)]).api.getProjectFolders)
  assert.equal(state.tree.length, 1)
  const error = new Error('无权读取该项目目录')
  const { api } = setup(error)
  await assert.rejects(() => api.getProjectFolders(projectId), failure => failure === error)
  assert.equal(await tree.loadProjectFolderTree(state, projectId, api.getProjectFolders), false)
  assert.equal(state.error, error.message)
  assert.equal(state.tree.length, 0)
  assert.equal(await tree.loadProjectFolderTree(state, projectId, setup([]).api.getProjectFolders), true)
  assert.equal(state.error, '')
  assert.equal(state.tree.length, 0)
})

test('Logical folder ID does not become a NAS directory or historical unregistered placement', async () => {
  const { api, calls } = setup([row(parentId, 0)])
  const rows = await api.getProjectFolders(projectId)
  assert.equal(Object.hasOwn(rows[0], 'directoryId'), false)
  assert.equal(Object.hasOwn(rows[0], 'storageDirectoryId'), false)
  assert.equal(Object.hasOwn(rows[0], 'controlledFileId'), false)
  assert.equal(calls[0].url.includes('file-placements'), false)
})
