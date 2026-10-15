const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const base = path.resolve('src'),
  context = {
    tenantId: '1',
    sourceControlledFileId: '9223372036854775701',
    masterId: '9007199254740993'
  }
const row = (id, folder, selected, version) => ({
  referenceId: id,
  projectId: '20',
  projectName: '目的项目',
  folderId: folder,
  folderName: `目录${folder}`,
  masterId: context.masterId,
  selectedControlledFileId: selected,
  fileNumber: 'SOP-1',
  fileName: '固定原文.pdf',
  versionNo: version,
  status: 'ACTIVE',
  controlled: true,
  pendingEffect: false,
  executable: true,
  canPreview: false
})
const response = () => ({
  ...context,
  referenceProjectCount: 2,
  visibleReferenceProjectCount: 1,
  total: 2,
  detailsRestricted: true,
  list: [
    row('1', '21', context.sourceControlledFileId, 'A/1'),
    row('2', '22', context.sourceControlledFileId, 'A/1')
  ]
})
function modules(transport = async () => response()) {
  const cache = new Map(),
    calls = []
  const load = (file) => {
    if (cache.has(file)) return cache.get(file)
    const exports = {}
    cache.set(file, exports)
    const resolve = (name) => {
      if (name === '@/config/axios')
        return {
          default: {
            get: async (options) => {
              calls.push(options)
              return transport(options)
            }
          }
        }
      if (name.startsWith('@/')) return load(name.slice(2) + '.ts')
      if (name.startsWith('.'))
        return load(path.posix.normalize(path.posix.join(path.posix.dirname(file), name)) + '.ts')
      throw Error(name)
    }
    new Function(
      'exports',
      'require',
      ts.transpileModule(fs.readFileSync(path.join(base, file), 'utf8'), {
        compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
      }).outputText
    )(exports, resolve)
    return exports
  }
  return {
    model: load('views/dcc/controlled-file/relations/reference-usage.ts'),
    api: load('api/dcc/controlledFile/relations.ts'),
    calls
  }
}
test('actual usage API preserves Long source identity and server project versus folder counts', async () => {
  const { api, calls } = modules()
  const page = await api.getProjectReferenceUsagePage(context, 1, 20)
  assert.equal(calls.length, 1)
  assert.deepEqual(calls[0].params, {
    selectedFileId: context.sourceControlledFileId,
    pageNo: 1,
    pageSize: 20
  })
  assert.equal(page.referenceProjectCount, 2)
  assert.equal(page.visibleReferenceProjectCount, 1)
  assert.equal(page.total, 2)
  assert.equal(page.detailsRestricted, true)
  assert.equal(page.list.length, 2)
  assert.deepEqual(
    page.list.map((row) => row.selectedControlledFileId),
    [context.sourceControlledFileId, context.sourceControlledFileId]
  )
  assert.deepEqual(
    page.list.map((row) => row.versionNo),
    ['A/1', 'A/1']
  )
})
test('exact page two and a retained obsolete selected version remain real fixed references', () => {
  const { model } = modules(),
    value = response()
  value.list = [{ ...value.list[1], status: 'OBSOLETE', controlled: false, executable: false }]
  const page = model.validateReferenceUsagePage(value, context, 2, 1)
  assert.equal(page.list[0].versionNo, 'A/1')
  assert.equal(page.list[0].selectedControlledFileId, context.sourceControlledFileId)
  assert.equal(page.list[0].status, 'OBSOLETE')
  assert.equal(page.total, 2)
})
test('foreign tenant, source and Master or duplicated destination relations reject rather than filtering', () => {
  const { model } = modules()
  for (const change of [
    (page) => {
      page.tenantId = '2'
    },
    (page) => {
      page.sourceControlledFileId = '8'
    },
    (page) => {
      page.masterId = '9'
    },
    (page) => {
      page.list[0].masterId = '9'
    },
    (page) => {
      page.list[1].referenceId = '1'
    },
    (page) => {
      page.list[1].folderId = '21'
    }
  ]) {
    const page = response()
    change(page)
    assert.throws(() => model.validateReferenceUsagePage(page, context, 1, 20), /不一致/)
  }
})
test('invalid authorized totals, incomplete pages or fabricated permission flags do not become an empty success', () => {
  const { model } = modules()
  for (const change of [
    (page) => {
      page.total = 10
    },
    (page) => {
      page.visibleReferenceProjectCount = 3
    },
    (page) => {
      page.detailsRestricted = false
    },
    (page) => {
      page.list = []
    },
    (page) => {
      page.list[0].canPreview = 1
    },
    (page) => {
      page.list[0].executable = true
      page.list[0].status = 'CONTROLLED_PENDING_EFFECTIVE'
      page.list[0].pendingEffect = true
    }
  ]) {
    const page = response()
    change(page)
    assert.throws(() => model.validateReferenceUsagePage(page, context, 1, 20), /不一致|缺失/)
  }
})
test('unsafe Long or invalid requested page rejects before transport', async () => {
  const { api, calls } = modules()
  await assert.rejects(
    api.getProjectReferenceUsagePage(
      { ...context, sourceControlledFileId: 9007199254740992 },
      1,
      20
    ),
    /精度/
  )
  await assert.rejects(api.getProjectReferenceUsagePage(context, 0, 20), /分页/)
  await assert.rejects(api.getProjectReferenceUsagePage(context, 1, 201), /分页/)
  assert.equal(calls.length, 0)
})
test('transport failure stays a failed read with no fabricated usage record', async () => {
  const failure = new Error('正式引用明细权限拒绝'),
    { api } = modules(async () => {
      throw failure
    })
  await assert.rejects(
    api.getProjectReferenceUsagePage(context, 1, 20),
    (error) => error === failure
  )
})

test('same Master does not permit a different fixed selected version inside the requested usage page', () => {
  const { model } = modules(),
    value = response()
  value.list[1].selectedControlledFileId = '9007199254740997'
  value.list[1].versionNo = 'A/2'
  assert.throws(() => model.validateReferenceUsagePage(value, context, 1, 20), /固定版本|不一致/)
})

test('summary API validates exact source/tenant/Master and never accepts a master-only count', async () => {
  const valid = { ...context, referenceProjectCount: 2, referenced: true }
  const { api, calls } = modules(async () => valid)
  assert.deepEqual(await api.getProjectReferenceUsage(context.sourceControlledFileId), valid)
  assert.deepEqual(calls[0].params, { selectedFileId: context.sourceControlledFileId })
  for (const bad of [
    { masterId: context.masterId, referenceProjectCount: 2, referenced: true },
    { ...valid, sourceControlledFileId: '12' },
    { ...valid, tenantId: null },
    { ...valid, referenced: false },
    { ...valid, referenceProjectCount: -1 }
  ]) {
    await assert.rejects(
      modules(async () => bad).api.getProjectReferenceUsage(context.sourceControlledFileId)
    )
  }
})
