const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const read = (file) =>
  fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file', file), 'utf8')
const load = (file) => {
  const exports = {}
  new Function(
    'exports',
    'require',
    ts.transpileModule(read(file), {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
    }).outputText
  )(exports, (id) => {
    if (id.endsWith('relation-contract')) return load('detail/relation-contract.ts')
    if (id.endsWith('project-reference-contract'))
      return load('relations/project-reference-contract.ts')
    throw Error(`Unexpected import ${id}`)
  })
  return exports
}
const file = {
  id: '9007199254740993',
  masterId: '100',
  dccProjectCodeId: '20',
  projectFolderId: '21',
  versionNo: 'A/1'
}
const selected = {
  controlledFileId: file.id,
  tenantId: '1',
  masterId: '100',
  projectId: '20',
  projectFolderId: '21',
  projectName: '正式项目',
  projectFolderName: '正式文件夹',
  versionNo: 'A/1',
  fileName: '文件.pdf',
  fileNumber: 'SOP-1',
  status: 'ACTIVE',
  controlled: true,
  hasCurrentControlledSource: true,
  latestControlledFileId: '9007199254740995'
}
const latest = {
  ...selected,
  controlledFileId: '9007199254740995',
  versionNo: 'B/1',
  status: 'CONTROLLED_PENDING_EFFECTIVE',
  pendingEffect: true
}
function readers(changes = {}) {
  const calls = []
  return {
    calls,
    getPermissions: async (id) => {
      calls.push(id)
      return id === file.id ? selected : latest
    },
    getCurrentRelations: async () => ({
      sourceControlledFileId: latest.controlledFileId,
      files: []
    }),
    getUsage: async id => ({ tenantId: '1', sourceControlledFileId: id, masterId: '100', referenceProjectCount: 2, referenced: true }),
    ...changes
  }
}
test('browse metadata keeps selected body, latest controlled and formal project folder identities separate', async () => {
  const api = readers(),
    result = await load('detail/browse-records.ts').loadBrowseMetadata(file, '1', api)
  assert.equal(result.selected.versionNo, 'A/1')
  assert.equal(result.latest.versionNo, 'B/1')
  assert.equal(result.latest.pendingEffect, true)
  assert.equal(result.referenceProjectCount, 2)
  assert.deepEqual(api.calls, [file.id, latest.controlledFileId])
})
test('no current controlled source stays unavailable instead of borrowing the selected historical version', async () => {
  const api = readers({
    getPermissions: async () => ({
      ...selected,
      hasCurrentControlledSource: false,
      latestControlledFileId: null
    }),
    getCurrentRelations: async () => {
      throw Error('must not call')
    }
  })
  assert.equal(
    (await load('detail/browse-records.ts').loadBrowseMetadata(file, '1', api)).latest,
    undefined
  )
})
test('wrong tenant, Master, selected version and uncontrolled latest fail explicitly', async () => {
  const model = load('detail/browse-records.ts')
  for (const changed of [
    { ...selected, tenantId: '2' },
    { ...selected, versionNo: 'A/2' },
    { ...selected, projectFolderId: '22' }
  ])
    await assert.rejects(
      model.loadBrowseMetadata(file, '1', readers({ getPermissions: async () => changed })),
      /不一致/
    )
  for (const changed of [
    { ...latest, masterId: '101' },
    { ...latest, controlled: false }
  ])
    await assert.rejects(
      model.loadBrowseMetadata(
        file,
        '1',
        readers({ getPermissions: async (id) => (id === file.id ? selected : changed) })
      ),
      /不一致|受控/
    )
  await assert.rejects(
    model.loadBrowseMetadata(
      file,
      '1',
      readers({
        getUsage: async () => {
          throw Error('引用读取失败')
        }
      })
    ),
    /引用读取失败/
  )
})
test('unified folder rows preserve separate owned and fixed reference keys and searchable source', () => {
  const merge = load('browser/folder-file-list.ts').buildFolderFileRows
  const owned = { ...selected, projectName: '源项目', folderName: '源文件夹' }
  const reference = {
    reference: { id: '55', selectedControlledFileId: file.id, masterId: '100' },
    selectedVersion: owned,
    sourceProjectName: '源项目',
    referenceProjectCount: 2
  }
  const result = merge(
    [owned],
    [reference],
    { 100: { referenced: true, referenceProjectCount: 2 } },
    ''
  )
  assert.equal(result.length, 2)
  assert.notEqual(result[0].key, result[1].key)
  assert.equal(result[1].reference.reference.selectedControlledFileId, file.id)
  assert.equal(merge([], [reference], {}, '源项目').length, 1)
  assert.equal(merge([], [reference], {}, '不存在').length, 0)
})
test('complete readonly records live in detail while body keeps only the content entry', () => {
  const { descriptor } = parse(read('detail/index.vue'))
  const branch = descriptor.template.ast.children.find(
    (node) => node.type === 1 && node.tag === 'ContentWrap'
  )
  assert.equal(branch.loc.source.includes('ControlledFileBrowseRecords'), false)
  const detailBranch = descriptor.template.ast.children.find(node => node.type === 1
    && node.props.some(p => p.type === 7 && p.name === 'else-if' && p.exp?.content === 'isBrowserTraceabilityPage'))
  assert.ok(detailBranch.loc.source.includes('ControlledFileBrowseRecords'), 'records must live in readonly detail')
  const records = parse(read('detail/ControlledFileBrowseRecords.vue')).descriptor
  for (const label of [
    '文件编号',
    '查看版本',
    '最新受控版本',
    '文件负责人',
    '受控日期',
    '生效方式',
    '下发日期',
    '检出时间'
  ])
    assert.ok(records.template.content.includes(label), label)
  assert.ok(records.template.content.includes('CONTROLLED_PENDING_EFFECTIVE'))
  assert.ok(records.template.content.includes('DetailApplicationHistory'))
  assert.ok(records.template.content.includes(':allow-edit="false"'))
  assert.deepEqual(
    compileTemplate({ source: records.template.content, filename: 'records.vue', id: 'records' })
      .errors,
    []
  )
})
test('checkout list renders the selected version checkout time and folder list exposes controlled cancellation', () => {
  const { descriptor } = parse(read('browser/index.vue'))
  function find(node, id) {
    if (
      node.type === 1 &&
      node.props.some((p) => p.type === 6 && p.name === 'data-testid' && p.value?.content === id)
    )
      return node
    for (const child of node.children || []) {
      const result = find(child, id)
      if (result) return result
    }
  }
  const tag = find(descriptor.template.ast, 'dcc-controlled-browser-checked-out-by')
  assert.ok(
    tag.loc.source.includes('checkedOutTime'),
    'checkout time belongs to the selected version'
  )
  const ref = parse(read('relations/DccProjectReferences.vue')).descriptor
  assert.ok(
    find(ref.template.ast, 'dcc-folder-cancel-reference'),
    'selected reference cancellation belongs to the toolbar'
  )
  const browser = read('browser/ProjectBrowserPanel.vue')
  assert.ok(browser.includes('ProjectFolderFileTable'))
  assert.ok(browser.includes('@select-reference="selectReference"'))
})

function compileComponent(file, resolve) {
  const { descriptor } = parse(read(file))
  const compiled = compileScript(descriptor, { id: file, inlineTemplate: true })
  const exports = {}
  new Function(
    'exports',
    'require',
    ts.transpileModule(compiled.content, {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
    }).outputText
  )(exports, resolve)
  return exports.default
}
const node = (type, text = '') => ({ type, text, children: [], props: {}, parent: null })
const renderer = vue.createRenderer({
  createElement: (type) => node(type),
  createText: (text) => node('#text', text),
  createComment: () => node('#comment'),
  patchProp: (n, k, old, value) => (n.props[k] = value),
  setText: (n, text) => (n.text = text),
  setElementText: (n, text) => {
    n.text = text
    n.children = []
  },
  insert: (n, p, anchor) => {
    if (n.parent) n.parent.children.splice(n.parent.children.indexOf(n), 1)
    n.parent = p
    const at = anchor ? p.children.indexOf(anchor) : -1
    p.children.splice(at < 0 ? p.children.length : at, 0, n)
  },
  remove: (n) => {
    if (n.parent) n.parent.children.splice(n.parent.children.indexOf(n), 1)
  },
  parentNode: (n) => n.parent,
  nextSibling: (n) => n.parent?.children[n.parent.children.indexOf(n) + 1]
})
const tick = async () => {
  for (let i = 0; i < 16; i++) {
    await Promise.resolve()
    await vue.nextTick()
  }
}
const nodes = (n, predicate) => [
  ...(predicate(n) ? [n] : []),
  ...n.children.flatMap((child) => nodes(child, predicate))
]
const text = (n) => n.text + n.children.map(text).join('')
const stub = { setup: () => () => vue.h('i') }
function mountReferences(leader = true, cancel = async () => 0) {
  const reference = {
    reference: {
      id: '55',
      projectId: '20',
      folderId: '21',
      masterId: '100',
      selectedControlledFileId: file.id
    },
    selectedVersion: selected,
    sourceProjectName: '源项目',
    referenceProjectCount: 1
  }
  const props = vue.reactive({
    contextKey: 'folder-21',
    selectionContextKey: 'search-1',
    projectId: '20',
    folderId: '21',
    folderName: '当前文件夹',
    isProjectLeader: leader,
    selectorSource: {},
    directories: [],
    loadReferences: async () => [reference],
    loadPage: async () => ({ list: [], total: 0 }),
    openPreview: async () => {},
    createReferences: async () => [],
    cancelReference: cancel
  })
  const component = compileComponent('relations/DccProjectReferences.vue', (id) =>
    id === 'vue' ? vue : { default: stub }
  )
  let scope
  const events = [],
    app = renderer.createApp({
      render: () =>
        vue.h(
          component,
          { ...props, onChanged: (...args) => events.push(args) },
          {
            default: (state) => {
              scope = state
              return vue.h('p', '合并文件列表')
            }
          }
        )
    })
  for (const tag of ['el-button', 'el-alert', 'el-input', 'el-table', 'el-table-column'])
    app.component(tag, {
      setup:
        (_p, { slots, attrs }) =>
        () =>
          vue.h(tag, attrs, slots.default?.())
    })
  app.directive('loading', {})
  app.component('el-dialog', {
    props: ['modelValue'],
    setup:
      (p, { slots }) =>
      () =>
        p.modelValue ? vue.h('dialog', {}, [slots.default?.(), slots.footer?.()]) : vue.h('i')
  })
  const root = node('root')
  app.mount(root)
  return { props, events, app, root, scope: () => scope, reference }
}
test('actual merged-list selection opens cancellation confirmation and sends only the selected fixed reference', async () => {
  const calls = [],
    h = mountReferences(true, async (...args) => {
      calls.push(args)
      return 0
    })
  try {
    await tick()
    h.scope().selectReference(h.scope().rows[0])
    await tick()
    const button = nodes(h.root, (n) => n.props['data-testid'] === 'dcc-folder-cancel-reference')[0]
    assert.equal(button.props.disabled, false)
    button.props.onClick()
    await tick()
    assert.equal(calls.length, 0, 'first confirmation cannot perform cancellation')
    assert.ok(text(h.root).includes('当前文件夹'))
    assert.ok(text(h.root).includes('源项目'))
    const input = nodes(h.root, (n) => n.type === 'el-input')[0]
    input.props['onUpdate:modelValue']('当前项目不再使用')
    await tick()
    const confirm = nodes(h.root, (n) => n.type === 'el-button' && text(n) === '确认取消引用')[0]
    await confirm.props.onClick()
    await tick()
    assert.deepEqual(calls, [['20', '21', '100', '55', '当前项目不再使用']])
    assert.equal(h.scope().rows.length, 0)
    assert.equal(h.events.length, 1)
  } finally {
    h.app.unmount()
  }
})
test('nonleader, changing search and changing folder clear selection without cancelling an old reference', async () => {
  const calls = [],
    h = mountReferences(false, async (...args) => {
      calls.push(args)
      return 0
    })
  try {
    await tick()
    h.scope().selectReference(h.scope().rows[0])
    await tick()
    assert.equal(h.scope().selectedReference, undefined)
    assert.equal(
      nodes(h.root, (n) => n.props['data-testid'] === 'dcc-folder-cancel-reference').length,
      0
    )
    h.props.isProjectLeader = true
    await tick()
    h.scope().selectReference(h.scope().rows[0])
    await tick()
    h.props.selectionContextKey = 'search-2'
    await tick()
    assert.equal(h.scope().selectedReference, undefined)
    h.scope().selectReference(h.scope().rows[0])
    await tick()
    nodes(
      h.root,
      (n) => n.props['data-testid'] === 'dcc-folder-cancel-reference'
    )[0].props.onClick()
    await tick()
    h.props.contextKey = 'folder-22'
    h.props.folderId = '22'
    await tick()
    assert.equal(h.scope().selectedReference, undefined)
    assert.equal(nodes(h.root, (n) => n.type === 'dialog').length, 0)
    assert.equal(calls.length, 0)
  } finally {
    h.app.unmount()
  }
})
test('failed cancel remains visible and retains the reference instead of reporting success', async () => {
  const h = mountReferences(true, async () => {
    throw Error('正式权限拒绝')
  })
  try {
    await tick()
    h.scope().selectReference(h.scope().rows[0])
    await tick()
    nodes(
      h.root,
      (n) => n.props['data-testid'] === 'dcc-folder-cancel-reference'
    )[0].props.onClick()
    await tick()
    nodes(h.root, (n) => n.type === 'el-input')[0].props['onUpdate:modelValue']('取消原因')
    await tick()
    await nodes(
      h.root,
      (n) => n.type === 'el-button' && text(n) === '确认取消引用'
    )[0].props.onClick()
    await tick()
    assert.equal(h.scope().rows.length, 1)
    assert.equal(h.events.length, 0)
    assert.ok(
      nodes(h.root, (n) => n.type === 'el-alert').some((n) =>
        String(n.props.title).includes('正式权限拒绝')
      )
    )
  } finally {
    h.app.unmount()
  }
})

function mountRecords(getPermissions = async (id) => (id === file.id ? selected : latest)) {
  const props = vue.reactive({
    file: {
      ...file,
      status: 'CONTROLLED_PENDING_EFFECTIVE',
      currentActiveVersionNo: 'A/1',
      fileNumber: 'SOP-1',
      fileName: '文件.pdf',
      fileOwnerNicknameSnapshot: '批准选择的负责人',
      effectiveDate: '2026-10-13',
      controlledTime: '2026-10-06 08:41:52',
      distributedTime: '2026-10-06 09:59:10',
      checkedOutBy: 99,
      checkedOutByName: '检出人员',
      checkedOutTime: '2026-10-06 10:00:00'
    },
    categoryName: '质量文件',
    userNames: new Map(),
    departmentNames: new Map()
  })
  const component = compileComponent('detail/ControlledFileBrowseRecords.vue', (id) => {
    if (id === 'vue') return vue
    if (id === 'vue-router')
      return {
        useRoute: () => ({ fullPath: '/detail', query: {} }),
        useRouter: () => ({ push: async () => {} })
      }
    if (id.endsWith('utils/auth')) return { getTenantId: () => '1', getVisitTenantId: () => null }
    if (id.endsWith('applicationRead'))
      return { getControlledFileRelationPermissions: getPermissions }
    if (id.endsWith('controlledFile/relations'))
      return {
        listCurrentRelations: async () => ({ sourceControlledFileId: latest.controlledFileId }),
        getProjectReferenceUsage: async id => ({
          tenantId: '1',
          sourceControlledFileId: id,
          masterId: '100',
          referenceProjectCount: 2,
          referenced: true
        })
      }
    if (id === './browse-records') return load('detail/browse-records.ts')
    if (id === './relation-contract') return load('detail/relation-contract.ts')
    if (id === './presentation')
      return {
        formatControlledFileDate: (value) => value || '-',
        formatControlledFileDateTime: (value) => value || '-',
        getDetailStatusLabel: (value) =>
          value === 'CONTROLLED_PENDING_EFFECTIVE' ? '已受控、待生效' : value,
        getDetailStatusTagType: () => 'info',
        getDistributionMediumLabel: (value) => value,
        getDistributionStatusLabel: (value) => value
      }
    if (id.endsWith('revision-model')) return { revisionChangeLabel: () => '局部变更' }
    if (id.endsWith('effective-facts')) return load('workflow/effective-facts.ts')
    if (id.endsWith('version-display')) return load('shared/version-display.ts')
    if (id.endsWith('signature-evidence'))
      return { getDccSignatureTaskActionLabel: (value) => value }
    if (id.endsWith('view/presentation'))
      return { buildControlledFileTraceabilityPath: (id) => `/trace/${id}` }
    if (id.endsWith('.vue')) return { default: stub }
    throw Error(id)
  })
  const app = renderer.createApp({ render: () => vue.h(component, props) })
  for (const tag of [
    'el-tag',
    'el-alert',
    'el-descriptions',
    'el-descriptions-item',
    'el-button',
    'el-table',
    'el-table-column'
  ])
    app.component(tag, {
      setup:
        (_p, { slots, attrs }) =>
        () =>
          vue.h(tag, attrs, slots.default && tag !== 'el-table-column' ? slots.default() : [])
    })
  const root = node('root')
  app.mount(root)
  return { props, root, app }
}
test('actual browse page renders owner, all dates, latest versus executing version and pending warning together', async () => {
  const h = mountRecords()
  try {
    await tick()
    for (const value of [
      '批准选择的负责人',
      '2026-10-13',
      '2026-10-06 08:41:52',
      '2026-10-06 09:59:10',
      '2026-10-06 10:00:00',
      '正式文件夹',
      'B/1',
      'A/1',
      '引用项目：2'
    ])
      assert.ok(text(h.root).includes(value), value)
    assert.ok(
      nodes(h.root, (n) => n.type === 'el-alert').some((n) =>
        String(n.props.title).includes('生效前不得作为正式执行文件使用')
      )
    )
  } finally {
    h.app.unmount()
  }
})

test('actual readonly records hide selected/source iteration labels without mutating stored identity', async () => {
  const h = mountRecords(async id => id === file.id ? { ...selected, versionNo: 'A/1-2' } : latest)
  try {
    h.props.file.versionNo = 'A/1-2'
    h.props.file.selectedIterationVersionNo = 'A/1-2'
    await tick()
    assert.ok(text(h.root).includes('草稿'))
    assert.equal(text(h.root).includes('A/1-2'), false)
    assert.equal(h.props.file.versionNo, 'A/1-2')
    assert.equal(h.props.file.id, '9007199254740993')
  } finally { h.app.unmount() }
})

test('actual basic info hides iteration label without changing its original version facts', () => {
  const file = { id: '9007199254740993', currentActiveVersionNo: 'A/1-2' }
  const component = compileComponent('shared/ControlledFileBasicInfoPanel.vue', id => {
    if (id === 'vue') return vue
    if (id.endsWith('version-display')) return load('shared/version-display.ts')
    if (id.endsWith('presentation')) return { formatControlledFileDateTime: value => value || '未记录' }
    throw Error(id)
  })
  const app = renderer.createApp({ render: () => vue.h(component, { file }) })
  for (const tag of ['el-descriptions', 'el-descriptions-item', 'Icon', 'el-button', 'el-link']) app.component(tag, {
    setup: (_props, ctx) => () => vue.h(tag, ctx.attrs, ctx.slots.default?.())
  })
  const dom = node('root'); app.mount(dom)
  try {
    assert.ok(text(dom).includes('草稿'))
    assert.equal(text(dom).includes('A/1-2'), false)
    assert.equal(file.currentActiveVersionNo, 'A/1-2')
  } finally { app.unmount() }
})
test('late metadata for the previous file cannot overwrite the new file browse records', async () => {
  let resolveOld
  const old = new Promise((resolve) => {
    resolveOld = resolve
  })
  const h = mountRecords((id) =>
    id === file.id
      ? old
      : Promise.resolve({
          ...selected,
          controlledFileId: id,
          versionNo: 'B/1',
          hasCurrentControlledSource: false,
          latestControlledFileId: null,
          projectName: '新文件项目'
        })
  )
  try {
    await tick()
    h.props.file = { ...h.props.file, id: '9007199254740997', versionNo: 'B/1' }
    await tick()
    assert.ok(text(h.root).includes('新文件项目'))
    resolveOld(selected)
    await tick()
    assert.ok(text(h.root).includes('新文件项目'))
    assert.equal(text(h.root).includes('正式项目'), false)
  } finally {
    h.app.unmount()
  }
})

test('initial pending controlled file does not imply that a nonexistent old version remains executable', async () => {
  const h = mountRecords()
  try {
    h.props.file.currentActiveVersionNo = null
    await tick()
    const warning = nodes(h.root, (n) => n.type === 'el-alert').find((n) =>
      String(n.props.title).includes('生效前不得作为正式执行文件使用')
    )
    assert.ok(warning)
    assert.equal(String(warning.props.title).includes('旧版继续执行'), false)
  } finally {
    h.app.unmount()
  }
})

test('inaccessible related target cannot block latest controlled or reference metadata', async () => {
  const api = readers({
    getCurrentRelations: async () => {
      throw Error('关联目标无权查看')
    }
  })
  const result = await load('detail/browse-records.ts').loadBrowseMetadata(file, '1', api)
  assert.equal(result.latest.versionNo, 'B/1')
  assert.equal(result.referenceProjectCount, 2)
})
test('reference rows honor selected lifecycle filter without inventing a working version', () => {
  const merge = load('browser/folder-file-list.ts').buildFolderFileRows
  const ref = {
    reference: { id: '55' },
    selectedVersion: selected,
    sourceProjectName: '源项目',
    referenceProjectCount: 1
  }
  assert.equal(merge([], [ref], {}, '', 'WORKING').length, 0)
  assert.equal(merge([], [ref], {}, '', 'OBSOLETE').length, 0)
  assert.equal(
    merge(
      [],
      [{ ...ref, selectedVersion: { ...selected, status: 'OBSOLETE', controlled: false } }],
      {},
      '',
      'OBSOLETE'
    ).length,
    1
  )
})
test('a new detail object for the same file triggers refreshed browse metadata', async () => {
  let calls = 0
  const h = mountRecords(async (id) => {
    calls++
    return id === file.id ? selected : latest
  })
  try {
    await tick()
    const before = calls
    h.props.file = { ...h.props.file, status: 'ACTIVE' }
    await tick()
    assert.ok(calls > before)
  } finally {
    h.app.unmount()
  }
})
test('failed cancellation offers a real list refresh that clears obsolete selection', async () => {
  const h = mountReferences(true, async () => {
    throw Error('取消结果未知，请核对')
  })
  try {
    await tick()
    h.scope().selectReference(h.scope().rows[0])
    await tick()
    nodes(
      h.root,
      (n) => n.props['data-testid'] === 'dcc-folder-cancel-reference'
    )[0].props.onClick()
    await tick()
    nodes(h.root, (n) => n.type === 'el-input')[0].props['onUpdate:modelValue']('取消原因')
    await tick()
    await nodes(
      h.root,
      (n) => n.type === 'el-button' && text(n) === '确认取消引用'
    )[0].props.onClick()
    await tick()
    const refresh = nodes(
      h.root,
      (n) => n.props['data-testid'] === 'dcc-folder-refresh-references'
    )[0]
    assert.ok(refresh, 'formal read refresh stays available after failure')
    await refresh.props.onClick()
    await tick()
    assert.equal(h.scope().selectedReference, undefined)
    assert.equal(nodes(h.root, (n) => n.type === 'dialog').length, 0)
  } finally {
    h.app.unmount()
  }
})
