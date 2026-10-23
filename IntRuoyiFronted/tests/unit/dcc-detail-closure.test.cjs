const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const read = (p) => fs.readFileSync(p, 'utf8')
const clone = (v) => JSON.parse(JSON.stringify(v))
const attrs = (m) => ({
  targetMarkets: [m],
  licenseHolder: 'Y',
  actualManufacturer: 'N',
  documentTransfer: 'N'
})
function mod(
  source,
  resolve = () => {
    throw Error('dependency')
  }
) {
  const c = {
    exports: {},
    require: resolve,
    Error,
    Date,
    JSON,
    BigInt,
    Set,
    Number,
    structuredClone,
    useMessage: () => ({ success() {} })
  }
  vm.runInNewContext(
    ts.transpileModule(source, {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
    }).outputText,
    c
  )
  return c.exports
}
const attributes = mod(read('src/views/dcc/controlled-file/project-attributes/state.ts'))
const uploadDepartments = mod(read('src/views/dcc/controlled-file/upload/signoff-departments.ts'))
const matrixHelper = require('./dcc-upload-test-loader.cjs'),
  matrixModule = matrixHelper.loadUploadMatrix()
const uploadSubmitter = mod(read('src/views/dcc/controlled-file/upload/submitter.ts'), (id) =>
  id === './approval-matrix'
    ? matrixModule
    : id === './signoff-departments'
      ? uploadDepartments
      : attributes
)
function api(get) {
  return mod(read('src/api/dcc/controlledFile/applicationRead.ts'), (id) =>
    id === '@/config/axios' ? { default: { get } } : attributes
  )
}
function mount(name, props, deps) {
  const file = `src/views/dcc/controlled-file/detail/${name}.vue`
  const { descriptor } = parse(read(file))
  const compiled = compileScript(descriptor, { id: name })
  deps = {
    '@/api/common/filePreview': require('./dcc-fixed-version-preview-test-loader.cjs')().common,
    './application-node-records': mod(
      read('src/views/dcc/controlled-file/detail/application-node-records.ts')
    ),
    './relation-contract': mod(read('src/views/dcc/controlled-file/detail/relation-contract.ts')),
    '../shared/version-display': mod(
      read('src/views/dcc/controlled-file/shared/version-display.ts')
    ),
    ...deps
  }
  const component = mod(compiled.content, (id) =>
    id === 'vue'
      ? vue
      : deps[id] ||
        (id.endsWith('.vue')
          ? {}
          : (() => {
              throw Error(id)
            })())
  ).default
  component.render = () => vue.h('section')
  const renderer = vue.createRenderer({
    createElement: (type) => ({ type, children: [] }),
    createText: (text) => ({ text }),
    createComment: (text) => ({ text }),
    insert: (c, p) => p.children.push(c),
    remove() {},
    setText() {},
    setElementText() {},
    parentNode: () => null,
    nextSibling: () => null,
    patchProp() {}
  })
  const reactive = vue.reactive(props),
    app = renderer.createApp({ render: () => vue.h(component, reactive) }),
    host = app.mount({ children: [] })
  return { state: host.$.subTree.component.setupState, props: reactive, app }
}
const deferred = () => {
  let resolve
  const promise = new Promise((r) => (resolve = r))
  return { resolve, promise }
}
const draft = (id) => ({
  controlledFileId: id,
  projectId: '20',
  applicationType: 'REVISION',
  defaultSource: attrs('CE'),
  actual: attrs('FDA'),
  effectiveMode: 'PRESET_DATE',
  effectiveDate: '2026-12-01',
  selectedDistributionDepartmentIds: ['50'],
  needTraining: false,
  selectedSignoffDepartmentIds: ['50'],
  changeDescription: '已保存',
  canSubmit: true,
  unavailableReason: null,
  draftApprovalMatrix: matrixHelper.matrixFixture({
    categoryId: '30',
    taxonomyId: '40',
    approvalIds: ['60']
  })
})
const options = {
  controlledBaselineId: '10',
  baselineVersionNo: 'A/1',
  masterId: '100',
  sourceOriginalFileName: '说明.pdf',
  partialTarget: { versionNo: 'A/2', unavailableReason: null },
  replacementTarget: { versionNo: 'B/1', unavailableReason: null },
  iterations: [
    { id: '12', versionNo: 'A/1-1', canPartial: true, canReplacement: true, canPreview: true },
    { id: '13', versionNo: 'A/1-2', canPartial: true, canReplacement: true, canPreview: true }
  ]
}
function application(
  readDraft = async (id) => draft(id),
  replacementRead = async (id) => draft(id)
) {
  return mount(
    'DetailApplicationPanel',
    {
      file: {
        id: '10',
        masterId: '100',
        dccProjectCodeId: '20',
        categoryId: 30,
        fileTypeTaxonomyId: 40,
        status: 'ACTIVE',
        versionNo: 'A/1',
        title: '说明.pdf'
      }
    },
    {
      'element-plus': { ElMessageBox: { confirm: async () => true } },
      '@/utils': { generateUUID: () => 'key' },
      '@/utils/auth': { getTenantId: () => '1', getVisitTenantId: () => undefined },
      '@/api/system/dept': { getSimpleDeptList: async () => [{ id: 50, name: '质量' }] },
      '@/api/system/user': { getSimpleUserList: async () => [{ id: 60, nickname: '批准' }] },
      '@/api/dcc/controlledFile/fileTypeTaxonomies': {
        getFileTypeTaxonomyUploadOptions: async () => {
          throw Error('ordinary application selection must not load type-change candidates')
        }
      },
      '@/api/dcc/controlledFile/approvalRoutes': {
        previewApprovalRoute: async () => [
          { stageCode: 'MATRIX_REVIEW', candidateSourceType: 'DEPT', candidateSourceIds: [50] },
          { stageCode: 'MATRIX_APPROVAL', resolvedUserIds: [60] }
        ]
      },
      '@/api/dcc/controlledFile/projectAttributes': {
        getProjectDefaults: async () => attrs('NMPA')
      },
      '@/api/dcc/controlledFile/applicationRead': {
        getWorkingApplicationAttributes: readDraft,
        getControlledFileReplacementAttributes: replacementRead,
        getControlledFileRevisionOptions: async () => clone(options),
        saveWorkingApplicationAttributes: async () => {
          throw Error('must not write original replacement source')
        },
        restoreWorkingApplicationAttributes: async () => {
          throw Error('must not restore original replacement source')
        }
      },
      '@/api/dcc/controlledFile/workflow': {
        getControlledFile: async (id) => ({
          id,
          masterId: '100',
          dccProjectCodeId: '20',
          categoryId: 30,
          fileTypeTaxonomyId: 40
        }),
        checkControlledFileRouteReadiness: async () =>
          matrixHelper.matrixReadiness(
            {
              ready: true,
              blockers: [],
              nodes: [
                {
                  stageCode: 'MATRIX_REVIEW',
                  candidateSourceType: 'DEPT',
                  candidateSourceIds: [50]
                },
                { stageCode: 'MATRIX_APPROVAL', resolvedUserIds: [60], approveMethod: 'ALL' }
              ]
            },
            { categoryId: '30', taxonomyId: '40', actionType: 'REVISION', approvalIds: ['60'] }
          )
      },
      '../project-attributes/state': attributes,
      '../revision/revision-model': mod(
        read('src/views/dcc/controlled-file/revision/revision-model.ts'),
        () => attributes
      ),
      '../workflow/workflow-actions': mod(
        read('src/views/dcc/controlled-file/workflow/workflow-actions.ts')
      ),
      './presentation': { formatControlledFileDateTime: (v) => v },
      '../upload/submitter': uploadSubmitter,
      '../upload/signoff-departments': uploadDepartments,
      '../upload/approval-matrix': matrixModule
    }
  )
}
test('formal rounds preserve failed revision and completed obsolete identities, reject foreign and duplicate mappings', async () => {
  let response = [
    {
      controlledFileId: '9007199254740993',
      applicationType: 'REVISION',
      bpmRound: 'failed-A2',
      attributeRound: 1
    },
    {
      controlledFileId: '9007199254740993',
      applicationType: 'OBSOLETE',
      bpmRound: 'completed-obsolete',
      attributeRound: 2
    }
  ]
  const calls = [],
    a = api(async (q) => {
      calls.push(q)
      return response
    })
  assert.deepEqual(clone(await a.getControlledFileApplicationRounds('9007199254740993')), response)
  assert.equal(calls[0].url, '/dcc/controlled-files/9007199254740993/application-rounds')
  response = [{ ...response[0], controlledFileId: '22' }]
  await assert.rejects(a.getControlledFileApplicationRounds('9007199254740993'), /不一致/)
  response = [1, 2].map(() => ({
    controlledFileId: '12',
    applicationType: 'UPLOAD',
    bpmRound: 'bpm',
    attributeRound: 1
  }))
  await assert.rejects(a.getControlledFileApplicationRounds('12'), /重复/)
})
test('formal relation permission binds boolean authority to the selected exact Long identity', async () => {
  const selectedId = '9007199254740993',
    currentId = selectedId,
    latestId = '9007199254740995'
  const valid = metadata(selectedId, {
    hasCurrentControlledSource: true,
    currentActiveControlledFileId: currentId,
    latestControlledFileId: latestId,
    canPreview: true
  })
  let response = valid
  const a = api(async () => response)
  const result = await a.getControlledFileRelationPermissions(selectedId)
  assert.equal(result.canPreview, true)
  assert.equal(result.controlledFileId, selectedId)
  assert.equal(result.currentActiveControlledFileId, currentId)
  assert.equal(result.latestControlledFileId, latestId)
  for (const patch of [
    { canEdit: 1 },
    { canPreview: 'true' },
    { hasCurrentControlledSource: 1 },
    { controlledFileId: '22' }
  ]) {
    response = { ...valid, ...patch }
    await assert.rejects(a.getControlledFileRelationPermissions(selectedId), /权限|不一致/)
  }
  for (const current of [
    undefined,
    null,
    '01',
    Number.MAX_SAFE_INTEGER + 1,
    '9223372036854775808'
  ]) {
    response = { ...valid, currentActiveControlledFileId: current }
    await assert.rejects(
      a.getControlledFileRelationPermissions(selectedId),
      /权限|身份|ID|编号|精度/
    )
  }
})
test('replacement read uses exact selected body and baseline strings, rejecting foreign or non-REVISION facts', async () => {
  let response = draft('9007199254740993')
  const calls = [],
    a = api(async (q) => {
      calls.push(q)
      return response
    })
  await a.getControlledFileReplacementAttributes('9007199254740993', '9007199254740995')
  assert.equal(calls[0].url, '/dcc/controlled-files/9007199254740993/replacement-attributes')
  assert.deepEqual(clone(calls[0].params), { controlledBaselineId: '9007199254740995' })
  response = { ...response, applicationType: 'UPLOAD' }
  await assert.rejects(
    a.getControlledFileReplacementAttributes('9007199254740993', '9007199254740995'),
    /换版/
  )
})
test('explicit replacement reads a legal owner context independently of ordinary canSubmit=false and never edits original draft', async () => {
  const ordinary = [],
    replacement = [],
    p = application(
      async (id) => {
        ordinary.push(id)
        return { ...draft(id), canSubmit: false, unavailableReason: '原稿本人资格不可提交' }
      },
      async (id, baseline) => {
        replacement.push([id, baseline])
        return draft(id)
      }
    )
  try {
    await p.state.open()
    await p.state.selectIteration('12')
    assert.equal(p.state.canSubmit, false)
    await p.state.selectIntent('REPLACEMENT')
    assert.deepEqual(replacement, [['12', '10']])
    assert.equal(p.state.canSubmit, true)
    assert.equal(p.state.error, '')
    assert.deepEqual(clone(p.state.snapshot.defaultSource), attrs('CE'))
    assert.deepEqual(clone(p.state.snapshot.actual), attrs('FDA'))
    assert.equal(p.state.canEditSourceDraft, false)
    await p.state.saveAttributes()
    await p.state.restoreAttributes()
    assert.match(p.state.error, /独立换版/)
  } finally {
    p.app.unmount()
  }
})
test('late ordinary draft denial cannot overwrite a newer legal explicit replacement selection', async () => {
  const pending = deferred(),
    p = application(
      () => pending.promise,
      async (id) => ({ ...draft(id), actual: attrs('NMPA') })
    )
  try {
    await p.state.open()
    const ordinary = p.state.selectIteration('12')
    await p.state.selectIntent('REPLACEMENT')
    pending.resolve({ ...draft('12'), canSubmit: false, unavailableReason: 'ordinary unavailable' })
    await ordinary
    assert.equal(p.state.canSubmit, true)
    assert.equal(p.state.error, '')
    assert.equal(p.state.snapshotIntent, 'REPLACEMENT')
    assert.deepEqual(clone(p.state.snapshot.actual), attrs('NMPA'))
  } finally {
    p.app.unmount()
  }
})
test('evidence rejects mixed BPM signatures even when the requested attribute snapshot is valid', async () => {
  const a = api(async () => ({
    controlledFileId: '10',
    versionNo: 'A/2',
    applicationType: 'REVISION',
    bpmRound: 'round-2',
    attributeRound: 2,
    recorded: true,
    unavailableReason: null,
    defaultSource: attrs('CE'),
    actualAttributes: attrs('FDA'),
    signatures: [{ processInstanceId: 'round-1' }]
  }))
  await assert.rejects(
    a.getControlledFileApplicationEvidence('10', 'REVISION', 'round-2'),
    /其他流程轮次/
  )
})
test('selected saved body shows CE default/FDA actual and a late previous draft cannot overwrite the new body', async () => {
  const pending = deferred(),
    ids = [],
    p = application(async (id) => {
      ids.push(String(id))
      return String(id) === '12'
        ? pending.promise
        : { ...draft(String(id)), actual: attrs('MDSAP') }
    })
  try {
    await p.state.open()
    assert.deepEqual(clone(p.state.snapshot.actual), attrs('NMPA'))
    const first = p.state.selectIteration('12')
    await p.state.selectIteration('13')
    pending.resolve(draft('12'))
    await first
    assert.deepEqual(ids, ['12', '13'])
    assert.equal(p.state.selectedIterationId, '13')
    assert.deepEqual(clone(p.state.snapshot.defaultSource), attrs('CE'))
    assert.deepEqual(clone(p.state.snapshot.actual), attrs('MDSAP'))
  } finally {
    p.app.unmount()
  }
})
test('unreadable selected draft clears prior values and submission instead of retaining current project defaults', async () => {
  const p = application(async () => {
    throw Error('本人合法草稿不可读')
  })
  try {
    await p.state.open()
    await p.state.selectIteration('12')
    assert.equal(p.state.snapshot, undefined)
    assert.equal(p.state.canSubmit, false)
    assert.match(p.state.error, /本人合法/)
  } finally {
    p.app.unmount()
  }
})
test('historical application selection loads exact mapped type/round and ignores late previous evidence', async () => {
  const pending = deferred(),
    calls = [],
    rows = [
      {
        controlledFileId: '10',
        applicationType: 'REVISION',
        bpmRound: 'failed-A2',
        attributeRound: 1
      },
      {
        controlledFileId: '10',
        applicationType: 'OBSOLETE',
        bpmRound: 'completed-obsolete',
        attributeRound: 2
      }
    ]
  const p = mount(
    'DetailApplicationHistory',
    { fileId: '10' },
    {
      '../shared/signature-evidence': mod(
        read('src/views/dcc/controlled-file/shared/signature-evidence.ts')
      ),
      './presentation': { formatControlledFileDateTime: (value) => value },
      './application-round-context': mod(
        read('src/views/dcc/controlled-file/detail/application-round-context.ts')
      ),
      '@/api/dcc/controlledFile/applicationRead': {
        getControlledFileApplicationRounds: async () => rows,
        getControlledFileApplicationEvidence: async (id, type, bpm) => {
          calls.push([id, type, bpm])
          return bpm === 'failed-A2'
            ? pending.promise
            : {
                controlledFileId: id,
                applicationType: type,
                bpmRound: bpm,
                attributeRound: 2,
                recorded: true,
                defaultSource: attrs('CE'),
                actualAttributes: attrs('FDA'),
                signatures: [
                  {
                    id: '22',
                    processInstanceId: bpm,
                    actorNicknameSnapshot: '本轮签名',
                    actionType: 'APPROVE',
                    signedAt: '2026-10-11 12:00:00'
                  }
                ]
              }
        }
      }
    }
  )
  try {
    for (let n = 0; n < 10; n++) await Promise.resolve()
    const first = p.state.selectRound(p.state.roundKey(rows[0]))
    await p.state.selectRound(p.state.roundKey(rows[1]))
    pending.resolve({
      controlledFileId: '10',
      applicationType: 'REVISION',
      bpmRound: 'failed-A2',
      attributeRound: 1,
      recorded: true,
      signatures: [
        {
          id: '21',
          processInstanceId: 'failed-A2',
          actorNicknameSnapshot: '旧签名',
          actionType: 'REJECT',
          signedAt: '2026-10-10 12:00:00'
        }
      ]
    })
    await first
    assert.equal(p.state.evidence.bpmRound, 'completed-obsolete')
    assert.equal(p.state.evidence.signatures[0].actorNicknameSnapshot, '本轮签名')
    assert.equal(p.state.evidence.signatures[0].actionType, 'APPROVE')
    assert.equal(p.state.evidence.signatures[0].signedAt, '2026-10-11 12:00:00')
    assert.deepEqual(calls.at(-1), ['10', 'OBSOLETE', 'completed-obsolete'])
  } finally {
    p.app.unmount()
  }
})
test('relation source rejects a different version even on the same Master and preserves history snapshot fields', () => {
  const m = mod(read('src/views/dcc/controlled-file/detail/relation-contract.ts'))
  const selected = {
    id: '10',
    masterId: '100',
    dccProjectCodeId: '20',
    versionNo: 'A/1',
    title: '源.pdf',
    fileNumber: 'S'
  }
  assert.throws(
    () => m.assertRelationSource(selected, { ...selected, id: '11', versionNo: 'A/2' }, '11'),
    /稳定/
  )
  assert.equal(m.assertRelationSource(selected, selected, '10'), true)
  assert.throws(
    () => m.assertRelationSource(selected, { ...selected, id: '11', masterId: '999' }, '11'),
    /稳定/
  )
  const snapshot = {
    relationId: '7',
    controlledFileId: '31',
    masterId: '300',
    projectCodeId: '20',
    fileName: '旧名.pdf',
    fileNumber: 'OLD',
    versionNo: 'A/1',
    status: null
  }
  const row = m.historicalRelationCandidate(snapshot, {
    tenantId: '1',
    masterId: '300',
    controlledFileId: '31',
    projectId: '30',
    projectName: '当前项目名称',
    folderName: '未记录',
    canPreview: false
  })
  assert.equal(row.fileName, '旧名.pdf')
  assert.equal(row.versionNo, 'A/1')
  assert.equal(row.fileNumber, 'OLD')
  assert.equal(row.status, 'HISTORICAL_SNAPSHOT')
  assert.equal(row.controlledFileId, '31')
  assert.equal(row.canPreview, false)
})
test('public detail relations loads only the selected snapshot, preserving fixed body and readonly boundaries', async () => {
  const calls = [],
    selected = {
      id: '10',
      masterId: '100',
      dccProjectCodeId: '20',
      versionNo: 'A/1',
      title: '源.pdf',
      fileNumber: 'S'
    }
  let foreignTenant = false,
    foreignSource = false
  const p = mount(
    'DetailRelationsPanel',
    { file: selected, allowEdit: true },
    {
      '@/utils/auth': { getTenantId: () => '1', getVisitTenantId: () => undefined },
      '@/api/dcc/controlledFile/applicationRead': {
        getControlledFileRelationPermissions: async (id) => {
          calls.push(['metadata', id])
          if (id === '10')
            return metadata(id, {
              controlledFileId: foreignSource ? '99' : id,
              currentActiveControlledFileId: '11',
              latestControlledFileId: '12',
              fileName: '源.pdf',
              fileNumber: 'S',
              versionNo: 'A/1'
            })
          if (id === '31')
            return metadata(id, {
              masterId: '300',
              projectId: '20',
              tenantId: foreignTenant ? '2' : '1',
              currentActiveControlledFileId: '32',
              fileName: '今天名称.pdf',
              fileNumber: 'NEW',
              versionNo: 'A/1',
              status: 'OBSOLETE',
              controlled: false,
              executable: false
            })
          throw Error('must not read another current version ' + id)
        }
      },
      '@/api/dcc/controlledFile/relations': {
        listCurrentRelations: async () => {
          throw Error('must not read current set')
        },
        listHistoricalRelations: async (id) => {
          calls.push(['history', id])
          return [
            {
              relationId: '7',
              controlledFileId: '31',
              masterId: '300',
              projectCodeId: '20',
              fileName: '旧名.pdf',
              fileNumber: 'OLD',
              versionNo: 'A/1'
            }
          ]
        },
        replaceCurrentRelations: async () => {
          throw Error('readonly must not write')
        }
      }
    }
  )
  try {
    await p.state.load()
    assert.equal(p.state.error, '')
    assert.equal(p.state.selectedSource.controlledFileId, '10')
    assert.equal(p.state.files[0].controlledFileId, '31')
    assert.equal(p.state.files[0].fileName, '旧名.pdf')
    assert.equal(p.state.files[0].fileNumber, 'OLD')
    assert.equal(p.state.files[0].versionNo, 'A/1')
    assert.ok(
      calls.every(([type, id]) => (type === 'history' ? id === '10' : ['10', '31'].includes(id)))
    )
    foreignTenant = true
    await p.state.load()
    assert.match(p.state.error, /租户/)
    assert.equal(p.state.files.length, 0)
    foreignTenant = false
    foreignSource = true
    calls.length = 0
    await p.state.load()
    assert.match(p.state.error, /稳定/)
    assert.equal(calls.filter(([type]) => type === 'history').length, 0)
  } finally {
    p.app.unmount()
  }
})

test('detail exposes exact revision attempts and source identity, and reads actual formal rounds instead of guessing workflow type', () => {
  const page = read('src/views/dcc/controlled-file/detail/index.vue')
  const version = page.slice(
    page.indexOf('<el-table-column label="本版变更事实"'),
    page.indexOf('</el-table-column>', page.indexOf('<el-table-column label="本版变更事实"'))
  )
  assert.match(version, /row\.revisionAttemptNo/)
  assert.match(version, /row\.reworkPredecessorControlledFileId/)
  assert.match(version, /row\.id/)
  assert.match(version, /row\.processInstanceId/)
  assert.match(page, /<DetailApplicationHistory\b/)
  assert.match(page, /<DetailRelationsPanel\b/)
  assert.doesNotMatch(page, /<ApplicationEvidencePanel\b/)
  const history = read('src/views/dcc/controlled-file/detail/DetailApplicationHistory.vue')
  assert.match(history, /getControlledFileApplicationRounds/)
  assert.match(
    history,
    /getControlledFileApplicationEvidence\(\s*round\.controlledFileId,\s*round\.applicationType,\s*round\.bpmRound\s*\)/
  )
  assert.match(history, /evidence\.signatures/)
})
const metadata = (id, patch = {}) => ({
  controlledFileId: id,
  tenantId: '1',
  masterId: id === '10' || id === '11' ? '100' : '300',
  projectId: id === '10' || id === '11' ? '20' : '30',
  projectName: '正式项目',
  projectFolderId: '201',
  projectFolderName: '质量',
  fileNumber: 'F',
  fileName: '元数据.pdf',
  versionNo: 'B/1',
  status: 'ACTIVE',
  controlled: true,
  hasCurrentControlledSource: false,
  currentActiveControlledFileId: null,
  latestControlledFileId: null,
  pendingEffect: false,
  executable: true,
  canEdit: false,
  canPreview: false,
  ...patch
})
test('relation projection wrapper validates exact tenant/Master/placement and lifecycle metadata without stronger detail calls', async () => {
  const valid = metadata('9007199254740993', {
    hasCurrentControlledSource: true,
    currentActiveControlledFileId: '9007199254740993',
    latestControlledFileId: '9007199254740995'
  })
  let result = valid
  const a = api(async () => result)
  const projection = await a.getControlledFileRelationPermissions('9007199254740993')
  assert.equal(projection.projectFolderId, '201')
  assert.equal(projection.currentActiveControlledFileId, '9007199254740993')
  assert.equal(projection.latestControlledFileId, '9007199254740995')
  result = { ...result, pendingEffect: true }
  await assert.rejects(a.getControlledFileRelationPermissions('9007199254740993'), /关联/)
  result = { ...valid, projectFolderName: null }
  await assert.rejects(a.getControlledFileRelationPermissions('9007199254740993'), /关联/)
  for (const patch of [
    { currentActiveControlledFileId: undefined },
    { currentActiveControlledFileId: 'not-an-id' },
    { masterId: Number.MAX_SAFE_INTEGER + 1 },
    { tenantId: null },
    { controlledFileId: '22' },
    { executable: 1 }
  ]) {
    result = { ...valid, ...patch }
    await assert.rejects(a.getControlledFileRelationPermissions('9007199254740993'))
  }
})
test('name-authorized relations do not request a current set and never need project/detail/folder queries', async () => {
  const calls = [],
    relationContract = mod(read('src/views/dcc/controlled-file/detail/relation-contract.ts'))
  let currentReads = 0,
    writes = 0
  const p = mount(
    'DetailRelationsPanel',
    {
      file: {
        id: '10',
        masterId: '100',
        dccProjectCodeId: '20',
        versionNo: 'A/1',
        title: '源.pdf',
        fileNumber: 'S'
      },
      allowEdit: true
    },
    {
      '@/utils/auth': { getTenantId: () => '1', getVisitTenantId: () => undefined },
      '@/api/dcc/controlledFile/workflow': {
        getControlledFile: async () => {
          calls.push('file')
          throw Error('strong detail forbidden')
        }
      },
      '@/api/dcc/controlledFile/projectDiscovery': {
        getProjectDiscovery: async () => {
          calls.push('project')
          throw Error('strong project forbidden')
        }
      },
      '@/api/dcc/controlledFile/projectAttributes': {
        getProjectFolders: async () => {
          calls.push('folders')
          throw Error('folder forbidden')
        }
      },
      '@/api/dcc/controlledFile/applicationRead': {
        getControlledFileRelationPermissions: async (id) =>
          metadata(
            id,
            id === '10'
              ? {
                  hasCurrentControlledSource: true,
                  currentActiveControlledFileId: '10',
                  latestControlledFileId: '11',
                  fileName: '源.pdf',
                  fileNumber: 'S',
                  versionNo: 'A/1'
                }
              : {
                  hasCurrentControlledSource: true,
                  currentActiveControlledFileId: '32',
                  latestControlledFileId: '33',
                  projectId: '20',
                  versionNo: 'A/1',
                  status: 'OBSOLETE',
                  controlled: false,
                  executable: false
                }
          ),
        loadDccSelectorPage: async () => {
          calls.push('selector')
          throw Error('readonly candidates forbidden')
        }
      },
      '@/api/dcc/controlledFile/relations': {
        listCurrentRelations: async (id) => {
          currentReads++
          assert.equal(id, '10')
          throw Error('CURRENT_SET_UNAVAILABLE')
        },
        listHistoricalRelations: async (id) => {
          assert.equal(id, '10')
          return [
            {
              relationId: '7',
              controlledFileId: '31',
              masterId: '300',
              projectCodeId: '20',
              fileName: '历史原名.pdf',
              fileNumber: 'OLD',
              versionNo: 'A/1'
            }
          ]
        },
        replaceCurrentRelations: async () => {
          writes++
          throw Error('readonly writes forbidden')
        }
      },
      '../basic-data/components/project-folder-tree': mod(
        read('src/views/dcc/controlled-file/basic-data/components/project-folder-tree.ts')
      ),
      '../relations/project-reference-contract': mod(
        read('src/views/dcc/controlled-file/relations/project-reference-contract.ts')
      ),
      './relation-contract': relationContract
    }
  )
  try {
    await p.state.load()
    assert.ok(p.state.selectedSource)
    assert.equal(p.state.error, '')
    assert.equal(currentReads, 0)
    const history = p.state.files
    assert.equal(history[0].fileName, '历史原名.pdf')
    assert.equal(history[0].controlledFileId, '31')
    assert.deepEqual(calls, [])
    assert.equal(history[0].fileNumber, 'OLD')
    assert.equal(history[0].versionNo, 'A/1')
    assert.equal(history[0].canPreview, false)
    assert.equal(writes, 0)
  } finally {
    p.app.unmount()
  }
})
test('readonly detail relations never read directories and keep selected frozen relations without writes', async () => {
  let directoryCalls = 0,
    writes = 0,
    selectorCalls = 0
  const p = mount(
    'DetailRelationsPanel',
    {
      file: {
        id: '10',
        masterId: '100',
        dccProjectCodeId: '20',
        versionNo: 'A/1',
        title: '源.pdf',
        fileNumber: 'S'
      },
      allowEdit: true
    },
    {
      '@/utils/auth': { getTenantId: () => '1', getVisitTenantId: () => undefined },
      '@/api/dcc/controlledFile/projectAttributes': {
        getProjectFolders: async () => {
          directoryCalls++
          throw Error('目录权限不可读')
        }
      },
      '@/api/dcc/controlledFile/applicationRead': {
        getControlledFileRelationPermissions: async (id) =>
          metadata(
            id,
            id === '10'
              ? {
                  hasCurrentControlledSource: true,
                  currentActiveControlledFileId: '10',
                  latestControlledFileId: '11',
                  fileName: '源.pdf',
                  fileNumber: 'S',
                  versionNo: 'A/1',
                  canEdit: true
                }
              : {
                  hasCurrentControlledSource: true,
                  currentActiveControlledFileId: '32',
                  latestControlledFileId: '33',
                  projectId: '20',
                  versionNo: 'A/1',
                  status: 'OBSOLETE',
                  controlled: false,
                  executable: false
                }
          ),
        loadDccSelectorPage: async () => {
          selectorCalls++
          throw Error('详情不能查询编辑候选')
        }
      },
      '@/api/dcc/controlledFile/relations': {
        listCurrentRelations: async (id) => {
          assert.equal(id, '10')
          return { sourceControlledFileId: '10', rowVersion: '2', files: [] }
        },
        listHistoricalRelations: async (id) => {
          assert.equal(id, '10')
          return [
            {
              relationId: '7',
              controlledFileId: '31',
              masterId: '300',
              projectCodeId: '20',
              fileName: '历史原名.pdf',
              fileNumber: 'OLD',
              versionNo: 'A/1'
            }
          ]
        },
        replaceCurrentRelations: async () => {
          writes++
          throw Error('详情不能自由编辑')
        }
      },
      '../basic-data/components/project-folder-tree': mod(
        read('src/views/dcc/controlled-file/basic-data/components/project-folder-tree.ts')
      ),
      '../relations/project-reference-contract': mod(
        read('src/views/dcc/controlled-file/relations/project-reference-contract.ts')
      ),
      './relation-contract': mod(read('src/views/dcc/controlled-file/detail/relation-contract.ts'))
    }
  )
  try {
    await p.state.load()
    assert.equal(p.state.error, '')
    assert.equal(directoryCalls, 0)
    assert.equal(selectorCalls, 0)
    assert.equal(p.state.selectedSource.controlledFileId, '10')
    const history = p.state.files
    assert.equal(history[0].versionNo, 'A/1')
    assert.equal(history[0].controlledFileId, '31')
    assert.equal(history[0].fileName, '历史原名.pdf')
    assert.equal(history[0].fileNumber, 'OLD')
    assert.equal(history[0].canPreview, false)
    assert.equal(writes, 0)
    assert.equal(directoryCalls, 0)
    assert.equal(selectorCalls, 0)
  } finally {
    p.app.unmount()
  }
})
test('project browser wrapper defaults to latest controlled and forwards an explicit all-version exact status without changing selector scope', async () => {
  const calls = [],
    a = api(async (q) => {
      calls.push(q)
      return { list: [], total: 0 }
    })
  const query = { keyword: '真实文件', pageNo: 2, pageSize: 20 }
  await a.loadDccProjectBrowserPage('1', query)
  assert.equal(calls[0].params.latestVersionOnly, true)
  assert.equal(calls[0].params.browserScope, 'GLOBAL')
  assert.equal(calls[0].params.selectorScope, undefined)
  await a.loadDccProjectBrowserPage(
    '1',
    { ...query, projectId: '9007199254740993', folderId: '9007199254740995' },
    { latestVersionOnly: false, status: 'WORKING' }
  )
  assert.equal(calls[1].params.latestVersionOnly, false)
  assert.equal(calls[1].params.status, 'WORKING')
  assert.equal(calls[1].params.browserScope, 'PROJECT_FOLDER')
  assert.equal(calls[1].params.dccProjectCodeId, '9007199254740993')
  for (const options of [
    { latestVersionOnly: 'false' },
    { status: ' working ' },
    { status: 'INVENTED' }
  ])
    await assert.rejects(a.loadDccProjectBrowserPage('1', query, options), /浏览|状态/)
  assert.equal(calls.length, 2)
})
