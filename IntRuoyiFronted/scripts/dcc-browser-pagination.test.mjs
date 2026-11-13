import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import ts from 'typescript'
import { createRenderer, h, nextTick, reactive } from 'vue'
import { ElPagination } from 'element-plus/es/components/pagination/index.mjs'

globalThis.MouseEvent = class MouseEvent extends Event {}

const source = readFileSync(new URL('../src/views/dcc/controlled-file/browser/project-browser.ts', import.meta.url), 'utf8')
const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
const node = type => ({ type, children: [], props: {} })
const renderer = createRenderer({
  createElement: node, createText: node, createComment: node,
  setText() {}, setElementText() {}, patchProp: (n, k, old, value) => { n.props[k] = value },
  insert(n, parent) { parent.children.push(n); n.parent = parent },
  remove(n) { n.parent.children.splice(n.parent.children.indexOf(n), 1) },
  parentNode: n => n.parent, nextSibling: () => null, setScopeId() {}
})
const settle = async () => { for (let i = 0; i < 10; i++) { await Promise.resolve(); await nextTick() } }

for (const projects of [false, true]) {
  test(`real Element Plus ${projects ? 'project' : 'file'} pagination survives a pending page request`, async () => {
    const requests = [], pages = []
    const load = query => {
      pages.push(query.pageNo)
      const request = deferred(); requests.push(request); return request.promise
    }
    const exports = {}
    new Function('exports', 'require', ts.transpileModule(source, {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
    }).outputText)(exports, name => {
      if (name.endsWith('/projectDiscovery')) return { getProjectDiscoveryPage: load }
      if (name.endsWith('/applicationRead')) return { loadDccProjectBrowserPage: (_, query) => load(query) }
      if (name.endsWith('/project-reference-contract')) return { referenceIdentity: String }
      return {}
    })
    const state = reactive(new exports.ProjectBrowserState('1', '1'))
    const page = projects ? 'projectPage' : 'pageNo', total = projects ? 'projectsTotal' : 'total'
    const reload = () => projects ? state.loadProjects() : state.loadFiles()
    state[total] = 34
    const app = renderer.createApp({ render: () => h(ElPagination, {
      currentPage: state[page], pageSize: 20, total: state[total], layout: 'total, prev, pager, next',
      'onUpdate:current-page': value => { state[page] = value }, onCurrentChange: reload
    }) })
    const root = node('root')
    app.mount(root)
    try {
      const buttons = n => [n, ...n.children.flatMap(buttons)]
      const click = className => buttons(root).find(n => n.type === 'button' && String(n.props.class).includes(className)).props.onClick(new MouseEvent('click'))
      click('btn-next'); await settle()
      assert.equal(state[page], 2)
      assert.equal(state[total], 34)
      assert.deepEqual(pages, [2], 'loading must not trigger an extra page 1 request')
      requests[0].resolve({ list: [], total: 34 }); await settle()
      assert.equal(state[page], 2)
      click('btn-prev'); await settle()
      assert.deepEqual(pages, [2, 1])
      requests[1].resolve({ list: [], total: 34 }); await settle()
      assert.equal(state[page], 1)
    } finally { app.unmount() }
  })
}
