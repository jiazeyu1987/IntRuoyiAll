const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const path = require('node:path')
const root = path.resolve(__dirname, '../..')
const filename = 'src/views/form-center/parser/components/SimpleParserHeader.vue'
const descriptor = parse(fs.readFileSync(path.join(root, filename), 'utf8'), { filename }).descriptor
const source = compileScript(descriptor, { id: 'parser-header', inlineTemplate: true }).content
const permissions = new Set(['import', 'publish'])
const context = { exports: {}, require: (name) => {
  if (name === 'vue') return vue
  if (name === '@/directives/permission/hasPermi')
    return { hasPermission: (values) => values.some((p) => permissions.has(p)) }
  throw new Error(name)
} }
vm.runInNewContext(ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText, context)
const component = context.exports.default

test('all parser headers share button order, gating, project lock and action events', async () => {
  const props = vue.reactive({ title: '解析', projects: [], projectId: undefined,
    busy: false, hasContent: false, published: false,
    importPermission: 'import', publishPermission: 'publish' })
  const events = []
  const remove = (node) => {
    const index = node.parent?.children.indexOf(node)
    if (index >= 0) node.parent.children.splice(index, 1)
  }
  const renderer = vue.createRenderer({
    createElement: (type) => ({ type, props: {}, children: [] }),
    createText: (text) => ({ text }), createComment: () => ({}),
    patchProp: (node, key, old, value) => { node.props[key] = value },
    setText: (node, text) => { node.text = text },
    setElementText: (node, text) => { node.text = text },
    parentNode: (node) => node.parent,
    nextSibling: (node) => node.parent?.children[node.parent.children.indexOf(node) + 1],
    remove, insert: (node, parent, anchor) => {
      remove(node); node.parent = parent
      const index = parent.children.indexOf(anchor)
      if (index < 0) parent.children.push(node)
      else parent.children.splice(index, 0, node)
    }
  })
  const container = { children: [] }
  const app = renderer.createApp({ render: () => vue.h(component, {
    ...props, onImport: (file) => events.push(['import', file]),
    onExport: () => events.push(['export']), onSave: () => events.push(['save']),
    onRestore: () => events.push(['restore']), onPreview: () => events.push(['preview'])
  }) })
  // Render the shared template while keeping controls as inspectable host elements.
  for (const [name, type] of [['ElButton', 'button'], ['ElSelect', 'select'],
    ['ElOption', 'option'], ['ElUpload', 'upload']]) {
    app.component(name, { inheritAttrs: false, setup: (_, { attrs, slots }) =>
      () => vue.h(type, attrs, slots.default?.()) })
  }
  const all = (type, nodes = container.children) => nodes.flatMap((node) => [
    ...(node.type === type ? [node] : []), ...all(type, node.children || [])
  ])
  const text = (node) => (node.text || '') + (node.children || []).map(text).join('')
  const disabled = () => all('button').map((node) => Boolean(node.props.disabled))
  app.mount(container)
  try {
    assert.deepEqual(all('button').map((node) => text(node).trim()),
      ['导入 Word', '导出 Word', '保存草稿', '恢复草稿', '预览发布'])
    assert.equal(all('select')[0].props.placeholder, '选择所属项目')
    assert.deepEqual(disabled(), [true, true, true, false, true])
    props.projectId = 42
    await vue.nextTick()
    assert.deepEqual(disabled(), [false, true, true, false, true])
    const file = { name: '记录.docx' }
    all('upload')[0].props['on-change'](file)
    assert.deepEqual(events.pop(), ['import', file])
    props.hasContent = true
    await vue.nextTick()
    assert.deepEqual(disabled(), [false, false, false, false, false])
    assert.equal(all('select')[0].props.disabled, true)
    for (const button of all('button').slice(1)) button.props.onClick()
    assert.deepEqual(events, [['export'], ['save'], ['restore'], ['preview']])
    props.busy = true
    await vue.nextTick()
    assert.deepEqual(disabled(), [true, true, true, true, true])
    props.busy = false
    props.published = true
    await vue.nextTick()
    assert.deepEqual(disabled(), [false, false, true, false, true])
    assert.equal(all('select')[0].props.disabled, false)
    props.published = false
    props.projectId = undefined
    props.requiresProject = false
    await vue.nextTick()
    assert.equal(all('select').length, 0)
    assert.deepEqual(disabled(), [false, false, false, false, false])
    props.busy = true
    await vue.nextTick()
    assert.deepEqual(disabled(), [true, true, true, true, true])
  } finally { app.unmount() }
})

test('each parser uses the shared header and its existing business handlers', () => {
  for (const [file, save, restore, preview] of [
    ['parser/SimpleParserPage.vue', 'saveDraft', 'restoreDraft', 'openPublish'],
    ['simple-pqc-parser/index.vue', 'saveLocal', 'restoreLocal', 'preview'],
    ['simple-common-parser/index.vue', 'saveLocal', 'restoreLocal', 'preview']
  ]) {
    const source = fs.readFileSync(path.join(root, 'src/views/form-center', file), 'utf8')
    const template = parse(source).descriptor.template.content
    assert.equal((template.match(/<SimpleParserHeader\b/g) || []).length, 1)
    const header = template.match(/<SimpleParserHeader\b[\s\S]*?\/>/)[0]
    for (const [event, handler] of Object.entries({
      import: 'importWord', export: 'exportWord', save, restore, preview
    })) assert.ok(header.includes(`@${event}="${handler}"`))
  }
})
