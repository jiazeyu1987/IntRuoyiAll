const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const modulePath = path.join(root, 'src/views/dcc/controlled-file/basic-data/components/project-folder-tree.ts')
assert.ok(fs.existsSync(modulePath), '项目独立目录必须有实际结构/状态读取合同')
const exportsObject = {}
vm.runInNewContext(ts.transpileModule(fs.readFileSync(modulePath, 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText,
  { exports: exportsObject, Error })
const { buildProjectFolderTree, createProjectFolderState, loadProjectFolderTree } = exportsObject
const row = (id, parentId, projectCodeId = '1') => ({ id, parentId, projectCodeId, name: '目录' + id, active: true,
  sortOrder: Number(id), sourceTemplateId: '9', sourceNodeKey: 'node' + id })
const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
async function run() {
  const rows = [row('2', '1'), row('1', '0')]
  const tree = buildProjectFolderTree('1', rows)
  assert.equal(tree.length, 1)
  assert.equal(tree[0].id, '1'); assert.equal(tree[0].children[0].id, '2')
  assert.equal(tree[0].children[0].sourceNodeKey, 'node2')
  assert.deepEqual(rows.map(item => item.id), ['2', '1'], '目录构建不修改正式响应集合')
  for (const invalid of [
    [row('1', '0', '2')], [row('1', '0'), row('1', '0')], [row('2', '9')],
    [row('1', '2'), row('2', '1')], [row('1', '1')]
  ]) assert.throws(() => buildProjectFolderTree('1', invalid))
  const state = createProjectFolderState()
  const slow = deferred()
  const first = loadProjectFolderTree(state, '1', async () => slow.promise)
  await loadProjectFolderTree(state, '2', async () => [row('3', '0', '2')])
  slow.resolve([row('1', '0')])
  assert.equal(await first, false)
  assert.equal(state.projectId, '2'); assert.equal(state.tree[0].id, '3')
  await loadProjectFolderTree(state, '2', async () => [])
  assert.equal(state.tree.length, 0)
  await loadProjectFolderTree(state, '1', async () => { throw new Error('目录读取无权限') })
  assert.equal(state.error, '目录读取无权限'); assert.equal(state.tree.length, 0)
  const component = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/basic-data/components/ProjectFolderTreePanel.vue'), 'utf8')
  assert.ok(component.includes('getProjectFolders') && component.includes('state.error'))
  assert.ok(component.includes('尚未生成独立目录'))
  assert.ok(!component.includes('getProjectCodeFileTemplate'), '目录为空不能借分类/文件名模板补齐')
  console.log('PASS: 正式项目目录树、归属/循环/重复拒绝、项目切换/晚返回/错误/历史空态')
}
run().catch(error => { console.error(error); process.exitCode = 1 })
