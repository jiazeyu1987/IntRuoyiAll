const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { computed } = require('vue')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'), 'utf8')
const slice = (start, end) => source.slice(source.indexOf(start), source.indexOf(end))
const actual = [
  slice('const canQueryAssociatedFiles =', 'const importVisible ='),
  slice('const getAssociatedFiles =', 'const fetchProjectCodeAssociatedFiles ='),
  slice('const loadAssociatedFilesForDetail =', 'const loadAssignmentUsers ='),
  'exports.api = { getAssociatedFiles, loadAssociatedFilesForDetail, canQueryAssociatedFiles }'
].join('\n')
const compile = input => ts.transpileModule(input, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
function instance({ project = true, files = true, taxonomyFailure, fileFailure, afterTaxonomy } = {}) {
  const calls = [], taxonomy = [{ id: 301, name: '技术文档', code: 'TECH' }]
  const context = {
    exports: {}, Error, computed,
    checkPermi: permissions => permissions.every(p => ({ 'dcc:project-code:query': project, 'dcc:controlled-file:query': files })[p]),
    detailRequestSequence: 1,
    selectedProjectCode: { value: { id: '101' } },
    associatedFilesLoading: { value: false }, associatedFilesError: { value: '' },
    fileTypeTaxonomies: { value: [] }, associatedNavigationFiles: { value: [{ id: 'stale' }] }, associatedFilesTotal: { value: 9 },
    DCC_PROJECT_CODE_ASSOCIATED_NAVIGATION_PAGE_SIZE: 1,
    resetAssociatedFilesState: () => {
      calls.push('reset'); context.associatedNavigationFiles.value = []; context.associatedFilesTotal.value = 0
    },
    getProjectCodeFileTypeTaxonomies: async id => {
      calls.push(['taxonomy', id]); if (taxonomyFailure) throw taxonomyFailure
      afterTaxonomy?.(context); return taxonomy
    },
    getProjectCodeControlledFilesPage: async (id, params) => {
      calls.push(['files', id, params.pageNo]); if (fileFailure) throw fileFailure
      return { list: [{ id: String(params.pageNo), fileTypeTaxonomyId: 301 }], total: 2 }
    },
    resetAssociatedFilePage: () => calls.push('resetPage'),
    applyAssociatedRouteFocus: () => false, ensureAssociatedSelection: () => calls.push('select')
  }
  vm.runInNewContext(compile(actual), context)
  return { ...context.exports.api, context, calls, taxonomy }
}

for (const permission of ['project', 'files']) {
  test(`Missing ${permission} query permission clears previous documents without calling protected sources`, async () => {
    const x = instance({ [permission]: false })
    assert.equal(x.canQueryAssociatedFiles.value, false)
    await x.loadAssociatedFilesForDetail('101', 1)
    assert.deepEqual(x.calls, ['reset'])
    assert.equal(x.context.associatedNavigationFiles.value.length, 0)
    assert.equal(x.context.associatedFilesLoading.value, false)
  })
}
test('Reader without category management loads the real project-scoped taxonomy and every file page', async () => {
  const x = instance()
  await x.loadAssociatedFilesForDetail('101', 1)
  assert.equal(x.canQueryAssociatedFiles.value, true)
  assert.deepEqual(x.calls, [['taxonomy', '101'], ['files', '101', 1], ['files', '101', 2], 'resetPage', 'select'])
  assert.equal(x.context.fileTypeTaxonomies.value, x.taxonomy)
  assert.equal(x.context.associatedNavigationFiles.value.length, 2)
  assert.equal(x.context.associatedFilesTotal.value, 2)
  assert.equal(x.context.associatedFilesError.value, '')
})
for (const failed of ['taxonomyFailure', 'fileFailure']) {
  test(`${failed} appears as a detail error instead of an empty successful section`, async () => {
    const failure = Error('正式数据读取失败'), x = instance({ [failed]: failure })
    await assert.rejects(x.getAssociatedFiles('101', 1), e => e === failure)
    await x.loadAssociatedFilesForDetail('101', 1)
    assert.equal(x.context.associatedFilesError.value, '关联文档加载失败：正式数据读取失败')
    assert.equal(x.context.associatedFilesLoading.value, false)
    assert.equal(x.context.associatedFilesTotal.value, 9)
  })
}
test('A switched detail cannot receive the previous project taxonomy, documents, or error', async () => {
  const x = instance({ afterTaxonomy: context => { context.detailRequestSequence = 2 } })
  await x.loadAssociatedFilesForDetail('101', 1)
  assert.equal(x.context.fileTypeTaxonomies.value.length, 0)
  assert.equal(x.context.associatedNavigationFiles.value[0].id, 'stale')
  assert.equal(x.context.associatedFilesError.value, '')
  const failure = instance({ afterTaxonomy: context => { context.detailRequestSequence = 2 }, fileFailure: Error('old detail failed') })
  await failure.loadAssociatedFilesForDetail('101', 1)
  assert.equal(failure.context.associatedFilesError.value, '')
})
test('API wrapper requests the exact project read endpoint and propagates a real denial', async () => {
  const calls = [], api = {}, denial = Error('无项目权限')
  vm.runInNewContext(compile(fs.readFileSync(path.join(root, 'src/api/dcc/controlledFile/projectCodes.ts'), 'utf8')), {
    exports: api, Error, require: id => {
      assert.equal(id, '@/config/axios')
      return { default: { get: async args => { calls.push(args); throw denial } } }
    }
  })
  await assert.rejects(api.getProjectCodeFileTypeTaxonomies('9223372036854775700'), e => e === denial)
  assert.equal(calls[0].url, '/dcc/project-codes/9223372036854775700/file-type-taxonomies')
})
