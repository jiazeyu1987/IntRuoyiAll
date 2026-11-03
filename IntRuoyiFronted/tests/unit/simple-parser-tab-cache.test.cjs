const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const pinia = require('pinia')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

function load(source, imports, globals = {}) {
  const context = { exports: {}, ...globals, require: (name) => {
    if (Object.hasOwn(imports, name)) return imports[name]
    throw new Error(`Unexpected import: ${name}`)
  } }
  vm.runInNewContext(ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return context.exports
}

for (const common of [false, true]) {
  test(`${common ? 'common' : 'production'} parser retains unsaved state across tabs and releases it on close`, async () => {
    const helper = load(read('src/utils/routerHelper.ts').replace(
      "import.meta.glob('../views/**/*.{vue,tsx}')", '{}'
    ), {
      'vue-router': {}, 'lodash-es': {}, qs: {}, '@/utils/is': { isUrl: () => false },
      '@/utils/edhrNavigationIcons': { edhrDirectoryIcons: {}, edhrPageIcons: {} }
    })
    const page = common ? 'simple-common-parser' : 'simple-parser'
    const name = common ? 'FormCenterSimpleCommonParser' : 'FormCenterSimpleParser'
    const route = helper.generateRoute([{
      path: `/mdm/form-center/${page}`, parentId: 991513,
      component: `form-center/${page}/index`,
      componentName: 'ServerMenuName', keepAlive: false, visible: true
    }])[0]
    assert.equal(route.meta.noCache, false)
    assert.equal(route.meta.keepAliveName, name)
    assert.equal(route.meta.tagsViewKeyMode, 'path')
    route.fullPath = route.path
    const other = { name: 'OtherPage', path: '/other', fullPath: '/other', meta: {} }
    const store = pinia.createPinia()
    const { useTagsViewStore } = load(read('src/store/modules/tagsView.ts'), {
      '@/router': {}, '@/utils/routerHelper': helper, pinia, '../index': { store },
      '@/utils': {}, './user': {}, 'lodash-es': {}
    })
    const tags = useTagsViewStore(store)
    tags.addView(route)
    tags.addView(other)
    let leaveGuard
    const warnings = []
    const savedDrafts = new Map()
    const globals = {
      useMessage: () => ({ warning: (text) => warnings.push(text) }), crypto: require('node:crypto').webcrypto,
      window: { addEventListener() {}, removeEventListener() {} },
      localStorage: { setItem: (key, value) => savedDrafts.set(key, value) }
    }
    const imports = {
      '../parser/components/ParserProcessToolbar.vue': {}, '../parser/components/ParserTrialNavigation.vue': {}, '../parser/components/ParserIssueList.vue': {},
      './components/ParserProcessToolbar.vue': {}, './components/ParserTrialNavigation.vue': {}, './components/ParserIssueList.vue': {},
      '../parser/useParserEditing': loadEditing(vue), './useParserEditing': loadEditing(vue),
      vuedraggable: {},
      vue, 'vue-router': { useRouter: () => ({}), onBeforeRouteLeave: (guard) => { leaveGuard = guard } },
      'element-plus': {}, vuedraggable: {},
      '@/api/mes/pro/batchrecordreport': {},
      '@/api/mes/pro/route': { getRouteProjectOptions: async () => [] },
      '@/api/mes/pro/batchrecordreport/simpleParser': { SimpleParserApi: { devices: async () => [] } },
      '@/api/mes/pro/process': {}, '@/api/mes/pro/craft': {}, '@/api/mes/md/item': {},
      '@/store/modules/user': { useUserStore: () => ({ getUser: { id: 7 } }) },
      '@/utils/auth': { getTenantId: () => 1, getVisitTenantId: () => undefined },
      './simpleRecognition': load(read('src/views/form-center/parser/simpleRecognition.ts'), {
        pizzip: require('pizzip')
      }),
      './components/SimpleParserHeader.vue': {}, '../parser/components/SimpleParserHeader.vue': {},
      './components/SimpleParameterDialog.vue': {}, './components/SimpleTrialPreview.vue': {},
      './simpleWord': {}, '@/assets/templates/simple-parser-export.docx?url': {},
      '../parser/components/SimplePqcTrialPreview.vue': {},
      '../parser/components/ParserRegulationResult.vue': {},
      '@/api/mes/qc/template/simpleCommonParser': {}, '@/api/mes/qc/template': {}, '@/api/mes/dv/machinery': {},
      '../parser/simplePqc': {},
      '@/api/dcc/controlledFile/projectCodes': { getProjectCodePage: async () => ({ list: [] }) },
      './draft': {}, '../parser/simplePqcWord': {}
    }
    const compile = (file, extraImports = {}) => {
      const descriptor = parse(read(file), { filename: file }).descriptor
      return load(compileScript(descriptor, { id: page }).content,
        { ...imports, ...extraImports }, globals).default
    }
    const component = compile(common
      ? 'src/views/form-center/simple-common-parser/index.vue'
      : 'src/views/form-center/parser/SimpleParserPage.vue')
    const originalSetup = component.setup
    let state, mounts = 0
    component.setup = (props, context) => {
      mounts++
      state = originalSetup(props, context)
      return state
    }
    component.render = () => vue.h('div')
    // Production caching must use the route wrapper's name, not its workbench child's name.
    const routeComponent = common ? component : compile('src/views/form-center/simple-parser/index.vue', {
      '../parser/SimpleParserPage.vue': component
    })
    if (!common) routeComponent.render = () => vue.h(component)
    assert.equal(routeComponent.name, route.meta.keepAliveName)
    const active = vue.ref(true)
    const remove = (node) => {
      const index = node.parent?.children.indexOf(node)
      if (index >= 0) node.parent.children.splice(index, 1)
    }
    const renderer = vue.createRenderer({
      createElement: () => ({ children: [] }), createText: (text) => ({ text }),
      createComment: () => ({}), patchProp() {}, setText() {}, setElementText() {},
      parentNode: (node) => node.parent,
      nextSibling: (node) => node.parent?.children[node.parent.children.indexOf(node) + 1],
      remove, insert: (node, parent, anchor) => {
        remove(node)
        node.parent = parent
        const index = parent.children.indexOf(anchor)
        if (index < 0) parent.children.push(node)
        else parent.children.splice(index, 0, node)
      }
    })
    const otherComponent = { name: 'OtherPage', render: () => vue.h('div') }
    const app = renderer.createApp({ render: () => vue.h(vue.KeepAlive,
      { include: tags.getCachedViews },
      { default: () => vue.h(active.value ? routeComponent : otherComponent,
        { key: active.value ? route.path : other.path }) }) })
    app.mount({ children: [] })
    try {
      await vue.nextTick()
      if (common) {
        state.projectId.value = 42
        state.draft.value = { regulationName: '未保存的规程', processes: [{ items: [] }] }
        state.processIndex.value = 2
        state.itemIndex.value = 3
        state.viewMode.value = 'trial'
      } else {
        await state.importWord({})
        assert.equal(warnings[0], '请先选择所属项目')
        state.projectId.value = 42
        state.projects.value = [{ id: 42, projectCode: 'P42', projectName: '所选项目', status: 'ENABLE' }]
        state.recognition.value = { product: {}, processes: [] }
        state.processes.value = [{ id: 'p1', value: { name: '未保存的工序' } }]
        state.routeName.value = '未保存的流程'
        state.selectedId.value = 'p1'
        state.mode.value = 'trial'
        assert.equal(state.dirty.value, true)
        assert.equal(state.buildRequest().projectId, 42)
        const previousRequest = state.requestId.value
        state.pendingRequest.value = { previewToken: 'old' }
        state.projectId.value = 43
        assert.notEqual(state.requestId.value, previousRequest)
        assert.equal(state.pendingRequest.value, undefined, 'changing project invalidates the publish preview')
        state.projectId.value = 42
        state.persistDraft()
        const saved = JSON.parse(savedDrafts.get(state.draftKey()))
        assert.equal(saved.project.id, 42)
        assert.equal(saved.project.projectCode, 'P42')
        state.routeName.value += '（修改）'
        assert.equal(leaveGuard(), true, 'switching tabs must not ask to discard cached edits')
      }
      const originalState = state
      for (let i = 0; i < 3; i++) {
        active.value = false
        await vue.nextTick()
        active.value = true
        await vue.nextTick()
      }
      assert.equal(mounts, 1)
      assert.equal(state, originalState)
      if (common) {
        assert.equal(state.projectId.value, 42)
        assert.equal(state.draft.value.regulationName, '未保存的规程')
        assert.equal(state.processIndex.value, 2)
        assert.equal(state.itemIndex.value, 3)
        assert.equal(state.viewMode.value, 'trial')
      } else {
        assert.equal(state.projectId.value, 42)
        assert.equal(state.processes.value[0].value.name, '未保存的工序')
        assert.equal(state.routeName.value, '未保存的流程（修改）')
        assert.equal(state.selectedId.value, 'p1')
        assert.equal(state.mode.value, 'trial')
        assert.equal(state.dirty.value, true)
      }
      active.value = false
      await vue.nextTick()
      tags.delView(route)
      await vue.nextTick()
      tags.addView(route)
      active.value = true
      await vue.nextTick()
      assert.equal(mounts, 2)
      if (common) assert.equal(state.draft.value, undefined)
      else assert.equal(state.processes.value.length, 0)
    } finally {
      app.unmount()
    }
  })
}

function loadEditing(vue) {
  const target = { exports: {}, require: () => vue }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/form-center/parser/useParserEditing.ts', 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, target)
  return target.exports
}
