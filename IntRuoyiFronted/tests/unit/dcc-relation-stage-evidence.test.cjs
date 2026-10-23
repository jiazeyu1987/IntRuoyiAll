const { test } = require('node:test')
const assert = require('node:assert/strict'),
  fs = require('node:fs'),
  vm = require('node:vm'),
  ts = require('typescript'),
  vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const filename = 'src/views/dcc/controlled-file/detail/DetailRelationStageEvidence.vue'
const source = () => fs.readFileSync(filename, 'utf8')
const id = '9007199254740993'
const header = (patch) => ({
  id,
  tenantId: '1',
  sourceFileId: '10',
  sourceMasterId: '100',
  projectId: '20',
  processInstanceId: 'round-1',
  taskId: 'task-1',
  obligationId: 'dept-5',
  stageCode: 'MATRIX_REVIEW',
  nodeInstanceId: 'node-1',
  departmentId: '5',
  actorId: '7',
  signatureId: '9007199254740995',
  signedAt: '2026-10-11 12:00:01',
  commandKey: 'key',
  payloadHash: 'a'.repeat(64),
  expectedDigest: 'b'.repeat(64),
  completeDigest: 'c'.repeat(64),
  actionType: 'ASSIGN',
  reason: '真实意见',
  applicationType: 'REVISION',
  attributeRound: 1,
  formActionInstanceId: null,
  ...patch
})
const member = (patch) => ({
  tenantId: '1',
  snapshotId: id,
  relatedMasterId: '300',
  selectedControlledFileId: '31',
  projectId: '20',
  fileNameSnapshot: '原A.pdf',
  fileNumberSnapshot: 'A',
  versionNoSnapshot: 'A/1',
  origin: 'UPLOAD',
  supplemental: false,
  assigneeUserId: null,
  dueAt: null,
  ...patch
})
const evidence = () => ({
  tenantId: '1',
  fileId: '10',
  masterId: '100',
  projectId: '20',
  applicationType: 'REVISION',
  attributeRound: 1,
  bpmRound: 'round-1',
  recorded: true,
  unavailableReason: null,
  originalUploadSelections: [],
  completeSelections: [],
  relationDigest: 'd'.repeat(64),
  stages: [
    {
      snapshot: { header: header(), members: [member()] },
      signatureEvidenceStatus: 'INVALID',
      signatureIdentityMatches: true
    }
  ]
})
function setup(value = evidence()) {
  const preview = require('./dcc-fixed-version-preview-test-loader.cjs')()
  const props = vue.reactive({
    evidence: value,
    tenantId: '1',
    fileId: '10',
    applicationType: 'REVISION',
    bpmRound: 'round-1',
    attributeRound: 1,
    contextKey: 'one'
  })
  const calls = [],
    permission = {
      tenantId: '1',
      controlledFileId: '31',
      masterId: '300',
      projectId: '20',
      versionNo: 'A/1',
      canPreview: true,
      status: 'OBSOLETE',
      pendingEffect: false
    }
  let permissionRead = async () => permission
  const { descriptor } = parse(source(), { filename })
  const scriptAst = ts.createSourceFile(
    'stage.ts',
    descriptor.scriptSetup.content,
    ts.ScriptTarget.Latest,
    true
  )
  const script = scriptAst.statements
    .filter((statement) => !ts.isImportDeclaration(statement))
    .map((statement) => statement.getText(scriptAst))
    .join('\n')
  const c = {
    exports: {},
    Error,
    BigInt,
    Set,
    Number,
    String,
    Array,
    ref: (value) => ({ value }),
    computed: (getter) => ({
      get value() {
        return getter()
      }
    }),
    watchers: [],
    onBeforeUnmount: () => {},
    getControlledFileRelationPermissions: async (selected) => {
      calls.push(selected)
      return permissionRead()
    }
  }
  c.defineProps = () => props
  c.watch = (_getter, callback) => {
    c.watchers.push(callback)
    callback()
  }
  c.buildDccControlledFilePreviewSource = preview.common.buildDccControlledFilePreviewSource
  vm.createContext(c)
  vm.runInContext(
    ts.transpileModule(
      script +
        '\nglobalThis.state={projection,error,previewId,openPreview, get previewSource(){return typeof previewSource === "undefined" ? undefined : previewSource}, closePreview: typeof closePreview === "undefined" ? undefined : closePreview}',
      { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }
    ).outputText,
    c
  )
  return {
    props,
    calls,
    permission,
    state: c.state,
    preview,
    setPermissionRead: (fn) => {
      permissionRead = fn
    },
    reset: () => c.watchers.forEach((fn) => fn())
  }
}
test('A33 stage evidence handler sends its target raw version to identical metadata and binary history context', async () => {
  const c = setup()
  await c.state.openPreview(member())
  const source = c.state.previewSource?.value
  assert.ok(source, 'stage handler must retain a target preview source')
  const findViewer = (node) =>
    node.type === 1 && node.tag === 'ProtectedPdfViewer'
      ? node
      : (node.children || []).map(findViewer).find(Boolean)
  const descriptor = parse(fs.readFileSync(filename, 'utf8')).descriptor
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
    expectedVersionNo: 'A/1'
  })
  const metadata = await c.preview.common.getOnlineFilePreviewMetadata(source)
  await c.preview.common.previewOnlineFileWithWatermark(source, metadata)
  assert.equal(
    c.preview.calls.binary[0].config.headers['X-DCC-Body-Read-Context'],
    c.preview.calls.metadata[0].params.bodyReadContext
  )
  c.state.closePreview()
  assert.equal(c.state.previewSource.value, undefined)
})
test('foreign file/tenant/round and nonboolean signature facts fail visibly, no fabricated stage rows', () => {
  for (const patch of [
    { fileId: '11' },
    { tenantId: '2' },
    { bpmRound: 'other' },
    { attributeRound: 2 }
  ]) {
    const c = setup({ ...evidence(), ...patch })
    assert.ok(c.state.error.value)
    assert.equal(c.state.projection.value, null)
  }
  const bad = evidence()
  bad.stages[0].signatureIdentityMatches = 1
  assert.ok(setup(bad).state.error.value)
})
test('fixed exact selected preview uses real permission lookup and refuses pending even when canPreview true', async () => {
  const c = setup()
  await c.state.openPreview(member())
  assert.deepEqual(c.calls, ['31'])
  assert.equal(c.state.previewId.value, '31')
  c.permission.status = 'CONTROLLED_PENDING_EFFECTIVE'
  c.permission.pendingEffect = true
  await c.state.openPreview(member())
  assert.equal(c.state.previewId.value, '')
  assert.match(c.state.error.value, /待生效/)
})
test('foreign selected metadata and missing body qualification show real error and never mount viewer', async () => {
  for (const patch of [
    { tenantId: '2' },
    { controlledFileId: '32' },
    { masterId: '999' },
    { projectId: '21' },
    { versionNo: 'A/2' },
    { canPreview: false }
  ]) {
    const c = setup()
    Object.assign(c.permission, patch)
    await c.state.openPreview(member())
    assert.equal(c.state.previewId.value, '')
    assert.ok(c.state.error.value)
  }
})
test('older preview result after round/context changes cannot open a previous body', async () => {
  const c = setup()
  let finish
  c.setPermissionRead(
    () =>
      new Promise((resolve) => {
        finish = resolve
      })
  )
  const pending = c.state.openPreview(member())
  c.props.contextKey = 'two'
  c.props.bpmRound = 'round-2'
  c.reset()
  finish(c.permission)
  await pending
  assert.equal(c.state.previewId.value, '')
  assert.equal(c.state.previewSource.value, undefined)
  assert.match(c.state.error.value, /申请轮次/)
})
test('A33 closing a stage preview while permissions are pending clears its source and cancels the late result', async () => {
  const c = setup()
  let finish
  c.setPermissionRead(
    () =>
      new Promise((resolve) => {
        finish = resolve
      })
  )
  const pending = c.state.openPreview(member())
  c.state.closePreview()
  finish(c.permission)
  await pending
  assert.equal(c.state.previewId.value, '')
  assert.equal(c.state.previewSource.value, undefined)
})
test('legacy explicit null and no signed head remain unrecorded without fabricating stage history', () => {
  const empty = setup(null)
  assert.equal(empty.state.projection.value, null)
  assert.equal(empty.state.error.value, '')
  const noHeads = evidence()
  noHeads.recorded = false
  noHeads.stages = []
  noHeads.unavailableReason = '本轮未记录'
  const c = setup(noHeads)
  assert.equal(c.state.error.value, '')
  assert.equal(c.state.projection.value.recorded, false)
})
test('snapshot signature names use exact frozen IDs and reject duplicate or lossy name identity', () => {
  for (const signatureFacts of [
    [{ id: Number.MAX_SAFE_INTEGER + 1, actorNicknameSnapshot: '错误当前名' }],
    [{ id: '7', actorNicknameSnapshot: 1 }],
    [{ id: '7' }, { id: '7' }]
  ]) {
    const c = setup()
    c.props.signatureFacts = signatureFacts
    c.reset()
    assert.equal(c.state.projection.value, null)
    assert.ok(c.state.error.value)
  }
})
test('real Vue mount displays independent same-actor obligations and preserved invalid-signature stages', async () => {
  const value = evidence(),
    secondHeader = header({
      id: '9007199254740997',
      signatureId: '9007199254740999',
      taskId: 'task-2',
      obligationId: 'dept-6',
      departmentId: '6',
      signedAt: '2026-10-11 13:00:02'
    })
  value.stages[0].snapshot.header.departmentName = '原义务质量部'
  value.stages.push({
    snapshot: {
      header: secondHeader,
      members: [
        member({ snapshotId: secondHeader.id }),
        member({
          snapshotId: secondHeader.id,
          relatedMasterId: '400',
          selectedControlledFileId: '41',
          fileNameSnapshot: '补充B.pdf',
          fileNumberSnapshot: 'B',
          versionNoSnapshot: 'B/1',
          origin: 'SUPPLEMENT',
          supplemental: true
        })
      ]
    },
    signatureEvidenceStatus: 'MISSING',
    signatureIdentityMatches: false
  })
  const { descriptor } = parse(source(), { filename }),
    script = compileScript(descriptor, { id: filename, inlineTemplate: true })
  const exports = {}
  new Function(
    'exports',
    'require',
    ts.transpileModule(script.content, {
      compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
    }).outputText
  )(exports, (dep) => {
    if (dep === 'vue') return vue
    if (dep === '@/api/common/filePreview')
      return require('./dcc-fixed-version-preview-test-loader.cjs')().common
    if (dep === '../shared/version-display') {
      const helper = { exports: {} }
      vm.runInNewContext(
        ts.transpileModule(
          fs.readFileSync('src/views/dcc/controlled-file/shared/version-display.ts', 'utf8'),
          { compilerOptions: { module: ts.ModuleKind.CommonJS } }
        ).outputText,
        helper
      )
      return helper.exports
    }
    if (dep === '@/api/dcc/controlledFile/applicationRead')
      return {
        getControlledFileRelationPermissions: async () => {
          throw Error('no body action in readonly display test')
        }
      }
    if (dep === '../view/index.vue')
      return { __esModule: true, default: { render: () => vue.h('protected-viewer') } }
    throw Error(dep)
  })
  const renderer = vue.createRenderer({
    createElement: (type) => ({ type, children: [], props: {} }),
    createText: (text) => ({ text, children: [] }),
    createComment: (text) => ({ text: '', children: [] }),
    insert: (child, parent) => parent.children.push(child),
    remove() {},
    setText: (node, text) => {
      node.text = text
    },
    setElementText: (node, text) => {
      node.text = text
    },
    patchProp: (node, key, _old, next) => {
      node.props[key] = next
    },
    parentNode: () => null,
    nextSibling: () => null
  })
  const props = {
    evidence: value,
    tenantId: '1',
    fileId: '10',
    applicationType: 'REVISION',
    bpmRound: 'round-1',
    attributeRound: 1,
    contextKey: 'readonly-mount',
    signatureFacts: [
      {
        id: '9007199254740995',
        actorNicknameSnapshot: '当时签名甲',
        actorDeptNameSnapshot: '当时实际研发部'
      }
    ]
  }
  const app = renderer.createApp(exports.default, props),
    root = { children: [] }
  app.config.warnHandler = () => {}
  for (const tag of ['el-alert', 'el-button', 'el-dialog'])
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
  const textContent = (node) => (node.text || '') + (node.children || []).map(textContent).join('')
  try {
    app.mount(root)
    await vue.nextTick()
    const visible = textContent(root)
    for (const text of [
      'dept-5',
      'dept-6',
      '原A.pdf',
      '补充B.pdf',
      'A/1',
      'B/1',
      '2026-10-11 12:00:01',
      '2026-10-11 13:00:02',
      '签名证据异常',
      '签名记录缺失',
      '本次补充',
      '当时签名甲',
      '当时实际研发部',
      '原义务质量部',
      '未记录'
    ])
      assert.ok(visible.includes(text), text)
    assert.ok(!visible.includes('签名人 7'))
    assert.ok(!visible.includes('义务部门：当时实际研发部'))
    assert.equal(value.stages[0].snapshot.members.length, 1)
    assert.equal(value.stages[1].snapshot.members.length, 2)
    assert.equal((visible.match(/原A.pdf/g) || []).length, 2)
  } finally {
    app.unmount()
  }
})
test('template presents each immutable obligation, fixed file version, real server time and independent invalid status, no edit/grant', () => {
  const text = source()
  assert.match(text, /signatureEvidenceStatus/)
  assert.match(text, /signedAt/)
  assert.match(text, /versionNoSnapshot/)
  assert.match(text, /supplemental/)
  assert.match(text, /DCC 签名投影/)
  assert.doesNotMatch(
    text,
    /replaceCurrentRelations|createGrant|requestGrant|download|el-select|el-checkbox|el-input/
  )
  const { descriptor, errors } = parse(text, { filename })
  assert.deepEqual(errors, [])
  const compiled = compileScript(descriptor, { id: filename })
  assert.deepEqual(
    compileTemplate({
      id: filename,
      filename,
      source: descriptor.template.content,
      compilerOptions: { bindingMetadata: compiled.bindings }
    }).errors,
    []
  )
})
