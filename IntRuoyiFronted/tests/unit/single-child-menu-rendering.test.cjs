const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

function setup() {
  const file = path.resolve(__dirname, '../../src/layout/components/Menu/src/components/useRenderMenuItem.tsx')
  const compiled = ts.transpileModule(fs.readFileSync(file, 'utf8'), {
    fileName: file,
    compilerOptions: {
      module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022,
      jsx: ts.JsxEmit.React, jsxFactory: 'h'
    }
  })
  const pushes = []
  const imports = {
    'element-plus': { ElSubMenu: 'submenu', ElMenuItem: 'item' },
    '@/utils/is': { isUrl: value => /^https?:\/\//.test(value) },
    './useRenderMenuTitle': { useRenderMenuTitle: () => ({ renderMenuTitle: meta => meta?.title }) },
    '@/utils/routerHelper': {
      pathResolve: (parent, child) => /^https?:\/\//.test(child)
        ? child : path.posix.resolve(parent, child || '.')
    }
  }
  const exports = {}
  vm.runInNewContext(compiled.outputText, {
    exports,
    h: (type, props, children) => ({ type, props, slots: children }),
    useRouter: () => ({ push: target => pushes.push(target) }),
    require(id) {
      assert.ok(Object.hasOwn(imports, id), `Unexpected dependency: ${id}`)
      return imports[id]
    }
  }, { filename: file })
  return { render: exports.useRenderMenuItem().renderMenuItem, pushes }
}

const route = (routePath, title, children, meta = {}) => ({
  path: routePath, meta: { title, ...meta }, ...(children ? { children } : {})
})
const pqc = () => route('/mes/pro/feedback/edhr-batch-pqc-fill', '一线PQC')
const leader = () => route('/mes/pro/process-pool/pqc-leader', 'PQC组长')
const execution = children => route('/mes/edhr-production-execution', '生产执行', children, { alwaysShow: true })
const edhr = children => route('/mes/edhr-batch-processing', 'eDHR批记录', children, { alwaysShow: true })

test('single visible page replaces both directory levels, including alwaysShow directories', () => {
  const routes = [edhr([execution([pqc()])])]
  const before = structuredClone(routes)
  const nodes = setup().render(routes)
  assert.equal(nodes.length, 1)
  assert.equal(nodes[0].type, 'item')
  assert.equal(nodes[0].slots.default(), '一线PQC')
  assert.equal(nodes[0].props.index, '/mes/pro/feedback/edhr-batch-pqc-fill')
  assert.deepEqual(routes, before, 'Rendering must not change registered routes or metadata')
})

test('two execution pages keep the execution group and omit only its single-child outer group', () => {
  const [node] = setup().render([edhr([execution([pqc(), leader()])])])
  assert.equal(node.type, 'submenu')
  assert.equal(node.props.index, '/mes/edhr-production-execution')
  assert.equal(node.slots.title().slots, '生产执行')
  const children = node.slots.default()
  assert.equal(children.length, 2)
  assert.deepEqual(Array.from(children, child => child.slots.default()), ['一线PQC', 'PQC组长'])
})

test('multiple sibling groups stay grouped while each single-child group is collapsed', () => {
  const [node] = setup().render([edhr([
    execution([pqc()]), route('/history', '表单追溯', [route('/trace', '历史追溯')])
  ])])
  assert.equal(node.type, 'submenu')
  assert.equal(node.slots.title().slots, 'eDHR批记录')
  assert.deepEqual(Array.from(node.slots.default(), child => child.slots.default()), ['一线PQC', '历史追溯'])
})

test('hidden siblings do not prevent collapsing and hidden parents do not expose descendants', () => {
  const nodes = setup().render([
    edhr([execution([pqc(), route('/hidden', '隐藏页面', undefined, { hidden: true })])]),
    route('/hidden-parent', '隐藏目录', [leader()], { hidden: true })
  ])
  assert.equal(nodes.length, 1)
  assert.equal(nodes[0].slots.default(), '一线PQC')
})

test('collapsed relative paths keep every ancestor segment; absolute and external targets stay intact', () => {
  const { render } = setup()
  for (const [leafPath, expected] of [
    ['leaf', '/outer/inner/leaf'], ['/absolute', '/absolute'], ['https://example.com/pqc', 'https://example.com/pqc']
  ]) {
    const [node] = render([route('/outer', 'Outer', [route('inner', 'Inner', [route(leafPath, 'Leaf')])])])
    assert.equal(node.props.index, expected)
  }
})

test('standalone pages with empty children and alwaysShow render as clickable items', () => {
  const nodes = setup().render([route('/leaf', 'Leaf', [], { alwaysShow: true })])
  assert.equal(nodes[0].type, 'item')
  assert.equal(nodes[0].props.index, '/leaf')
})

test('retained multi-child parent pages preserve their title navigation', () => {
  const { render, pushes } = setup()
  const [node] = render([route('/parent', 'Parent', [pqc(), leader()], { hasParentRoute: true })])
  node.slots.title().props.onClick({ target: { closest: () => null } })
  assert.deepEqual(pushes, ['/parent'])
  node.slots.title().props.onClick({ target: { closest: () => ({}) } })
  assert.deepEqual(pushes, ['/parent'], 'The expansion arrow must not navigate')
})
