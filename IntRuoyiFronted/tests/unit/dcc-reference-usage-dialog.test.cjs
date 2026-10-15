const { test } = require('node:test'),
  assert = require('node:assert/strict'),
  fs = require('node:fs'),
  path = require('node:path'),
  ts = require('typescript'),
  vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..'),
  read = (p) => fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file', p), 'utf8')
const source = {
  tenantId: '1',
  controlledFileId: '9007199254740993',
  masterId: '100',
  fileName: '原件.pdf',
  fileNumber: 'A',
  versionNo: 'A/1'
}
const row = () => ({
  referenceId: '71',
  projectId: '20',
  projectName: '目的项目',
  folderId: '21',
  folderName: '质量',
  masterId: '100',
  selectedControlledFileId: source.controlledFileId,
  fileName: '原件.pdf',
  fileNumber: 'A',
  versionNo: 'A/1',
  status: 'SUPERSEDED',
  controlled: false,
  pendingEffect: false,
  executable: false,
  canPreview: true
})
const page = () => ({
  tenantId: '1',
  sourceControlledFileId: source.controlledFileId,
  masterId: '100',
  referenceProjectCount: 2,
  visibleReferenceProjectCount: 1,
  total: 1,
  detailsRestricted: true,
  list: [row()]
})
function moduleOf(text, deps = {}) {
  const exports = {}
  new Function(
    'exports',
    'require',
    ts.transpileModule(text, {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
    }).outputText
  )(exports, (id) =>
    id === 'vue'
      ? vue
      : deps[id] ||
        (() => {
          throw Error(id)
        })()
  )
  return exports
}
function mount(options = {}) {
  const preview = require('./dcc-fixed-version-preview-test-loader.cjs')()
  const calls = [],
    opened = [],
    exports = {},
    events = []
  const deps = {
    '@/api/dcc/controlledFile/relations': {
      getProjectReferenceUsagePage: async (context) => {
        calls.push(['page', context])
        return options.getPage ? options.getPage(context) : page()
      }
    },
    '@/api/dcc/controlledFile/applicationRead': {
      getControlledFileRelationPermissions: async (id) => {
        calls.push(['metadata', id])
        return options.getMetadata
          ? options.getMetadata(id)
          : { ...row(), controlledFileId: id, tenantId: '1' }
      }
    },
    './project-reference-contract': moduleOf(read('relations/project-reference-contract.ts')),
    '../view/presentation': preview.view,
    '@/api/dcc/controlledFile/bodyReadContext': preview.body,
    '../shared/lifecycle': { getDccControlledFileStatusLabel: (v) => v },
    '../shared/version-display': moduleOf(read('shared/version-display.ts'))
  }
  const desc = parse(read('relations/DccReferenceUsageDialog.vue')).descriptor
  const compiled = compileScript(desc, { id: 'g11-usage', inlineTemplate: Boolean(options.inline) })
  new Function(
    'exports',
    'require',
    'window',
    ts.transpileModule(compiled.content, {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
    }).outputText
  )(
    exports,
    (dep) =>
      dep === 'vue'
        ? vue
        : deps[dep] ||
          (() => {
            throw Error(dep)
          })(),
    {
      open: (url) => {
        opened.push(url)
        return { opener: {} }
      }
    }
  )
  const component = exports.default
  if (!options.inline) component.render = () => vue.h('section')
  const props = vue.reactive({
    modelValue: true,
    source: { ...source },
    contextKey: 'one',
    returnTo: '/detail',
    ...(options.props || {})
  })
  const renderer = vue.createRenderer({
    createElement: (type) => ({ type, children: [], props: {} }),
    createText: (text) => ({ text, children: [] }),
    createComment: () => ({ children: [] }),
    insert: (n, p) => {
      if (!p.children.includes(n)) p.children.push(n)
      n.parent = p
    },
    remove: (n) => {
      const i = n.parent?.children.indexOf(n)
      if (i >= 0) n.parent.children.splice(i, 1)
    },
    setText: (n, text) => {
      n.text = text
    },
    setElementText: (n, text) => {
      n.text = text
      n.children = []
    },
    patchProp: (n, key, _old, v) => {
      n.props[key] = v
    },
    parentNode: (n) => n.parent,
    nextSibling: () => null
  })
  const app = renderer.createApp({
    render: () =>
      vue.h(component, { ...props, 'onUpdate:modelValue': (value) => events.push(value) })
  })
  app.config.warnHandler = () => {}
  app.directive('loading', {})
  for (const tag of ['el-dialog', 'el-alert', 'el-button', 'el-pagination'])
    app.component(tag, {
      setup:
        (_props, { attrs, slots }) =>
        () =>
          vue.h(tag, attrs, slots.default?.())
    })
  app.component('el-table', {
    props: ['data'],
    setup: (props, { slots }) => {
      vue.provide('rows', vue.toRef(props, 'data'))
      return () => vue.h('table', {}, slots.default?.())
    }
  })
  app.component('el-table-column', {
    setup: (_props, { attrs, slots }) => {
      const rows = vue.inject('rows')
      return () =>
        vue.h(
          'column',
          {},
          rows.value.map((row) =>
            slots.default ? slots.default({ row }) : String(row[attrs.prop] ?? '')
          )
        )
    }
  })
  const tree = { children: [] },
    host = app.mount(tree),
    text = (n) => (n.text || '') + (n.children || []).map(text).join('')
  return {
    calls,
    preview,
    opened,
    events,
    app,
    props,
    state: host.$.subTree.component.setupState,
    visible: () => text(tree)
  }
}
const drain = async () => {
  for (let i = 0; i < 30; i++) await Promise.resolve()
  await vue.nextTick()
}
test('actual dialog preview uses the saved exact row even if handler receives a changed selected version', async () => {
  const c = mount()
  try {
    await drain()
    await c.state.preview({ ...row(), selectedControlledFileId: '12' })
    assert.equal(c.calls.at(-1)[1], source.controlledFileId)
    assert.equal(
      new URL(c.opened[0], 'https://review.invalid').pathname,
      `/dcc/controlled-file/detail/${source.controlledFileId}`
    )
  } finally {
    c.app.unmount()
  }
})
test('A33 actual reference handler navigates its saved target history context and metadata/binary keep the same wire', async () => {
  const c = mount()
  try {
    await drain()
    await c.state.preview(row())
    const route = new URL(c.opened[0], 'https://review.invalid')
    const context = c.preview.body.readDccBodyReadContextRoute(
      source.controlledFileId,
      Object.fromEntries(route.searchParams)
    )
    assert.deepEqual(JSON.parse(JSON.stringify(context)), {
      intent: 'VERSION_HISTORY',
      controlledFileId: source.controlledFileId,
      expectedVersionNo: 'A/1'
    })
    const target = c.preview.common.buildDccControlledFilePreviewSource(
      source.controlledFileId,
      context
    )
    const metadata = await c.preview.common.getOnlineFilePreviewMetadata(target)
    await c.preview.common.previewOnlineFileWithWatermark(target, metadata)
    assert.equal(
      c.preview.calls.binary[0].config.headers['X-DCC-Body-Read-Context'],
      route.searchParams.get('bodyReadContext')
    )
    assert.equal(
      c.preview.calls.metadata[0].params.bodyReadContext,
      route.searchParams.get('bodyReadContext')
    )
  } finally {
    c.app.unmount()
  }
})
test('fixed reference pending state, foreign identity and lost permission cannot open body', async () => {
  for (const patch of [
    { status: 'CONTROLLED_PENDING_EFFECTIVE', pendingEffect: true, canPreview: true },
    { status: 'PENDING_MATRIX_REVIEW', canPreview: true },
    { tenantId: '2' },
    { controlledFileId: '12' },
    { masterId: '101' },
    { versionNo: 'A/2' },
    { canPreview: false }
  ]) {
    const c = mount({
      getMetadata: async (id) => ({ ...row(), tenantId: '1', controlledFileId: id, ...patch })
    })
    try {
      await drain()
      await c.state.preview(row())
      assert.equal(c.opened.length, 0)
      assert.ok(c.state.error)
    } finally {
      c.app.unmount()
    }
  }
})
test('context switch or close/unmount during a pending preview prevents the old window', async () => {
  for (const action of ['switch', 'close', 'unmount']) {
    let finish
    const c = mount({
      getMetadata: async () =>
        new Promise((r) => {
          finish = r
        })
    })
    try {
      await drain()
      const pending = c.state.preview(row())
      await drain()
      if (action === 'switch') {
        c.props.contextKey = 'two'
        await vue.nextTick()
      } else if (action === 'close') c.state.close()
      else c.app.unmount()
      finish({ ...row(), controlledFileId: source.controlledFileId, tenantId: '1' })
      await pending
      assert.equal(c.opened.length, 0)
    } finally {
      if (action !== 'unmount') c.app.unmount()
    }
  }
})
test('actual dialog shows precise source and fixed versions through draft helper without changing request identity', async () => {
  const c = mount({
    inline: true,
    props: { source: { ...source, versionNo: 'A/1-2' } },
    getPage: async () => ({ ...page(), list: [{ ...row(), versionNo: 'A/1-2' }] })
  })
  try {
    await drain()
    assert.match(c.visible(), /草稿/)
    assert.doesNotMatch(c.visible(), /A\/1-2/)
    assert.equal(c.calls[0][1].sourceControlledFileId, source.controlledFileId)
    assert.equal(
      c.state.page?.sourceControlledFileId ?? source.controlledFileId,
      source.controlledFileId
    )
  } finally {
    c.app.unmount()
  }
})
