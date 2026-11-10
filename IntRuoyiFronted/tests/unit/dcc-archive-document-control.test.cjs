const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const root = 'src/views/dcc/controlled-file/'
const source = fs.readFileSync(root + 'browser/index.vue', 'utf8')
const ast = ts.createSourceFile('browser.ts', parse(source).descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const access = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.text === 'canAccessArchive'))

test('archive entry requires both document-control role and archive permission, including direct links', () => {
  for (const roles of [[], ['super_admin'], ['doc_control']]) {
    for (const granted of [false, true]) {
      const context = { userStore: { getRoles: roles }, computed: fn => fn(), checkPermi: () => granted }
      vm.runInNewContext(ts.transpileModule(access.getText(ast) + '\nexports = canAccessArchive', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
      assert.equal(context.exports, roles.includes('doc_control') && granted)
    }
  }
  assert.match(source, /<ArchiveBrowserPanel v-if="browserMode === 'archive' && canAccessArchive"/)
})

test('archive panel and obsolete-reference indicator compile without the retired application workflow', () => {
  for (const file of ['browser/ArchiveBrowserPanel.vue', 'shared/ArchiveDownloadEntry.vue']) {
    const source = fs.readFileSync(root + file, 'utf8')
    const { descriptor, errors } = parse(source)
    assert.deepEqual(errors, [])
    compileScript(descriptor, { id: file })
    assert.deepEqual(compileTemplate({ id: file, filename: file, source: descriptor.template.content }).errors, [])
    assert.doesNotMatch(source, /archive-download-requests|archiveFileId|提交申请|我的下载申请|下载审批|申请下载/)
  }
})
