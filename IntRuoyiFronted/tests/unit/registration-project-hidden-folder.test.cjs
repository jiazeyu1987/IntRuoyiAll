const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')
const panel = fs.readFileSync('src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue', 'utf8')
const descriptor = parse(panel).descriptor
const transpile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const helpers = {}
new Function('exports', transpile(fs.readFileSync('src/views/dcc/controlled-file/shared/file-type-taxonomy-stage.ts', 'utf8')))(helpers)

function state() {
  const names = [
    'DCC_PROJECT_CODE_UNCLASSIFIED_TYPE', 'associatedNavigationFiles', 'showHiddenAssociatedFolders',
    'associatedFilesTotal', 'selectedAssociatedFileIds', 'selectedAssociatedStageKey', 'selectedAssociatedTypeKey',
    'focusedAssociatedFileId', 'normalizeAssociatedLevel', 'associatedTaxonomyStageRows',
    'associatedTaxonomyStageOptions', 'associatedTaxonomyStageNames', 'associatedTaxonomyStageNameMap',
    'associatedTaxonomyStageTypeNameMap', 'associatedTaxonomyStageTypeOptionsMap', 'hiddenAssociatedTaxonomyIds',
    'isAssociatedTaxonomyHidden', 'hiddenAssociatedFileCount', 'resolveAssociatedStageKey', 'resolveAssociatedTypeName',
    'createAssociatedStageGroup', 'associatedStageGroups', 'selectedAssociatedStageGroup',
    'selectedAssociatedTypeGroup', 'associatedFilePage', 'selectedAssociatedFilesTotal', 'selectedAssociatedPagedFiles',
    'resetAssociatedFilePage', 'handleAssociatedFilePagination', 'resolveAssociatedInitialTypeKey',
    'ensureAssociatedSelection', 'selectAssociatedStage', 'selectAssociatedType', 'handleHiddenAssociatedFoldersChange',
    'isSameAssociatedFileId', 'resolveAssociatedRouteFocusFile', 'applyAssociatedRouteFocus', 'resetAssociatedFilesState'
  ]
  const script = ts.createSourceFile('panel.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const declarations = script.statements.filter(ts.isVariableStatement).flatMap(s => s.declarationList.declarations)
    .filter(d => names.includes(d.name.getText(script)))
  assert.equal(declarations.length, names.length, 'execute the actual SFC declarations, without replacement implementations')
  const source = declarations.map(d => 'const ' + d.getText(script) + ';').join('\n') + '\nreturn {' + names.join(',') + '}'
  const taxonomy = vue.ref([
    { id: 101, code: 'FT_TECH_DOC', name: '技术文档', children: [
      { id: 102, code: 'FT_DESIGN_REGISTRATION_STAGE', name: '注册资料汇编', children: [
        { id: 103, code: 'REGISTRATION_CERTIFICATE', name: '重命名注册证目录', sort: 1 },
        { id: 104, code: 'GSPR', name: 'GSPR', sort: 2 },
        { id: 105, code: 'OTHER', name: '注册证', sort: 3 }
      ] }
    ] }
  ])
  const route = { query: {} }
  return { ...new Function('ref', 'computed', 'reactive', 'fileTypeTaxonomies', 'helpers', 'route',
    'resolveQueryAssociatedFileId', 'resolveQueryAssociatedTaxonomyId',
    'const {' + Object.keys(helpers).join(',') + '} = helpers;\n' + transpile(source))(
    vue.ref, vue.computed, vue.reactive, taxonomy, helpers, route,
    () => route.query.associatedFileId, () => route.query.taxonomyId), route }
}

test('actual project SFC hides the stable registration folder by default and toggles without losing ordinary files', () => {
  const s = state()
  const certificate = { id: 11, fileTypeTaxonomyId: 103, businessSourceType: 'DCC_REGISTRATION_CERTIFICATE' }
  const ordinary = { id: 12, fileTypeTaxonomyId: 104 }
  const sameNameDifferentCode = { id: 13, fileTypeTaxonomyId: 105 }
  s.associatedNavigationFiles.value = [certificate, ordinary, sameNameDifferentCode]
  s.ensureAssociatedSelection()
  assert.equal(s.showHiddenAssociatedFolders.value, false)
  assert.equal(s.hiddenAssociatedFileCount.value, 1)
  assert.deepEqual(s.associatedStageGroups.value[0].types.map(t => t.taxonomyId), [104, 105])
  assert.equal(s.associatedStageGroups.value[0].count, 2)
  s.showHiddenAssociatedFolders.value = true
  s.handleHiddenAssociatedFoldersChange()
  s.selectAssociatedType('重命名注册证目录')
  assert.deepEqual(s.selectedAssociatedPagedFiles.value.map(f => f.id), [11])
  s.associatedFilePage.pageNo = 3
  s.selectedAssociatedFileIds.value = [11]
  s.showHiddenAssociatedFolders.value = false
  s.handleHiddenAssociatedFoldersChange()
  assert.equal(s.associatedFilePage.pageNo, 1)
  assert.deepEqual(s.selectedAssociatedFileIds.value, [])
  assert.equal(s.selectedAssociatedTypeKey.value, 'GSPR')
  assert.deepEqual(s.selectedAssociatedPagedFiles.value.map(f => f.id), [12])
})

test('actual deep-link focus explicitly reveals its file and a new project resets default visibility', () => {
  const s = state()
  s.associatedNavigationFiles.value = [{ id: 11, fileTypeTaxonomyId: 103 }]
  s.route.query.associatedFileId = 11
  assert.equal(s.applyAssociatedRouteFocus(), true)
  assert.equal(s.showHiddenAssociatedFolders.value, true)
  assert.deepEqual(s.selectedAssociatedPagedFiles.value.map(f => f.id), [11])
  s.resetAssociatedFilesState()
  assert.equal(s.showHiddenAssociatedFolders.value, false)
  assert.deepEqual(s.associatedNavigationFiles.value, [])
})

test('the actual rendered checkbox updates the same SFC visibility state and selection handler', async () => {
  const s = state()
  s.associatedNavigationFiles.value = [{ id: 11, fileTypeTaxonomyId: 103 }]
  const checkbox = descriptor.template.content.match(/<el-checkbox v-model="showHiddenAssociatedFolders"[\s\S]*?<\/el-checkbox>/)?.[0]
  assert.ok(checkbox)
  const compiled = compileTemplate({ id: 'hidden-folder', source: checkbox })
  assert.deepEqual(compiled.errors, [])
  const rendered = {}
  new Function('exports', 'require', transpile(compiled.code))(rendered, () => vue)
  const node = type => ({ type, children: [], props: {}, parent: null })
  const renderer = vue.createRenderer({ createElement: node, createText: node, createComment: node,
    insert(c, p) { c.parent = p; p.children.push(c) }, remove() {}, setText() {}, setElementText() {},
    parentNode: n => n.parent, nextSibling: () => null, patchProp: (n, k, old, value) => { n.props[k] = value } })
  const app = renderer.createApp({ setup: () => s, render: rendered.render })
  app.component('el-checkbox', { inheritAttrs: false, setup: (_, ctx) => () => vue.h('checkbox', ctx.attrs, ctx.slots.default?.()) })
  const root = node('root')
  app.mount(root)
  try {
    const checkboxNode = root.children.find(n => n.type === 'checkbox')
    assert.equal(checkboxNode.props.modelValue, false)
    checkboxNode.props['onUpdate:modelValue'](true)
    checkboxNode.props.onChange(true)
    await vue.nextTick()
    assert.equal(s.showHiddenAssociatedFolders.value, true)
    assert.equal(s.associatedStageGroups.value[0].types.some(t => t.taxonomyId === 103), true)
  } finally { app.unmount() }
})
