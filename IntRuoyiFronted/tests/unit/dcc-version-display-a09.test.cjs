const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const frontendRoot = path.resolve(__dirname, '../..')
const sourceRoot = path.join(frontendRoot, 'src/views/dcc/controlled-file')
const ID = '9223372036854775701', SECOND = '9223372036854775702'
const compile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true } }).outputText
function moduleFrom(filename, dependencies = {}) {
  const exports = {}
  new Function('exports', 'require', compile(fs.readFileSync(filename, 'utf8')))(exports, name => {
    if (name in dependencies) return dependencies[name]
    if (name === 'dayjs') return require(name)
    const resolved = name.startsWith('@/') ? path.join(frontendRoot, 'src', name.slice(2) + '.ts') : path.resolve(path.dirname(filename), name + '.ts')
    return moduleFrom(resolved)
  })
  return exports
}
const helper = moduleFrom(path.join(sourceRoot, 'shared/version-display.ts'))
const detailPresentation = moduleFrom(path.join(sourceRoot, 'detail/presentation.ts'))
const blankChild = { setup: () => () => vue.h('aside') }
function component(file, dependencies = {}, globals = {}) {
  const filename = path.join(sourceRoot, file)
  const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename })
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: file, inlineTemplate: true })
  const exports = {}
  new Function('exports', 'require', ...Object.keys(globals), compile(script.content))(exports, name => {
    if (name in dependencies) return dependencies[name]
    if (name === 'vue') return vue
    if (name === '../shared/version-display') return helper
    if (name === '../detail/presentation') return detailPresentation
    if (name.endsWith('.vue')) return { __esModule: true, default: blankChild }
    return moduleFrom(path.resolve(path.dirname(filename), name + '.ts'))
  }, ...Object.values(globals))
  return exports.default
}
const node = (type, text = '') => ({ type, text, props: {}, children: [], parent: null })
const renderer = vue.createRenderer({
  createElement: type => node(type), createText: text => node('#text', text), createComment: () => node('#comment'),
  patchProp: (n, key, _old, value) => { n.props[key] = value },
  setText: (n, text) => { n.text = text }, setElementText: (n, text) => { n.text = text; n.children = [] },
  insert: (n, parent, anchor) => { if (n.parent) n.parent.children.splice(n.parent.children.indexOf(n), 1); n.parent = parent; const i = parent.children.indexOf(anchor); i < 0 ? parent.children.push(n) : parent.children.splice(i, 0, n) },
  remove: n => { if (n.parent) n.parent.children.splice(n.parent.children.indexOf(n), 1); n.parent = null },
  parentNode: n => n.parent, nextSibling: n => n.parent?.children[n.parent.children.indexOf(n) + 1] || null
})
const text = n => n.text + n.children.map(text).join('')
const nodes = (n, type) => [...(n.type === type ? [n] : []), ...n.children.flatMap(child => nodes(child, type))]
function mount(c, props) {
  const root = node('root'), errors = []
  const app = renderer.createApp(c, props)
  app.config.warnHandler = () => {}
  app.config.errorHandler = error => errors.push(error)
  app.directive('loading', {})
  for (const type of ['ContentWrap', 'Dialog', 'el-descriptions', 'el-descriptions-item', 'el-alert', 'el-button', 'el-tag', 'el-select', 'el-option', 'el-form', 'el-form-item', 'el-radio-group', 'el-radio', 'el-input']) {
    app.component(type, { inheritAttrs: false, setup: (_, ctx) => () => vue.h(type, ctx.attrs, ctx.slots.default?.()) })
  }
  app.component('el-table', { props: ['data'], setup: (p, { slots }) => { vue.provide('rows', vue.toRef(p, 'data')); return () => vue.h('el-table', {}, slots.default?.()) } })
  app.component('el-table-column', { inheritAttrs: false, setup: (_, ctx) => { const rows = vue.inject('rows'); return () => vue.h('el-table-column', ctx.attrs, rows.value.map(row => ctx.slots.default ? ctx.slots.default({ row }) : String(row[ctx.attrs.prop] ?? ''))) } })
  app.mount(root)
  return { root, errors, unmount: () => app.unmount() }
}
const flush = async () => { for (let i = 0; i < 12; i++) { await Promise.resolve(); await vue.nextTick() } }
const tableShell = { setup: (_, { slots }) => () => vue.h('main', {}, slots.table?.({ sortColumnAttrs: () => ({}), handleSortChange() {} })) }
function logHost(rows, error) {
  const calls = [], messages = []
  const api = moduleFrom(path.join(frontendRoot, 'src/api/dcc/controlledFile/logs.ts'), { '@/config/axios': { __esModule: true, default: { get: async request => { calls.push(JSON.parse(JSON.stringify(request))); if (error) throw error; return { list: rows, total: rows.length } } } } })
  const dependencies = {
    '@/api/dcc/controlledFile/logs': api,
    '@/components/UnifiedListTemplate/index.vue': { __esModule: true, default: tableShell },
    '@/hooks/web/useUserTableColumns': { useUserTableColumns: () => ({ columns: vue.ref([]), saving: vue.ref(false), isColumnVisible: () => true, getColumnWidthString: () => '100px', handleHeaderDragend() {}, saveConfig() {} }) },
    '@/hooks/web/useTableQuickFilter': { useTableQuickFilter: () => ({ state: {}, selectedDefinition: vue.ref(undefined), operatorOptions: vue.ref([]), updateState() {}, applyQuickFilter() {} }) },
    '@/utils/formatTime': { dateFormatter: (_row, _col, value) => String(value) }
  }
  const globals = { ref: vue.ref, reactive: vue.reactive, computed: vue.computed, onMounted: vue.onMounted, useRoute: () => ({ query: { controlledFileId: ID } }), useMessage: () => ({ error: message => messages.push(message) }) }
  return { mounted: mount(component('logs/index.vue', dependencies, globals)), calls, messages }
}
function entry(versionNo = 'A/1-2') {
  return Object.freeze({ id: ID, logType: 'FILE_CHECKOUT', actionLabel: '检入', resultLabel: '已生成工作稿', versionNo, fileNumber: 'F1', fileName: '编制记录', operatorUserId: '7', summary: '审计摘要 A/1-2 保留', oldValueText: '旧值 A/1-1', newValueText: '新值 A/1-2', reason: '真实原因 A/1-2', detailJson: '{"versionNo":"A/1-2","id":"9223372036854775701"}' })
}

test('mounted full log page uses 草稿 only in its structured version cell and preserves audit originals and exact read ID', async () => {
  const row = entry(), { mounted, calls } = logHost([row])
  try {
    await flush(); assert.deepEqual(mounted.errors, []); assert.equal(calls.length, 1)
    assert.equal(calls[0].url, '/dcc/controlled-file-logs/page'); assert.equal(calls[0].params.controlledFileId, ID)
    nodes(mounted.root, 'el-button').find(n => text(n) === '详情').props.onClick(); await flush()
    const cells = nodes(mounted.root, 'el-descriptions-item')
    assert.equal(text(cells.find(n => n.props.label === '版本')), '草稿')
    for (const [label, field] of [['摘要', 'summary'], ['旧值', 'oldValueText'], ['新值', 'newValueText'], ['原因', 'reason'], ['详情 JSON', 'detailJson']]) {
      assert.equal(text(cells.find(n => n.props.label === label)), row[field])
    }
    assert.equal(row.versionNo, 'A/1-2'); assert.equal(row.id, ID)
  } finally { mounted.unmount() }
})

test('mounted log structured version keeps formal A/1 and explicitly reports unrecorded version', async () => {
  for (const [versionNo, expected] of [['A/1', 'A/1'], [null, '未记录']]) {
    const { mounted } = logHost([entry(versionNo)])
    try {
      await flush(); nodes(mounted.root, 'el-button').find(n => text(n) === '详情').props.onClick(); await flush()
      assert.deepEqual(mounted.errors, [])
      assert.equal(text(nodes(mounted.root, 'el-descriptions-item').find(n => n.props.label === '版本')), expected)
    } finally { mounted.unmount() }
  }
})

test('full log request failure remains visible without fabricated rows or a detail action', async () => {
  const { mounted, messages } = logHost([], new Error('本次日志读取被拒绝'))
  try {
    await flush(); assert.deepEqual(mounted.errors, []); assert.deepEqual(messages, ['本次日志读取被拒绝'])
    assert.equal(nodes(mounted.root, 'el-alert')[0].props.title, '本次日志读取被拒绝')
    assert.equal(nodes(mounted.root, 'el-button').length, 0)
  } finally { mounted.unmount() }
})

function revisionHost(iterations, effectiveMode = 'AFTER_STANDARDIZATION_SIGNATURE') {
  const commands = []
  const applicationFacts = { projectAttributes: { targetMarkets: ['NA'], licenseHolder: 'NA', actualManufacturer: 'NA', documentTransfer: 'N' }, selectedSignoffDepartmentIds: ['100'], selectedDistributionDepartmentIds: [], needTraining: false, effectiveMode, effectiveDate: effectiveMode === 'PRESET_DATE' ? '2026-12-01' : null }
  const mounted = mount(component('revision/DccRevisionPanel.vue'), { baselineId: '300', baselineVersionNo: 'A/1', partialTarget: { versionNo: 'A/2', unavailableReason: null }, replacementTarget: { versionNo: 'B/1', unavailableReason: null }, idempotencyKey: 'g08-a09-labels', applicationFacts, applicationReady: true, facts: {}, iterations, canPartial: true, canReplacement: true, submitRevision: async command => commands.push(command) })
  return { mounted, commands }
}
const iteration = (id, versionNo, extra = {}) => Object.freeze({ id, versionNo, canPartial: true, canReplacement: true, canPreview: false, ...extra })

test('real revision read wrapper and mounted labels preserve each supplied time and description while submitting exact second ID', async () => {
  const iterations = [iteration(ID, 'A/1-1', { createTime: '2026-10-15 09:01:02', changeDescription: '规范术语' }), iteration(SECOND, 'A/1-2', { createTime: '2026-10-15 10:02:03', changeDescription: '明确职责' })]
  const requests = []
  const api = moduleFrom(path.join(frontendRoot, 'src/api/dcc/controlledFile/applicationRead.ts'), {
    '@/config/axios': { __esModule: true, default: { get: async request => { requests.push(request); return { controlledBaselineId: '300', iterations } } } }
  })
  const result = await api.getControlledFileRevisionOptions('300')
  assert.equal(requests[0].url, '/dcc/controlled-files/300/revision-options')
  const { mounted, commands } = revisionHost(result.iterations)
  try {
    await flush(); assert.deepEqual(mounted.errors, [])
    const options = nodes(mounted.root, 'el-option')
    assert.match(options[0].props.label, /草稿.*2026-10-15 09:01:02.*规范术语/)
    assert.match(options[1].props.label, /草稿.*2026-10-15 10:02:03.*明确职责/)
    assert.doesNotMatch(options.map(n => n.props.label).join(' '), /A\/1-[12]/)
    assert.equal(nodes(mounted.root, 'el-select')[0].props.modelValue, '')
    nodes(mounted.root, 'el-select')[0].props['onUpdate:modelValue'](SECOND)
    nodes(mounted.root, 'el-radio-group')[0].props['onUpdate:modelValue']('PARTIAL')
    nodes(mounted.root, 'el-input')[0].props['onUpdate:modelValue']('本次新申请说明')
    await flush(); await nodes(mounted.root, 'el-button').find(n => text(n) === '提交正式变更').props.onClick(); await flush()
    assert.equal(commands.length, 1); assert.equal(commands[0].selectedIterationId, SECOND)
    assert.equal(iterations[1].versionNo, 'A/1-2'); assert.equal(iterations[1].changeDescription, '明确职责')
    assert.equal(Object.hasOwn(commands[0], 'createTime'), false)
  } finally { mounted.unmount() }
})

test('revision label missing formal metadata stays explicitly unrecorded and never invents time or a first draft selection', async () => {
  const { mounted, commands } = revisionHost([iteration(ID, 'A/1-1', { createTime: null, changeDescription: null }), iteration(SECOND, 'A/1-2')])
  try {
    await flush(); assert.deepEqual(mounted.errors, [])
    for (const option of nodes(mounted.root, 'el-option')) {
      assert.match(option.props.label, /草稿.*编制时间未记录.*说明未记录/)
      assert.doesNotMatch(option.props.label, /\d{4}-\d{2}-\d{2}/)
    }
    assert.equal(nodes(mounted.root, 'el-select')[0].props.modelValue, '')
    await nodes(mounted.root, 'el-button').find(n => text(n) === '提交正式变更').props.onClick(); await flush()
    assert.equal(commands.length, 0)
  } finally { mounted.unmount() }
})

test('formal revision option keeps its existing version label without adding draft metadata messages', async () => {
  const { mounted } = revisionHost([iteration(ID, 'A/1')])
  try {
    await flush(); assert.deepEqual(mounted.errors, [])
    assert.equal(nodes(mounted.root, 'el-option')[0].props.label, `A/1 · 文件 #${ID}`)
  } finally { mounted.unmount() }
})

test('revision label consumes the real Jackson LocalDateTime numeric timestamp without coercing or inventing a date', async () => {
  const createTime = 1792040523000
  const iterations = [iteration(ID, 'A/1-2', { createTime, changeDescription: '服务器实际记录' })]
  const api = moduleFrom(path.join(frontendRoot, 'src/api/dcc/controlledFile/applicationRead.ts'), {
    '@/config/axios': { __esModule: true, default: { get: async () => ({ controlledBaselineId: '300', iterations }) } }
  })
  const result = await api.getControlledFileRevisionOptions('300')
  const { mounted } = revisionHost(result.iterations)
  try {
    await flush(); assert.deepEqual(mounted.errors, [])
    const label = nodes(mounted.root, 'el-option')[0].props.label
    assert.ok(label.includes('编制时间：' + detailPresentation.formatControlledFileDateTime(createTime)))
    assert.match(label, /服务器实际记录/); assert.doesNotMatch(label, /未记录|A\/1-2/)
  } finally { mounted.unmount() }
})

test('R08 actual mode helper describes immediate delayed and legacy preset facts without yes-no ambiguity', () => {
  const { revisionEffectiveModeLabel } = moduleFrom(path.join(sourceRoot, 'revision/revision-model.ts'))
  for (const [mode, expected] of [['AFTER_STANDARDIZATION_SIGNATURE', '文控审核后立即生效'], ['DELAYED', '延迟生效'], ['PRESET_DATE', '按预设日期生效（历史申请）'], [null, '未记录'], [undefined, '未记录'], ['UNKNOWN', '未记录']]) {
    assert.equal(revisionEffectiveModeLabel(mode), expected)
  }
})

test('R08 real revision SFC labels recorded modes as 生效方式 and DELAYED needs no preset date', async () => {
  for (const [mode, expected] of [['AFTER_STANDARDIZATION_SIGNATURE', '文控审核后立即生效'], ['DELAYED', '延迟生效'], ['PRESET_DATE', '按预设日期生效（历史申请）'], [null, '未记录'], ['UNKNOWN', '未记录']]) {
    const { mounted, commands } = revisionHost([iteration(ID, 'A/1-2', { createTime: null, changeDescription: null })], mode)
    try {
      await flush(); assert.deepEqual(mounted.errors, [])
      const rows = nodes(mounted.root, 'el-form-item')
      const row = rows.find(n => n.props.label === '生效方式')
      assert.ok(row, 'actual summary must label the mode as 生效方式')
      assert.equal(text(row), expected)
      assert.equal(rows.some(n => n.props.label === '预设生效日期'), mode === 'PRESET_DATE')
      assert.equal(commands.length, 0)
    } finally { mounted.unmount() }
  }
})
