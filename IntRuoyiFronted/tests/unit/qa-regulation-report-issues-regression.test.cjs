const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/processpool/QaRegulationPage.vue'), 'utf8')

test('project selectors bind exact normalized IDs to the string-valued selection model', () => {
  assert.equal((source.match(/:value="resolveDccProjectId\(project\.id\)"/g) || []).length, 2)
})

test('QA Word import rejects duplicate submits and explains the equipment-save guard', () => {
  const start = source.indexOf('const submitQaWordImport = async () => {')
  const end = source.indexOf('\nconst ', start + 10)
  const handler = source.slice(start, end)
  assert.ok(handler.indexOf('if (qaWordImportSubmitting.value) return') < handler.indexOf('qaWordImportSubmitting.value = true'))
  assert.match(handler, /if \(qaEquipmentAutoSaving\.value\)\s*\{\s*ElMessage\.warning\(/)
})

test('QA publish confirmation shows regulation identity, source/target versions and common links', () => {
  const start = source.indexOf('const runQaPublishPrecheck = async () => {')
  const end = source.indexOf('\nconst ', start + 10)
  const handler = source.slice(start, end)
  assert.match(handler, /payload\.regulationCode/)
  assert.match(handler, /源版本：[\s\S]*loadedVersionNo/)
  assert.match(handler, /目标版本：[\s\S]*payload\.versionNo/)
  assert.match(handler, /getCommonRegulationPublishedVersion\(versionId\)/)
  assert.match(handler, /关联通用规程版本详情与指定版本不一致/)
  assert.match(handler, /resolveRequiredText\(snapshot\.versionNo, '关联通用规程版本'\)/)
})

test('browser reload warns only for unsaved equipment and unload listener is removed on unmount', () => {
  const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const ast = ts.createSourceFile('page.ts', script, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(statement => ts.isVariableStatement(statement) &&
    statement.declarationList.declarations.some(item => item.name.getText(ast) === 'warnBeforeQaEquipmentUnload'))
  const ctx = { qaEquipmentDirtyRows: new Set() }
  vm.runInNewContext(ts.transpileModule(declaration.getText(ast), {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText + '\nglobalThis.warn = warnBeforeQaEquipmentUnload', ctx)
  let prevented = 0
  const event = { preventDefault: () => prevented++, returnValue: undefined }
  ctx.warn(event)
  assert.equal(prevented, 0)
  ctx.qaEquipmentDirtyRows.add({})
  ctx.warn(event)
  assert.equal(prevented, 1)
  assert.equal(event.returnValue, '')
  assert.match(source, /onBeforeUnmount\(\(\) => window\.removeEventListener\('beforeunload', warnBeforeQaEquipmentUnload\)\)/)
})
