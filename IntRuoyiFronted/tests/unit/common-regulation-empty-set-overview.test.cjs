const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { parse } = require('vue/compiler-sfc')
const { compile, createSSRApp, h } = require('vue')
const { renderToString } = require('vue/server-renderer')

const filename = path.resolve(__dirname, '../../src/views/mes/pro/processpool/QaRegulationPage.vue')
const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'))
assert.deepEqual(errors, [])

function findOverview(node) {
  if (node.props?.some((prop) => prop.name === 'data-qa-common-overview')) return node
  for (const child of node.children || []) {
    const found = findOverview(child)
    if (found) return found
  }
}

const overview = findOverview(descriptor.template.ast)
assert.ok(overview, 'Common regulation overview must exist')
const render = compile(overview.loc.source)
const container = {
  setup(_, { slots }) {
    return () => h('div', [slots.header?.(), slots.default?.()])
  }
}
const button = {
  setup(_, { slots }) {
    return () => h('button', slots.default?.())
  }
}

async function renderOverview(selectedCommonRegulationSet) {
  const app = createSSRApp({
    render,
    setup: () => ({
      commonRegulationActiveTab: 'overview',
      selectedCommonRegulationSet,
      commonRegulationOverviewVersionText: 'A',
      commonRegulationOverviewDetailText: '已发布的 Word 规程',
      openCommonRegulationSetDialog() {},
      openCommonRegulationSetVersionDialog() {},
      deleteCommonRegulationSet() {}
    })
  })
  for (const name of ['el-card', 'el-tag', 'el-form', 'el-form-item', 'el-empty', 'el-input']) {
    app.component(name, container)
  }
  app.component('el-button', button)
  return renderToString(app)
}

for (const [name, selected] of [
  ['empty set list', undefined],
  ['selected existing set', { id: 1, setCode: 'PKG-1', setName: '序列1', setStatus: 'ENABLED' }]
]) {
  test(`${name}: first-set creation is visible and dependent actions require a selection`, async () => {
    const html = await renderOverview(selected)
    const create = html.match(/<button\b[^>]*data-qa-common-set-create[^>]*>/)?.[0]
    assert.ok(create, '新增套 must remain visible even before the first set exists')
    assert.doesNotMatch(create, /\bdisabled(?:[\s=>]|$)/)
    for (const action of ['version-create', 'edit', 'delete']) {
      const tag = html.match(
        new RegExp(`<button\\b[^>]*data-qa-common-overview-${action}[^>]*>`)
      )?.[0]
      assert.ok(tag, `${action} must remain visible`)
      assert.equal(/\bdisabled(?:[\s=>]|$)/.test(tag), !selected, `${action} selection requirement`)
    }
  })
}
