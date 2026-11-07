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

test('unsaved PQC state survives tab switches and is released when the tab closes', async () => {
  // Exercise production route metadata even when the server menu disables keepAlive.
  const helper = load(read('src/utils/routerHelper.ts').replace(
    "import.meta.glob('../views/**/*.{vue,tsx}')", '{}'
  ), {
    'vue-router': {}, 'lodash-es': {}, qs: {}, '@/utils/is': { isUrl: () => false },
    '@/utils/edhrNavigationIcons': { edhrDirectoryIcons: {}, edhrPageIcons: {} }
  })
  const route = helper.generateRoute([{
    path: '/mdm/form-center/simple-pqc-parser', parentId: 991513,
    component: 'form-center/simple-pqc-parser/index',
    componentName: 'FormCenterSimplePqcParser', keepAlive: false, visible: true
  }])[0]
  assert.equal(route.meta.noCache, false)
  assert.equal(route.meta.keepAliveName, 'FormCenterSimplePqcParser')
  assert.equal(route.meta.tagsViewKeyMode, 'path')
  const other = { name: 'OtherPage', path: '/other', fullPath: '/other', meta: {} }
  route.fullPath = route.path
  const store = pinia.createPinia()
  const { useTagsViewStore } = load(read('src/store/modules/tagsView.ts'), {
    '@/router': {}, '@/utils/routerHelper': helper, pinia, '../index': { store },
    '@/utils': {}, './user': {}, 'lodash-es': {}
  })
  const tags = useTagsViewStore(store)
  tags.addView(route)
  tags.addView(other)

  const filename = 'src/views/form-center/simple-pqc-parser/index.vue'
  const descriptor = parse(read(filename), { filename }).descriptor
  const component = load(compileScript(descriptor, { id: 'pqc-cache' }).content, {
    '@/views/mes/qc/template/SamplingRuleEditor.vue': {},
    '../parser/components/SimpleParserHeader.vue': {},
    vue, 'vue-router': { useRouter: () => ({}) }, vuedraggable: {},
      '../parser/components/ParserProcessToolbar.vue': {}, '../parser/components/ParserTrialNavigation.vue': {}, '../parser/components/ParserIssueList.vue': {},
      './components/ParserProcessToolbar.vue': {}, './components/ParserTrialNavigation.vue': {}, './components/ParserIssueList.vue': {},
      '../parser/useParserEditing': loadEditing(vue), './useParserEditing': loadEditing(vue),
    '../parser/components/SimplePqcTrialPreview.vue': {},
    '@/api/mes/qc/template': {}, '@/api/mes/dv/machinery': {},
    '@/api/dcc/controlledFile/projectCodes': { getProjectCodePage: async () => ({ list: [] }) },
    '@/utils/auth': {}, '@/store/modules/user': {},
    '../parser/simplePqc': load(read('src/views/form-center/parser/simplePqc.ts'), {}),
    '../parser/simplePqcWord': load(read('src/views/form-center/parser/simplePqcWord.ts'), { pizzip: require('pizzip') })
  }, { useMessage: () => ({}) }).default
  const originalSetup = component.setup
  let state, mounts = 0
  component.setup = (props, context) => {
    mounts++
    state = originalSetup(props, context)
    return state
  }
  component.render = () => vue.h('div')
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
    { default: () => vue.h(active.value ? component : otherComponent,
      { key: active.value ? route.path : other.path }) }) })
  app.mount({ children: [] })
  await vue.nextTick()
  state.projectId.value = 42
  state.draft.value = { regulationName: '未保存的解析修改', processes: [{ items: [] }] }
  state.processIndex.value = 2
  state.itemIndex.value = 3
  state.viewMode.value = 'trial'
  state.confirmedUnbound.value = ['I1']
  const originalState = state
  for (let i = 0; i < 3; i++) {
    active.value = false
    await vue.nextTick()
    active.value = true
    await vue.nextTick()
  }
  assert.equal(mounts, 1)
  assert.equal(state, originalState)
  assert.equal(state.projectId.value, 42)
  assert.equal(state.draft.value.regulationName, '未保存的解析修改')
  assert.equal(state.processIndex.value, 2)
  assert.equal(state.itemIndex.value, 3)
  assert.equal(state.viewMode.value, 'trial')
  assert.equal(state.confirmedUnbound.value[0], 'I1')
  active.value = false
  await vue.nextTick()
  tags.delView(route)
  await vue.nextTick()
  tags.addView(route)
  active.value = true
  await vue.nextTick()
  assert.equal(mounts, 2)
  assert.equal(state.draft.value, undefined)
  app.unmount()
})

function loadEditing(vue) {
  const target = { exports: {}, require: () => vue }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync('src/views/form-center/parser/useParserEditing.ts', 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, target)
  return target.exports
}
