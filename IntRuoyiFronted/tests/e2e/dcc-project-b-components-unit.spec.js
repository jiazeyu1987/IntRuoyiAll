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
  await test('损坏模板选择阻止保存旧对象', async () => {
    let writes = 0
    const instance = setup(folderComponent, {
      [apiPath]: { getFolderTemplates: async () => [template(1, 'A')], saveFolderTemplate: async () => { writes++; return 1 } },
      'element-plus': { ElMessageBox: { confirm: async () => undefined } }
    })
    await instance.exposed.open()
    instance.bindings.choose(template(1, 'A'))
    instance.bindings.choose({ ...template(2, 'B'), structureJson: '{"nodes":null}' })
    await instance.bindings.save()
    assert.equal(writes, 0, '无法解析B后不能保存仍留在内存中的A')
    assert.ok(instance.bindings.error.value.includes('结构'))
    instance.stop()
  })
  await test('模板写入成功刷新失败仍通知保存成功且不重写', async () => {
    let reloads = 0, writes = 0
    const instance = setup(folderComponent, {
      [apiPath]: {
        getFolderTemplates: async () => { if (++reloads > 1) throw new Error('刷新失败'); return [] },
        saveFolderTemplate: async () => { writes++; return 3 }
      },
      'element-plus': { ElMessageBox: { confirm: async () => undefined } }
    })
    await instance.exposed.open()
    instance.bindings.form.name = '新模板'
    instance.bindings.form.changeReason = '新建'
    instance.bindings.add()
    instance.bindings.form.structure.nodes[0].name = '根'
    await instance.bindings.save()
    assert.equal(writes, 1)
    assert.equal(instance.bindings.form.id, 3)
    assert.equal(instance.events.filter(event => event[0] === 'saved').length, 1, '保存事实不能被后续读失败隐藏')
    assert.ok(instance.bindings.error.value.includes('已保存'))
    instance.stop()
  })
  await test('保存A期间选择B旧返回不得改B身份', async () => {
    const save = deferred()
    const instance = setup(folderComponent, {
      [apiPath]: { getFolderTemplates: async () => [template(1, 'A'), template(2, 'B', true)], saveFolderTemplate: async () => save.promise },
      'element-plus': { ElMessageBox: { confirm: async () => undefined } }
    })
    await instance.exposed.open()
    instance.bindings.choose(template(1, 'A'))
    instance.bindings.form.changeReason = '编辑A'
    const writing = instance.bindings.save()
    instance.bindings.choose(template(2, 'B', true))
    save.resolve(1)
    await writing
    assert.equal(instance.bindings.form.id, 2)
    assert.equal(instance.bindings.form.name, 'B')
    assert.equal(instance.bindings.selectedUsed.value, true)
    instance.stop()
  })
  await test('A加载晚失败不能污染B或解除B加载', async () => {
    const first = deferred(), second = deferred()
    const instance = setup(configComponent, {
      '@/api/system/user': { getSimpleUserList: async () => [] },
      '@/api/dcc/controlledFile/projectCodes': { getProjectCode: id => String(id) === '1' ? first.promise : second.promise },
      [apiPath]: { configureProjectAttributes: async () => true }
    })
    const loadingA = instance.exposed.open(1)
    const loadingB = instance.exposed.open(2)
    first.reject(new Error('A加载失败'))
    await loadingA
    assert.equal(instance.bindings.loading.value, true, 'B请求仍未完成，不能解除加载')
    assert.equal(instance.bindings.error.value, '', 'A错误不能显示到B')
    second.resolve({ id: 2, projectLeaderUserId: 8, defaultAttributesJson: JSON.stringify(attributes('CE')) })
    await loadingB
    assert.equal(instance.bindings.ready.value, true)
    assert.equal(instance.bindings.leaderId.value, 8)
    instance.stop()
  })
  await test('A保存完成不能关闭刚重开的B或把A记为B', async () => {
    const writing = deferred()
    const instance = setup(configComponent, {
      '@/api/system/user': { getSimpleUserList: async () => [] },
      '@/api/dcc/controlledFile/projectCodes': { getProjectCode: async id => ({ id, projectLeaderUserId: 7, defaultAttributesJson: JSON.stringify(attributes('FDA')) }) },
      [apiPath]: { configureProjectAttributes: async () => writing.promise }
    })
    await instance.exposed.open(1)
    instance.bindings.changeReason.value = '修改A'
    const savingA = instance.bindings.save()
    await instance.exposed.open(2)
    writing.resolve(true)
    await savingA
    assert.equal(instance.bindings.visible.value, true)
    assert.equal(instance.bindings.projectId.value, 2)
    assert.ok(instance.events.some(event => event[0] === 'saved' && event[1] === 1), '保存事件必须属于实际写入的A')
    assert.ok(!instance.events.some(event => event[0] === 'saved' && event[1] === 2))
    instance.stop()
  })
  await test('损坏草稿属性必须在组件内明确报错而不初始化当前默认', async () => {
    let reads = 0
    const instance = setup(applicationComponent, {
      [apiPath]: { getProjectDefaults: async () => { reads++; return attributes('FDA') } },
      'element-plus': { ElMessageBox: { confirm: async () => undefined } }
    }, { action: 'UPLOAD', savedSnapshot: { projectId: '1', applicationType: 'UPLOAD',
      defaultSource: attributes('CE'), actual: { ...attributes('CE'), targetMarkets: [] } } })
    assert.ok(instance.bindings.state.error.includes('目标市场'))
    assert.equal(instance.bindings.state.actual, undefined)
    assert.equal(reads, 0)
    instance.stop()
  })
  await test('草稿恢复默认发出明确恢复事件供正式来源同步', async () => {
    const instance = setup(applicationComponent, {
      [apiPath]: { getProjectDefaults: async () => attributes('FDA') },
      'element-plus': { ElMessageBox: { confirm: async () => undefined } }
    }, { action: 'UPLOAD', savedSnapshot: { projectId: '1', applicationType: 'UPLOAD',
      defaultSource: attributes('NMPA'), actual: attributes('CE') } })
    await instance.bindings.restore()
    const event = instance.events.find(item => item[0] === 'restore-defaults')
    assert.ok(event, '恢复默认必须由A识别为正式恢复操作，不能仅普通保存实际值')
    assert.deepEqual(plain(event[1].defaultSource.targetMarkets), ['FDA'])
    assert.deepEqual(plain(event[1].actual.targetMarkets), ['FDA'])
    instance.stop()
  })
  await test('选项目请求失败显示实际原因并通知父页面恢复原项目', async () => {
    const instance = setup(applicationComponent, {
      [apiPath]: { getProjectDefaults: async () => { throw new Error('目标项目无编制权限') } },
      'element-plus': { ElMessageBox: { confirm: async () => undefined } }
    }, { action: 'REVISION', savedSnapshot: { projectId: '1', applicationType: 'REVISION',
      defaultSource: attributes('NMPA'), actual: attributes('CE') } })
    assert.equal(await instance.exposed.selectProject('2'), false)
    assert.equal(instance.bindings.state.projectId, '1')
    assert.equal(instance.bindings.state.error, '目标项目无编制权限')
    assert.deepEqual(plain(instance.bindings.state.actual.targetMarkets), ['CE'])
    assert.ok(instance.events.some(event => event[0] === 'project-change-cancelled' && event[1] === '1'))
    instance.stop()
  })
  for (const action of ['UPLOAD', 'REVISION', 'OBSOLETE']) {
    await test(action + '只读申请保留已保存实际值且拒绝迟到编辑事件', async () => {
      let reads = 0
      const instance = setup(applicationComponent, {
        [apiPath]: { getProjectDefaults: async () => { reads++; return attributes('FDA') } },
        'element-plus': { ElMessageBox: { confirm: async () => undefined } }
      }, { action, readonly: true, savedSnapshot: { projectId: '1', applicationType: action,
        defaultSource: attributes('NMPA'), actual: attributes('CE') } })
      const before = plain(instance.exposed.getSnapshot())
      instance.bindings.edit(attributes('FDA'))
      await instance.bindings.restore()
      assert.deepEqual(plain(instance.exposed.getSnapshot()), before)
      assert.equal(reads, 0)
      assert.equal(instance.events.length, 0, '只读历史不能发出修改申请的事件')
      instance.stop()
    })
    await test(action + '默认请求完成前转为只读不得覆盖快照', async () => {
      const reading = deferred()
      const instance = setup(applicationComponent, {
        [apiPath]: { getProjectDefaults: async () => reading.promise },
        'element-plus': { ElMessageBox: { confirm: async () => undefined } }
      }, { action, readonly: false, savedSnapshot: { projectId: '1', applicationType: action,
        defaultSource: attributes('NMPA'), actual: attributes('CE') } })
      const before = plain(instance.exposed.getSnapshot())
      const pending = instance.exposed.selectProject('2')
      await Promise.resolve()
      instance.props.readonly = true
      await vue.nextTick()
      reading.resolve(attributes('FDA'))
      assert.equal(await pending, false)
      assert.deepEqual(plain(instance.exposed.getSnapshot()), before)
      assert.equal(instance.events.length, 0)
      assert.equal(instance.bindings.loading.value, false)
      instance.stop()
    })
  }
  await test('只读属性字段不向父申请转发编辑事件', async () => {
    const instance = setup('src/views/dcc/controlled-file/project-attributes/ProjectAttributesFields.vue', {},
      { modelValue: attributes('CE'), readonly: true })
    instance.bindings.setField('licenseHolder', 'N')
    instance.bindings.marketsChanged(['FDA'])
    instance.bindings.transferChanged('Y')
    assert.equal(instance.events.length, 0)
    assert.deepEqual(plain(instance.props.modelValue), attributes('CE'))
    instance.stop()
  })
  await test('文件类型维护读取唯一启用类别且不猜审批配置', async () => {
    const requested = []
    const instance = setup(mappingComponent, {
      '@/api/dcc/controlledFile/fileTypeTaxonomies': {
        resolveFileTypeActiveCategory: async id => { requested.push(id); return '51' }
      }
    })
    await instance.exposed.open('31', '项目策划书')
    assert.deepEqual(requested, ['31'])
    assert.equal(String(instance.bindings.categoryId.value), '51')
    assert.equal(instance.bindings.error.value, '')
    instance.stop()
  })
  await test('文件类型无唯一映射明确报错不沿用前一次类别', async () => {
    const instance = setup(mappingComponent, {
      '@/api/dcc/controlledFile/fileTypeTaxonomies': {
        resolveFileTypeActiveCategory: async id => { if (id === '31') return '51'; throw new Error('该类型绑定多个启用文件类别') }
      }
    })
    await instance.exposed.open('31', '类型A')
    await instance.exposed.open('32', '类型B')
    assert.equal(instance.bindings.categoryId.value, undefined)
    assert.equal(instance.bindings.error.value, '该类型绑定多个启用文件类别')
    instance.stop()
  })
  await test('文件类型映射旧请求不能覆盖新选类型', async () => {
    const old = deferred()
    const instance = setup(mappingComponent, {
      '@/api/dcc/controlledFile/fileTypeTaxonomies': {
        resolveFileTypeActiveCategory: id => id === '31' ? old.promise : Promise.resolve('52')
      }
    })
    const pending = instance.exposed.open('31', '类型A')
    await instance.exposed.open('32', '类型B')
    old.resolve('51')
    await pending
    assert.equal(instance.bindings.typeName.value, '类型B')
    assert.equal(String(instance.bindings.categoryId.value), '52')
    instance.stop()
  })
  const folderRows = projectId => [{ id: '10', projectCodeId: String(projectId), parentId: '0', name: '正式根目录',
    sortOrder: 0, active: true, sourceTemplateId: '5', sourceNodeKey: 'root' }]
  await test('项目人工目录保存正式身份并不伪造模板来源', async () => {
    const calls = []
    const instance = setup(projectFolderEditor, {
      [apiPath]: { getProjectFolders: async id => folderRows(id), saveProjectFolder: async (id, payload) => {
        calls.push({ id, payload }); return { id: '11', projectCodeId: String(id), ...payload, active: true }
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
    writing.resolve({ ...folderRows('1')[0], name: '已保存A' })
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
  if (cases.length) throw new Error(cases.length + ' component behavior tests failed')
}
run().catch(error => { console.error(error); process.exitCode = 1 })
