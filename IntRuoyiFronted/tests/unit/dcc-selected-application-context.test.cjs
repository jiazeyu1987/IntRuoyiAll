const { test } = require('node:test'),
  assert = require('node:assert/strict'),
  fs = require('node:fs'),
  path = require('node:path'),
  ts = require('typescript'),
  vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..'),
  read = (file) => fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file', file), 'utf8')
const id = '9007199254740993'
const rows = () => [
  { controlledFileId: id, applicationType: 'REVISION', bpmRound: 'round-1', attributeRound: 1 },
  { controlledFileId: id, applicationType: 'OBSOLETE', bpmRound: 'round-2', attributeRound: 2 }
]
const evidence = (round) => ({
  ...round,
  versionNo: 'A/1-2',
  recorded: true,
  defaultSource: {},
  actualAttributes: {},
  nodeRecords: [],
  relationEvidence: null,
  signatures: [{ id: '71', processInstanceId: round.bpmRound }]
})
const deferred = () => {
  let resolve
  const promise = new Promise((r) => {
    resolve = r
  })
  return { resolve, promise }
}
const plain = (value) => JSON.parse(JSON.stringify(value))
function moduleOf(source, deps = {}) {
  const exports = {}
  new Function(
    'exports',
    'require',
    ts.transpileModule(source, {
      compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
    }).outputText
  )(exports, (dep) => {
    if (dep === 'vue') return vue
    if (dep in deps) return deps[dep]
    if (dep.endsWith('.vue'))
      return { __esModule: true, default: { render: () => vue.h('evidence-part') } }
    throw Error('unexpected import ' + dep)
  })
  return exports
}
function mount(options = {}) {
  const events = [],
    calls = [],
    desc = parse(read('detail/DetailApplicationHistory.vue')).descriptor
  const component = moduleOf(
    compileScript(desc, { id: 'g11-history', inlineTemplate: Boolean(options.inline) }).content,
    {
      '@/utils/auth': { getTenantId: () => '1', getVisitTenantId: () => null },
      '@/api/dcc/controlledFile/applicationRead': {
        getControlledFileApplicationRounds: async (file) => {
          calls.push(['rounds', file])
          return options.getRounds ? options.getRounds(file) : rows()
        },
        getControlledFileApplicationEvidence: async (...args) => {
          calls.push(['evidence', ...args])
          return options.getEvidence
            ? options.getEvidence(...args)
            : evidence(rows().find((row) => row.bpmRound === args[2]))
        }
      },
      './presentation': { formatControlledFileDateTime: (value) => value },
      './application-round-context': moduleOf(read('detail/application-round-context.ts')),
      './application-node-records': moduleOf(read('detail/application-node-records.ts')),
      './relation-contract': moduleOf(read('detail/relation-contract.ts')),
      '../shared/signature-evidence': moduleOf(read('shared/signature-evidence.ts')),
      '../shared/version-display': moduleOf(read('shared/version-display.ts'))
    }
  ).default
  if (!options.inline) component.render = () => vue.h('section')
  const props = vue.reactive({
    fileId: id,
    contextKey: 'context-1',
    showNodeRecords: true,
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
      const index = n.parent?.children.indexOf(n)
      if (index >= 0) n.parent.children.splice(index, 1)
    },
    setText: (n, text) => {
      n.text = text
    },
    setElementText: (n, text) => {
      n.text = text
      n.children = []
    },
    patchProp: (n, k, _old, next) => {
      n.props[k] = next
    },
    parentNode: (n) => n.parent,
    nextSibling: () => null
  })
  const app = renderer.createApp({
    render: () =>
      vue.h(component, {
        ...props,
        'onSelection-change': (value) => events.push(value === null ? null : plain(value))
      })
  })
  app.config.warnHandler = () => {}
  app.directive('loading', {})
  for (const tag of [
    'ContentWrap',
    'el-alert',
    'el-select',
    'el-option',
    'el-empty',
    'el-collapse',
    'el-collapse-item',
    'el-table',
    'el-table-column'
  ])
    app.component(tag, {
      setup:
        (_props, { attrs, slots }) =>
        () =>
          vue.h(tag, attrs, slots.default?.({ row: {} }))
    })
  const tree = { children: [] },
    host = app.mount(tree),
    text = (n) => (n.text || '') + (n.children || []).map(text).join('')
  return {
    props,
    state: host.$.subTree.component.setupState,
    calls,
    events,
    app,
    visible: () => text(tree)
  }
}
const drain = async () => {
  for (let n = 0; n < 30; n++) await Promise.resolve()
  await vue.nextTick()
}
test('real parent receives initial null and verified exact selected application only after successful evidence read', async () => {
  const c = mount()
  try {
    await drain()
    assert.deepEqual(c.events, [null])
    await c.state.selectRound(c.state.roundKey(rows()[0]))
    assert.deepEqual(c.events, [null, null, rows()[0]])
    assert.equal(c.state.evidence.versionNo, 'A/1-2')
    assert.deepEqual(c.calls.at(-1), ['evidence', id, 'REVISION', 'round-1'])
  } finally {
    c.app.unmount()
  }
})
test('context switch clears selection synchronously and pending old evidence cannot emit the old round', async () => {
  const pending = deferred(),
    c = mount({
      getEvidence: async (_id, _type, bpm) =>
        bpm === 'round-1' ? pending.promise : evidence(rows()[1])
    })
  try {
    await drain()
    const first = c.state.selectRound(c.state.roundKey(rows()[0]))
    await drain()
    await c.state.selectRound(c.state.roundKey(rows()[1]))
    assert.deepEqual(c.events.at(-1), rows()[1])
    c.props.contextKey = 'context-2'
    await vue.nextTick()
    assert.equal(c.events.at(-1), null)
    pending.resolve(evidence(rows()[0]))
    await first
    await drain()
    assert.equal(c.events.at(-1), null)
    assert.equal(c.events.filter((row) => row?.bpmRound === 'round-1').length, 0)
  } finally {
    c.app.unmount()
  }
})
test('foreign file/type/BPM/attribute or signature cannot emit a verified round', async () => {
  for (const mutate of [
    (e) => {
      e.controlledFileId = '12'
    },
    (e) => {
      e.controlledFileId = Number.MAX_SAFE_INTEGER + 1
    },
    (e) => {
      e.applicationType = 'UPLOAD'
    },
    (e) => {
      e.bpmRound = 'other'
    },
    (e) => {
      e.attributeRound = 2
    },
    (e) => {
      e.recorded = false
      e.attributeRound = null
    },
    (e) => {
      e.signatures[0].processInstanceId = 'other'
    }
  ]) {
    const c = mount({
      getEvidence: async () => {
        const result = evidence(rows()[0])
        mutate(result)
        return result
      }
    })
    try {
      await drain()
      await c.state.selectRound(c.state.roundKey(rows()[0]))
      assert.ok(c.state.error)
      assert.equal(c.events.at(-1), null)
      assert.equal(c.events.filter(Boolean).length, 0)
      assert.equal(c.state.evidence, undefined)
    } finally {
      c.app.unmount()
    }
  }
})
test('invalid round mapping/read denial/locked wrong round clear selection and never publish', async () => {
  for (const options of [
    { getRounds: async () => [{ ...rows()[0], controlledFileId: '12' }] },
    {
      getEvidence: async () => {
        throw Error('EVIDENCE_DENIED')
      }
    },
    { props: { primaryBpmRound: 'round-1', lockPrimaryRound: true } }
  ]) {
    const c = mount(options)
    try {
      await drain()
      if (c.state.rounds.length) await c.state.selectRound(c.state.roundKey(rows()[1]))
      assert.ok(c.state.error)
      assert.equal(c.events.at(-1), null)
    } finally {
      c.app.unmount()
    }
  }
})
test('unmount clears a previously verified selection and suppresses late reads', async () => {
  const c = mount()
  await drain()
  await c.state.selectRound(c.state.roundKey(rows()[0]))
  c.app.unmount()
  assert.equal(c.events.at(-1), null)
})
test('real History template displays draft while evidence raw version and selected identity remain unchanged', async () => {
  const c = mount({
    inline: true,
    props: { primaryBpmRound: 'round-1', lockPrimaryRound: true, showNodeRecords: false }
  })
  try {
    await drain()
    assert.match(c.visible(), /草稿/)
    assert.doesNotMatch(c.visible(), /A\/1-2/)
    assert.deepEqual(c.events.at(-1), rows()[0])
  } finally {
    c.app.unmount()
  }
})

test('formal unrecorded evidence remains visible but cannot publish a verified attribute-round selection', async () => {
  const c = mount({
    getEvidence: async () => ({
      ...evidence(rows()[0]),
      recorded: false,
      attributeRound: null,
      actualAttributes: null,
      defaultSource: null,
      unavailableReason: 'NOT_RECORDED'
    })
  })
  try {
    await drain()
    await c.state.selectRound(c.state.roundKey(rows()[0]))
    assert.equal(c.state.error, '')
    assert.equal(c.state.evidence.unavailableReason, 'NOT_RECORDED')
    assert.equal(c.events.at(-1), null)
  } finally {
    c.app.unmount()
  }
})
