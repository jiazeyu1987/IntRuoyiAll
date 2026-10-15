const { test } = require('node:test'),
  assert = require('node:assert/strict'),
  fs = require('node:fs'),
  path = require('node:path'),
  vm = require('node:vm'),
  ts = require('typescript'),
  vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..'),
  read = (file) => fs.readFileSync(path.join(root, file), 'utf8')
function mod(source, deps = {}) {
  const c = {
    exports: {},
    Error,
    String,
    Number,
    BigInt,
    Object,
    Array,
    JSON,
    Promise,
    require: (id) =>
      id === 'vue'
        ? vue
        : deps[id] ||
          (id.endsWith('.vue')
            ? {}
            : (() => {
                throw Error(id)
              })())
  }
  vm.runInNewContext(
    ts.transpileModule(source, {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
    }).outputText,
    c
  )
  return c.exports
}
const metadata = {
  controlledFileId: '10',
  tenantId: '1',
  masterId: '100',
  projectId: '20',
  projectName: '项目',
  projectFolderId: '30',
  projectFolderName: '文件夹',
  fileName: '原件.docx',
  fileNumber: 'SOP',
  versionNo: 'A/1',
  status: 'PENDING_MATRIX_REVIEW',
  controlled: false,
  pendingEffect: false,
  executable: false,
  canEdit: true,
  canPreview: false
}
function mount(availability) {
  const calls = [],
    desc = parse(read('src/views/dcc/controlled-file/detail/DetailRelationsPanel.vue')).descriptor
  const relation = mod(read('src/views/dcc/controlled-file/detail/relation-contract.ts'))
  const compile = compileScript(desc, { id: 'selected-source' }),
    component = mod(compile.content, {
      '@/utils/auth': { getTenantId: () => 1, getVisitTenantId: () => null },
      '@/api/common/filePreview': require('./dcc-fixed-version-preview-test-loader.cjs')().common,
      '@/api/dcc/controlledFile/applicationRead': {
        getControlledFileRelationPermissions: async (id) => {
          calls.push(['metadata', id])
          return {
            ...metadata,
            hasCurrentControlledSource: availability,
            currentActiveControlledFileId: availability ? '11' : null
          }
        }
      },
      '@/api/dcc/controlledFile/relations': {
        listCurrentRelations: async () => {
          throw Error('detail must never load current relations')
        },
        listHistoricalRelations: async (id) => {
          calls.push(['history', id])
          return []
        }
      },
      './relation-contract': relation,
      '../shared/version-display': mod(
        read('src/views/dcc/controlled-file/shared/version-display.ts')
      )
    }).default
  component.render = () => vue.h('section')
  const renderer = vue.createRenderer({
    createElement: (type) => ({ type, children: [] }),
    createText: (text) => ({ text }),
    createComment: () => ({}),
    insert: (n, p) => p.children.push(n),
    remove() {},
    setText() {},
    setElementText() {},
    patchProp() {},
    parentNode: () => null,
    nextSibling: () => null
  })
  const props = {
    file: {
      id: '10',
      masterId: '100',
      dccProjectCodeId: '20',
      projectFolderId: '30',
      versionNo: 'A/1',
      title: '原件.docx',
      fileNumber: 'SOP'
    },
    allowEdit: true
  }
  const app = renderer.createApp({ render: () => vue.h(component, props) }),
    host = app.mount({ children: [] })
  return { state: host.$.subTree.component.setupState, calls, app }
}
const drain = async () => {
  for (let i = 0; i < 16; i++) await Promise.resolve()
  await vue.nextTick()
}
for (const availability of [false, true])
  test(`selected snapshot loads independently of current source availability=${availability}`, async () => {
    const c = mount(availability)
    try {
      await drain()
      assert.deepEqual(c.calls, [
        ['metadata', '10'],
        ['history', '10']
      ])
      assert.equal(c.state.selectedSource.controlledFileId, '10')
      assert.equal(c.state.files.length, 0)
      assert.equal(c.state.error, '')
    } finally {
      c.app.unmount()
    }
  })
test('formal permission wrapper still requires explicit boolean source facts and valid active pointer', async () => {
  let response = { ...metadata }
  const api = mod(read('src/api/dcc/controlledFile/applicationRead.ts'), {
    '@/config/axios': { default: { get: async () => response } },
    '@/views/dcc/controlled-file/project-attributes/state': { validateAttributes: (v) => v }
  })
  await assert.rejects(api.getControlledFileRelationPermissions('10'), /权限|事实|响应/)
  response = { ...metadata, hasCurrentControlledSource: false }
  assert.equal(
    (await api.getControlledFileRelationPermissions('10')).hasCurrentControlledSource,
    false
  )
  response = { ...metadata, hasCurrentControlledSource: true }
  await assert.rejects(api.getControlledFileRelationPermissions('10'), /权限|事实|响应/)
  response = { ...response, currentActiveControlledFileId: '11' }
  assert.equal(
    (await api.getControlledFileRelationPermissions('10')).currentActiveControlledFileId,
    '11'
  )
})
