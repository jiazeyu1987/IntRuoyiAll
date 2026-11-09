// Offline component tests: run the actual compiled SFC setup with Vue reactivity.
// No browser/network/service, no external app-server, no shared dependency changes.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { parse, compileScript } = require('vue/compiler-sfc')
const ts = require('typescript')
const vue = require('vue')
const root = path.resolve(__dirname, '../..')
const plain = value => JSON.parse(JSON.stringify(value))
const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
const attributes = market => ({ targetMarkets: [market], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' })
const template = (id, name, used = false) => ({
  id, name, description: name + '说明', active: true, editedByUserId: 7, everUsed: used,
  structureJson: JSON.stringify({ nodes: [{ key: 'root', parentKey: null, name, sortOrder: 0 }] })
})
function loadState() {
  const source = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/project-attributes/state.ts'), 'utf8')
  const exportsObject = {}
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText,
    { exports: exportsObject, Error })
  return exportsObject
}
function loadFolderTree() {
  const source = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/basic-data/components/project-folder-tree.ts'), 'utf8')
  const exportsObject = {}
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText,
    { exports: exportsObject, Error })
  return exportsObject
}
function setup(relative, mocks = {}, props = {}) {
  const filename = path.join(root, relative)
  const { descriptor } = parse(fs.readFileSync(filename, 'utf8'), { filename })
  const script = compileScript(descriptor, { id: relative })
  const code = ts.transpileModule(script.content, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
  const exportsObject = {}
  vm.runInNewContext(code, {
    exports: exportsObject, Error, crypto: require('node:crypto').webcrypto,
    require: id => {
      if (Object.hasOwn(mocks, id)) return mocks[id]
      if (id === 'vue') return vue
      if (id === '@/utils/permission') return { checkPermi: () => true }
      if (id.endsWith('/state') || id === './state') return loadState()
      if (id.endsWith('.vue')) return {}
      throw new Error('Unconfigured component test dependency: ' + id)
    }
  })
  let exposed
  const events = []
  const scope = vue.effectScope()
  const componentProps = vue.reactive(props)
  const bindings = scope.run(() => exportsObject.default.setup(componentProps, {
    expose: value => { exposed = value }, emit: (...event) => events.push(event)
  }))
  return { bindings, exposed, events, props: componentProps, stop: () => scope.stop() }
}
const folderComponent = 'src/views/dcc/controlled-file/basic-data/components/FolderTemplateLibraryEditor.vue'
const configComponent = 'src/views/dcc/controlled-file/basic-data/components/ProjectAttributeConfigurationDialog.vue'
const applicationComponent = 'src/views/dcc/controlled-file/project-attributes/ProjectApplicationAttributes.vue'
const mappingComponent = 'src/views/dcc/controlled-file/basic-data/file-type-taxonomy/FileTypeCategoryMappingDialog.vue'
const projectFolderEditor = 'src/views/dcc/controlled-file/basic-data/components/ProjectFolderEditor.vue'
const apiPath = '@/api/dcc/controlledFile/projectAttributes'
async function run() {
  const cases = []
  async function test(name, action) {
    try { await action(); console.log('PASS: ' + name) }
    catch (error) { cases.push({ name, error }); console.error('FAIL: ' + name + ': ' + error.message) }
  }
  const folderRows = projectId => [{ id: '10', projectCodeId: String(projectId), parentId: '0', name: '正式根目录',
    sortOrder: 0, active: true, sourceTemplateId: '5', sourceNodeKey: 'root' }]
  await test('项目人工目录保存正式身份并不伪造模板来源', async () => {
    const calls = []
    const instance = setup(projectFolderEditor, {
      [apiPath]: { getProjectFolders: async id => folderRows(id), saveProjectFolder: async (id, payload) => {
        calls.push({ id, payload }); return { ...payload, id: '11', projectCodeId: String(id), active: true }
      } },
      './project-folder-tree': loadFolderTree(),
      'element-plus': { ElMessageBox: { confirm: async () => undefined } }
    })
    await instance.exposed.open('1')
    instance.bindings.form.name = '人工目录'
    instance.bindings.form.parentId = '10'
    instance.bindings.form.changeReason = '新增项目内目录'
    await instance.bindings.save()
    assert.equal(calls.length, 1)
    assert.equal(calls[0].id, '1')
    assert.equal(calls[0].payload.parentId, '10')
    assert.equal(calls[0].payload.sourceTemplateId, undefined)
    assert.equal(instance.events[0][0], 'saved')
    assert.equal(instance.events[0][1], '1')
    assert.equal(instance.bindings.visible.value, false)
    instance.stop()
  })
  await test('目录保存A晚返回不能关闭重开B且事件归真实项目', async () => {
    const writing = deferred()
    const instance = setup(projectFolderEditor, {
      [apiPath]: { getProjectFolders: async id => folderRows(id), saveProjectFolder: async () => writing.promise },
      './project-folder-tree': loadFolderTree(),
      'element-plus': { ElMessageBox: { confirm: async () => undefined } }
    })
    await instance.exposed.open('1', '10')
    instance.bindings.form.changeReason = '编辑A'
    const pending = instance.bindings.save()
    await instance.exposed.open('2')
    writing.resolve(folderRows('1')[0])
    await pending
    assert.equal(instance.bindings.visible.value, true)
    assert.equal(instance.bindings.projectId.value, '2')
    assert.equal(instance.bindings.form.id, undefined)
    assert.ok(instance.events.some(event => event[0] === 'saved' && event[1] === '1'))
    assert.ok(!instance.events.some(event => event[0] === 'saved' && event[1] === '2'))
    instance.stop()
  })
  await test('目录保存错误在本地可见且不发成功事件', async () => {
    const instance = setup(projectFolderEditor, {
      [apiPath]: { getProjectFolders: async id => folderRows(id), saveProjectFolder: async () => { throw new Error('目录形成循环，拒绝保存') } },
      './project-folder-tree': loadFolderTree(),
      'element-plus': { ElMessageBox: { confirm: async () => undefined } }
    })
    await instance.exposed.open('1', '10')
    instance.bindings.form.changeReason = '测试父级校验'
    await instance.bindings.save()
    assert.equal(instance.bindings.visible.value, true)
    assert.equal(instance.bindings.error.value, '目录形成循环，拒绝保存')
    assert.equal(instance.events.length, 0)
    instance.stop()
  })
  await test('目录加载跨项目返回明确拒绝且保存门禁关闭', async () => {
    let writes = 0
    const instance = setup(projectFolderEditor, {
      [apiPath]: { getProjectFolders: async () => folderRows('2'), saveProjectFolder: async () => { writes++; return {} } },
      './project-folder-tree': loadFolderTree(),
      'element-plus': { ElMessageBox: { confirm: async () => undefined } }
    })
    await instance.exposed.open('1')
    assert.equal(instance.bindings.ready.value, false)
    assert.ok(instance.bindings.error.value.includes('身份'))
    await instance.bindings.save()
    assert.equal(writes, 0)
    instance.stop()
  })
  await test('项目目录删除必须二次确认并发送精确ID和用户原因', async () => {
    const writes = []
    let confirmed = false
    const instance = setup(projectFolderEditor, {
      [apiPath]: { getProjectFolders: async id => folderRows(id), deleteProjectFolder: async (project, folder, data) => { writes.push({ project, folder, data }); return true } },
      './project-folder-tree': loadFolderTree(),
      'element-plus': { ElMessageBox: { confirm: async () => { if (!confirmed) throw 'cancel' } } }
    })
    await instance.exposed.openDelete('1', '10')
    instance.bindings.form.changeReason = '删除当前空目录'
    await instance.bindings.remove()
    assert.equal(writes.length, 0, '取消二次确认不能删除')
    confirmed = true
    await instance.bindings.remove()
    assert.deepEqual(plain(writes), [{ project: '1', folder: '10', data: { confirmed: true, changeReason: '删除当前空目录' } }])
    assert.ok(instance.events.some(event => event[0] === 'deleted' && event[1] === '1'))
    instance.stop()
  })
  for (const libraryScope of ['QMS', 'GENERAL']) await test(`${libraryScope}编辑可增改删，浏览不能维护，写入走对应文件库`, async () => {
    const calls = []
    const instance = setup(projectFolderEditor, {
      [apiPath]: { getProjectFolders: async id => folderRows(id),
        saveLibraryFolder: async (scope, data) => { calls.push(['save',scope,plain(data)]);return {...data,id:data.id || '11',projectCodeId:'1',active:true} },
        deleteLibraryFolder: async (scope,id,data) => { calls.push(['delete',scope,id,plain(data)]);return true } },
      '@/utils/permission': {checkPermi:()=>false},
      './project-folder-tree': loadFolderTree(),
      'element-plus': { ElMessageBox: { confirm: async () => undefined } }
    }, {libraryScope,libraryCanEdit:true})
    await instance.exposed.open('1',undefined,'10')
    assert.equal(instance.bindings.form.parentId,'10')
    instance.bindings.form.name='子目录';instance.bindings.form.changeReason='新增子目录'
    await instance.bindings.save();assert.equal(calls.length,1)
    assert.equal(calls[0][1],libraryScope);assert.equal(calls[0][2].parentId,'10')
    await instance.exposed.open('1','10');instance.bindings.form.name='改名';instance.bindings.form.changeReason='修改目录'
    await instance.bindings.save();assert.equal(calls[1][2].id,'10')
    await instance.exposed.openDelete('1','10');instance.bindings.form.changeReason='删除空目录'
    await instance.bindings.remove();assert.equal(calls[2][0],'delete');assert.equal(calls[2][1],libraryScope)
    instance.props.libraryCanEdit=false
    await instance.exposed.open('1');instance.bindings.form.name='禁止';instance.bindings.form.changeReason='禁止'
    await instance.bindings.save();await instance.exposed.openDelete('1','10');await instance.bindings.remove()
    assert.equal(calls.length,3)
    instance.stop()
  })
  if (cases.length) throw new Error(cases.length + ' component behavior tests failed')
}
run().catch(error => { console.error(error); process.exitCode = 1 })
