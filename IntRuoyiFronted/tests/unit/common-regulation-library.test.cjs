const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const { ref, computed, watch, nextTick } = require('vue')

const filename = path.resolve(
  __dirname,
  '../../src/views/mes/pro/processpool/CommonRegulationLibrary.vue'
)
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'))
const script = ts.createSourceFile(
  filename + '.ts',
  descriptor.scriptSetup.content,
  ts.ScriptTarget.Latest,
  true
)
const code = ts.transpileModule(
  script.statements
    .filter(
      (statement) => !ts.isImportDeclaration(statement) && !ts.isTypeAliasDeclaration(statement)
    )
    .map((statement) => statement.getText(script))
    .join('\n'),
  {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
  }
).outputText
const createHarness = new Function(
  'ref',
  'computed',
  'watch',
  'onMounted',
  'onActivated',
  'defineEmits',
  'QcTemplateApi',
  'ElMessage',
  'ElMessageBox',
  code +
    `
return { versions, loading, loadError, keyword, status, pageNo, pageSize, filteredVersions, pagedVersions,
  detailVisible, detailLoading, detailError, detail, loadVersions, openDetail, closeDetail,
  deletingVersionId, deleteError, deleteVersion }
`
)
const noop = () => {}
const records = [
  {
    commonRegulationVersionId: 101,
    commonRegulationCode: 'COMMON-1',
    commonRegulationName: '初包装规程',
    versionNo: 'B/1',
    lifecycleStatus: 'PUBLISHED'
  },
  {
    commonRegulationVersionId: 100,
    commonRegulationCode: 'COMMON-1',
    commonRegulationName: '初包装规程',
    versionNo: 'A/1',
    lifecycleStatus: 'RETIRED'
  },
  {
    commonRegulationVersionId: 102,
    commonRegulationCode: 'COMMON-2',
    commonRegulationName: '大中包装规程',
    versionNo: 'A/2',
    lifecycleStatus: 'PUBLISHED'
  }
]
function harness(api = {}, confirm = async () => {}) {
  return createHarness(
    ref,
    computed,
    watch,
    noop,
    noop,
    noop,
    {
      listCommonRegulationReleasedVersions: async () => structuredClone(records),
      ...api
    },
    { success: noop, error: noop },
    { confirm }
  )
}

test('deletion confirms the exact version, refreshes and preserves other versions', async () => {
  let rows = structuredClone(records)
  let confirmation
  const h = harness(
    {
      listCommonRegulationReleasedVersions: async () => structuredClone(rows),
      deleteCommonRegulationVersion: async (id) => {
        assert.equal(id, 100)
        rows = rows.filter((r) => r.commonRegulationVersionId !== id)
      }
    },
    async (text) => {
      confirmation = text
    }
  )
  await h.loadVersions()
  await h.deleteVersion(records[1])
  assert.match(confirmation, /COMMON-1/)
  assert.match(confirmation, /A\/1/)
  assert.deepEqual(
    h.versions.value.map((r) => r.commonRegulationVersionId),
    [101, 102]
  )
  assert.equal(h.deleteError.value, '')
  assert.equal(h.deletingVersionId.value, undefined)
})

test('cancelling deletion never sends a delete request', async () => {
  let calls = 0
  const h = harness(
    {
      deleteCommonRegulationVersion: async () => {
        calls++
      }
    },
    async () => {
      throw 'cancel'
    }
  )
  await h.deleteVersion(records[0])
  assert.equal(calls, 0)
  assert.equal(h.deleteError.value, '')
  assert.equal(h.deletingVersionId.value, undefined)
})

test('referenced-version rejection stays visible and leaves the list intact', async () => {
  const h = harness({
    deleteCommonRegulationVersion: async () => {
      throw Error('该版本已被规程套引用，不能删除')
    }
  })
  await h.loadVersions()
  await h.deleteVersion(records[0])
  assert.match(h.deleteError.value, /规程套引用/)
  assert.equal(h.versions.value.length, 3)
  assert.equal(h.deletingVersionId.value, undefined)
})

test('duplicate clicks do not send a second deletion', async () => {
  let resolve
  let calls = 0
  const h = harness(
    {
      deleteCommonRegulationVersion: async () => {
        calls++
      }
    },
    () =>
      new Promise((done) => {
        resolve = done
      })
  )
  const pending = h.deleteVersion(records[0])
  await h.deleteVersion(records[1])
  resolve()
  await pending
  assert.equal(calls, 1)
})

test('lists unbound published documents and historical versions without needing a set', async () => {
  const h = harness()
  await h.loadVersions()
  assert.equal(h.filteredVersions.value.length, 3)
  for (const [keyword, ids] of [
    ['初包装', [101, 100]],
    ['common-2', [102]],
    ['A/1', [100]],
    ['不存在', []]
  ]) {
    h.keyword.value = keyword
    assert.deepEqual(
      h.filteredVersions.value.map((v) => v.commonRegulationVersionId),
      ids
    )
  }
  h.keyword.value = ''
  h.status.value = 'PUBLISHED'
  assert.deepEqual(
    h.filteredVersions.value.map((v) => v.commonRegulationVersionId),
    [101, 102]
  )
  h.status.value = 'RETIRED'
  assert.deepEqual(
    h.filteredVersions.value.map((v) => v.commonRegulationVersionId),
    [100]
  )
})

test('search and page-size changes return to the first page', async () => {
  const h = harness()
  await h.loadVersions()
  h.pageSize.value = 1
  await nextTick()
  h.pageNo.value = 2
  assert.equal(h.pagedVersions.value[0].commonRegulationVersionId, 100)
  h.keyword.value = '大中包装'
  await nextTick()
  assert.equal(h.pageNo.value, 1)
  assert.equal(h.pagedVersions.value[0].commonRegulationVersionId, 102)
})

test('list failure is visible and refresh can recover', async () => {
  let failed = true
  const h = harness({
    listCommonRegulationReleasedVersions: async () => {
      if (failed) throw Error('无查看权限')
      return records
    }
  })
  await h.loadVersions()
  assert.match(h.loadError.value, /无查看权限/)
  assert.equal(h.loading.value, false)
  failed = false
  await h.loadVersions()
  assert.equal(h.loadError.value, '')
  assert.equal(h.versions.value.length, 3)
})

test('detail loads the selected version and shows errors without previous content', async () => {
  const calls = []
  const h = harness({
    getCommonRegulationPublishedVersion: async (id) => {
      calls.push(id)
      if (id === 102) throw Error('版本不存在')
      return { publishedVersionId: id, processes: [] }
    }
  })
  await h.openDetail(records[1])
  assert.deepEqual(calls, [100])
  assert.equal(h.detail.value.publishedVersionId, 100)
  await h.openDetail(records[2])
  assert.equal(h.detail.value, undefined)
  assert.match(h.detailError.value, /版本不存在/)
  assert.equal(h.detailLoading.value, false)
})

test('closing detail discards an in-flight response', async () => {
  let resolve
  const h = harness({
    getCommonRegulationPublishedVersion: () =>
      new Promise((done) => {
        resolve = done
      })
  })
  const pending = h.openDetail(records[0])
  h.closeDetail()
  resolve({ publishedVersionId: 101 })
  await pending
  assert.equal(h.detail.value, undefined)
  assert.equal(h.detailLoading.value, false)
})

test('published library is the default page and parser opens the standalone regulation page', () => {
  const page = fs.readFileSync(
    path.resolve(__dirname, '../../src/views/mes/pro/processpool/QaRegulationPage.vue'),
    'utf8'
  )
  const parser = fs.readFileSync(
    path.resolve(__dirname, '../../src/views/form-center/simple-common-parser/index.vue'),
    'utf8'
  )
  assert.match(page, /ref<CommonRegulationTabName>\('published'\)/)
  assert.match(page, /<el-tab-pane label="已发布规程" name="published"/)
  assert.match(parser, /查看已发布规程/)
  const click = parser.match(/@click="\s*(router\.push\([\s\S]*?\))\s*"/)
  assert.ok(click, 'published-regulation button must navigate through the router')
  const navigate = new Function('router', 'publishedResult', click[1])
  const destinations = []
  const router = { push: destination => destinations.push(destination) }
  navigate(router, undefined)
  navigate(router, { regulationId: 79 })
  assert.deepEqual(destinations, [
    { path: '/mes/pro/process-pool/common-regulation', query: {} },
    { path: '/mes/pro/process-pool/common-regulation', query: { regulationId: 79 } }
  ])
})
