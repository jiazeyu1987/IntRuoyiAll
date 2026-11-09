const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const source = fs.readFileSync('src/views/dcc/controlled-file/upload/index.vue', 'utf8')
test('project option displays code only and has accessible ready/blocked state', () => {
  assert.match(source, /:label="project.projectCode"/)
  assert.match(source, /:disabled="!project.uploadSelectable"/)
  assert.match(source, /project.uploadSelectable\s*\?\s*'[^']*green[^']*'\s*:\s*'[^']*red[^']*'/)
  assert.match(source, /:title="project.uploadIssues.join/)
})
test('all candidates use authoritative server readiness and stale searches cannot overwrite', () => {
  const start = source.indexOf('const loadProjectCodeOptions =')
  const end = source.indexOf('const handleProjectCodeOptionsVisibleChange', start)
  const body = ts.transpileModule(source.slice(start, end) + '\nexports.load = loadProjectCodeOptions', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  const pending = []
  const context = { exports: {}, projectOptionsRequestSequence: 0, isLibraryUpload: { value: false }, libraryScope: { value: 'PROJECT' },
    projectCodeOptionsLoading: { value: false }, projectCodeOptionsError: { value: '' }, projectCodeOptions: { value: [] },
    acceptedProjectCodeOption: { value: undefined },
    getProjectUploadOptions: keyword => new Promise(resolve => pending.push({ keyword, resolve })),
    resolveUploadErrorMessage: e => e.message, message: { error: () => {} } }
  vm.runInNewContext(body, context)
  return (async () => {
    const old = context.exports.load('old')
    const latest = context.exports.load('IDI')
    const rows = [{ id: '9007199254740993', projectCode: 'IDI', uploadSelectable: true, uploadIssues: [] }]
    pending[1].resolve(rows); await latest
    pending[0].resolve([]); await old
    assert.equal(context.projectCodeOptions.value[0].projectCode, 'IDI')
    assert.equal(context.projectCodeOptionsLoading.value, false)
  })()
})

test('option API retains all rows and red incomplete projects without inventing valid data', async () => {
  let response = Array.from({ length: 75 }, (_, i) => ({ id: String(i + 1), projectCode: `P${i}`,
    projectName: '正式名称', status: 'ENABLE', uploadSelectable: true, uploadIssues: [] }))
  const context = { exports: {}, require: () => ({ default: { get: async () => response } }) }
  const api = fs.readFileSync('src/api/dcc/controlledFile/projectDiscovery.ts', 'utf8')
  vm.runInNewContext(ts.transpileModule(api, { compilerOptions: { module: ts.ModuleKind.CommonJS,
    target: ts.ScriptTarget.ES2022 } }).outputText, context)
  assert.equal((await context.exports.getProjectUploadOptions()).length, 75)
  response = [{ ...response[0], projectName: null, uploadSelectable: false, uploadIssues: ['缺少项目名称'] }]
  const rows = await context.exports.getProjectUploadOptions()
  assert.equal(rows[0].uploadSelectable, false)
  assert.equal(rows[0].projectName, '')
  response[0].uploadSelectable = true
  response[0].uploadIssues = []
  await assert.rejects(context.exports.getProjectUploadOptions(), /不合法/)
})

test('ordinary QMS upload uses the real library identity and permission response', async () => {
  const start = source.indexOf('const loadProjectCodeOptions =')
  const end = source.indexOf('const handleLibraryScopeChange', start)
  const context = { exports: {}, projectOptionsRequestSequence: 0, isLibraryUpload: { value: true }, libraryScope: { value: 'QMS' },
    projectCodeOptionsLoading: { value: false }, projectCodeOptionsError: { value: '' }, projectCodeOptions: { value: [] },
    getControlledImportLibrary: async scope => { assert.equal(scope, 'QMS'); return { id: '9007199254740993', projectName: 'QMS文件', projectCode: 'QMS', status: 'ENABLE' } },
    getProjectUploadOptions: () => { throw Error('library must not call project readiness/product lookup') },
    resolveUploadErrorMessage: e => e.message, message: { error: () => {} } }
  vm.runInNewContext(ts.transpileModule(source.slice(start, end) + '\nexports.load = loadProjectCodeOptions', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  await context.exports.load()
  assert.equal(context.projectCodeOptions.value[0].id, '9007199254740993')
  assert.equal(context.projectCodeOptions.value[0].uploadSelectable, true)
})
