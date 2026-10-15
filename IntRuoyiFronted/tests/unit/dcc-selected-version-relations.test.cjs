const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'),
  path = require('node:path'),
  ts = require('typescript'),
  vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const read = (file) =>
  fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail', file), 'utf8')
const file = {
  id: '9007199254740993',
  masterId: '100',
  dccProjectCodeId: '20',
  versionNo: 'A/1',
  title: '历史A.pdf',
  fileNumber: 'A'
}
const selection = {
  controlledFileId: file.id,
  applicationType: 'REVISION',
  bpmRound: 'round-A1',
  attributeRound: 2
}
const metadata = (id) => ({
  controlledFileId: id,
  tenantId: '1',
  masterId: [file.id, '12'].includes(id) ? '100' : '300',
  projectId: '20',
  projectName: '项目',
  projectFolderId: '21',
  projectFolderName: '质量',
  fileName: [file.id, '12'].includes(id) ? '历史A.pdf' : '现名X.pdf',
  fileNumber: [file.id, '12'].includes(id) ? 'A' : 'NEW-X',
  versionNo: id === file.id ? 'A/1' : id === '12' ? 'A/2' : 'X/1',
  status: 'SUPERSEDED',
  controlled: false,
  pendingEffect: false,
  executable: false,
  canPreview: true,
  canEdit: false,
  hasCurrentControlledSource: true,
  currentActiveControlledFileId: [file.id, '12'].includes(id) ? '12' : '32',
  latestControlledFileId: [file.id, '12'].includes(id) ? '12' : '32'
})
const snapshot = () => ({
  relationId: '7',
  controlledFileId: '31',
  masterId: '300',
  projectCodeId: '20',
  fileName: '冻结X.pdf',
  fileNumber: 'OLD-X',
  versionNo: 'X/1'
})
const selected = (id, master, name, origin = 'UPLOAD') => ({
  selectedControlledFileId: id,
  relatedMasterId: master,
  projectId: '20',
  fileName: name,
  fileNumber: name,
  versionNo: 'X/1',
  origin,
  supplemental: origin === 'SUPPLEMENT'
})
const evidence = () => {
  const upload = selected('31', '300', '上传X'),
    extra = selected('41', '400', '后补Z', 'SUPPLEMENT')
  const head = {
    id: '51',
    tenantId: '1',
    sourceFileId: file.id,
    sourceMasterId: '100',
    projectId: '20',
    processInstanceId: selection.bpmRound,
    taskId: 'task-1',
    obligationId: 'dept-5',
    stageCode: 'MATRIX_REVIEW',
    nodeInstanceId: 'node-1',
    departmentId: '5',
    actorId: '7',
    signatureId: '61',
    signedAt: '2026-10-14 10:00:00',
    commandKey: 'key',
    payloadHash: 'a'.repeat(64),
    expectedDigest: 'b'.repeat(64),
    completeDigest: 'c'.repeat(64),
    actionType: 'ASSIGN',
    reason: '原签名意见',
    applicationType: 'REVISION',
    attributeRound: 2,
    formActionInstanceId: null
  }
  const member = (row) => ({
    tenantId: '1',
    snapshotId: '51',
    relatedMasterId: row.relatedMasterId,
    selectedControlledFileId: row.selectedControlledFileId,
    projectId: row.projectId,
    fileNameSnapshot: row.fileName,
    fileNumberSnapshot: row.fileNumber,
    versionNoSnapshot: row.versionNo,
    origin: row.origin,
    supplemental: row.supplemental,
    assigneeUserId: row.supplemental ? '71' : null,
    dueAt: row.supplemental ? '2026-10-16 12:00:00' : null
  })
  return {
    controlledFileId: file.id,
    versionNo: 'A/1',
    ...selection,
    recorded: true,
    signatures: [
      { id: '61', processInstanceId: selection.bpmRound, actorNicknameSnapshot: '当时负责人' }
    ],
    relationEvidence: {
      tenantId: '1',
      fileId: file.id,
      masterId: '100',
      projectId: '20',
      applicationType: selection.applicationType,
      bpmRound: selection.bpmRound,
      attributeRound: 2,
      recorded: true,
      unavailableReason: null,
      relationDigest: 'd'.repeat(64),
      originalUploadSelections: [upload],
      completeSelections: [upload, extra],
      stages: [
        {
          signatureEvidenceStatus: 'INVALID',
          signatureIdentityMatches: true,
          snapshot: { header: head, members: [member(upload), member(extra)] }
        }
      ]
    }
  }
}
function moduleOf(source, deps = {}) {
  const exports = {}
  new Function(
    'exports',
    'require',
    ts.transpileModule(source, {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
    }).outputText
  )(exports, (id) => {
    if (id === 'vue') return vue
    if (id in deps) return deps[id]
    if (id.endsWith('.vue'))
      return { __esModule: true, default: { render: () => vue.h('unused-viewer') } }
    throw Error('unexpected import ' + id)
  })
  return exports
}
function mount(options = {}) {
  const preview = require('./dcc-fixed-version-preview-test-loader.cjs')()
  const calls = [],
    getMetadata =
      options.getMetadata ||
      (async (id) => ({ ...metadata(id), ...(id === '41' ? { masterId: '400' } : {}) }))
  const deps = {
    '@/api/common/filePreview': preview.common,
    '@/utils/auth': { getTenantId: () => '1', getVisitTenantId: () => null },
    '@/api/dcc/controlledFile/applicationRead': {
      getControlledFileRelationPermissions: async (id) => {
        calls.push(['metadata', id])
        return getMetadata(id)
      },
      getControlledFileApplicationEvidence: async (...args) => {
        calls.push(['evidence', ...args])
        return options.getEvidence ? options.getEvidence(...args) : evidence()
      },
      loadDccSelectorPage: async () => {
        throw Error('readonly selector must not run')
      }
    },
    '@/api/dcc/controlledFile/relations': {
      listCurrentRelations: async (id) => {
        calls.push(['current', id])
        return { sourceControlledFileId: id, rowVersion: '0', files: [] }
      },
      listHistoricalRelations: async (id) => {
        calls.push(['history', id])
        return options.getHistory ? options.getHistory(id) : [snapshot()]
      }
    },
    './relation-contract': moduleOf(read('relation-contract.ts')),
    '../shared/version-display': moduleOf(
      fs.readFileSync(
        path.join(root, 'src/views/dcc/controlled-file/shared/version-display.ts'),
        'utf8'
      )
    )
  }
  if (options.inline) {
    const stage = compileScript(parse(read('DetailRelationStageEvidence.vue')).descriptor, {
      id: 'stage',
      inlineTemplate: true
    })
    deps['./DetailRelationStageEvidence.vue'] = {
      __esModule: true,
      default: moduleOf(stage.content, deps).default
    }
  }
  const desc = parse(read('DetailRelationsPanel.vue')).descriptor
  const compiled = compileScript(desc, { id: 'g11', inlineTemplate: Boolean(options.inline) })
  const component = moduleOf(compiled.content, deps).default
  if (!options.inline) component.render = () => vue.h('section')
  const renderer = vue.createRenderer({
    createElement: (type) => ({ type, children: [], props: {} }),
    createText: (text) => ({ text, children: [] }),
    createComment: () => ({ children: [] }),
    insert: (node, parent, anchor) => {
      const old = parent.children.indexOf(node)
      if (old >= 0) parent.children.splice(old, 1)
      const index = parent.children.indexOf(anchor)
      parent.children.splice(index < 0 ? parent.children.length : index, 0, node)
      node.parent = parent
    },
    remove: (node) => {
      const index = node.parent?.children.indexOf(node)
      if (index >= 0) node.parent.children.splice(index, 1)
    },
    setText: (node, text) => {
      node.text = text
    },
    setElementText: (node, text) => {
      node.text = text
      node.children = []
    },
    patchProp: (node, key, _old, next) => {
      node.props[key] = next
    },
    parentNode: (node) => node.parent,
    nextSibling: () => null
  })
  const props = vue.reactive({ file: { ...file }, allowEdit: true, ...(options.props || {}) })
  const app = renderer.createApp({ render: () => vue.h(component, props) })
  app.config.warnHandler = () => {}
  for (const tag of ['ContentWrap', 'el-alert', 'el-dialog', 'el-button'])
    app.component(tag, {
      setup:
        (_props, { attrs, slots }) =>
        () =>
          vue.h(tag, attrs, slots.default?.())
    })
  app.directive('loading', {})
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
    host = app.mount(tree)
  const text = (node) => (node.text || '') + (node.children || []).map(text).join('')
  return {
    props,
    calls,
    state: host.$.subTree.component.setupState,
    app,
    preview,
    visible: () => text(tree)
  }
}
test('A33 actual relation handler keeps target history context identical for metadata and binary', async () => {
  const c = mount()
  try {
    await drain()
    await c.state.openPreview(c.state.files[0])
    const source = c.state.previewSource
    assert.ok(source, 'actual handler must expose the saved target preview source')
    const { descriptor } = parse(read('DetailRelationsPanel.vue'))
    const findViewer = (node) =>
      node.type === 1 && node.tag === 'ProtectedPdfViewer'
        ? node
        : (node.children || []).map(findViewer).find(Boolean)
    assert.ok(
      findViewer(descriptor.template.ast).props.some(
        (prop) =>
          prop.type === 7 &&
          prop.name === 'bind' &&
          prop.arg?.content === 'preview-source' &&
          prop.exp?.content === 'previewSource'
      )
    )
    assert.deepEqual(JSON.parse(JSON.stringify(source.bodyReadContext)), {
      intent: 'VERSION_HISTORY',
      controlledFileId: '31',
      expectedVersionNo: 'X/1'
    })
    const metadata = await c.preview.common.getOnlineFilePreviewMetadata(source)
    await c.preview.common.previewOnlineFileWithWatermark(source, metadata)
    const wire = c.preview.calls.metadata[0].params.bodyReadContext
    assert.equal(c.preview.calls.binary[0].config.headers['X-DCC-Body-Read-Context'], wire)
    assert.equal(JSON.parse(Buffer.from(wire, 'base64url')).expectedVersionNo, 'X/1')
    c.state.previewVisible = false
    assert.equal(c.state.previewId, '')
    assert.equal(c.state.previewSource, undefined)
  } finally {
    c.app.unmount()
  }
})
const drain = async () => {
  for (let n = 0; n < 40; n++) await Promise.resolve()
  await vue.nextTick()
}

test('history A/1 loads only its snapshot; current A/2 and target X/2 are never requested', async () => {
  const c = mount()
  try {
    await drain()
    assert.deepEqual(c.calls, [
      ['metadata', file.id],
      ['history', file.id],
      ['metadata', '31']
    ])
    assert.equal(c.state.error, '')
    assert.equal(c.state.files[0].fileName, '冻结X.pdf')
    assert.equal(c.state.files[0].fileNumber, 'OLD-X')
    assert.equal(c.state.files[0].versionNo, 'X/1')
    await c.state.openPreview(c.state.files[0])
    assert.equal(c.state.previewId, '31')
  } finally {
    c.app.unmount()
  }
})
test('real compiled Vue template renders frozen X/1 and no current-version section', async () => {
  const c = mount({ inline: true })
  try {
    await drain()
    assert.match(c.visible(), /冻结X.pdf/)
    assert.match(c.visible(), /OLD-X/)
    assert.match(c.visible(), /X\/1/)
    assert.doesNotMatch(c.visible(), /现名X|NEW-X|当前文件固定版本关联|当前关联来源/)
  } finally {
    c.app.unmount()
  }
})
test('selected exact application reads complete upload, supplement and remediation evidence instead of version history', async () => {
  const c = mount({ inline: true, props: { applicationSelection: selection } })
  try {
    await drain()
    assert.deepEqual(
      c.calls.filter((row) => row[0] === 'evidence'),
      [['evidence', file.id, 'REVISION', 'round-A1']]
    )
    assert.equal(c.calls.filter((row) => ['history', 'current'].includes(row[0])).length, 0)
    for (const text of [
      '上传X',
      '后补Z',
      '原上传选择',
      '本次补充',
      '负责人 71',
      '2026-10-16 12:00:00',
      '签名证据异常',
      '当时负责人'
    ])
      assert.ok(c.visible().includes(text), text)
  } finally {
    c.app.unmount()
  }
})
test('foreign source version/tenant/master/project and snapshot self/duplicate/project are rejected visibly', async () => {
  for (const patch of [
    { controlledFileId: '12' },
    { tenantId: '2' },
    { masterId: '101' },
    { projectId: '21' },
    { versionNo: 'A/2' }
  ]) {
    const c = mount({
      getMetadata: async (id) => ({ ...metadata(id), ...(id === file.id ? patch : {}) })
    })
    try {
      await drain()
      assert.ok(c.state.error)
      assert.equal(c.calls.filter((row) => row[0] === 'history').length, 0)
    } finally {
      c.app.unmount()
    }
  }
  for (const rows of [
    [{ ...snapshot(), masterId: '100' }],
    [snapshot(), snapshot()],
    [{ ...snapshot(), projectCodeId: '21' }]
  ]) {
    const c = mount({ getHistory: async () => rows })
    try {
      await drain()
      assert.ok(c.state.error)
      assert.equal(c.state.files.length, 0)
    } finally {
      c.app.unmount()
    }
  }
})
test('selected application context and envelope cannot drift to another file, tenant, master, project or round', async () => {
  for (const mutate of [
    (e) => {
      e.controlledFileId = '12'
    },
    (e) => {
      e.versionNo = 'A/2'
    },
    (e) => {
      e.applicationType = 'UPLOAD'
    },
    (e) => {
      e.bpmRound = 'other'
    },
    (e) => {
      e.attributeRound = 3
    },
    (e) => {
      e.relationEvidence.tenantId = '2'
    },
    (e) => {
      e.relationEvidence.fileId = '12'
    },
    (e) => {
      e.relationEvidence.masterId = '101'
    },
    (e) => {
      e.relationEvidence.projectId = '21'
    },
    (e) => {
      e.relationEvidence.bpmRound = 'other'
    },
    (e) => {
      e.relationEvidence.applicationType = 'UPLOAD'
    },
    (e) => {
      e.relationEvidence.attributeRound = 3
    }
  ]) {
    const c = mount({
      props: { applicationSelection: selection },
      getEvidence: async () => {
        const e = evidence()
        mutate(e)
        return e
      }
    })
    try {
      await drain()
      assert.ok(c.state.error)
      assert.equal(c.state.files.length, 0)
      assert.equal(c.calls.filter((row) => row[0] === 'history').length, 0)
    } finally {
      c.app.unmount()
    }
  }
})
test('read failure and unrecorded application remain explicit without current or version-history substitutes', async () => {
  const c = mount({
    getHistory: async () => {
      throw Error('SNAPSHOT_READ_FAILED')
    }
  })
  try {
    await drain()
    assert.match(c.state.error, /SNAPSHOT_READ_FAILED/)
    assert.equal(c.state.files.length, 0)
  } finally {
    c.app.unmount()
  }
  const missing = mount({
    props: { applicationSelection: selection },
    getEvidence: async () => ({ ...evidence(), relationEvidence: null })
  })
  try {
    await drain()
    assert.match(missing.state.unavailableReason, /未记录/)
    assert.equal(
      missing.calls.filter((row) => row[0] === 'history' || row[0] === 'current').length,
      0
    )
  } finally {
    missing.app.unmount()
  }
})
test('switching exact application while an old response is pending discards its evidence', async () => {
  let finish
  const c = mount({
    props: { applicationSelection: selection },
    getEvidence: async (_id, _type, round) =>
      round === 'round-A1'
        ? new Promise((resolve) => {
            finish = resolve
          })
        : { ...evidence(), bpmRound: round, relationEvidence: null }
  })
  try {
    await drain()
    c.props.applicationSelection = { ...selection, bpmRound: 'round-A2' }
    await drain()
    finish(evidence())
    await drain()
    assert.equal(c.state.files.length, 0)
    assert.match(c.state.unavailableReason, /未记录/)
    assert.equal(c.state.previewId, '')
  } finally {
    c.app.unmount()
  }
})
test('pending preview after switching version or unmount cannot open a previous body; arbitrary rows cannot be previewed', async () => {
  for (const unmount of [false, true]) {
    let finish,
      blocked = false
    const c = mount({
      getMetadata: async (id) =>
        id === '31' && blocked
          ? new Promise((resolve) => {
              finish = resolve
            })
          : metadata(id),
      getHistory: async (id) => (id === '12' ? [] : [snapshot()])
    })
    try {
      await drain()
      blocked = true
      const pending = c.state.openPreview(c.state.files[0])
      await drain()
      if (unmount) c.app.unmount()
      else {
        c.props.file = { ...file, id: '12', versionNo: 'A/2' }
        await drain()
      }
      finish(metadata('31'))
      await pending
      assert.equal(c.state.previewId, '')
      assert.equal(c.state.previewSource, undefined)
      if (!unmount) {
        assert.equal(c.state.selectedSource.controlledFileId, '12')
        assert.equal(c.state.files.length, 0)
        assert.equal(c.state.error, '')
      }
    } finally {
      if (!unmount) c.app.unmount()
    }
  }
  const c = mount()
  try {
    await drain()
    await c.state.openPreview({ ...c.state.files[0], controlledFileId: '32' })
    assert.equal(c.state.previewId, '')
    assert.ok(c.state.error)
  } finally {
    c.app.unmount()
  }
})
test('target preview rechecks precise permission and refuses pending or foreign metadata', async () => {
  let patch = {}
  const c = mount({
    getMetadata: async (id) => ({ ...metadata(id), ...(id === '31' ? patch : {}) })
  })
  try {
    await drain()
    for (const value of [
      { canPreview: false },
      { controlledFileId: '32' },
      { tenantId: '2' },
      { masterId: '301' },
      { projectId: '21' },
      { versionNo: 'X/2' },
      { pendingEffect: true, status: 'CONTROLLED_PENDING_EFFECTIVE' }
    ]) {
      patch = value
      await c.state.openPreview(c.state.files[0])
      assert.equal(c.state.previewId, '')
      assert.equal(c.state.previewSource, undefined)
      assert.ok(c.state.error)
    }
  } finally {
    c.app.unmount()
  }
})
test('A33 closing relation preview cancels a pending permission result without an old source', async () => {
  let finish,
    pendingRead = false
  const c = mount({
    getMetadata: async (id) =>
      pendingRead && id === '31'
        ? new Promise((resolve) => {
            finish = resolve
          })
        : metadata(id)
  })
  try {
    await drain()
    pendingRead = true
    const pending = c.state.openPreview(c.state.files[0])
    c.state.previewVisible = false
    finish(metadata('31'))
    await pending
    assert.equal(c.state.previewId, '')
    assert.equal(c.state.previewSource, undefined)
  } finally {
    c.app.unmount()
  }
})
test('old snapshot response after switching to a valid new version is discarded before target permission reads', async () => {
  let finish
  const c = mount({
    getHistory: async (id) =>
      id === file.id
        ? new Promise((resolve) => {
            finish = resolve
          })
        : []
  })
  try {
    await drain()
    c.props.file = { ...file, id: '12', versionNo: 'A/2' }
    await drain()
    finish([snapshot()])
    await drain()
    assert.equal(c.state.selectedSource.controlledFileId, '12')
    assert.equal(c.state.files.length, 0)
    assert.equal(c.state.error, '')
    assert.equal(c.calls.filter((row) => row[0] === 'metadata' && row[1] === '31').length, 0)
  } finally {
    c.app.unmount()
  }
})
test('selected application input, missing projection and malformed frozen fields fail without substitute reads', async () => {
  for (const applicationSelection of [
    { ...selection, controlledFileId: '12' },
    { ...selection, applicationType: 'OTHER' },
    { ...selection, bpmRound: ' ' },
    { ...selection, attributeRound: 0 }
  ]) {
    const c = mount({ props: { applicationSelection } })
    try {
      await drain()
      assert.ok(c.state.error)
      assert.deepEqual(c.calls, [])
    } finally {
      c.app.unmount()
    }
  }
  for (const mutate of [
    (e) => {
      delete e.relationEvidence
    },
    (e) => {
      e.relationEvidence.completeSelections[0].fileName = ''
    },
    (e) => {
      e.relationEvidence.completeSelections[0].supplemental = null
    },
    (e) => {
      e.relationEvidence.completeSelections[0].origin = 'UNKNOWN'
    }
  ]) {
    const c = mount({
      props: { applicationSelection: selection },
      getEvidence: async () => {
        const e = evidence()
        mutate(e)
        return e
      }
    })
    try {
      await drain()
      assert.ok(c.state.error)
      assert.equal(c.state.files.length, 0)
      assert.equal(c.calls.filter((row) => row[0] === 'history' || row[0] === 'current').length, 0)
    } finally {
      c.app.unmount()
    }
  }
})
test('SFC compiles script and template with the real binding metadata', () => {
  const filename = 'DetailRelationsPanel.vue',
    { descriptor, errors } = parse(read(filename), { filename })
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: filename })
  assert.deepEqual(
    compileTemplate({
      id: filename,
      filename,
      source: descriptor.template.content,
      compilerOptions: { bindingMetadata: script.bindings }
    }).errors,
    []
  )
})

test('explicit unresolved application context never falls back to the version snapshot', async () => {
  const c = mount({ props: { applicationSelection: null } })
  try {
    await drain()
    assert.deepEqual(c.calls, [])
    assert.match(c.state.error, /申请轮次/)
    assert.equal(c.state.files.length, 0)
  } finally {
    c.app.unmount()
  }
})

test('working source displays draft while formal request identity and version validation stay exact', async () => {
  const c = mount({
    inline: true,
    props: { file: { ...file, versionNo: 'A/1-2' } },
    getMetadata: async (id) => ({
      ...metadata(id),
      ...(id === file.id
        ? { versionNo: 'A/1-2', status: 'PENDING_MATRIX_REVIEW', canPreview: false }
        : {})
    })
  })
  try {
    await drain()
    assert.match(c.visible(), /草稿/)
    assert.doesNotMatch(c.visible(), /A\/1-2/)
    assert.match(c.visible(), /X\/1/)
    assert.deepEqual(c.calls, [
      ['metadata', file.id],
      ['history', file.id],
      ['metadata', '31']
    ])
  } finally {
    c.app.unmount()
  }
})
