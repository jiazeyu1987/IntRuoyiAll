const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const Vue = require('vue')
const { renderToString } = require('vue/server-renderer')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const cache = new Map()
function load(file) {
  const absolute = path.resolve(root, file)
  if (cache.has(absolute)) return cache.get(absolute)
  const source = fs.readFileSync(absolute, 'utf8')
  const code = file.endsWith('.vue')
    ? compileScript(parse(source, { filename: absolute }).descriptor, {
        id: file,
        inlineTemplate: true
      }).content
    : source
  const js = ts.transpileModule(code, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
      esModuleInterop: true
    }
  }).outputText
  const exports = {}
  function resolve(name) {
    if (name.startsWith('@/'))
      return load('src/' + name.slice(2) + (path.extname(name) ? '' : '.ts'))
    if (name.startsWith('.'))
      return load(
        path.relative(root, path.resolve(path.dirname(absolute), name)) +
          (path.extname(name) ? '' : '.ts')
      )
    return require(name)
  }
  Function('exports', 'require', ...Object.keys(Vue), js)(exports, resolve, ...Object.values(Vue))
  cache.set(absolute, exports)
  return exports
}
const { readBusinessEvidence } = load(
  'src/views/mes/pro/processpool/components/businessEvidenceDisplay.ts'
)
const ReadableData = load('src/components/BusinessData/ReadableData.vue').default
const Input = load('src/components/BusinessData/BusinessJsonInput.vue').default
const ApplicationConfirmation = load('src/views/dcc/controlled-file/detail/ApplicationConfirmationContent.vue').default
test('certificate history labels and exact snapshot identities remain readable without losing source JSON', async () => {
  const { certificateHistoryLabel } = load('src/views/dcc/registration-certificate/history/historyDisplay.ts')
  assert.equal(certificateHistoryLabel('ACTIVATION_APPLIED'), '新版本生效')
  assert.match(certificateHistoryLabel('FUTURE'), /未识别.*FUTURE/)
  const source = '{"oldVersionId":1900000000000000001,"certificateId":990819131,"currentVersionId":990819214,"currentSnapshotId":990819218}'
  const { rows, error } = readBusinessEvidence(source)
  assert.equal(error, '')
  assert.deepEqual(rows.map(row => row.label), ['原版本编号', '注册证编号', '当前版本编号', '当前内容快照编号'])
  assert.equal(rows[0].value, '1900000000000000001')
  const app = Vue.createSSRApp(ReadableData, { source })
  app.component('el-alert', elementStub('div'))
  const html = await renderToString(app)
  assert.match(html, /原版本编号/)
  assert.doesNotMatch(html, /<details[^>]*\sopen/)
  assert.ok(html.includes(source.replaceAll('"', '&quot;')) || html.includes(source))
})

test('permission audit distinguishes failed, denied and unknown results and explains scoped data', () => {
  const { permissionAuditResultLabel, permissionAuditSubjectLabel } = load('src/views/system/role/permissionAuditDisplay.ts')
  assert.equal(permissionAuditResultLabel('SUCCESS'), '操作成功')
  assert.equal(permissionAuditResultLabel('FAILED'), '操作失败')
  assert.equal(permissionAuditResultLabel('DENIED'), '权限拒绝')
  assert.match(permissionAuditResultLabel('FUTURE'), /未识别.*FUTURE/)
  assert.equal(permissionAuditSubjectLabel('SYSTEM_ROLE:1900000000000000001'), '角色编号 1900000000000000001')
  const source = JSON.stringify({ state: 'PRESENT', objectVersion: '2', canonicalJson: JSON.stringify({ roleId: '12', menuIds: [5, 8], dataScope: 4, dataScopeDeptIds: [11, 22] }) })
  const { rows, error } = readBusinessEvidence(source)
  assert.equal(error, '')
  assert.ok(rows.some(row => row.value === '存在记录'))
  assert.ok(rows.some(row => row.value === '本部门及下级部门数据'))
  for (const value of ['5', '8', '11', '22']) assert.ok(rows.some(row => row.value === value))
})

test('supplier attachment parsing respects newline contract, URL commas and explicit malformed-address errors', () => {
  const { readPortalAttachments } = load('src/views/srm/supplier-portal/review/attachmentDisplay.ts')
  const address = 'https://example.com/%E8%B5%84%E8%B4%A8.pdf?types=a,b&key=original'
  const rows = readPortalAttachments(`${address}\r\n\nhttps://example.com/\nnot-an-address\njavascript:alert(1)\nhttps://example.com/%ZZ.pdf`)
  assert.equal(rows.length, 5)
  assert.equal(rows[0].name, '资质.pdf')
  assert.equal(rows[0].address, address)
  assert.equal(rows[0].host, 'example.com')
  assert.equal(rows[0].error, '')
  assert.match(rows[1].name, /未提供文件名/)
  assert.match(rows[2].error, /格式无效/)
  assert.match(rows[3].error, /不是网页地址/)
  assert.match(rows[4].error, /文件名编码无效/)
  assert.equal(rows[4].address, 'https://example.com/%ZZ.pdf')
})

test('signed dossier sizes accept actual numeric manifest values and preserve large integer byte counts', () => {
  const { dossierFileSizeLabel } = load('src/views/mes/pro/edhr-release/releaseDisplay.ts')
  assert.equal(dossierFileSizeLabel(0), '0 字节')
  assert.equal(dossierFileSizeLabel(1536), '1.5 KB')
  assert.equal(dossierFileSizeLabel('1048576'), '1.0 MB')
  assert.match(dossierFileSizeLabel('1900000000000000001'), /TB$/)
  for (const value of [-1, 1.5, Number.MAX_SAFE_INTEGER + 1, '', 'invalid']) assert.match(dossierFileSizeLabel(value), /无效/)
})

test('traveler labels never classify unknown or blocked events as success', () => {
  const { travelerEventLabel, travelerResultLabel } = load('src/views/mes/pro/edhr-traveler/travelerDisplay.ts')
  assert.equal(travelerEventLabel('GENERATE'), '生成流转单')
  assert.equal(travelerEventLabel('GENERATE_DUPLICATE'), '重复生成请求')
  assert.equal(travelerResultLabel('BLOCKED'), '已阻止')
  assert.match(travelerResultLabel('FUTURE'), /未识别.*FUTURE/)
})

test('application confirmation separates critical facts and safely preserves all expanded attachments', async () => {
  const sections = [
    { title: '申请对象与版本', items: [{ label: '文件名称', value: '<原文件>.pdf' }, { label: '目标受控版本', value: 'B/1' }] },
    { title: '审批、培训与生效', items: [{ label: '主管签署', value: '由主管本人独立签名' }, { label: '批准人', value: ['批准人甲', '批准人乙'] }] },
    { title: '已保存附件（2）与关联文件（1）', collapsed: true, items: [{ label: '附件', value: ['附件甲.pdf', '<附件乙>.pdf'] }, { label: '关联文件', value: ['关联文件甲（版本 A/1）'] }] }
  ]
  const original = JSON.stringify(sections)
  const html = await renderToString(Vue.createSSRApp(ApplicationConfirmation, { sections }))
  assert.doesNotMatch(html, /<details[^>]*\sopen/)
  assert.match(html, /&lt;原文件&gt;\.pdf/)
  assert.match(html, /&lt;附件乙&gt;\.pdf/)
  for (const text of ['目标受控版本', '主管本人独立签名', '批准人甲', '批准人乙', '附件甲.pdf', '关联文件甲', '新版受控且已生效时才启用并作废旧版']) assert.ok(html.includes(text))
  assert.equal(JSON.stringify(sections), original)
})
const Node = load('src/components/BusinessData/BusinessJsonNode.vue').default
function templateApp(file, context, tags) {
  const descriptor = parse(fs.readFileSync(path.join(root, file), 'utf8')).descriptor
  const compiled = compileTemplate({ source: descriptor.template.content, filename: file, id: file })
  assert.deepEqual(compiled.errors, [])
  const exports = {}
  Function('exports', 'require', ts.transpileModule(compiled.code, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText)(exports, require)
  const app = Vue.createSSRApp({ setup: () => context, render: exports.render })
  for (const tag of tags) app.component(tag, elementStub(tag))
  app.directive('loading', {})
  return app
}

test('projection repair labels preserve every original identifier, status and hash in closed technical details', async () => {
  const display = load('src/views/dcc/controlled-file/detail/projectionRepairDisplay.ts')
  const preview = {
    fileId: '1900000000000000001', masterId: '1900000000000000002', versionRefId: '1900000000000000003', tenantId: '1', versionNo: 'B/1',
    dccStatus: 'ACTIVE', canonicalStatus: 'FINALIZING', domainStatus: 'FINALIZING', processInstanceId: 'process-original', signatureIds: ['signature-1', 'signature-2'],
    controlledTime: '2026-11-02 09:00:00', activatedTime: '2026-11-02 09:01:00', approvedTime: null, publishedTime: null, effectiveDate: null,
    publishedFileId: '1900000000000000004', stampedFileId: '1900000000000000005', latestControlledFileId: '1900000000000000006', currentActiveControlledFileId: '1900000000000000007',
    sourceFactsHash: 'original-source-hash', preimageHash: 'original-preimage-hash', canRepair: false, expectedTargetStatus: 'ACTIVE', expectedActions: ['COMPLETE_CONTROL', 'ACTIVATE_CONTROLLED']
  }
  const original = JSON.stringify(preview)
  assert.equal(display.projectionStatusLabel('ACTIVE'), '受控（已生效）')
  assert.match(display.projectionStatusLabel('FUTURE'), /未识别.*FUTURE/)
  assert.match(display.projectionActionLabel('FUTURE'), /未识别.*FUTURE/)
  const app = templateApp('src/views/dcc/controlled-file/detail/DetailLifecycleProjectionRepairDialog.vue', {
    modelValue: true, preview, loading: false, saving: false, error: '', reason: '', canSubmit: false,
    close() {}, load() {}, submit() {}, ...display
  }, ['el-dialog', 'el-alert', 'el-descriptions', 'el-descriptions-item', 'el-button', 'el-form', 'el-form-item', 'el-input'])
  const html = await renderToString(app)
  const technical = html.match(/<details\b[^>]*>[\s\S]*?<\/details>/)?.[0]
  assert.ok(technical)
  assert.doesNotMatch(technical, /<details[^>]*\sopen/)
  for (const key of ['fileId', 'masterId', 'versionRefId', 'processInstanceId', 'publishedFileId', 'stampedFileId', 'latestControlledFileId', 'currentActiveControlledFileId', 'sourceFactsHash', 'preimageHash']) assert.ok(technical.includes(preview[key]), key)
  for (const id of preview.signatureIds) assert.ok(technical.includes(id))
  const main = html.replace(technical, '')
  assert.match(main, /受控（已生效）/)
  assert.match(main, /受控处理中/)
  assert.match(main, /完成受控/)
  assert.match(main, /启用已受控版本/)
  assert.doesNotMatch(main, /original-source-hash|original-preimage-hash|FINALIZING/)
  assert.equal(JSON.stringify(preview), original)
})

test('showroom diff keeps bilingual business content while release identifiers and blocker codes stay closed', async () => {
  const { formatVersionCenterBlocker } = load('src/views/showroom-admin/version-center/contracts.ts')
  const app = templateApp('src/views/showroom-admin/version-center/VersionDiffPanel.vue', {
    fieldDiffs: [{ fieldCode: 'TITLE', label: '标题', labelEn: 'Title', changed: true, selectedValueZh: '原标题', selectedValueEn: 'Original title', currentContentValueZh: '新标题', currentContentValueEn: 'New title' }],
    currentContentVersion: { revisionNo: 2 }, currentPublicVersion: null, showCurrentPublicSummary: false,
    currentRelease: { releaseId: 'release-original-id', manifestHash: 'original-manifest-hash', companyRevisionId: 17, productCurrentReleaseRevisionId: 18, publishedAt: '2026-11-02 09:00:00' },
    republishReadiness: { ready: false, blockers: [{ scope: 'CURRENT_RELEASE', blockerCode: 'ORIGINAL_BLOCKER', message: '请补充发布图片', affectedRevisionIds: [], contentHash: 'original-blocker-hash' }] },
    disabledReason: '请先处理发布阻塞项，再发布此版本', republishing: false, formatVersionCenterBlocker,
    formatDateTimeValue: value => value, emit() {}
  }, ['el-tag', 'el-descriptions', 'el-descriptions-item', 'el-alert', 'el-button'])
  const html = await renderToString(app)
  assert.doesNotMatch(html, /<details[^>]*\sopen/)
  for (const text of ['release-original-id', 'original-manifest-hash', 'ORIGINAL_BLOCKER', 'original-blocker-hash']) assert.ok(html.includes(text))
  const main = html.replace(/<details\b[^>]*>[\s\S]*?<\/details>/g, '')
  for (const text of ['当前内容差异', '当前发布信息', '原标题', 'Original title', '新标题', 'New title', '请补充发布图片']) assert.ok(main.includes(text))
  assert.doesNotMatch(main, /release-original-id|original-manifest-hash|ORIGINAL_BLOCKER|original-blocker-hash|Not filled|Manifest Hash/)
})
const elementStub = (tag) => ({
  inheritAttrs: false,
  setup:
    (_, { attrs, slots }) =>
    () =>
      Vue.h(tag, attrs, slots.default?.())
})
const renderer = Vue.createRenderer({
  createElement: (type) => ({ type, props: {}, children: [], parent: null }),
  createText: (text) => ({ type: '#text', text, children: [] }),
  createComment: (text) => ({ type: '#comment', text, children: [] }),
  setText: (node, text) => {
    node.text = text
  },
  setElementText: (node, text) => {
    node.text = text
    node.children = []
  },
  parentNode: (node) => node.parent,
  nextSibling: () => null,
  patchProp: (node, key, _, value) => {
    node.props[key] = value
  },
  insert(node, parent, anchor) {
    node.parent = parent
    const i = parent.children.indexOf(anchor)
    if (i < 0) parent.children.push(node)
    else parent.children.splice(i, 0, node)
  },
  remove(node) {
    const children = node.parent?.children
    if (children) children.splice(children.indexOf(node), 1)
  }
})
function mount(component, props) {
  const state = Vue.reactive(props)
  const instance = Vue.ref()
  const container = { children: [] }
  const app = renderer.createApp({
    render: () =>
      Vue.h(component, {
        ...state,
        ref: instance,
        'onUpdate:modelValue': (value) => {
          state.modelValue = value
        }
      })
  })
  for (const name of ['el-input', 'el-switch', 'el-button', 'el-select', 'el-option', 'el-alert'])
    app.component(name, elementStub(name))
  app.mount(container)
  return { state, instance, container, stop: () => app.unmount() }
}
function find(container, predicate) {
  for (const child of container.children || []) {
    if (predicate(child)) return child
    const found = find(child, predicate)
    if (found) return found
  }
}
test('approval contracts show Chinese fields, statuses, nested records and exact IDs', () => {
  const value = readBusinessEvidence(
    String.raw`{"identity":{"activeOrderId":9223372036854775806},"precheckSnapshot":{"items":[{"code":"DHR_COMPLETENESS","result":"PASS"}]},"formalSourceSnapshotJsonRaw":"{\"outputQuantity\":12}","contentHash":"abc"}`
  )
  assert.equal(value.error, '')
  assert.deepEqual(
    value.rows.map((row) => row.value),
    ['9223372036854775806', '批记录完整性', '通过', '12']
  )
  assert.ok(value.rows.every((row) => !row.label.includes('未标注字段')))
})

test('real document access details explain audit fields and preview purpose in Chinese', () => {
  const source = JSON.stringify({
    requestId: 'DCC-REQ-43ADCB6554C34B3D917E352C76A5DA66',
    sourceIp: '127.0.0.1',
    userAgent: 'Chrome/153.0.0.0',
    purpose: 'ONLINE_FILE_PREVIEW',
    accessEventCode: 'AE-20261021-4A8480A9017A'
  })
  const result = readBusinessEvidence(source)
  assert.equal(result.error, '')
  assert.deepEqual(
    result.rows.map((row) => row.label),
    ['请求追踪编号', '访问来源地址', '浏览器信息', '使用用途', '访问事件编号']
  )
  assert.equal(result.rows[3].value, '在线文件预览')
})
test('readable evidence keeps original bytes and hashes in collapsed technical details and escapes markup', async () => {
  const source = '{ "name":"<script>bad</script>", "sha256":"' + 'a'.repeat(64) + '" }'
  const app = Vue.createSSRApp(ReadableData, { source, checksum: 'b'.repeat(64) })
  app.component('el-alert', elementStub('el-alert'))
  const html = await renderToString(app)
  assert.match(html, /&lt;script&gt;bad&lt;\/script&gt;/)
  assert.doesNotMatch(html, /<script>|<details[^>]*open/)
  const business = html.slice(0, html.indexOf('<details'))
  assert.ok(!business.includes('a'.repeat(64)))
  assert.match(html, /a{64}/)
  assert.match(html, /b{64}/)
})
test('formal file import retains exact evidence bytes and rejects broken replacement', async () => {
  const view = mount(Input, { modelValue: '', label: '签名资料', fileOnly: true })
  try {
    const source = '{ "userId":9223372036854775806, "signedAt":"2026-11-02T09:00:00+08:00" }\n'
    const input = find(
      view.container,
      (node) => node.type === 'input' && node.props.type === 'file'
    )
    await input.props.onChange({ target: { files: [{ text: async () => source }], value: 'file' } })
    await Vue.nextTick()
    assert.equal(view.state.modelValue, source)
    assert.equal(view.instance.value.validate(), '')
    await input.props.onChange({ target: { files: [{ text: async () => '{bad' }], value: 'file' } })
    await Vue.nextTick()
    assert.equal(view.state.modelValue, '')
    assert.match(view.instance.value.validate(), /格式错误/)
  } finally {
    view.stop()
  }
})
test('editable file rejects unsafe numbers instead of rounding and submitting them', async () => {
  const view = mount(Input, { modelValue: '', label: '业务资料' })
  try {
    const input = find(
      view.container,
      (node) => node.type === 'input' && node.props.type === 'file'
    )
    await input.props.onChange({
      target: { files: [{ text: async () => '{"userId":9223372036854775806}' }], value: 'file' }
    })
    await Vue.nextTick()
    assert.equal(view.state.modelValue, '')
    assert.match(view.instance.value.validate(), /精度范围/)
  } finally {
    view.stop()
  }
})
test('nested editing preserves types and reports invalid numeric drafts before submit', async () => {
  const view = mount(Node, {
    modelValue: { quantity: 12, details: [{ value: false }, { value: null }] }
  })
  try {
    const number = find(
      view.container,
      (node) => node.type === 'el-input' && node.props['aria-label'] === '数量'
    )
    for (const handler of [number.props['onUpdate:modelValue']].flat()) handler('invalid')
    await Vue.nextTick()
    assert.match(view.instance.value.validate(), /有效数值/)
    assert.equal(view.state.modelValue.quantity, 12)
    for (const handler of [number.props['onUpdate:modelValue']].flat()) handler('13.5')
    await Vue.nextTick()
    assert.equal(view.instance.value.validate(), '')
    assert.deepEqual(JSON.parse(JSON.stringify(view.state.modelValue)), {
      quantity: 13.5,
      details: [{ value: false }, { value: null }]
    })
  } finally {
    view.stop()
  }
})
test('all changed pages compile with registered readable-data imports', () => {
  for (const file of [
    'signature-governance/components/SignatureGovernanceRecordsPane.vue',
    'signature-governance/components/SignatureGovernanceMySignaturePane.vue',
    'dcc/registration-certificate/config/components/ReminderScheduleSetting.vue',
    'mes/pro/edhr/ExecutionRenderer.vue',
    'mes/pro/edhr-report/ReportPage.vue',
    'system/nas/components/NasPermissionRestorePanel.vue',
    'system/config-package/index.vue',
    'dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue',
    'dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue',
    'approval-center/index.vue',
    'mes/pro/edhr-batch/BatchExecutionListPage.vue',
    'mes/pro/edhr/FieldAuditDetailPage.vue',
    'mes/pro/edhr/DomainTraceDetailPage.vue',
    'mes/pro/processpool/TimelinePage.vue',
    'mes/pro/processpool/ReviewCopyPage.vue',
    'mes/pro/processpool/EventRevisionPage.vue',
    'mes/pro/batchrecordformlist/BatchRecordVersionReviewDialog.vue',
    'mes/pro/edhr-init-batch/InitBatchPage.vue',
    'mes/pro/edhr-dhr-template/DhrTemplatePage.vue',
    'dcc/controlled-file/logs/index.vue',
    'dcc/controlled-file/signatures/index.vue'
  ]) {
    const { descriptor, errors } = parse(
      fs.readFileSync(path.join(root, 'src/views', file), 'utf8')
    )
    assert.deepEqual(errors, [], file)
    assert.deepEqual(
      compileTemplate({ source: descriptor.template.content, filename: file, id: file }).errors,
      [],
      file
    )
    if (descriptor.template.content.includes('<ReadableData'))
      assert.match(descriptor.scriptSetup.content, /import ReadableData/)
  }
})

test('signature comments separate trace markers while ordinary signing reasons remain exact', () => {
  const { recordRoles, signatureCommentSummary } = load(
    'src/views/signature-governance/components/signatureRecordDisplay.ts'
  )
  const raw = 'Stage1模拟[stage=STAGE1][run=STAGE1-1793174996154]'
  assert.equal(signatureCommentSummary(raw), '模拟测试签署记录')
  assert.equal(raw, 'Stage1模拟[stage=STAGE1][run=STAGE1-1793174996154]')
  assert.equal(signatureCommentSummary('同意放行，备注：A/B'), '同意放行，备注：A/B')
  assert.equal(signatureCommentSummary('复核完成[run=TEST-123]'), '复核完成')
  assert.deepEqual(recordRoles('生产组长、PQC组长，管理代表;审核员'), [
    '生产组长',
    'PQC组长',
    '管理代表',
    '审核员'
  ])
})

test('report metadata and unknown states are explicit without misreporting success', () => {
  const { reportStatusLabel, reportCellText } = load(
    'src/views/mes/pro/edhr-report/reportDisplay.ts'
  )
  const { businessFieldLabel } = load(
    'src/views/mes/pro/processpool/components/businessEvidenceDisplay.ts'
  )
  assert.equal(reportStatusLabel('READY'), '可用')
  assert.equal(reportStatusLabel('FUTURE_FAILURE'), '未识别状态（FUTURE_FAILURE）')
  assert.equal(reportCellText('sourceObject', 'mes_pro_batch_record_execution'), '批记录执行记录')
  assert.equal(businessFieldLabel('recordCount'), '记录数量')
  assert.equal(businessFieldLabel('futureKey'), '未标注字段（futureKey）')
  const rows = readBusinessEvidence(
    '{"fields":[{"field":"batchCode","label":"批号"}],"scope":"tenant_role","合格数量":3}'
  ).rows
  assert.deepEqual(
    rows.map((row) => row.value),
    ['生产批号', '批号', '租户与角色权限', '3']
  )
  assert.equal(rows.at(-1).label, '合格数量')
})

test('execution snapshot renders nested records and more than twelve fields without exposing raw JSON by default', async () => {
  const Renderer = load('src/views/mes/pro/edhr/ExecutionRenderer.vue').default
  const snapshot = Object.fromEntries(
    Array.from({ length: 14 }, (_, i) => ['测量项目' + (i + 1), i])
  )
  snapshot.details = [{ value: true, userId: '9223372036854775806' }]
  const source = JSON.stringify(snapshot)
  const app = Vue.createSSRApp({
    render: () => Vue.h(Renderer, { execution: { executionSnapshotJson: source } })
  })
  app.component('el-alert', elementStub('el-alert'))
  const html = await renderToString(app)
  assert.match(html, /测量项目14/)
  assert.match(html, /9223372036854775806/)
  assert.match(html, />是</)
  assert.match(html, /<details[^>]*><summary>/)
  assert.doesNotMatch(html, /<details[^>]* open/)
  assert.match(readBusinessEvidence('{broken').error, /解析|格式/)
})

test('trace blockers explain required business data and retain unknown reasons', () => {
  const { traceItemLabel, traceBlockerMessage } = load('src/views/mes/pro/edhr/traceDisplay.ts')
  assert.equal(traceItemLabel('WORK_ORDER'), '生产订单')
  assert.equal(
    traceBlockerMessage({
      blockerCode: 'EDHR_DOMAIN_TRACE_EXECUTION_SNAPSHOT_REQUIRED',
      blockerMessage: 'Execution snapshot JSON is required'
    }),
    '缺少执行快照'
  )
  assert.equal(
    traceBlockerMessage({ blockerCode: 'CUSTOM', blockerMessage: '需补充来源版本' }),
    '需补充来源版本'
  )
  assert.match(traceItemLabel('CUSTOM'), /未识别/)
})

test('review sources group stages without dropping repeated or distinct role evidence', () => {
  const { groupReviewSources } = load(
    'src/views/dcc/controlled-file/categories/components/reviewMatrixDisplay.ts'
  )
  const sources = [
    { source: 'CURRENT_REVIEW_MATRIX', stageNo: 2, stageName: '审核会签', positionId: 1, positionName: '质量审核' },
    { source: 'CURRENT_REVIEW_MATRIX', stageNo: 2, stageName: '审核会签', positionId: 2, positionName: '注册审核' },
    { source: 'CURRENT_REVIEW_MATRIX', stageNo: 3, stageName: '批准', positionId: 3 },
    { source: 'CURRENT_REVIEW_MATRIX', stageNo: 2, stageName: '审核会签', positionId: 1, positionName: '质量审核' }
  ]
  const before = JSON.stringify(sources)
  const groups = groupReviewSources(sources)
  assert.deepEqual(groups.map((group) => [group.label, group.sources.length]), [
    ['第 2 阶段：审核会签', 3], ['第 3 阶段：批准', 1]
  ])
  assert.deepEqual(groups[0].sources.map((source) => source.positionId), [1, 2, 1])
  assert.equal(groups.flatMap((group) => group.sources).length, sources.length)
  assert.equal(JSON.stringify(sources), before)
})

test('review rule copy distinguishes missing participation from missing rules and never assumes mandatory participation', () => {
  const { activeStageRules, ruleParticipationLabel, formatStageRuleSummary } = load(
    'src/views/dcc/controlled-file/categories/components/reviewMatrixDisplay.ts'
  )
  const rules = [
    { stageType: 'SIGNOFF', subjectLabel: '质量审核', marker: '●' },
    { stageType: 'SIGNOFF', subjectLabel: '注册审核' },
    { stageType: 'SIGNOFF', subjectLabel: '已停用审核', active: false },
    { stageType: 'APPROVAL', subjectLabel: '部门负责人', marker: '◎' }
  ]
  assert.equal(activeStageRules(rules, 'SIGNOFF').length, 2)
  assert.equal(formatStageRuleSummary(rules, 'SIGNOFF'), '质量审核：必须参与；注册审核：参与条件未设置')
  assert.equal(formatStageRuleSummary([], 'SIGNOFF'), '未配置规则')
  assert.equal(ruleParticipationLabel('◎'), '可选参与')
  assert.equal(ruleParticipationLabel('▲'), '沿用原标记（▲）')
  assert.match(ruleParticipationLabel('FUTURE'), /无法识别.*FUTURE/)
})

test('correction history renders human verification while exact evidence references stay closed', async () => {
  const file = 'src/views/mes/pro/processpool/components/ActiveOrderCorrectionHistoryPanel.vue'
  const descriptor = parse(fs.readFileSync(path.join(root, file), 'utf8')).descriptor
  const compiled = compileTemplate({ source: descriptor.template.content, filename: file, id: file })
  assert.deepEqual(compiled.errors, [])
  const exports = {}
  Function('exports', 'require', ts.transpileModule(compiled.code, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText)(exports, require)
  const row = {
    revisionId: '1900000000000000001', eventId: '1900000000000000002',
    signatureId: '1900000000000000003', signerUserId: '1900000000000000004',
    supersededReviewId: '1900000000000000005', eventType: 'PRODUCTION_SUBMIT',
    processName: '清洗', signerName: '核对人员', revisedAt: '2026-11-02 09:00:00',
    signedAt: '2026-11-02 08:59:00', reason: '<核对计数>', verificationStatus: 'VALID', changes: []
  }
  const app = Vue.createSSRApp({
    setup: () => ({
      context: {}, history: { state: { visible: true, timeline: { corrections: [row] } }, open() {}, close() {} },
      formatDateTimeValue: (value) => value
    }),
    render: exports.render
  })
  for (const tag of ['el-button', 'el-dialog', 'el-alert', 'el-skeleton', 'el-empty', 'el-tag'])
    app.component(tag, elementStub(tag))
  app.component('el-table', { setup: () => () => Vue.h('table') })
  app.component('el-table-column', { setup: () => () => null })
  app.component('BusinessEvidenceDetails', elementStub('div'))
  const html = await renderToString(app)
  const references = html.match(/<details\b[^>]*>[\s\S]*?<\/details>/)?.[0]
  assert.ok(references)
  assert.doesNotMatch(references, /<details[^>]*\sopen/)
  for (const key of ['revisionId', 'eventId', 'signatureId', 'signerUserId', 'supersededReviewId'])
    assert.ok(references.includes(row[key]), key)
  const main = html.replace(references, '')
  assert.match(main, /验证通过/)
  assert.match(main, /核对人员/)
  assert.match(main, /&lt;核对计数&gt;/)
  assert.doesNotMatch(main, /PRODUCTION_SUBMIT|>VALID</)
})
