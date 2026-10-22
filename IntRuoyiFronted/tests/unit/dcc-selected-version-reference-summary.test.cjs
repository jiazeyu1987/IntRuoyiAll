const { test } = require('node:test'),
  assert = require('node:assert/strict'),
  fs = require('node:fs'),
  path = require('node:path'),
  ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const file = {
  id: '9007199254740993',
  masterId: '100',
  dccProjectCodeId: '20',
  projectFolderId: '21',
  versionNo: 'A/1'
}
function load(file) {
  const exports = {},
    source = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail', file), 'utf8')
  new Function(
    'exports',
    'require',
    ts.transpileModule(source, {
      compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
    }).outputText
  )(exports, () => load('relation-contract.ts'))
  return exports
}
const selected = {
  tenantId: '1',
  controlledFileId: file.id,
  masterId: '100',
  projectId: '20',
  projectFolderId: '21',
  versionNo: 'A/1',
  hasCurrentControlledSource: false
}
test('actual browse loader refuses a same-Master summary from another selected version or tenant', async () => {
  const model = load('browse-records.ts')
  for (const patch of [
    { sourceControlledFileId: '12' },
    { tenantId: '2' },
    { masterId: '101' },
    { sourceControlledFileId: null },
    { referenced: false }
  ]) {
    await assert.rejects(
      model.loadBrowseMetadata(file, '1', {
        getPermissions: async () => selected,
        getUsage: async () => ({
          tenantId: '1',
          sourceControlledFileId: file.id,
          masterId: '100',
          referenceProjectCount: 1,
          referenced: true,
          ...patch
        })
      })
    )
  }
  const result = await model.loadBrowseMetadata(file, '1', {
    getPermissions: async () => selected,
    getUsage: async (id) => ({
      tenantId: '1',
      sourceControlledFileId: id,
      masterId: '100',
      referenceProjectCount: 1,
      referenced: true
    })
  })
  assert.equal(result.referenceProjectCount, 1)
  assert.equal(result.selected.controlledFileId, file.id)
})
